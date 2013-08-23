/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.heliosapm.shorthand.accumulator.AccumulatorThreadStats;
import com.heliosapm.shorthand.accumulator.MemSpaceAccessor;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator.HeaderOffsets;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.datamapper.DataMapperBuilder;
import com.heliosapm.shorthand.datamapper.IDataMapper;
import com.heliosapm.shorthand.util.ConfigurationHelper;
import com.heliosapm.shorthand.util.OrderedShutdownService;
import com.heliosapm.shorthand.util.StringHelper;
import com.heliosapm.shorthand.util.ThreadRenamer;
import com.heliosapm.shorthand.util.jmx.JMXHelper;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;
import com.heliosapm.shorthand.util.unsafe.collections.ConcurrentLongSlidingWindow;
import com.higherfrequencytrading.chronicle.Chronicle;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;
import com.higherfrequencytrading.chronicle.impl.IntIndexedChronicle;

/**
 * <p>Title: ChronicleStore</p>
 * <p>Description: A store implemented using chronicle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.store.ChronicleStore</code></p>
 * @param <T> The collector set type
 */

public class ChronicleStore<T extends Enum<T> & ICollector<T>> extends AbstractStore<T> implements ChronicleStoreMBean {
	/** The default directory */
	public static final String DEFAULT_DIRECTORY = String.format("%s%sshorthand", System.getProperty("java.io.tmpdir"), File.separator);
	/** System property name to override the default directory */
	public static final String CHRONICLE_DIR_PROP = "shorthand.store.chronicle.dir";
	
