/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.ByteOrder;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.accumulator.AccumulatorThreadStats;
import com.heliosapm.shorthand.accumulator.MemSpaceAccessor;
import com.heliosapm.shorthand.accumulator.PeriodClock;
import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.ConfigurationHelper;
import com.heliosapm.shorthand.util.OrderedShutdownService;
import com.heliosapm.shorthand.util.StringHelper;
import com.heliosapm.shorthand.util.ThreadRenamer;
import com.heliosapm.shorthand.util.jmx.JMXHelper;
import com.heliosapm.shorthand.util.ref.RunnableReferenceQueue;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;
import com.heliosapm.shorthand.util.unsafe.collections.ConcurrentLongSlidingWindow;
import com.heliosapm.shorthand.util.unsafe.collections.LongSortedSet;
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

public class ChronicleStore<T extends Enum<T> & ICollector<T>> implements IStore<T>, ChronicleStoreMBean, NotificationBroadcaster,  RejectedExecutionHandler, Thread.UncaughtExceptionHandler, ThreadFactory {
	
	/** The index of metric name to slab id to find the snapshot */
	protected final NonBlockingHashMap<String, Long> SNAPSHOT_INDEX;
	/** The index of metric name to chronicle index for unloaded metrics */
	protected final NonBlockingHashMap<String, Long> UNLOADED_INDEX;
	
	/** Indicates if the mem-spaces should be padded */
	protected boolean padCache = true;
	
	
	
	/** JMX notification type for a period end event */
	public static final String NOTIF_PERIOD_END = "shorthand.store.period.end";
	/** JMX notification type for new metric names at period end */
	public static final String NOTIF_NEW_METRICS = "shorthand.store.period.metrics.new";
	/** JMX notification type for stale metric names at period end */
	public static final String NOTIF_STALE_METRICS = "shorthand.store.period.metrics.stale";
	
	/** The notification infos for this mbean */
	private static final MBeanNotificationInfo[] NOTIFS = new MBeanNotificationInfo[]{
		new MBeanNotificationInfo(new String[]{NOTIF_PERIOD_END}, Notification.class.getName(), "Notification indicating a metric period has ended"),
		new MBeanNotificationInfo(new String[]{NOTIF_NEW_METRICS}, Notification.class.getName(), "Notification indicating new metrics were created in the prior period"),
		new MBeanNotificationInfo(new String[]{NOTIF_STALE_METRICS}, Notification.class.getName(), "Notification indicating stale metrics were cleared")
	};
	
	/** The singleton instance */
	private static volatile ChronicleStore<?> instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The notification broadcaster delegate */
	protected final NotificationBroadcasterSupport notificationBroadcasterSupport; 
	/** A set of metric names that have been added this period */
	protected final Set<String> addedMetricNames = new CopyOnWriteArraySet<String>();
	/** Serial number factory for notification sequences */
	protected final AtomicLong notificationSerial = new AtomicLong();
	/** Serial number factory for notification thread pool thread names */
	protected final AtomicInteger threadSerial = new AtomicInteger();
	/** Notification thread pool */
	protected final ThreadPoolExecutor notificationProcessors = new ThreadPoolExecutor(2, 2, 15000, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(1000, false), this, this); 
	
	/**
	 * Acquires the singleton ChronicleStore instance
	 * @return the singleton ChronicleStore instance
	 */
	@SuppressWarnings("rawtypes")
	public static ChronicleStore<?> getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ChronicleStore();
					instance.loadSnapshotNameIndex();
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
	
