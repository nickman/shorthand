/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.store;

import java.util.concurrent.ConcurrentHashMap;

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
	 * @param snapshotIndex the map to load
	 */
	public void loadSnapshotNameIndex(ConcurrentHashMap<String, Long> snapshotIndex);
	
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
