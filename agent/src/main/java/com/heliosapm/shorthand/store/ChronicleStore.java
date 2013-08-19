/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.ConfigurationHelper;
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
	
	/** A sliding window of flush elapsed times in ns. */
	protected final ConcurrentLongSlidingWindow flushTimes = new ConcurrentLongSlidingWindow(100); 
	
	/** A decode of enum class names to a set of all enum entries */
	protected final TObjectIntHashMap<Class<T>> ENUM_CACHE = new TObjectIntHashMap<Class<T>>(32, 0.2f, -1); 
	
	
	@SuppressWarnings("javadoc")
	public static void log(String fmt, Object...msgs) {
		System.out.println(String.format("[ChronicleStore]" + fmt, msgs));
	}
	@SuppressWarnings("javadoc")
	public static void loge(String fmt, Throwable t, Object...msgs) {
		System.err.println(String.format("[ChronicleStore]" + fmt, msgs));
		if(t!=null) t.printStackTrace(System.err);
	}
	@SuppressWarnings("javadoc")
	public static void loge(String fmt, Object...msgs) {
		loge(fmt, null, msgs);
	}
	
	public static void main(String[] args) {
		log("Dumping Shorthand DB");
		ChronicleStore store = new ChronicleStore();
		long start = System.currentTimeMillis();
		//store.dump();
		for(String name: store.SNAPSHOT_INDEX) {
			
		}
		long elapsed = System.currentTimeMillis()-start;
		log("Dump completed in [%s] ms.", elapsed);		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetric(java.lang.String)
	 */
	@Override
	public IMetric<T> getMetric(String name) {
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
		int dpIndexSize = nex.readInt();
		
		Map<T, IMetricDataPoint<T>> dataPoints = new LinkedHashMap<T, IMetricDataPoint<T>>(dpIndexSize);
		//SimpleMetric(String name, long startTime, long endTime, Map<T, IMetricDataPoint<T>> dataPoints) ;
		SimpleMetric<T> simpleMetric = new SimpleMetric<T>(name, start, end, dataPoints); 
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
	public ChronicleStore() {		
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
	public ChronicleStore(String dataDirectory) {
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
		Runtime.getRuntime().addShutdownHook(new Thread("ChronicleShutdownHook"){
			public void run() {
				log("Stopping Chronicle...");
//				try { enumIndexEx.close(); } catch (Exception ex) {}
//				try { enumIndex.close(); } catch (Exception ex) {}
//				try { nameIndexEx.close(); } catch (Exception ex) {}
//				try { nameIndex.close(); } catch (Exception ex) {}
			}
		});
		
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
				if(SNAPSHOT_INDEX.put(name, (index * -1L)) != null) {
					log("WARNING:  Duplicate name [" + name + "] at index [" + index + "]");
				} else {
					log("Loaded [" + name + "]");
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
		int enumIndex = getEnum(collector);		
		int nameLength = metricName.getBytes().length;		
		int tier1AddressCount = collector.getDeclaringClass().getEnumConstants().length;
		int tier1AddressSize = tier1AddressCount*8;
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
	

	
	/** The offset in the name index for the metric name start */
	public static final int CR_TS_INDEX = 	
			1 + 				// (byte)) The lock byte
			1 + 				// (byte)) The deleted indicator byte
			4 + 				// (int) the enum index 
			4;	 				// (int) the enabled bitmask
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#updatePeriod(long, long, long)
	 */
	@Override
	public long[] updatePeriod(long nameIndex, long periodStart, long periodEnd) {
		nameIndexEx.index(nameIndex);
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
		long[] tierAddresses = new long[tierAddressCount];
		// read the tier addresses
		for(int i = 0; i < tierAddressCount; i++) {
			tierAddresses[i] = nameIndexEx.readLong();
		}
		// done
		nameIndexEx.finish();
		return tierAddresses;		
	}
	

	
	
	public void updateSlots(long[] indexes, long[][] values) {
		Excerpt ex = tier1Data.createExcerpt();
		try {
			for(int i = 0; i < indexes.length; i++) {
				long index = indexes[i];
				if(index < 1) continue;
				ex.index(index);
				ex.position(TIER_1_SIZE-4);
				int slots = ex.readInt();
				if(slots!=values[i].length) {
					loge("Invalid slot update. Slot count is [%s] but values array length is [%s]", slots, values[i].length);
					return;
				}
				for(long value: values[i]) {
					ex.writeLong(value);
				}
				ex.finish();
			}
		} catch (Exception x) {
			x.printStackTrace(System.err);
		} finally {
			ex.close();
		}
	}
	
	public static final int TIER_1_SIZEX = 
			1+ 					// the lock
			1+ 					// the delete indicator 
			8+ 					// the name index
			4+ 					// the enum ordinal index 
			4; 					// the sub metric count
	
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
	 * @see com.heliosapm.shorthand.store.IStore#lastFlushTime(long)
	 */
	public void lastFlushTime(long nanos) {
		flushTimes.insert(nanos);
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
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getLastFlushElapsedNs()
	 */
	@Override
	public long getLastFlushElapsedNs() {
		return flushTimes.getLast();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getLastFlushElapsedMs()
	 */
	@Override
	public long getLastFlushElapsedMs() {		
		return TimeUnit.MILLISECONDS.convert(flushTimes.getLast(), TimeUnit.NANOSECONDS);
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getAverageFlushElapsedNs()
	 */
	@Override
	public long getAverageFlushElapsedNs() {
		return flushTimes.avg();
		
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getAverageFlushElapsedMs()
	 */
	@Override
	public long getAverageFlushElapsedMs() {
		return TimeUnit.MILLISECONDS.convert(flushTimes.avg(), TimeUnit.NANOSECONDS);
		
	}

}