	/** Stale metric addresses and names are registered here pending clearing */
	protected final NonBlockingHashMapLong<String> untouched = new NonBlockingHashMapLong<String>();
	
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
	protected final ConcurrentLongSlidingWindow firstPhaseFlushTimes = new ConcurrentLongSlidingWindow((1000*60*5)/ConfigurationHelper.getIntSystemThenEnvProperty(ShorthandProperties.PERIOD_PROP, 15000)); 
	/** A sliding window of second phase flush elapsed times in ns. */
	protected final ConcurrentLongSlidingWindow secondPhaseFlushTimes = new ConcurrentLongSlidingWindow((1000*60*5)/ConfigurationHelper.getIntSystemThenEnvProperty(ShorthandProperties.PERIOD_PROP, 15000));
	/** A map of addresses pending de-allocation */
	private final NonBlockingHashMapLong<Boolean> pendingDeallocates = new NonBlockingHashMapLong<Boolean>(CORES, false);
	/** A sliding window of period update times in ns. */
	protected final ConcurrentLongSlidingWindow periodUpdateTimes = new ConcurrentLongSlidingWindow(100);
	/** A sliding window of new metric creation times in ns. */
	protected final ConcurrentLongSlidingWindow newMetricTimes = new ConcurrentLongSlidingWindow(100); 
	
	
	/** This store's JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("com.heliosapm.shorthand.store:service=Store,type=Chronicle");
	
	/** A decode of enum class names to a set of all enum entries */
	protected final TObjectIntHashMap<Class<T>> ENUM_CACHE = new TObjectIntHashMap<Class<T>>(32, 0.2f, -1); 
	
	/** A cummulative count of the number of releases */
	protected final AtomicLong releaseCount = new AtomicLong(0L);
	
