/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import java.util.Map;
import java.util.Set;

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
	/**
	 * Loads the snapshot index at startup
	 */
	public void loadSnapshotNameIndex();
	
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
	 * @return The address of the metric space. Will be negative if the metric name
	 * exists but has not been allocated, or null if the metric name is not found.
	 */
	public Long getMetricAddress(String metricName);
	
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
	 * Updates the period start and end of a name index entry
	 * @param nameIndex The index of the name
	 * @param periodStart The start time of the period being flushed in ms.
	 * @param periodEnd The end time of the period being flushed in ms.
	 *  @return The name indexes of the enabled metrics in the live tier
	 */
	public long[] updatePeriod(long nameIndex, long periodStart, long periodEnd);
	
	/**
	 * Updates the data slots in the live tier
	 * @param indexes The indexes to update
	 * @param values The data values to update
	 */
	public void updateSlots(long[] indexes, long[][] values);
	
	/**
	 * Pass the last elapsed time of the flush in ns.
	 * @param nanos the elapsed time in ns.
	 */
	public void lastFlushTime(long nanos);
}
