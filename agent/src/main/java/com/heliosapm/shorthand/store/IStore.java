/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import java.util.Set;

import com.heliosapm.shorthand.accumulator.MemSpaceAccessor;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.datamapper.IDataMapper;

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
	 * @param metricName
	 * @param dataMapper
	 * @param collectedValues
	 */
	public void doSnap(String metricName, IDataMapper<T> dataMapper, long...collectedValues);
	

	
	
	
	
}