	/**
	 * Clears the store 
	 */
	@Override
	public void clear() {
		SNAPSHOT_INDEX.clear();
		UNLOADED_INDEX.clear();
		nameIndex.clear();
		tier1Data.clear();
		writeZeroRec(nameIndex);
		writeZeroRec(tier1Data);
	}
	
	
	

	
//	public static void main(String[] args) {
//		log("Dumping Shorthand DB");
//		ChronicleStore store = ChronicleStore.getInstance();
//		long count = 0;
//		long start = System.currentTimeMillis();
////		store.dump();
//		IMetric metric = null;
//		for(Object o: store.UNLOADED_INDEX.keySet()) {
//			String name = o.toString();
//			metric = store.getMetric(name);			
//			if(metric==null) continue;			
//			count++;
////			if(count%100==0) {
////				log("Loaded [%s] IMetrics", count);
////			}
//		}
//		long elapsed = System.currentTimeMillis()-start;
//		if(metric!=null) {
//			log(metric.toString());
//		}
//		System.gc();
//		try { Thread.sleep(1000); } catch (Exception ex) {}
//
//		
//		
//		log("Dump of [%s] metrics complete in [%s] ms.", count, elapsed);		
//	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetric(java.lang.String)
	 */
	@Override
	public IMetric<T> getMetric(String name) {
		Long address = SNAPSHOT_INDEX.get(name);
		if(address==null) {
			address = UNLOADED_INDEX.get(name);
			if(address==null) {
				return null;
			}
		}
		long nameIndex = -1;
		if(address<0) {
			nameIndex = (address * -1L);
		} else {
			long ref = lock(address);
			nameIndex = MemSpaceAccessor.get(ref).getNameIndex();
			unlock(address);
		}
		
		Excerpt nameEx = this.nameIndex.createExcerpt();
		Excerpt dataEx = this.tier1Data.createExcerpt();
		DirectMetric<T> dm = new DirectMetric<T>(nameIndex, nameEx, dataEx);
		nameEx.close();
		dataEx.close();
		return dm;
	}
	
	
	/**
	 * Dumps the contents of the store
	 */
	public void dump() {
		Excerpt dx = tier1Data.createExcerpt();
		try {
			nameIndexEx.index(1);
			StringBuilder b = new StringBuilder();
			
			while(nameIndexEx.nextIndex()) {
				b.append(nameIndexEx.index()).append(",");
				b.append(nameIndexEx.read()).append(",");
				b.append(nameIndexEx.read()).append(",");
				@SuppressWarnings("unchecked")
				Class<T> collectorType = (Class<T>) EnumCollectors.getInstance().type(nameIndexEx.readInt());
				ICollector<?>[] collectors = collectorType.getEnumConstants();
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
					ICollector<?> collector = collectors[enumIndex];
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
		this(ConfigurationHelper.getSystemThenEnvProperty(ShorthandProperties.CHRONICLE_DIR_PROP, ShorthandProperties.DEFAULT_CHRONICLE_DIR));
	}
	
	/**
	 * Writes a header record to the passed chronicle
	 * @param chr The chronicle to write to
	 */
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
		SNAPSHOT_INDEX = new NonBlockingHashMap<String, Long>(1024);
		UNLOADED_INDEX = new NonBlockingHashMap<String, Long>(1024);
		padCache = System.getProperty(ShorthandProperties.USE_POW2_ALLOC_PROP, ShorthandProperties.DEFAULT_USE_POW2_ALLOC).toLowerCase().trim().equals("true");
		
		try {
			enumIndex = getIntChronicle(ENUM_INDEX);
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
			notificationBroadcasterSupport = new NotificationBroadcasterSupport(notificationProcessors, NOTIFS); 
			JMXHelper.registerMBean(this, OBJECT_NAME);			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
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
					try { tier1Data.close(); } catch (Exception ex) {}					
				}
			});
		globalLockAddress = UnsafeAdapter.allocateMemory(UnsafeAdapter.LONG_SIZE);
		RunnableReferenceQueue.getInstance().buildPhantomReference(this, globalLockAddress);
		UnsafeAdapter.putLong(globalLockAddress, UNLOCKED);
		
		
	}
	
	/**
	 * Returns a string providing some details on the passed chronicle
	 * @param chronicle The chronicle to print details for
	 * @return the chronicle details message
	 */
	protected String printChronicleDetails(IndexedChronicle chronicle) {
		return String.format("[%s] Chronicle Created.\n\tUsing Unsafe:%s\n\tSynchronous:%s\n\tMultithreaded:%s\n\tSize (bytes):%s\n\tSize (entries):%s",
				chronicle.name(), chronicle.useUnsafe(), chronicle.synchronousMode(), chronicle.multiThreaded(), chronicle.sizeInBytes(), chronicle.size() 
		);
	}

	
	/**
	 * Loads all enums from the enum chronicle
	 */
	protected void cacheEnums() {
		int index = 1;
		int loaded = 0;
		while(enumIndexEx.index(index)) {
			String className = enumIndexEx.readByteString();
			@SuppressWarnings("unchecked")
			Class<T> type = (Class<T>) EnumCollectors.getInstance().typeForName(className);
			ENUM_CACHE.put(type, index);
			loaded++; index++;
		}
		log("Loaded [%s] Collector Enums into Cache\n\t", loaded);		
	}
	
	/**
	 * Looks up the enum collector index for the passed class name, creating the cache entry if not present
	 * @param collectorClassName The name of the enum collector class
	 * @return the enum chronicle index
	 */
	protected int getEnum(String collectorClassName) {
		@SuppressWarnings("unchecked")
		Class<T> collectorClass = (Class<T>) EnumCollectors.getInstance().typeForName(collectorClassName);
		int index = ENUM_CACHE.get(collectorClass);
		if(index==-1) {
			synchronized(ENUM_CACHE) {
				index = ENUM_CACHE.get(collectorClass);
				if(index==-1) {
					String name = collectorClass.getName();
					enumIndexEx.startExcerpt(name.length()+2);
					enumIndexEx.writeBytes(name);
					enumIndexEx.finish();		
					index = (int)enumIndexEx.index();
					ENUM_CACHE.put(collectorClass, index);
				}
			}
		}
		return index;
	}
	
	/**
	 * Looks up the enum collector index for the passed enum collector member instance, creating the cache entry if not present
	 * @param collector An enum collector member instance
	 * @return the enum chronicle index
	 */
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
	 * @throws IOException An exception occured creating or opening the chronicle
	 */
	protected IndexedChronicle getChronicle(String name) throws IOException {
		return new IndexedChronicle(dataDir.getAbsolutePath() + File.separator + name, 1, ByteOrder.nativeOrder(), true, false);
	}
	
	/**
	 * Acquires the named int chronicle
	 * @param name The name of the chronicle
	 * @return the named chronicle
	 * @throws IOException An exception occured creating or opening the chronicle
	 */
	protected IntIndexedChronicle getIntChronicle(String name) throws IOException {
		return new IntIndexedChronicle(dataDir.getAbsolutePath() + File.separator + name, 1, ByteOrder.nativeOrder());
	}	
	
	/**
	 * Marks a name index chronicle entry as deleted.
	 * @param index The index to mark deleted
	 */
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
	 * Loads the unloaded name index on startup
	 */
	protected void loadSnapshotNameIndex() {
		log("Loading NameIndex....");
		long index = 1;
		while(nameIndexEx.index(index)) {
			byte deleted = nameIndexEx.readByte(); 
			if(deleted==0) {  															// check the deleted flag
				int enumIndex = (int)ChronicleOffset.EnumIndex.get(index, nameIndexEx);	// the enum index
				String name = ChronicleOffset.getName(index, nameIndexEx);				// the metric name
				if(!ENUM_CACHE.containsValue(enumIndex)) {
					loge("Warning: MetricName [%s] had unrecognized enum index [%s]", name, enumIndex);
					markNameIndexDeleted(nameIndexEx.index());
				}
				if(UNLOADED_INDEX.put(name, (index * -1L))!=null) {
					log("WARNING:  Duplicate name [" + name + "] at index [" + index + "]");
				} else {
//					log("Loaded [" + name + "]");
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

	/**
	 * <p>Closes the chronicles on finalization</p>
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 */
	@Override
	public void finalize() throws Throwable {		
		try { enumIndex.close(); } catch (Exception ex) {}
		try { nameIndex.close(); } catch (Exception ex) {}
		try { tier1Data.close(); } catch (Exception ex) {}
		super.finalize();
	}
	
	/**
	 * Creates a new metric name
	 * @param metricName The metric name
	 * @param enumIndex The enum collector index
	 * @param bitMask The enabled metric bitmask
	 * @return the name index
	 */
	protected long newMetricName(String metricName, int enumIndex, int bitMask) {
		final long start = System.nanoTime();
		long index = ChronicleOffset.writeNewNameIndex(enumIndex, bitMask, metricName);
		newMetricTimes.insert(System.nanoTime()-start);		
		return index;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#cacheMetricAddress(java.lang.String, long)
	 */
	@Override
	public void cacheMetricAddress(String metricName, long address) {
		SNAPSHOT_INDEX.put(metricName, address);
	}

	

	
	/** The offset in the name index for the metric name start */
	public static final int CR_TS_INDEX = 	
			1 + 				// (byte)) The lock byte
			1 + 				// (byte)) The deleted indicator byte
			4 + 				// (int) the enum index 
			4;	 				// (int) the enabled bitmask
	
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#updatePeriod(com.heliosapm.shorthand.accumulator.MemSpaceAccessor, long, long)
	 */
	@Override
	public void updatePeriod(MemSpaceAccessor<T> msa, long periodStart, long periodEnd) {
		final long start = System.nanoTime();
		ChronicleOffset.updatePeriod(msa, periodStart, periodEnd);
		periodUpdateTimes.insert(System.nanoTime()-start);		
	}
	
	
	/**
     * Finds the next positive power of 2 for the passed value
     * @param value the value to find the next power of 2 for
     * @return the next power of 2
     */
    public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}	
    
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricCacheSize()
	 */
	@Override
	public int getMetricCacheSize() {
		return SNAPSHOT_INDEX.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricCacheKeys()
	 */
	@Override
	public Set<String> getMetricCacheKeys() {
		Set<String> allKeys = new HashSet<String>(SNAPSHOT_INDEX.size() + UNLOADED_INDEX.size());
		allKeys.addAll(SNAPSHOT_INDEX.keySet());
		allKeys.addAll(UNLOADED_INDEX.keySet());
		return allKeys;
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
	 * Returns an indirect address reference of a mem-space allocated for the passed metric name
	 * @param metricName The metric name
	 * @param collectorSet The collector set
	 * @return the address
	 */
	protected long getMetricAddress(String metricName, CollectorSet<T> collectorSet) {		
		Long address = SNAPSHOT_INDEX.get(metricName);
		if(address==null) address = UNLOADED_INDEX.get(metricName);
		if(address==null || address <0) {
			synchronized(SNAPSHOT_INDEX) {
				address = SNAPSHOT_INDEX.get(metricName);
				if(address==null) address = UNLOADED_INDEX.get(metricName);
				if(address==null || address <0) {
					int requestedMem = (int)(collectorSet.getTotalAllocation());
					int memSize = padCache ? findNextPositivePowerOfTwo(requestedMem) : requestedMem;
					long nameIndex;
					boolean unloaded = false;
					if(address==null) {
						nameIndex = newMetricName(metricName, collectorSet.getEnumIndex(), collectorSet.getBitMask());
					} else {
						nameIndex = address * -1;
						unloaded = true;
					}
					address = UnsafeAdapter.allocateMemory(memSize);
					MemSpaceAccessor.get(address).initializeHeader(memSize, nameIndex, collectorSet.getBitMask(), EnumCollectors.getInstance().index(collectorSet.getReferenceCollector().getDeclaringClass().getName()));
					MemSpaceAccessor.get(address).reset();
					long memSpaceRef = UnsafeAdapter.allocateMemory(UnsafeAdapter.LONG_SIZE * 2);					
					UnsafeAdapter.putLong(memSpaceRef, 0);
					UnsafeAdapter.putLong(memSpaceRef + UnsafeAdapter.LONG_SIZE, address);
					address = memSpaceRef;
					SNAPSHOT_INDEX.put(metricName, memSpaceRef);
					if(unloaded) UNLOADED_INDEX.remove(metricName);					
				}
			}
		}
		return address;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#doSnap(java.lang.String, com.heliosapm.shorthand.collectors.CollectorSet, long[])
	 */
	@Override
	public void doSnap(String metricName, CollectorSet<T> collectorSet, long...collectedValues) {
		long address = getMetricAddress(metricName, collectorSet);
		long ref = -1L;
		try {
			ref = lock(address);
			@SuppressWarnings("unchecked")
			MemSpaceAccessor<T> msa = MemSpaceAccessor.get(ref);
			if(msa.isInvalidated()) {
				msa.delete(address);
				ref = -1L;
				SNAPSHOT_INDEX.remove(metricName);
				doSnap(metricName, collectorSet, collectedValues);
				return;
			}
			collectorSet.put(ref, collectedValues);
		} finally {
			if(ref!=-1L) {
				unlock(address);
			}
		}
	}
		
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#flush(long, long)
	 */
	@Override
	public void flush(long priorStartTime, long priorEndTime) {
		log("Flushing Index Size: [%s]", SNAPSHOT_INDEX.size());
		LongSortedSet dirtyKeys = null;
		
		long bufferCount = 0;
		try {
//			long address = -1L;
			bufferCount = SNAPSHOT_INDEX.size();
			log("Processing Period Update for [%s] Store Name Index Values", bufferCount);
			@SuppressWarnings("unchecked")
			MemSpaceAccessor<T> msa = MemSpaceAccessor.get(-1L);
			// =========================================================================
			// Phase 1 Flush
			// =========================================================================
			final long startTime = System.nanoTime();
			dirtyKeys = new LongSortedSet(SNAPSHOT_INDEX.size());
			for(String metricName: SNAPSHOT_INDEX.keySet()) {
				long address = SNAPSHOT_INDEX.get(metricName);
				// =========================================================================
				long ref = lockNoYield(address);
				//log("Locked address for [%s] [%s]", metricName, ref);
				msa.setAddress(ref);
				if(msa.isInvalidated()) {
					//log("Clearing Invalid Metric Ref [%s]", metricName);
					SNAPSHOT_INDEX.remove(metricName);
					msa.delete(address);
					bufferCount--;
					continue;
				}
				if(!msa.isTouched()) {
					//log("Pending stale for Metric Ref [%s]", metricName);
					untouched.put(address, metricName);
					bufferCount--;	
					continue;					
				}
				dirtyKeys.add(ref);
				UnsafeAdapter.putLong(address + UnsafeAdapter.LONG_SIZE, msa.copy());
				unlock(address);
			}
			long elapsed = System.nanoTime()-startTime;
			this.firstPhaseFlushTimes.insert(elapsed);
			log(StringHelper.reportTimes("First Phase Flush [" + bufferCount + "], Elapsed Time", elapsed));
			// =========================================================================
			// Phase 2 Flush
			// =========================================================================
			for(int i = 0; i < dirtyKeys.size(); i++) {
				long address = dirtyKeys.get(i);
				msa.setAddress(address);
				msa.preFlush();
				updatePeriod(msa, priorStartTime, priorEndTime);
				UnsafeAdapter.freeMemory(address);
			}
			dirtyKeys.clear();
			closedPeriodNotif(priorStartTime, priorEndTime);
			newMetricNotifs();
			elapsed = System.nanoTime()-startTime;
			secondPhaseFlushTimes.insert(elapsed);			
			log(StringHelper.reportTimes("Second Phase Flush Elapsed Time", elapsed));
			// =========================================================================
			// Phase 3 Flush / Purge Stale Mem-Spaces
			// =========================================================================
			if(!untouched.isEmpty()) {
				log("\n\t==============================\n\tClearing [%s] Stale Mem-Spaces\n\t==============================", untouched.size());
				final long stage3start = System.nanoTime();
				for(long address: untouched.keySet()) {
					String metricName  = untouched.get(address);
					long ref = lockNoYield(address);
					msa.setAddress(ref);						
					long nameIndex = msa.getNameIndex();
					msa.setAddress(-1L);
					UnsafeAdapter.freeMemory(ref);
					UnsafeAdapter.putLong(address + UnsafeAdapter.LONG_SIZE, -1L);
					UNLOADED_INDEX.put(metricName, nameIndex * -1L);
					unlock(address);
					untouched.remove(address);
				}
				long stage3elapsed = System.nanoTime()-stage3start;
				log(StringHelper.reportTimes("Third Phase Flush Elapsed Time", stage3elapsed));
				
			}
//			untouched.clear();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			//globalUnlock();
			if(dirtyKeys!=null) dirtyKeys.destroy();
//			if(untouched!=null) untouched.destroy();
			
		}		
	}
	
	/**
	 * Sends a closed period notification
	 * @param priorStartTime The start time of the closed period
	 * @param priorEndTime The end time of the closed period
	 */
	protected void closedPeriodNotif(long priorStartTime, long priorEndTime) {
		Notification n = new Notification(NOTIF_PERIOD_END, OBJECT_NAME, notificationSerial.incrementAndGet(), System.currentTimeMillis(), "Metric Period End [" + new Date(priorStartTime) + "] to [" + new Date(priorEndTime) + "]");
		n.setUserData(new long[]{priorStartTime, priorEndTime});
		sendNotification(n);
	}
	
	/**
	 * Sends a new metrics JMX notification
	 */
	protected void newMetricNotifs() {
		if(!addedMetricNames.isEmpty()) {
			Set<String> names = new HashSet<String>(addedMetricNames);
			addedMetricNames.removeAll(names);
			Notification n = new Notification(NOTIF_NEW_METRICS, OBJECT_NAME, notificationSerial.incrementAndGet(), System.currentTimeMillis(), "New Metrics [" + names.size() + "]");
			n.setUserData(names);
			sendNotification(n);
		}
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
	
	
	
	
	
	
//	protected long[] readLongArray(Excerpt ex, int longCount) {
//		long[] values = new long[longCount];
//		long address = ((UnsafeExcerpt)ex). 
//				ex.     //public long getIndexData(long indexId);  
//		UnsafeAdapter.copyMemory(null, ex.position(), destBase, destOffset, bytes)
//		
//		
//	}
	
	

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
		return firstPhaseFlushTimes.getFirst();
	}
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getFirstPhaseLastFlushElapsedMs()
	 */
	@Override
	public long getFirstPhaseLastFlushElapsedMs() {		
		return TimeUnit.MILLISECONDS.convert(firstPhaseFlushTimes.getFirst(), TimeUnit.NANOSECONDS);
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
	 * Locks the passed address reference. Yield spins while waiting for the lock.
	 * Once the address reference is locked, then returns the associated actual mem-space address.
	 * @param address The address reference of the lock
	 * @return the actual address of the mem-space
	 */
	protected long lock(long address) {
		long id = Thread.currentThread().getId();
		if(UnsafeAdapter.getLong(address)!=id) {
			while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
				Thread.yield();
			}
		}
		return UnsafeAdapter.getLong(address + UnsafeAdapter.LONG_SIZE);
	}
	
	
	/**
	 * Acquires the passed address reference with no yielding
	 * @param address the address to the mem-space reference
	 * @return the address to the mem-space
	 */
	protected long lockNoYield(long address) {
		long id = Thread.currentThread().getId();
		if(UnsafeAdapter.getLong(address)!=id) {
			while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
				// Thread.yield(); No yielding
			}
		}
		return UnsafeAdapter.getLong(address + UnsafeAdapter.LONG_SIZE);
	}
	
	
	/**
	 * Unlocks the passed address
	 * @param address the address to unlock
	 */
	protected void unlock(long address) {		
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
	protected void globalLockNoYield() {
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
		return secondPhaseFlushTimes.getFirst();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.ChronicleStoreMBean#getSecondPhaseLastFlushElapsedMs()
	 */
	@Override
	public long getSecondPhaseLastFlushElapsedMs() {
		return TimeUnit.MILLISECONDS.convert(secondPhaseFlushTimes.getFirst(), TimeUnit.NANOSECONDS);
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



	/**
	 * Adds a listener. 
	 * @param listener The listener to receive notifications.
	 * @param filter The filter object. If filter is null, no filtering will be performed before handling notifications.
	 * @param handback An opaque object to be sent back to the listener when a notification is emitted. This object cannot be used by the Notification broadcaster object. It should be resent unchanged with the notification to the listener.
	 * @see javax.management.NotificationBroadcasterSupport#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
		notificationBroadcasterSupport.addNotificationListener(listener, filter, handback);
	}



	/**
	 * Removes a listener from this MBean. 
	 * @param listener A listener that was previously added to this MBean.
	 * @throws ListenerNotFoundException The listener is not registered with the MBean.
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		notificationBroadcasterSupport.removeNotificationListener(listener);
	}

	/**
	 * Unsubscribes a notification listener
	 * @param listener A listener that was previously added to this MBean.
	 * @param filter The filter that was specified when the listener was added. 
	 * @param handback The handback that was specified when the listener was added. 
	 * @throws ListenerNotFoundException The listener is not registered with the MBean, or it is not registered with the given filter and handback.
	 * @see javax.management.NotificationBroadcasterSupport#removeNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */ 
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		notificationBroadcasterSupport.removeNotificationListener(listener, filter, handback);
	}



	/**
	 * Returns the JMX notification meta-data describing notifications emitted by this service
	 * @return the JMX notification meta-data
	 * @see javax.management.NotificationBroadcasterSupport#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() {
		return notificationBroadcasterSupport.getNotificationInfo();
	}



	/**
	 * Sends a JMX notification
	 * @param notification The notification to send
	 * @see javax.management.NotificationBroadcasterSupport#sendNotification(javax.management.Notification)
	 */
	public void sendNotification(Notification notification) {
		notificationBroadcasterSupport.sendNotification(notification);
	}

	


	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "ShorthandNotificationThread#" + threadSerial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		loge("Uncaught exception on thread [%s]", e, t.getName());
	}



	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		loge("Rejected execution for [%s]", r);	
	}
	
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

}
