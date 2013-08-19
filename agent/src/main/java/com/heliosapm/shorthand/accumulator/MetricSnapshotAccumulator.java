/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.accumulator;

import gnu.trove.map.hash.TIntLongHashMap;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.datamapper.DefaultDataMapper;
import com.heliosapm.shorthand.store.ChronicleStore;
import com.heliosapm.shorthand.store.IStore;
import com.heliosapm.shorthand.util.jmx.ShorthandJMXConnectorServer;
import com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadManager;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;


/**
 * <p>Title: MetricSnapshotAccumulator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator</code></p>
 * @param <T> 
 */

public class MetricSnapshotAccumulator<T extends Enum<T> & ICollector<T>> implements PeriodEventListener  {
	/** The singleton instance */
	private static volatile MetricSnapshotAccumulator<?> instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	
	/** Indicates if the mem-spaces should be padded */
	protected boolean padCache = true;
	/** Indicates if DEBUG is on */
	protected boolean DEBUG = false;

	
	/** The store impl we're using  FIXME: Needs to be configurable */
	protected final IStore<T> store = new ChronicleStore<T>();
	/** The address of the global lock for this instance */
	protected final long globalLockAddress;
//	/** The global lock for this instance */
//	protected final AtomicLong globalLockAddress = new AtomicLong(0L);
	
	/** The current total unsafely allocated memory  */
	protected final AtomicLong unsafeMemoryAllocated = new AtomicLong();
	
	
    /** The number of bytes in an int */
    public static final int SIZE_OF_INT = 4;
    /** The number of bytes in a long */
    public static final int SIZE_OF_LONG = 8;
    
    /** The system prop name to indicate if mem-spaces should be padded to the next largest pow(2) */
    public static final String USE_POW2_ALLOC_PROP = "shorthand.memspace.padcache";
	
    /**
     * Finds the next positive power of 2 for the passed value
     * @param value the value to find the next power of 2 for
     * @return the next power of 2
     */
    public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}
	
	/**
	 * Acquires the singleton MetricSnapshotAccumulator instance
	 * @return the singleton MetricSnapshotAccumulator instance
	 */
	public static MetricSnapshotAccumulator<?> getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new MetricSnapshotAccumulator();
				}
			}
		}
		return instance;
	}
	
	/** The byte state for unlocked */
	public static final long UNLOCKED = 0;
	
	/**
	 * Allocates a native memory space of the passed size
	 * @param size The size to be allocated
	 * @return the memory address of the allocated space
	 */
	private long allocateMemory(long size) {
		long address = UnsafeAdapter.allocateMemory(size);
		//unsafeMemoryAllocated.addAndGet(size);
		return address;
	}
	
	/**
	 * Frees the memory at the passed address
	 * @param address the address of the memory space to free
	 */
	private void freeMemory(long address) {
//		long size = UnsafeAdapter.getLong(address + HeaderOffsets.MemSize.offset);
		UnsafeAdapter.freeMemory(address);
//		unsafeMemoryAllocated.addAndGet(size*-1);
	}
	
	/**
	 * Returns the total allocated native memory in bytes
	 * @return the total allocated native memory in bytes
	 */
	public long getAllocatedMemory() {
		return unsafeMemoryAllocated.get();
	}
	
	/**
	 * Returns the total allocated native memory in KB
	 * @return the total allocated native memory in KB
	 */
	public long getAllocatedMemoryKb() {
		return unsafeMemoryAllocated.get()/1024;
	}
	
	/**
	 * Returns the total allocated native memory in MB
	 * @return the total allocated native memory in MB
	 */
	public long getAllocatedMemoryMb() {
		return getAllocatedMemoryKb()/1024;
	}
	
	
	
	
	private MetricSnapshotAccumulator() {	
		ShorthandJMXConnectorServer.getInstance();
		
		
		globalLockAddress = UnsafeAdapter.allocateMemory(SIZE_OF_LONG);		
		UnsafeAdapter.putLong(globalLockAddress, UNLOCKED);
		padCache = System.getProperty(USE_POW2_ALLOC_PROP, "true").toLowerCase().trim().equals("true");
		ExtendedThreadManager.install();
		PeriodClock.getInstance().registerListener(this);
		log("MetricSnapshotAccumulator Created");
	}
	
	/**
	 * Returns the collector class for the passed index
	 * @param index The index of the enum collector
	 * @return the enum collector class or null if one was not found
	 */	
	public Class<T> decodeEnumIndex(int index) {
		return (Class<T>) EnumCollectors.getInstance().type(index);
	}
	
	/**
	 * Locks the passed address. Yield spins while waiting for the lock.
	 * @param address The address of the lock
	 */
	protected void lock(long address) {

		long id = Thread.currentThread().getId();
		int loops = 0;
//		ReentrantLock lock = SNAPSHOT_LOCKS.get(address);
//		lock.lock();
//		while(!lock.compareAndSet(UNLOCKED, id)) {
		while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, id)) {
			Thread.yield();
			loops++;
		}
