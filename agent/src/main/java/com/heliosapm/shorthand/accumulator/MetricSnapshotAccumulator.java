/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.accumulator;

import gnu.trove.map.hash.TIntLongHashMap;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.datamapper.DefaultDataMapper;
import com.heliosapm.shorthand.store.ChronicleStore;
import com.heliosapm.shorthand.store.IStore;
import com.heliosapm.shorthand.util.StringHelper;
import com.heliosapm.shorthand.util.ThreadRenamer;
import com.heliosapm.shorthand.util.jmx.ShorthandJMXConnectorServer;
import com.heliosapm.shorthand.util.jmx.threadinfo.ExtendedThreadManager;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;


/**
 * <p>Title: MetricSnapshotAccumulator</p>
 * <p>Description: The central accumulator and aggregator of submitted snapshot metrics</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator</code></p>
 * @param <T> The enum collector type inference
 */

public class MetricSnapshotAccumulator<T extends Enum<T> & ICollector<T>> implements PeriodEventListener  {
	/** The singleton instance */
	private static volatile MetricSnapshotAccumulator<?> instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	
	/** Indicates if DEBUG is on */
	protected boolean DEBUG = false;

	
	/** The store impl we're using  FIXME: Needs to be configurable */
	protected final IStore<T> store = new ChronicleStore<T>();
	
	/** The current total unsafely allocated memory  */
	protected final AtomicLong unsafeMemoryAllocated = new AtomicLong();
	
	
    /** The number of bytes in an int */
    public static final int SIZE_OF_INT = 4;
    /** The number of bytes in a long */
    public static final int SIZE_OF_LONG = 8;
    
	
 
	
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
		Lock(0, SIZE_OF_LONG),									// At offset 0 
		/** The metric bitmask */
		BitMask(Lock.size + Lock.offset, SIZE_OF_INT),			// At offset 8
		/** The enum index */
		EnumIndex(BitMask.size + BitMask.offset, SIZE_OF_INT),	// At offset 12
		/** The total memory size of this allocation */
		MemSize(EnumIndex.size + EnumIndex.offset, SIZE_OF_INT),// At offset 16
		/** The name index */
		NameIndex(MemSize.size + MemSize.offset, SIZE_OF_LONG); // At offset 20
		
		private HeaderOffsets(int offset, int size) {
			this.offset = offset;
			this.size = size;			
		}
		
		public static void main(String[] args) {
			log("Header Offsets");
			for(HeaderOffsets off: HeaderOffsets.values()) {
				log(String.format("[%s] Offset:%s  Size:%s", off.name(), off.offset, off.size));
			}
			log("Total Header Size:" + HEADER_SIZE);
		}
				
		
		/** The length of the header in bytes */
		public static final long HEADER_SIZE;
		
		static {
			long offset = 0;
			for(HeaderOffsets off: HeaderOffsets.values()) {
				offset += off.size;
			}
			HEADER_SIZE = offset;
		}
		
		/** The offset of this header element */
		public final int offset;
		/** The size of this header element */
		public final int size;
		
		
		/**
		 * Return the header value at the passed address
		 * @param address The address
		 * @return the value as a long (it may be an int)
		 */
		public long get(long address) {
			if(size==4) {
				return UnsafeAdapter.getInt(address + offset);
			}
			return UnsafeAdapter.getLong(address + offset);
		}
		
		/**
		 * Sets the value of this header at the passed address
		 * @param address The starting address for these headers
		 * @param value The value to set the header to
		 */
		public void set(long address, long value) {
			if(size==4) {
				UnsafeAdapter.putInt(address + offset, (int)value);
			} else {
				UnsafeAdapter.putLong(address + offset, value);
			}
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
		store.flush(priorStartTime, priorEndTime);
		AccumulatorThreadStats.reset();
	}	
	


	


	/**
	 * INTERNAL Process a submission of collected metrics for the passed metric name
	 * @param metricName The metric name
	 * @param collectorSet The collector set created when the code was instrumented
	 * @param collectedValues The collected values
	 */
	public void snap(String metricName, CollectorSet<T> collectorSet, long...collectedValues) {
		long address = store.getMetricAddress(metricName, collectorSet);		
		try {
			store.lock(address);
			collectorSet.put(address, collectedValues);				
		} finally {
			store.unlock(address);
		}
	}
	
	

	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}




}
