/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import java.util.Map;
import java.util.Set;

import com.heliosapm.shorthand.accumulator.MemSpaceAccessor;
import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.ICollector;

/**
 * <p>Title: IStore</p>
 * <p>Description: Defines a persistent store for period metric flushes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.store.IStore</code></p>
 * @param <T> The collector type
 */

public interface IStore<T extends Enum<T> & ICollector<T>> {
	
	/** The byte state for unlocked */
	public static final long UNLOCKED = 0;
	
	/**
	 * Loads the snapshot index at startup
	 */
	public void loadSnapshotNameIndex();
	
	/**
	 * Executes a period flush
	 * @param priorStartTime The period start timestamp
	 * @param priorEndTime The period end timestamp
	 */
	public void flush(long  priorStartTime, long priorEndTime);
	
	/**
	 * Clears the name index and tier1 values.
	 * <b>USE WITH CAUTION.</b>
	 */
	public void clear();
	
	/**
	 * Returns a metric pojo for the passed name
	 * @param name The name of the metric
	 * @return the metric or null if the metric for the passed name does not exist or is not loaded
	 */
	public IMetric<T> getMetric(String name);
	
	/**
	 * Returns an entry set of all the keys and addresses from the name index
	 * @return an entry set of all the keys and addresses
	 */
	public Set<Map.Entry<String, Long>> indexKeyValues();
	
	/**
	 * Returns the address of a metric's address space
	 * @param metricName The metric name
	 * @param collectorSet The collector set submitting, used to create a mem-space if this metric does not already exist 
	 * @return The address of the mem-space for the named metric.
	 */
	public long getMetricAddress(String metricName, CollectorSet<T> collectorSet);
	
	/**
	 * Caches a meric memory space address
	 * @param metricName The metric name
	 * @param address The address
	 */
	public void cacheMetricAddress(String metricName, long address);
	
	/**
	 * Returns the number of entries in the metric name cache
	 * @return the number of entries in the metric name cache
	 */
	public int getMetricCacheSize();
	
	/**
	 * Returns an unmodifiable set of the metric cache keys
	 * @return an unmodifiable set of the metric cache keys
	 */
	public Set<String> getMetricCacheKeys();
	
	/**
	 * Adds a new metric name and returns the new metric name index
	 * @param metricName The metric name
	 * @param collector An enum member of the collector set
	 * @param bitMask The enabled bitmask
	 * @return the new name index id
	 */
	public long newMetricName(String metricName, T collector, int bitMask);
	
	/**
	 * Updates the name index and live tier of the store
	 * @param memSpaceAccessor The mem space reader for the accumulator name space being flushed
	 * @param periodStart The start time of the period being flushed in ms.
	 * @param periodEnd The end time of the period being flushed in ms.
	 */
	public void updatePeriod(MemSpaceAccessor<T> memSpaceAccessor, long periodStart, long periodEnd);
	

	/**
	 * Releases the read/write global lock address for this accumulator
	 */
	public void globalUnlock();
	
	/**
	 * Acquires the read/write global lock address for this accumulator
	 * This will lock out all other threads while it is held, so it should be used sparingly and released quickly.
	 * Intended for period flushes or data exports.
	 */
	public void globalLock();
	
	/**
	 * Unlocks the passed address
	 * @param address The address of the lock
	 */
	public void unlock(long address);		
	
	
	/**
	 * Locks the passed address. Yield spins while waiting for the lock.
	 * @param address The address of the lock
	 * @return indicates if the locked memspace is valid. If false, the memspace 
	 * has been invalidated and the snapshot index should be re-queried for a new address
	 */
	public boolean lock(long address);	
	
	/**
	 * Unlocks the passed address if it is held by the current thread.
	 * @param address The address to unlock
	 */
	public void unlockIfHeld(long address);

	/**
	 * Releases a locked and invalidated mem-space back to the store for de-allocation
	 * @param address the mem-space address to release
	 * @return true if address was de-allocated, false if it had already been cleared
	 */
	public boolean release(long address);
	
	/**
	 * Retrieves the new mem-space allocation address assigned in place of the prior invalidated address 
	 * @param oldAddress the prior invalidated address
	 * @return the new address
	 */
	public Long getTransferAddress(long oldAddress);
	
	
	
	
}