////		if(address==globalLockAddress) AccumulatorThreadStats.incrementGlobalLockSpins(loops);
////		else AccumulatorThreadStats.incrementNameLockSpins(loops);
		AccumulatorThreadStats.incrementNameLockSpins(loops);		
	}
	
//	/**
//	 * Read locks the passed address. This only verifies that the lock is not held by anyone else.
//	 * Yield spins while waiting for the lock.
//	 * @param address The address of the lock
//	 * @return true if the lock was acquired without yielding
//	 */
//	private boolean readLock(long address) {
//		boolean unheld = true;
//		while(!UnsafeAdapter.compareAndSwapLong(null, address, UNLOCKED, UNLOCKED)) {
//			unheld = false;
//			Thread.yield();
//		}
//		return unheld;
//	}
	
	
	/**
	 * Unlocks the passed address
	 * @param address The address of the lock
	 */
	protected void unlock(long address) {		
//		ReentrantLock lock = SNAPSHOT_LOCKS.get(address);
//		lock.lock();

//		if(!lock.compareAndSet(Thread.currentThread().getId(), UNLOCKED)) {
//		
		if(!UnsafeAdapter.compareAndSwapLong(null, address, Thread.currentThread().getId(), UNLOCKED)) {			
			System.err.println("[" + Thread.currentThread().toString() + "] Yikes! Tried to unlock, but was not locked.Expected " +  Thread.currentThread().getId());
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
//		while(!globalLockAddress.compareAndSet(UNLOCKED, id)) {
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
	protected void globalLock() {
		long id = Thread.currentThread().getId();
		int loops = 0;
//		while(!globalLockAddress.compareAndSet(UNLOCKED, id)) {
		while(!UnsafeAdapter.compareAndSwapLong(null, globalLockAddress, UNLOCKED, id)) {			
			Thread.yield();
			loops++;
		}
		AccumulatorThreadStats.incrementGlobalLockSpins(loops);
	}
	
//	/**
//	 * Indicates if the global lock is taken
//	 * @return true if the global lock is taken, false otherwise
//	 */
//	protected boolean isGlobalLock() {
//		return 0!=UnsafeAdapter.getLong(null, globalLockAddress);
//	}
	
//	/**
//	 * Acquires the read only global lock address for this accumulator.
//	 * It's not really a read-only since individual metrics can be updated once this lock is acquired,
//	 * but it is a validation that no one holds the global read/write lock.
//	 */
//	protected void globalLockForRead() {
//		readLock(globalLockAddress);
//	}
	
	/**
	 * Releases the read/write global lock address for this accumulator
	 */
	protected void globalUnlock() {
		//log(String.format("[%s] Unlocking GlobalLock at [%s]", Thread.currentThread().getName(), globalLockAddress));
//		unlock(globalLockAddress);
		long id = Thread.currentThread().getId();
		if(!UnsafeAdapter.compareAndSwapLong(null, globalLockAddress, id, UNLOCKED)) {
//		if(!globalLockAddress.compareAndSet(id, UNLOCKED)) {			
			System.err.println("[" + Thread.currentThread().toString() + "] Yikes! Tried to unlock GLOBAL , but was not locked.");
			new Throwable().printStackTrace(System.err);
		}
	}
	
	
	
	
	/**
	 * <p>Deallocate allocated native memory</p>
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 * FIXME:  Implement
	 */
	public void finalize() {
		
	}
	
	
	
	
	static {
		
	}
	
	/** The size of the accumulator lock */
	public static final int LOCK_SIZE = SIZE_OF_LONG;
	/** The size of the memory allocation */
	public static final int MEM_SIZE = SIZE_OF_INT;
	
	/** The size of the accumulator bitmask */
	public static final int BITMASK_SIZE = SIZE_OF_INT;
	/** The size of the accumulator name index */
	public static final int NAMEINDEX_SIZE = SIZE_OF_LONG;
	/** The size of the accumulator enum decode index */
	public static final int ENUMINDEX_SIZE = SIZE_OF_INT;
	
	/** The size of the accumulator header */
	public static final long HEADER_SIZE = LOCK_SIZE + BITMASK_SIZE + NAMEINDEX_SIZE + ENUMINDEX_SIZE + MEM_SIZE;
	
	
	/**
	 * Initializes the header of the memory space allocated for a new metric
	 * @param address The address of the memory space
	 * @param memorySize The amount of memory allocated
	 * @param nameIndex The name index of the new metric
	 * @param bitMask The enabled bitmask of the new metric
	 */
	private static void initializeHeader(long address, int memorySize, long nameIndex, int bitMask, int enumIndex) {		
		long pos = address;
		UnsafeAdapter.putLong(address, 0);   	// Lock
		pos += SIZE_OF_LONG;
		UnsafeAdapter.putInt(pos, bitMask);		// BitMask
		pos += SIZE_OF_INT;
		UnsafeAdapter.putInt(pos, enumIndex);	// EnumIndex
		pos += SIZE_OF_INT;
		UnsafeAdapter.putInt(pos, memorySize);	// Mem Size
		pos += SIZE_OF_INT;				
		UnsafeAdapter.putLong(pos,nameIndex);	// Name Index
		pos += SIZE_OF_LONG;
		assert pos==HEADER_SIZE;
	}
	
	/**
	 * <p>Title: HeaderOffsets</p>
	 * <p>Description: Enumeration of memory space header elements and their relative offsets</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.accumulator.HeaderOffsets</code></p>
	 */
	public static enum HeaderOffsets {
		/** The memory space lock */
		Lock(0, SIZE_OF_LONG, new HeaderAccessor() { public long get(long address){return UnsafeAdapter.getLong(address);}}),
		/** The metric bitmaek */
		BitMask(Lock.size + Lock.offset, SIZE_OF_INT, new HeaderAccessor() { public long get(long address){return UnsafeAdapter.getInt(address + Lock.size + Lock.offset);}}),
		/** The enum index */
		EnumIndex(BitMask.size + BitMask.offset, SIZE_OF_INT, new HeaderAccessor() { public long get(long address){return UnsafeAdapter.getInt(address + BitMask.size + BitMask.offset);}}),
		/** The total memory size of this allocation */
		MemSize(EnumIndex.size + EnumIndex.offset, SIZE_OF_INT, new HeaderAccessor() { public long get(long address){return UnsafeAdapter.getInt(address + EnumIndex.size + EnumIndex.offset);}}),
		/** The name index */
		NameIndex(MemSize.size + MemSize.offset, SIZE_OF_LONG, new HeaderAccessor() { public long get(long address){return UnsafeAdapter.getLong(address + MemSize.size + MemSize.offset);}});
		
		private HeaderOffsets(int offset, int size, HeaderAccessor accessor) {
			this.offset = offset;
			this.size = size;
			this.accessor = accessor;
		}
		/** The offset of this header element */
		public final int offset;
		/** The size of this header element */
		public final int size;
		/** The accessor for this field */
		public final HeaderAccessor accessor;
		
		public static void main(String[] args) {
			log("Header Offsets");
			for(HeaderOffsets off: HeaderOffsets.values()) {
				log(String.format("[%s] Offset:%s  Size:%s", off.name(), off.offset, off.size));
			}
			log("Total Header Size:" + HEADER_SIZE);
		}
		
		/**
		 * Return the header value at the passed address
		 * @param address The address
		 * @return the value as a long (it may be an int)
		 */
		public long get(long address) {
			return accessor.get(address);
		}
		
		/**
		 * <p>Title: HeaderAccessor</p>
		 * <p>Description: Header field accessor</p> 
		 * <p>Company: Helios Development Group LLC</p>
		 * @author Whitehead (nwhitehead AT heliosdev DOT org)
		 * <p><code>com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator.HeaderAccessor</code></p>
		 */
		protected interface HeaderAccessor {
			/**
			 * Return the header value at the passed address
			 * @param address The address
			 * @return the value as a long (it may be an int)
			 */
			public long get(long address);
		}
		
	}
	
	/**
	 * Returns the index code for the passed collector
	 * @param collector the collector
	 * @return the index
	 */
	protected int getEnumIndex(T collector) {
		return EnumCollectors.getInstance().index(collector.getDeclaringClass().getName());
	}
	
	public boolean isDebug() {
		return DEBUG;
	}
	
	public void setDebug(boolean enabled) {
		DEBUG = enabled;
	}
	
	private static final long[] EMPTY_LONG = {};
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.accumulator.PeriodEventListener#onNewPeriod(long, long, long, long)
	 */
	@Override
	public void onNewPeriod(final long newStartTime, final long newEndTime, final long priorStartTime, final long priorEndTime) {
		int loopCount = 2000;
		long start = System.nanoTime();
		long tier1Updates = 0;
		for(int i = 0; i < loopCount; i++) {
			globalLockNoYield();
			try {
				long address = -1L;
				String name = null;			
				
				for(Map.Entry<String, Long> entry: store.indexKeyValues()) {
					address = entry.getValue();
					if(address<0) continue;
					name = entry.getKey();
					lock(address);
					long addressCopy = -1L;
					long addressSize = HeaderOffsets.MemSize.accessor.get(address);
					addressCopy = UnsafeAdapter.allocateMemory(addressSize);
					UnsafeAdapter.copyMemory(address, addressCopy, addressSize);
					unlock(address);	
					long nameIndex = HeaderOffsets.NameIndex.get(addressCopy);
					int enumIndex = (int)HeaderOffsets.EnumIndex.get(addressCopy);
					//int bitMask = (int)HeaderOffsets.BitMask.get(addressCopy);
					T[] collectors = (T[]) EnumCollectors.getInstance().type(enumIndex).getEnumConstants();
					
					long[] tier1Indexes = store.updatePeriod(nameIndex, newStartTime, newEndTime);
					long[][] tier1Values = new long[tier1Indexes.length][]; 
					//updateSlots(long[] indexes, long[][] values);
					int enabled = 0;
					long offset = addressCopy + HEADER_SIZE;
					for(int x = 0; x < tier1Indexes.length; x++) {
						long tIndex = tier1Indexes[x];
						if(tIndex<1) {
							tier1Values[x] = EMPTY_LONG;
							continue;
						}
						T collector = collectors[x];
						long[] values = new long[collector.getDataStruct().size];
						for(int c = 0; c < values.length; c++) {
							values[c] = UnsafeAdapter.getLong(offset);
							offset += UnsafeAdapter.LONG_SIZE;
						}				
						tier1Values[x] = values;
					}
					store.updateSlots(tier1Indexes, tier1Values);
					tier1Updates += enabled;
					UnsafeAdapter.freeMemory(addressCopy);
				}
			} catch (Exception ex) {
				ex.printStackTrace(System.err);
			} finally {
				globalUnlock();
			}
		}
		long elapsed = System.nanoTime()-start; 
		store.lastFlushTime(elapsed);
//		log("Period Flush Completed. Elapsed: [%s] ns. ThreadStats:%s", elapsed, AccumulatorThreadStats.report());
//		log(reportSummary("PeriodFlush", elapsed, loopCount));
		AccumulatorThreadStats.reset();
	}	
	
	public static String reportTimes(String title, long nanos) {
		StringBuilder b = new StringBuilder(title).append(":  ");
		b.append(nanos).append( " ns.  ");
		b.append(TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " \u00b5s.  ");
		b.append(TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " ms.  ");
		b.append(TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " s.");
		return b.toString();
	}
	
	public static String reportAvgs(String title, long nanos, long count) {
		if(nanos==0 || count==0) return reportTimes(title, 0);
		return reportTimes(title, (nanos/count));
	}
	
	public static String reportSummary(String title, long nanos, long count) {
		return reportTimes(title, nanos) + 
				"\n" +
				reportAvgs(title + "  AVGS", nanos, count);
	}

	


	/**
	 * Process a submission of collected metrics for the passed metric name
	 * @param metricName The metric name
	 * @param collectorSet The collector set created when the code was instrumented
	 * @param collectedValues The collected values
	 */
	public void snap(String metricName, CollectorSet<?> collectorSet, long...collectedValues) {
		final int bitMask = collectorSet.getBitMask();
		Long address = store.getMetricAddress(metricName);
		boolean inited = true;
		if(address==null || address<1) {
			synchronized(store) {
				address = store.getMetricAddress(metricName);				
				if(address==null || address<0) {					
					final long start = System.nanoTime();
					globalLock();			
					try {
						int requestedMem = (int)(collectorSet.getTotalAllocation() + HEADER_SIZE);
						long memSize = padCache ? findNextPositivePowerOfTwo(requestedMem) : requestedMem;
						long nameIndex = -1;
						if(address==null) {
							nameIndex = store.newMetricName(metricName, (T) collectorSet.getReferenceCollector(), bitMask);
							inited = false;
							//log("Creating new metric [%s] with index [%s]", metricName, nameIndex);
						} else {
							nameIndex = Math.abs(address);
							//log("Initializing metric [%s] with index [%s]", metricName, nameIndex);
						}
						 
								
								
						address = this.allocateMemory(memSize);
						collectorSet.reset(address);
						int enumIndex = getEnumIndex((T) collectorSet.getReferenceCollector());
						initializeHeader(address, (int)memSize, nameIndex, bitMask, enumIndex); 					
						store.cacheMetricAddress(metricName, address);	
					} catch (Throwable ex) {
						ex.printStackTrace(System.err);
					} finally {
						globalUnlock();
						long elapsed = System.nanoTime()-start;
						//log("Prepared metric [" + metricName + "] in [" + elapsed + "] ns.");
						if(inited) AccumulatorThreadStats.incrementInitMetricTime(elapsed);
						else AccumulatorThreadStats.incrementNewMetricTime(elapsed);
						//log(AccumulatorThreadStats.report());
					}					
				}
			}
		}
		try {
			lock(address);			
			collectorSet.put(address, collectedValues);
			//log("Processing [%s] with [%s]", metricName, Arrays.toString(collectedValues));
			
		} finally {
			unlock(address);
		}
	}
	
	/**
	 * Returns a copy of the memory space for the passed metric name or -1 if the metric name was not found.
	 * Use advisedly. The memory should be released when you're done.
	 * @param metricName The metric name
	 * @param procedure The procedure to execute in the copied address space
	 * @param refs Objects to be passed back in the procedure callback
	 * @return the address of the copy of the memory space for the passed metric name or null if the metric name was not found.
	 */
	public <R> R getMemorySpaceCopy(String metricName, CopiedAddressProcedure<R> procedure, Object...refs) {
		long copyAddress = -1;
		try {
			try {
				globalLock();
				Long address = store.getMetricAddress(metricName);
				if(address!=null && address>0) {
					int memSize = UnsafeAdapter.getInt(address + HeaderOffsets.MemSize.offset);
					copyAddress = allocateMemory(memSize);
					UnsafeAdapter.copyMemory(address, copyAddress, memSize);
				}
			} finally {
				globalUnlock();
			}
			return procedure.addressSpace(metricName, copyAddress, refs);
		} finally {
			if(copyAddress!=-1) freeMemory(copyAddress);
		}
	}
	
	
	public static void main(String[] args) {
		ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
		tmx.setThreadContentionMonitoringEnabled(true);
		tmx.setThreadCpuTimeEnabled(true);
		log("MetricSnapshotAccumulator Test");
		//int bitMask = MethodInterceptor.defaultMetricsMask;
		int bitMask = MethodInterceptor.allMetricsMask;
		long[] snap = null;
		CollectorSet<?> cs = new CollectorSet<MethodInterceptor>(MethodInterceptor.class, bitMask); 
		
		final Random r = new Random(System.currentTimeMillis());
		byte[] randomBytes = new byte[2048];
		
		int innerLoops = 1000;
		long loops = 1600;
		for(int i = 0; i < loops; i++) {			
			snap = MethodInterceptor.methodEnter(bitMask);
			for(int x = 0; x < innerLoops; x++) {
				r.nextBytes(randomBytes);			
			}
			//try { Thread.sleep(r.nextInt(5)); } catch (Exception e) {}
			MetricSnapshotAccumulator.getInstance().snap("Foo", (CollectorSet<?>) cs, MethodInterceptor.methodExit(snap));
		}
		log("Warmup Complete");
		long start = System.nanoTime();
		//loops = 300;
		for(int i = 0; i < loops; i++) {	

			snap = MethodInterceptor.methodEnter(bitMask);			
//			for(int x = 0; x < innerLoops; x++) {
//				r.nextBytes(randomBytes);	
//				factorial(1000);
//				
//			}
//			try { Thread.sleep(r.nextInt(5)); } catch (Exception e) {}			
			MetricSnapshotAccumulator.getInstance().snap("Foo2", (CollectorSet<?>) cs, MethodInterceptor.methodExit(snap));
			//log("======================================");
		}
		long elapsed = System.nanoTime() - start;
		log("Elapsed:" + elapsed + " ns.   " + (TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS)) + "  ms.");
		long avgNs = (elapsed/loops);
		log("Avg:\n\t" + avgNs + " ns.\n\t" 
				+ TimeUnit.MILLISECONDS.convert(avgNs, TimeUnit.NANOSECONDS) + " ms.\n\t"
				+ TimeUnit.MICROSECONDS.convert(avgNs, TimeUnit.NANOSECONDS) + " us\n\t"				
		);
		for(int i = 0; i < loops; i++) {
			MetricSnapshotAccumulator.getInstance().summary();
		}
		
		start = System.nanoTime();
		for(int i = 0; i < loops; i++) {
			MetricSnapshotAccumulator.getInstance().summary();
		}
		log(MetricSnapshotAccumulator.getInstance().summary());
		elapsed = System.nanoTime() - start;
		long avg = elapsed/(loops+1);
		log("Summary elapsed:%s ns.  %s us.  %s ms.", elapsed, 
				TimeUnit.MICROSECONDS.convert(elapsed, TimeUnit.NANOSECONDS),
				TimeUnit.MILLISECONDS.convert(elapsed, TimeUnit.NANOSECONDS)
		);
		log("Summary AVG:%s ns.  %s us.  %s ms.", avg, 
				TimeUnit.MICROSECONDS.convert(avg, TimeUnit.NANOSECONDS),
				TimeUnit.MILLISECONDS.convert(avg, TimeUnit.NANOSECONDS)
		);
		
		
		//log(MetricSnapshotAccumulator.getInstance().summary());
//		final Random r = new Random(System.currentTimeMillis());
//		int numberOfArrays = 3;
//		int numberOfValues = 10;		
//		long[][] values = new long[numberOfArrays][];		
//		for(int i = 0; i < numberOfArrays; i++) {
//			values[i] = new long[numberOfValues];
//			for(int x = 0; x < numberOfValues; x++) {
//				values[i][x] = r.nextLong();
//			}
//		}
//		
//		int index = 0;
//		long[] addresses = new long[numberOfArrays];
//		for(long[] entry: values) {
////			snapshot.move(index);
////			snapshot.setBaseline(entry);
//			addresses[index] = put(entry); 
//			index++;
//		}
//		log("Slab loaded");
//		long[][] values2 = new long[numberOfArrays][];
//		for(int i = 0; i < numberOfArrays; i++) {
//			values2[i] = get(addresses[i]);
//		}
//		log("Slab read");
//		boolean ok = true;
//		for(int i = 0; i < numberOfArrays; i++) {
//			for(int x = 0; x < numberOfValues; x++) {
//				if(values[i][x]!=values2[i][x]) {
//					ok = false;
//					log("Fail  [" + values[i][x] + "] vs. [" + values2[i][x] + "]");
//				}
//			}
//		}
//		if(ok) log("All tests passed");
//			
		
		
	}
	
	public static int factorial(int n) {
        int fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }
	
	
	
	
	
	public String summary() {
		Set<String> keys = null;
		try {
			globalLock();
			log("SnapshotIndex Size:" + store.getMetricCacheSize());
			keys = store.getMetricCacheKeys();
			
			//for(String s: keys) {
				//log("\t[%s] : %s", s, SNAPSHOT_INDEX.get(s));
			//}
		} finally {
			globalUnlock();
		}
		StringBuilder b = new StringBuilder();
		for(String metricName: keys) {			
			Map<T, TIntLongHashMap> map = DefaultDataMapper.INSTANCE.get(metricName);
			if(map.isEmpty()) continue;
			b.append(metricName);
			for(Map.Entry<T, TIntLongHashMap> col: map.entrySet()) {
				String[] subNames = col.getKey().getSubMetricNames();
				b.append("\n\t").append(col.getKey().name()).append("(").append(col.getKey().getUnit()).append(")");
				for(int i = 0; i < col.getValue().size(); i++) {
					b.append("\n\t\t").append(subNames[i]).append(":").append(col.getValue().get(i));
				}
			 }
			//log("Completed [%s]", metricName);
			 b.append("\n");
//			 log(b);
		}
		return b.toString();
//		return "";
	}
	
	public String summary(String...names) {
		StringBuilder b = new StringBuilder();
		for(String metricName: names) {	
			log("Summary for [%s]", metricName);
			Map<T, TIntLongHashMap> map = DefaultDataMapper.INSTANCE.get(metricName);
			if(map.isEmpty()) continue;
			b.append(metricName);
			for(Map.Entry<T, TIntLongHashMap> col: map.entrySet()) {
				String[] subNames = col.getKey().getSubMetricNames();
				b.append("\n\t").append(col.getKey().name()).append("(").append(col.getKey().getUnit()).append(")");
				for(int i = 0; i < col.getValue().size(); i++) {
					b.append("\n\t\t").append(subNames[i]).append(":").append(col.getValue().get(i));
				}
			 }
			//log("Completed [%s]", metricName);
			 b.append("\n");
//			 log(b);
		}
		return b.toString();
//		return "";
	}
	
	
	
	
	
	
	

	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}




}
