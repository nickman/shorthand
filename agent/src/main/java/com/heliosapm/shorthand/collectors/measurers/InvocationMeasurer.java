/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.collectors.measurers;


/**
 * <p>Title: InvocationMeasurer</p>
 * <p>Description: Measurer for invocations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.InvocationMeasurer</code></p>
 */

public class InvocationMeasurer extends AbstractMeasurer {

	/**
	 * Creates a new InvocationMeasurer
	 * @param metricOrdinal
	 */
	public InvocationMeasurer(int metricOrdinal) {
		super(metricOrdinal);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.measurers.Measurer#measure(boolean, long[])
	 */
	@Override
	public long measure(boolean open, long[] values) {
		int v = open ? 1 : 0;
		values[metricOrdinal] = values[metricOrdinal] + v; 
		return v;
	}



}
