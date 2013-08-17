package com.heliosapm.shorthand.collectors.measurers;


/**
 * <p>Title: Measurer</p>
 * <p>Description: Defines an actual measurement procedure.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.Measurer</code></p>
 */
public interface Measurer {
	/**
	 * Capture a measurement and return the value
	 * @param open if true, this is a opening measurement, if false, it is closing.
	 * @param values The values to measure against
	 * @return the value of the measurement
	 */
	public long measure(boolean open, long[] values);
	
	/**
	 * Returns the ordinal this measurer is set to
	 * @return the ordinal this measurer is set to
	 */
	public int getOrdinal();
}