	/** The singleton instance */
	private static volatile ChronicleStore<?> instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Acquires the singleton ChronicleStore instance
	 * @return the singleton ChronicleStore instance
	 */
	public static ChronicleStore<?> getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ChronicleStore();
				}
			}
		}
		return instance;
	}

	
	/** The chronicle name for the name index */
	public static final String NAME_INDEX = "metricNameIndex";
	/** The chronicle name for the enum index */
	public static final String ENUM_INDEX = "enumIndex";
	/** The chronicle name for the tier 1 data */
	public static final String TIER_1_DATA = "tier1Data";
	
	/** The known portion length of a name index entry */
	public static final int NAME_ENTRY_SIZE = 
			1 + 				// (byte)) The lock byte
			1 + 				// (byte)) The deleted indicator byte
			1 + 				// (byte) metric name stop byte 
			4 + 				// (int) collector enum index
			4 + 				// (int) the enabled bitmask
			8 + 				// (long) the created timestamp
			8 + 				// (long) the period start time
			8 +	 				// (long) the period end time
			4;					// (int) the number of tier addresses
	
	
	/** The size of the known part of a tier 1 entry */
	public static final int TIER_1_SIZE = 
			1+ 					// the lock
			1+ 					// the delete indicator 
			8+ 					// the name index
			4+ 					// the enum ordinal index 
			4; 					// the sub metric count
	
	
	/** The offset in a name index to the metric name */
	public static final int NAME_OFFSET=10;
	
	/** The available processors */
	public static final int CORES = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
	/** The ThreadMXBean */
	protected static final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
	
	/** The store directory */
	protected final File dataDir;
	/** The store name index chronicle */
	protected final IndexedChronicle nameIndex;
	/** The store name index chronicle excerpt */
	protected final Excerpt nameIndexEx;
	/** The store enum index chronicle */
	protected final IntIndexedChronicle enumIndex;
	/** The store enum index chronicle excerpt */
	protected final Excerpt enumIndexEx;
	/** The tier 1 data chronicle */
	protected final IndexedChronicle tier1Data;
	
	/** The address of the global lock for this instance */
	protected final long globalLockAddress;

	
	/** A sliding window of first phase flush elapsed times in ns. */
	protected final ConcurrentLongSlidingWindow firstPhaseFlushTimes = new ConcurrentLongSlidingWindow(100); 
	/** A sliding window of second phase flush elapsed times in ns. */
	protected final ConcurrentLongSlidingWindow secondPhaseFlushTimes = new ConcurrentLongSlidingWindow(100); 
	/** A map of addresses pending de-allocation */
	private final NonBlockingHashMapLong<Boolean> pendingDeallocates = new NonBlockingHashMapLong<Boolean>(CORES, false);
	/** A map of new allocation addresses keyed by the prior deallocated address */
	private final NonBlockingHashMapLong<Long> allocationTransfers = new NonBlockingHashMapLong<Long>(1024, false);
	
	/** A decode of enum class names to a set of all enum entries */
	protected final TObjectIntHashMap<Class<T>> ENUM_CACHE = new TObjectIntHashMap<Class<T>>(32, 0.2f, -1); 
	
	/** A cummulative count of the number of releases */
	protected final AtomicLong releaseCount = new AtomicLong(0L);
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#clear()
	 */
	@Override
	public void clear() {
		SNAPSHOT_INDEX.clear();
		nameIndex.clear();
		tier1Data.clear();
		writeZeroRec(nameIndex);
		writeZeroRec(tier1Data);
	}
	

	
	public static void main(String[] args) {
		log("Dumping Shorthand DB");
		ChronicleStore store = new ChronicleStore();
		long count = 0;
		long start = System.currentTimeMillis();
//		store.dump();
		for(Object o: store.SNAPSHOT_INDEX.keySet()) {
			String name = o.toString();
			IMetric metric = store.getMetric(name);
			if(metric==null) continue;
			count++;
			log(metric.toString());
		}
		long elapsed = System.currentTimeMillis()-start;
		log("Dump of [%s] metrics complete in [%s] ms.", count, elapsed);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetric(java.lang.String)
	 */
	@Override
	public IMetric<T> getMetric(String name) {
		log("Fetching metric name from Index [%s]  Size: [%s]", System.identityHashCode(SNAPSHOT_INDEX), SNAPSHOT_INDEX.size());
		Long address = SNAPSHOT_INDEX.get(name);
		if(address==null) return null;
		long index = -1;
		if(address>0) {
			index = MetricSnapshotAccumulator.HeaderOffsets.NameIndex.get(address);
		} else {
			index = Math.abs(address);
		}
		Excerpt nex = nameIndex.createExcerpt();
		Excerpt dex = this.tier1Data.createExcerpt();
		nex.index(index);
		nex.skipBytes(2);
		Class<T> type = (Class<T>) EnumCollectors.getInstance().type(nex.readInt());
		T[] collectors = type.getEnumConstants();
		int bitMask = nex.readInt();
		nex.readByteString(); nex.readLong();
		long start = nex.readLong(), end = nex.readLong();
		log("CHRONICLE READ PERIOD WITH: [%s]  to  [%s]", new Date(start), new Date(end));
		int dpIndexSize = nex.readInt();
		
		Map<T, IMetricDataPoint<T>> dataPoints = new LinkedHashMap<T, IMetricDataPoint<T>>(dpIndexSize);
		//SimpleMetric(String name, long startTime, long endTime, Map<T, IMetricDataPoint<T>> dataPoints) ;
		SimpleMetric<T> simpleMetric = new SimpleMetric<T>(name, type, start, end, dataPoints); 
		for(int i = 0; i < dpIndexSize; i++) {
			long dIndex = nex.readLong();
			if(dIndex < 1L) continue;
			dataPoints.put(collectors[i], getTier1DataEntry(dex, collectors[i] , dIndex));
			dex.index(dIndex);
			dex.skipBytes(10);
			int ord = dex.readInt();
			int subCount = dex.readInt();
			String[] subNames = type.getEnumConstants()[ord].getSubMetricNames();			
			long[] subValues = new long[subCount];
			for(int x = 0; x < subCount; x++) {
				subValues[x] = dex.readLong();
			}
		}
		return simpleMetric;
	}
	
	/**
	 * Returns a map of the data point values keyed by the submetric name
	 * @param ex The excerpt to read with. Optional. If null, will create a new tier1 exceprpt and close it on completion.
	 * @param collector The collector for which datapoints are being retrieved
	 * @param index The index of the tier1 entry
	 * @return a map of the data point values keyed by the submetric name
	 */
	public IMetricDataPoint<T> getTier1DataEntry(Excerpt ex, T collector, long index) {
		final boolean hasEx = ex!=null;
		try {
			if(!hasEx) ex = this.tier1Data.createExcerpt();
			ex.index(index);
			ex.position(TIER_1_SIZE);
			String[] subNames = collector.getSubMetricNames();
			TObjectLongHashMap<String> map = new TObjectLongHashMap<String>(subNames.length, 0.1f);		
			for(int i = 0; i < subNames.length; i++) {
				map.put(subNames[i], ex.readLong());
			}
			return new SimpleMetricDataPoint<T>(collector, map);
		} finally {
			if(!hasEx) ex.close(); 
		}
	}
	
	public void dump() {
		Excerpt dx = tier1Data.createExcerpt();
		try {
			nameIndexEx.index(1);
			StringBuilder b = new StringBuilder();
			
			while(nameIndexEx.nextIndex()) {
				b.append(nameIndexEx.index()).append(",");
				b.append(nameIndexEx.read()).append(",");
				b.append(nameIndexEx.read()).append(",");
				Class<T> collectorType = (Class<T>) EnumCollectors.getInstance().type(nameIndexEx.readInt());
				ICollector[] collectors = collectorType.getEnumConstants();
				b.append(collectorType.getSimpleName()).append(",");
				b.append(nameIndexEx.readInt()).append(",");
				b.append(nameIndexEx.readByteString()).append(",");
				b.append(new Date(nameIndexEx.readLong())).append(",");
				b.append(new Date(nameIndexEx.readLong())).append(",");
				b.append(new Date(nameIndexEx.readLong())).append(",");
				int dataIndexes = nameIndexEx.readInt();
				b.append(dataIndexes).append(",");
				for(int i = 0; i < dataIndexes; i++) {
					long dataIndex = nameIndexEx.readLong();
					if(dataIndex<1) continue;
					dx.index(dataIndex);
					b.append(dx.read()).append(",");
					b.append(dx.read()).append(",");
					dx.readLong();
					int enumIndex = dx.readInt();
					ICollector collector = collectors[enumIndex];
					b.append(collector.getShortName()).append(",");
					String[] subNames = collector.getSubMetricNames();
					int subCount = dx.readInt();
					for(int x = 0; x < subCount; x++) {
						b.append(subNames[x]).append(",");
						b.append(dx.readLong()).append(",");
					}
//					ex.writeByte(0);				// the lock
//					ex.writeByte(0);				// the delete indicator
//					ex.writeLong(nameIndex);		// the name index
//					ex.writeInt(enumIndex);			// the enum index  (i.e. the ordinal)
//					ex.writeInt(subCount);			// the sub count
//					for(int i = 0; i < subCount; i++) {
//						ex.writeLong(-1L);			// the sub value slot
//					}
					
				}
				
				log(b.toString());
				b.delete(0, b.length()-1);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			try { dx.close(); } catch (Exception ex) {}
		}
	}
	

	/**
	 * Creates a new ChronicleStore persisting to the default directory
	 */
	protected ChronicleStore() {		
		this(ConfigurationHelper.getSystemThenEnvProperty(CHRONICLE_DIR_PROP, DEFAULT_DIRECTORY));
	}
	
	protected void writeZeroRec(Chronicle chr) {
		if(chr.size()==0) {
			Excerpt ex = chr.createExcerpt();			
			String header = String.format("Shorthand Metric Repository [%s] created [%s]\n", chr.name(), new Date());
			int size = header.getBytes().length + 2;
			ex.startExcerpt(size);
			ex.writeUTF(header);
			ex.finish();
			ex.close();
		}
	}

	/**
	 * Creates a new ChronicleStore persisting to the specified directory
	 * @param dataDirectory the name of the directory to persist in
	 */
	protected ChronicleStore(String dataDirectory) {
		dataDir = new File(dataDirectory);
		if(!dataDir.exists()) {
			dataDir.mkdirs();
		}
		if(!dataDir.isDirectory()) {
			throw new IllegalArgumentException("The directory [" + dataDirectory + "] is not valid");
		}
		try {
			enumIndex = getIntChronicle(ENUM_INDEX);
//			enumIndex.multiThreaded(true);
			enumIndex.useUnsafe(true);				
			writeZeroRec(enumIndex);
			log(printChronicleDetails(enumIndex));
			enumIndexEx = enumIndex.createExcerpt();
			cacheEnums();
			nameIndex = getChronicle(NAME_INDEX);
			nameIndex.multiThreaded(true);
			
			nameIndex.useUnsafe(true);			
			writeZeroRec(nameIndex);
			log(printChronicleDetails(nameIndex));			
			nameIndexEx = nameIndex.createExcerpt();
			tier1Data = getChronicle(TIER_1_DATA);
			tier1Data.multiThreaded(true);
			tier1Data.useUnsafe(true);
			writeZeroRec(tier1Data);
			log(printChronicleDetails(tier1Data));
			loadSnapshotNameIndex();
			
			JMXHelper.registerMBean(this, JMXHelper.objectName("com.heliosapm.shorthand.store:service=Store,type=Chronicle"));

			
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initialize name index chronicle", ex);
		}
		OrderedShutdownService.getInstance().add(
			new Thread("ChronicleShutdownHook"){
				@Override
				public void run() {
					log("Stopping Chronicle...");
					try { enumIndexEx.close(); } catch (Exception ex) {}
					try { enumIndex.close(); } catch (Exception ex) {}
					try { nameIndexEx.close(); } catch (Exception ex) {}
					try { nameIndex.close(); } catch (Exception ex) {}
				}
			});
		globalLockAddress = UnsafeAdapter.allocateMemory(UnsafeAdapter.LONG_SIZE);
		UnsafeAdapter.putLong(globalLockAddress, UNLOCKED);
		
	}
	
	protected String printChronicleDetails(IndexedChronicle chronicle) {
		return String.format("[%s] Chronicle Created.\n\tUsing Unsafe:%s\n\tSynchronous:%s\n\tMultithreaded:%s\n\tSize (bytes):%s\n\tSize (entries):%s",
				chronicle.name(), chronicle.useUnsafe(), chronicle.synchronousMode(), chronicle.multiThreaded(), chronicle.sizeInBytes(), chronicle.size() 
		);
	}

	
	protected void cacheEnums() {
		int index = 1;
		int loaded = 0;
		while(enumIndexEx.index(index)) {
			String className = enumIndexEx.readByteString();
			Class<T> type = (Class<T>) EnumCollectors.getInstance().typeForName(className);
			ENUM_CACHE.put(type, index);
			loaded++; index++;
		}
		log("Loaded [%s] Collector Enums into Cache\n\t", loaded);		
	}
	
	protected int getEnum(T collector) {
		int index = ENUM_CACHE.get(collector.getDeclaringClass());
		if(index==-1) {
			synchronized(ENUM_CACHE) {
				index = ENUM_CACHE.get(collector.getDeclaringClass());
				if(index==-1) {
					String name = collector.getDeclaringClass().getName();
					enumIndexEx.startExcerpt(name.length()+2);
					enumIndexEx.writeBytes(name);
					enumIndexEx.finish();		
					index = (int)enumIndexEx.index();
					ENUM_CACHE.put(collector.getDeclaringClass(), index);
				}
			}
		}
		return index;
	}

	/**
	 * Acquires the named chronicle
	 * @param name The name of the chronicle
	 * @return the named chronicle
	 * @throws IOException
	 */
	protected IndexedChronicle getChronicle(String name) throws IOException {
		return new IndexedChronicle(dataDir.getAbsolutePath() + File.separator + name, 1, ByteOrder.nativeOrder(), true, false);
	}
	
	/**
	 * Acquires the named int chronicle
	 * @param name The name of the chronicle
	 * @return the named chronicle
	 * @throws IOException
	 */
	protected IntIndexedChronicle getIntChronicle(String name) throws IOException {
		return new IntIndexedChronicle(dataDir.getAbsolutePath() + File.separator + name, 1, ByteOrder.nativeOrder());
	}	
	
	protected void markNameIndexDeleted(long index) {
		final long cindex = nameIndexEx.index();
		final int cpos = nameIndexEx.position();
		try {
			nameIndexEx.position(1);
			nameIndexEx.writeByte(1);
			nameIndexEx.toEnd();
			nameIndexEx.finish();
		} finally {
			nameIndexEx.index(cindex);
			nameIndexEx.position(cpos);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#loadSnapshotNameIndex(java.util.concurrent.ConcurrentHashMap)
	 */
	@Override
	public void loadSnapshotNameIndex() {
		log("Loading NameIndex....");
		long index = 1;
		while(nameIndexEx.index(index)) {
			nameIndexEx.position(0);
			nameIndexEx.readByte();  							// the lock
			byte deleted = nameIndexEx.readByte(); 
			if(deleted==0) {  									// check the deleted flag
				int enumIndex = nameIndexEx.readInt();			// the enum index
				nameIndexEx.position(NAME_OFFSET);				// jump to the metric name
				String name = nameIndexEx.readByteString();		// the metric name
				if(!ENUM_CACHE.containsValue(enumIndex)) {
					loge("Warning: MetricName [%s] had unrecognized enum index [%s]", name, enumIndex);
					markNameIndexDeleted(nameIndexEx.index());
				}
				if(SNAPSHOT_INDEX.put(name, (index * -1L))!=null) {
					log("WARNING:  Duplicate name [" + name + "] at index [" + index + "]");
				} else {
					//log("Loaded [" + name + "]");
				}
			}
			index++;
		}
		if(index>0) {
			log(String.format("Loaded\n\tNameIndexes: [%s]", index));
		} else {
			log("Initialized new name index.");
		}
	}

	@Override
	public void finalize() throws Throwable {		
		try { nameIndex.close(); } catch (Exception ex) {}
		super.finalize();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#newMetricName(java.lang.String, java.lang.Enum, int)
	 */
	@Override
	public long newMetricName(String metricName, T collector, int bitMask) {
		// Check for DUPS here.
		int enumIndex = getEnum(collector);		
		int nameLength = metricName.getBytes().length;		
		int tier1AddressCount = collector.getDeclaringClass().getEnumConstants().length;
		int tier1AddressSize = tier1AddressCount << 3;
		long now = System.currentTimeMillis();
		int estSize = 	NAME_ENTRY_SIZE + 						// the known part of the total length
						nameLength + 							// the number of bytes in the metric name
						tier1AddressSize;						// the number of bytes for the tier addresses for each sub-metric			
		
		try {
			nameIndexEx.startExcerpt(estSize);
			nameIndexEx.writeByte(0);				// the lock
			nameIndexEx.writeByte(0);				// the delete indicator
			nameIndexEx.writeInt(enumIndex);		// the enum index (i.e. which enum it is)
			nameIndexEx.writeInt(bitMask);			// the enabled bitmask
			nameIndexEx.writeBytes(metricName);		// the metric name bytes
			nameIndexEx.writeLong(now);				// the creation timestamp
			nameIndexEx.writeLong(now);				// the period start time.
			nameIndexEx.writeLong(now);				// the period end time.
			nameIndexEx.writeInt(tier1AddressCount);// (int) the number of tier addresses
			final int pos = nameIndexEx.position(); 
			for(int i = 0; i < tier1AddressCount; i++) {
				nameIndexEx.writeLong(-3L);
			}
			nameIndexEx.finish();
			long nindex = nameIndexEx.index();
			nameIndexEx.index(nindex);
			int offset = pos;
			for(ICollector<?> t: collector.collectors()) {
				if(t.isEnabled(bitMask)) {
					//log("Creating New Tier 1 Entry for [%s]: [%s]  Data Points: [%s]", metricName, t.name(), t.getDataStruct().size);
					nameIndexEx.writeLong(offset,
						newTier1Entry(nindex, t.ordinal(), t.getDataStruct().size)
					);
					
				}
				offset += UnsafeAdapter.LONG_SIZE;
			}
			nameIndexEx.finish();
			return nindex;
		} catch (Exception ex) {
			loge("Failed to write new metric name [%s]", ex, metricName);
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#cacheMetricAddress(java.lang.String, long)
	 */
	public void cacheMetricAddress(String metricName, long address) {
		SNAPSHOT_INDEX.put(metricName, address);
	}

	

	
	/** The offset in the name index for the metric name start */
	public static final int CR_TS_INDEX = 	
			1 + 				// (byte)) The lock byte
			1 + 				// (byte)) The deleted indicator byte
			4 + 				// (int) the enum index 
			4;	 				// (int) the enabled bitmask
	
	
	
	@Override
	public void updatePeriod(MemSpaceAccessor<T> memSpaceAccessor, long periodStart, long periodEnd) {
		final long start = System.nanoTime();
		long address = memSpaceAccessor.getNameIndex();
		//if(address<0) address = address*-1;
		nameIndexEx.index(address);
		// skip to the name
		nameIndexEx.position(CR_TS_INDEX);
		// read the name
		nameIndexEx.readByteString();
		// skip the creation date
		nameIndexEx.skip(UnsafeAdapter.LONG_SIZE);
		// write the period start and end
		nameIndexEx.writeLong(periodStart);
		nameIndexEx.writeLong(periodEnd);
		// read the number of tier addresses
		int tierAddressCount = nameIndexEx.readInt();
		int actualCount = EnumCollectors.getInstance().type(memSpaceAccessor.getEnumIndex()).getEnumConstants().length;
		if(tierAddressCount != actualCount) {
			loge("Invalid name index entry for tier address count. Enum says [%s] Count was [%s]", tierAddressCount, actualCount);
		}
		long[] tierAddresses = readLongArray(nameIndexEx, tierAddressCount);
		Excerpt ex = tier1Data.createExcerpt();
		if(!updateSlots(ex, tierAddresses, memSpaceAccessor.getDataPoints())) {
			loge("Slot update failed:\n%s", memSpaceAccessor);
		}
		ex.close();
		nameIndexEx.finish();
		final long elapsed = System.nanoTime()-start;
		
	}
	
	

	
	
	/**
	 * Updates the live tier for a flush
	 * @param ex The excerpt to write with
	 * @param indexes The indexes to write to
	 * @param values The values to write
	 */
	public boolean updateSlots(Excerpt ex, long[] indexes, long[][] values) {
		try {
			int vindex = 0;
			for(int i = 0; i < indexes.length; i++) {
				long index = indexes[i];
				if(index < 1) continue;
				ex.index(index);
				ex.position(TIER_1_SIZE);
				
//				ex.position(TIER_1_SIZE-4);
//				int slots = ex.readInt();
//				if(slots!=values[vindex].length) {
//					loge("Invalid slot update. Slot count is [%s] but values array length is [%s]", slots, values[vindex].length);
//					return;
//				}    /---------------10------------20/
				int t = ex.remaining() + ex.position();
				writeLongArray(ex, values[vindex]);
				if(ex.position()>t) {
					loge("!!  ERROR  !! Overwriting slot size. t [%s]  p [%s]", t, ex.position());
					return false;
				}
//				for(long value: values[vindex]) {
//					ex.writeLong(value);
//				}
				ex.finish();
				vindex++;
			}
			return true;
		} catch (Exception x) {
			x.printStackTrace(System.err);
			return false;
		}
	}
	
	/**
	 * Writes the entire content of a long array to the current offset in the passed excerpt
	 * @param ex the excerpt to write to
	 * @param values the logn array to write
	 */
	protected void writeLongArray(Excerpt ex, long[] values) {
		for(long v: values) {
			ex.writeLong(v);
		}
	}
	
	/**
	 * Reads and returns the specified number of longs from the excerpt at its current offset
	 * @param ex The excerpt to read from
	 * @param size The number of longs to read
	 * @return the read longs
	 */
	protected long[] readLongArray(Excerpt ex, int size) {
		long[] values = new long[size];
		for(int i = 0; i < size; i++) {
			values[i] = ex.readLong();
		}
		return values;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#flush(long, long)
	 */
	@Override
	public void flush(long  priorStartTime, long priorEndTime) {
		log("Flushing Index [%s]  Size: [%s]", System.identityHashCode(SNAPSHOT_INDEX), SNAPSHOT_INDEX.size());
		final long startTime = System.nanoTime();		
		long bufferCount = 0;
		try {
			long address = -1L;
			bufferCount = SNAPSHOT_INDEX.size();
			log("Processing Period Update for [%s] Store Name Index Values", bufferCount);
			Set<String> keys = new HashSet<String>(SNAPSHOT_INDEX.keySet());
			MemSpaceAccessor<T> msa = null;
			for(String key: keys) {				
				address = SNAPSHOT_INDEX.get(key);
				if(address<0) {
					bufferCount--;
					continue;
				}
				lock(address);
				msa = MemSpaceAccessor.get(address);
				msa.preFlush();
				updatePeriod(msa, priorStartTime, priorEndTime);
				// ======================================================================
				// mem4time: to save memory, we can write the negative name index back
				// into the off-cycle index. On the next period switch, the accumulator threads
				// will re-create the mem-space as they encounter the negative index.
				// time4mem: to save time and avoid having to recreate mem-spaces each period
				// we can simply reset the address; the trade-off being that we're carrying
				// double mem-space allocations.
				// ======================================================================
				
				//  Do this for time4mem
				msa.reset();
				unlock(address);
				
				
				// Do this for mem4time - still need to deal with pending spinners
//				priorIndex.put(key, msa.getNameIndex()*-1);
//				UnsafeAdapter.freeMemory(address);
				
			}
			keys.clear();			
			long elapsed = System.nanoTime()-startTime;
			secondPhaseFlushTimes.insert(elapsed);			
					
			log("Flushing Index [%s]  Size: [%s]", System.identityHashCode(SNAPSHOT_INDEX), SNAPSHOT_INDEX.size());
			log(StringHelper.reportSummary("Period Flush Time", elapsed, bufferCount));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			//globalUnlock();
		}		
	}
	
	/**
	 * Captures all the current mem-spaces during a flush, copies and resets the copy and inserts the new reset copy back into the snapshot index.
	 * @return A map of the dirty buffers to be flushed into the store
	 */
	protected NonBlockingHashMap<String, Long> dirtyBuffers() {		
		NonBlockingHashMap<String, Long> dirty = new NonBlockingHashMap<String, Long>(SNAPSHOT_INDEX.size());
		allocationTransfers.clear();
		for(Map.Entry<String, Long> entry: SNAPSHOT_INDEX.entrySet()) {			
			long dirtyAddress = entry.getValue();			
			// ==================================================
			// FIXME
			// Do we need to check for invalidated memspaces ?
			// ==================================================
			//=======================================================
			// Lock the dirty mem-space and put into the dirty map
			// If the address is unassigned (<0) the continue
			//=======================================================
			if(dirtyAddress<1) continue;
			lockNoYield(dirtyAddress);
			dirty.put(entry.getKey(), dirtyAddress);
			
			//=======================================================
			// Copy the dirty mem-space to create a new period mem-space
			// [CHECK: if the metric sub-space is still at the reset value, skip this]
			//=======================================================			
			long memSize = MetricSnapshotAccumulator.HeaderOffsets.MemSize.get(dirtyAddress);
			int enumIndex = (int)MetricSnapshotAccumulator.HeaderOffsets.EnumIndex.get(dirtyAddress);
			long newAddress = UnsafeAdapter.allocateMemory(memSize);
			int bitMask = (int)HeaderOffsets.BitMask.get(dirtyAddress);
			UnsafeAdapter.copyMemory(dirtyAddress, newAddress, memSize);
			//=====================================
			// Get the reference collector for this mem-space
			//=====================================			
			T refCollector = (T) EnumCollectors.getInstance().ref(enumIndex);
			//=====================================
			// Reset/Prepare the new mem-space
			//=====================================			
			refCollector.resetMemSpace(newAddress, bitMask);
			//=====================================
			// Insert new mem-space back into index
			// and that's it for the new space
			//=====================================			
			entry.setValue(newAddress);
			//=====================================
			// There may be a worker thread spinning
			// on the lock of the dirty address right
			// now so we need to mark it as invalid
			// by setting the mem size to -1L
			// then we can unlock.
			//=====================================			
			HeaderOffsets.MemSize.set(dirtyAddress, -1L);
//			log("Marked buffer for [%s] as invalid", entry.getKey());
			pendingDeallocates.put(dirtyAddress, Boolean.TRUE);
			allocationTransfers.put(dirtyAddress, (Long)newAddress);
			unlock(dirtyAddress);
			// Any spinner will now grab this lock
		}
		return dirty;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getTransferAddress(long)
	 */
	@Override
	public Long getTransferAddress(long oldAddress) {
		return allocationTransfers.get(oldAddress);
	}
	
	 
	
	/**
	 * Deallocates the passed address if it can be immediately locked.
	 * Otherwise, whoever holds the lock should do so.
	 * @param address The dirty address to deallocate
	 * @return true if address was deallocated, false otherwise
	 */
	protected boolean deAllocateDirtyMemSpace(long address) {
		try {
			ThreadRenamer.push("Deallocating invalidated mem-space [%s]", address);
			Boolean rem = pendingDeallocates.remove(address);
			if(rem!=null) {
				if(UnsafeAdapter.compareMultiAndSwapLong(null, address, UNLOCKED, Thread.currentThread().getId(), UNLOCKED)) {			
					UnsafeAdapter.freeMemory(address);					
					return true;
				}
				System.err.println(String.format("!! ERROR !! Thread [%s] Attempted to deallocate address [%s] but it was locked by another thread. This is a programmer error. ", Thread.currentThread().toString(), address));
				return false;
			}
			return false;
		} finally {
			ThreadRenamer.pop();
		}
	}
	
	/*
	 * deAllocateDirtyMemSpace and release are both methods intended to de-allocate an invalidated mem-space.
	 * The first is by the flush thread, the second by an accumulator thread that acquired an invalidated mem-space.
	 * It's impossible to know if an address has been deallocated or not, so this is tricky.
	 * 
	 */
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#release(long)
	 */
	@Override
	public boolean release(long address) {
		// this means an accumulator thread acquired a lock on an invalidated mem-space.
		if(deAllocateDirtyMemSpace(address)) {
			releaseCount.incrementAndGet();
			return true;
		}
		return false;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getReleaseCount()
	 */
	@Override
	public long getReleaseCount() {
		return releaseCount.get();
	}
	
	
	
//	protected long[] readLongArray(Excerpt ex, int longCount) {
//		long[] values = new long[longCount];
//		long address = ((UnsafeExcerpt)ex). 
//				ex.     //public long getIndexData(long indexId);  
//		UnsafeAdapter.copyMemory(null, ex.position(), destBase, destOffset, bytes)
//		
//		
//	}
	
	

	/**
	 * Writes a new metric data slot
	 * @param nameIndex The name index of the parent name
	 * @param enumIndex The metric enum collector index
	 * @param subCount The number of sub values
	 * @return the index of the slot
	 */
	public long newTier1Entry(long nameIndex, int enumIndex, int subCount) {
		Excerpt ex = tier1Data.createExcerpt();
		int size = (int) (TIER_1_SIZE + (subCount*UnsafeAdapter.LONG_SIZE));
		ex.startExcerpt(size);
		ex.writeByte(0);				// the lock 1
		ex.writeByte(0);				// the delete indicator 1
		ex.writeLong(nameIndex);		// the name index  8
		ex.writeInt(enumIndex);			// the enum index  (i.e. the ordinal) 4
		ex.writeInt(subCount);			// the sub count 4
		for(int i = 0; i < subCount; i++) {
			ex.writeLong(-1L);			// the sub value slot
		}
		ex.finish();
		long index = ex.index();
		ex.close();
		return index;
		
	}
	

	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getEnumCollectorCount()
	 */
	@Override
	public int getEnumCollectorCount() {
		return (int)enumIndex.size();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getMetricNameCount()
	 */
	@Override
	public long getMetricNameCount() {
		return nameIndex.size();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getMetricDataPointCount()
	 */
	@Override
	public long getMetricDataPointCount() {
		return tier1Data.size();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getMetricNameSize()
	 */
	@Override
	public long getMetricNameSize() {
		return nameIndex.sizeInBytes();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getMetricDataPointSize()
	 */
	@Override
	public long getMetricDataPointSize() {
		return tier1Data.sizeInBytes();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getFirstPhaseLastFlushElapsedNs()
	 */
	@Override
	public long getFirstPhaseLastFlushElapsedNs() {
		return firstPhaseFlushTimes.getLast();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getFirstPhaseLastFlushElapsedMs()
	 */
	@Override
	public long getFirstPhaseLastFlushElapsedMs() {		
		return TimeUnit.MILLISECONDS.convert(firstPhaseFlushTimes.getLast(), TimeUnit.NANOSECONDS);
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getFirstPhaseAverageFlushElapsedNs()
	 */
	@Override
	public long getFirstPhaseAverageFlushElapsedNs() {
		return firstPhaseFlushTimes.avg();		
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getFirstPhaseAverageFlushElapsedMs()
	 */
	@Override
	public long getFirstPhaseAverageFlushElapsedMs() {
		return TimeUnit.MILLISECONDS.convert(firstPhaseFlushTimes.avg(), TimeUnit.NANOSECONDS);		
	}
	
	/**
	 * Locks the passed address. Yield spins while waiting for the lock.
	 * @param address The address of the lock
	 * @return indicates if the locked memspace is valid. If false, the memspace 
	 * has been invalidated and the snapshot index should be re-queried for a new address
	 */
	@Override
	public boolean lock(long address) {
		long id = Thread.currentThread().getId();
		if(UnsafeAdapter.getLong(address)!=id) {
			int loops = 0;
			while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
				Thread.yield();
				loops++;
				if(loops%1000000000==0) log("[%s]->Attempting to lock address [%s] loops [%s] held by [%s]", Thread.currentThread().getName(), address, loops, StringHelper.formatThreadName(UnsafeAdapter.getLong(address)));
			}
		}
//		AccumulatorThreadStats.incrementNameLockSpins(loops);
		return MemSpaceAccessor.get(address).getMemSize()>0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#unlockIfHeld(long)
	 */
	@Override
	public void unlockIfHeld(long address) {
		if(UnsafeAdapter.getLong(address)==Thread.currentThread().getId()) {
			unlock(address);
		}
	}
	
	/**
	 * Acquires the passed address with no yielding
	 * @param address the address to lock
	 */
	protected void lockNoYield(long address) {
		long id = Thread.currentThread().getId();
		if(UnsafeAdapter.getLong(address)==id) return;
		while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
			/* No Op */
		}
	}
	
	
	/**
	 * Unlocks the passed address
	 * @param address The address of the lock
	 */
	@Override
	public void unlock(long address) {		
		if(!UnsafeAdapter.compareAndSwapLong(null, address, Thread.currentThread().getId(), UNLOCKED)) {			
			System.err.println("[" + Thread.currentThread().toString() + "] Yikes! Tried to unlock, but was not locked.Expected " +  Thread.currentThread().getId());
			new Throwable().printStackTrace(System.err);
		}
	}
	
	/**
	 * Acquires the read/write global lock address for this accumulator
	 * This will lock out all other threads while it is held, so it should be used sparingly and released quickly.
	 * Intended for period flushes or data exports.
	 */
	private void globalLockNoYield() {
		long id = Thread.currentThread().getId();
		int loops = 0;
		while(!UnsafeAdapter.compareAndSwapLong(null, globalLockAddress, UNLOCKED, id)) {			
			loops++;
		}
		AccumulatorThreadStats.incrementGlobalLockSpins(loops);		
	}
	
	/**
	 * Acquires the read/write global lock address for this accumulator
	 * This will lock out all other threads while it is held, so it should be used sparingly and released quickly.
	 * Intended for period flushes or data exports.
	 */
	@Override
	public void globalLock() {
		long id = Thread.currentThread().getId();
		int loops = 0;
		while(!UnsafeAdapter.compareAndSwapLong(null, globalLockAddress, UNLOCKED, id)) {			
			Thread.yield();
			loops++;
		}
		AccumulatorThreadStats.incrementGlobalLockSpins(loops);
	}
	
	
	/**
	 * Releases the read/write global lock address for this accumulator
	 */
	@Override
	public void globalUnlock() {
		long id = Thread.currentThread().getId();
		if(!UnsafeAdapter.compareAndSwapLong(null, globalLockAddress, id, UNLOCKED)) {
			System.err.println("[" + Thread.currentThread().toString() + "] Yikes! Tried to unlock GLOBAL , but was not locked.");
			new Throwable().printStackTrace(System.err);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getSecondPhaseLastFlushElapsedNs()
	 */
	@Override
	public long getSecondPhaseLastFlushElapsedNs() {		
		return secondPhaseFlushTimes.getLast();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getSecondPhaseLastFlushElapsedMs()
	 */
	@Override
	public long getSecondPhaseLastFlushElapsedMs() {
		return TimeUnit.MILLISECONDS.convert(secondPhaseFlushTimes.getLast(), TimeUnit.NANOSECONDS);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getSecondPhaseAverageFlushElapsedNs()
	 */
	@Override
	public long getSecondPhaseAverageFlushElapsedNs() {
		return secondPhaseFlushTimes.avg();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getSecondPhaseAverageFlushElapsedMs()
	 */
	@Override
	public long getSecondPhaseAverageFlushElapsedMs() {
		return TimeUnit.MILLISECONDS.convert(secondPhaseFlushTimes.avg(), TimeUnit.NANOSECONDS);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getPendingDeallocateReprobes()
	 */
	@Override
	public long getPendingDeallocateReprobes() {
		return pendingDeallocates.reprobes();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getNameIndexReprobes()
	 */
	@Override
	public long getNameIndexReprobes() {
		return SNAPSHOT_INDEX.reprobes();
	}

}