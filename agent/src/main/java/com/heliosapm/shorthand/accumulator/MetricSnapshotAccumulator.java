/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.accumulator;

import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.datamapper.IDataMapper;
import com.heliosapm.shorthand.store.ChronicleStore;
import com.heliosapm.shorthand.store.IStore;
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
	protected final IStore<T> store = (IStore<T>) ChronicleStore.getInstance();
	
	/** The current total unsafely allocated memory  */
	protected final AtomicLong unsafeMemoryAllocated = new AtomicLong();
	
    /** The number of bytes in a byte */
    public static final int SIZE_OF_BYTE = 1;
	
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
	 * Frees the memory at the passed address
	 * @param address the address of the memory space to free
	 */
	private void freeMemory(long address) {
//		long size = UnsafeAdapter.getLong(address + HeaderOffset.MemSize.offset);
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
	/** The period touched flag */
	public static final int PERIOD_TOUCHED = SIZE_OF_BYTE;
	
	/** The size of the memory allocation */
	public static final int MEM_SIZE = SIZE_OF_INT;
	
	/** The size of the accumulator bitmask */
	public static final int BITMASK_SIZE = SIZE_OF_INT;
	/** The size of the accumulator name index */
	public static final int NAMEINDEX_SIZE = SIZE_OF_LONG;
	/** The size of the accumulator enum decode index */
	public static final int ENUMINDEX_SIZE = SIZE_OF_INT;
	
	
	

	
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
	public void snap(String metricName, IDataMapper dataMapper, long...collectedValues) {
		store.doSnap(metricName, dataMapper, collectedValues);
	}
	
	

	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}




}
