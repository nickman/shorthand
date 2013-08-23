/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.datamapper;

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.Map;

import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.store.IStore;




/**
 * <p>Title: IDataMapper</p>
 * <p>Description: Defines a class that knows how to write and read an {@link com.heliosapm.shorthand.collectors.ICollector} to and from a memory address</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.IDataMapper</code></p>
 * @param <T> The collector type
 */

public interface IDataMapper<T extends Enum<T> & ICollector<T>> {
	/**
	 * Resets the memory space pointed to by the passed address
	 * @param address The address of the memory space
	 * @param offsets A map of offsets keyed by the collector for which the memory space is allocated
	 */
	public void reset(long address, TObjectLongHashMap<T> offsets);
	/**
	 * Calculates/Aggregates and applies the final value to the memory space allocated for each collector
	 * @param address The address of the memory space
	 * @param offsets A map of offsets keyed by the collector for which the memory space is allocated
	 * @param data The collected values to apply from
	 */
	public void put(long address, TObjectLongHashMap<T> offsets, long[] data);
	
	/**
	 * Returns a map of sub metric names and values keyed by the parent enum collector member
	 * @param address The address of the accumulator memory space where the metric is resident
	 * @return a map of sub metric indexes and values keyed by the parent enum collector member
	 */
	public Map<T, TIntLongHashMap> get(long address);
	
	/**
	 * Executes the associated pre-flush procedure for the enum collector's data at the specified address
	 * @param address The address of the accumulator memory space where the metric is resident
	 */
	public void preFlush(long address);
	
	
//	/**
//	 * Returns the datapoints for the metric at the passed address
//	 * as an array of longs keyed by the ordinal of the enabled metrics.
//	 * Non-enabled metrics are zero-length long arrays
//	 * @param address The address to retrieve the data points from
//	 * @return the datapoints
//	 */
//	public long[][] getDataPoints(long address);
	
	/**
	 * Calculates/Aggregates and applies the final value to the memory space allocated for each preApply collector
	 * @param bitMask The bitmask of the enabled metrics
	 * @param address The address of the memory space
	 * @param offsets A map of offsets keyed by the collector for which the memory space is allocated
	 * @param data The collected values to apply from
	 */
	public void prePut(int bitMask, long address, TObjectLongHashMap<T> offsets, long[] data);
	
	/**
	 * Flushes the accumulated metric at the passed address to the live tier store
	 * @param address The address of the metric copy
	 * @param store The store to flush to
	 * @param periodStart The start time of the period being flushed in ms.
	 * @param periodEnd The end time of the period being flushed in ms.
	 * @return The name indexes of the enabled metrics in the live tier
	 */
	public long[] flush(long address, IStore<T> store, long periodStart, long periodEnd);
}
