/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.collectors;

import java.util.Map;
import java.util.Set;

import com.heliosapm.shorthand.collectors.measurers.Measurer;
import com.heliosapm.shorthand.util.BitMaskSequenceFactory;


/**
 * <p>Title: ICollector</p>
 * <p>Description: Defines an enum based metric collector where each enum instance is responsible for collecting a different aspect
 * of the overall collector's metrics.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.ICollector</code></p>
 * @param <T> The type of the {@link ICollector} implementation
 */

public interface ICollector<T extends Enum<? extends ICollector<T>>> {

	/** The enum bit mask seed */
	final BitMaskSequenceFactory seed = new BitMaskSequenceFactory();

	/**
	 * Returns an array of all the enum members
	 * @return an array of all the enum members
	 */
	public ICollector<?>[] collectors();
	
	/**
	 * Callback to "massage" the values before writing to the tier 1 data store
	 * @param address The address of the metric's memspace in the accumulator
	 * @param bitMask The configured bitMask
	 */
	public void preFlush(long address, int bitMask);

	
	/**
	 * Returns the enum ordinal of this collector
	 * @return the enum ordinal of this collector
	 */
	public int ordinal();
	
	/**
	 * Returns the name of the metric
	 * @return the name of the metric
	 */
	public String name();
	
	/**
	 * Returns the index of the bitmask for this enum
	 * @return the index of the bitmask for this enum
	 */
	public int getBitMaskIndex();

	/**
	 * Returns a collector's measurer.
	 * @return the measurer
	 */
	public Measurer getMeasurer();


	/**
	 * Indicates if this metric is turned on by default
	 * @return true if on by default
	 */
	public boolean isDefaultOn();

	/**
	 * Determines if this metric is enabled for the passed bitMask
	 * @param bitMask the bitMask to test
	 * @return true if this metric is enabled for the passed bitMask
	 */
	public boolean isEnabled(int bitMask);

	/**
	 * Returns an int as the passed bitMask enabled for this metric
	 * @param bitMask the bitmask to enable against
	 * @return the passed bitMask enabled for this metric
	 */
	public int enable(int bitMask);

	/**
	 * Returns the metric collection bit mask code.
	 * @return the metric collection bit mask code
	 */
	public int getMask();

	/**
	 * Returns the unit of the metric
	 * @return the unit
	 */
	public String getUnit();

	/**
	 * Returns the short name of the metric
	 * @return the shortName
	 */
	public String getShortName();

	/**
	 * Returns the description of the metric
	 * @return the description
	 */
	public String getDescription();
	
	/**
	 * Returns the data struct required to store the metrics collected by the passed collector
	 * @return a DataStruct
	 */
	public DataStruct getDataStruct();
	
	/**
	 * Returns the sub-metric names (e.g.  Min, Max, Avg).
	 * The number of names should be the same as the size of the associated {@link DataStruct}.
	 * @return the sub metric names
	 */
	public String[] getSubMetricNames();
	
	/**
	 * Returns the total allocation for the passed bitmask
	 * @param bitMask The bitmask to calculate total allocation for
	 * @return the total number of bytes needed for the passed bitmask
	 */
	public long getAllocationFor(int bitMask);
	
	/**
	 * Returns a set of the enabled collectors
	 * @param bitmask The bitmask to test for
	 * @return a set of the enabled collectors
	 */
	public Set<T> getEnabledCollectors(int bitmask);
	
	/**
	 * Resets the values in a metrics's accumulator memspace after the flush is complete
	 * @param address The address of the metric's accumulator namespace
	 * @param bitmask The bitmask of the enabled metrics
	 */
	public void resetMemSpace(long address, int bitmask);
	
	/**
	 * Returns the default [reset] values for a collector's mem-space
	 * @param bitMask The bitmask of all enabled metrics
	 * @return an array of default values for each enabled metric
	 */
	public long[][] getDefaultValues(int bitMask);
	
	/**
	 * Returns an array of pre-apply collectors, in the order in which they should be applied
	 * @param bitmask The enabled bitmask
	 * @return an array of collectors, possibly empty
	 */
	public T[] getPreApplies(int bitmask);
	
	/**
	 * Indicates if this collector is a pre-apply
	 * @return true if this collector is a pre-apply, false otherwise
	 */
	public boolean isPreApply();
	
	/**
	 * Aggregates/Calculates and applies the final value to the the passed address
	 * @param address The address to which the final value should be applied to
	 * @param collectedValues The collected values
	 */
	public void apply(long address, long[] collectedValues);
	
	/**
	 * Same as {@link #apply(long, long[])} but in addition, writes back the dependent pre-apply final value
	 * to the passed collectedValues so other collectors can use it for their calcs.
	 * @param address The address to which the final value should be applied to
	 * @param collectedValues The collected values
	 */
	public void preApply(long address, long[] collectedValues);
	
	/**
	 * Returns the offsets for the passed bitMask
	 * @param bitMask The bitmask to get offsets for
	 * @return a map of offsets keyed by the associated collector
	 */
	public Map<T, Long> getOffsets(int bitMask);

	
		
	

}