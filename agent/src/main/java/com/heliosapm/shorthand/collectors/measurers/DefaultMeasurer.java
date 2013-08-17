/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.collectors.measurers;


/**
 * <p>Title: DefaultMeasurer</p>
 * <p>Description: A default measurer used for unimplemented enums or where the measurer may be replaced at runtime.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.DefaultMeasurer</code></p>
 */

public class DefaultMeasurer extends AbstractMeasurer {
	/**
	 * Creates a new DefaultMeasurer
	 * @param metricOrdinal the metric ordinal
	 */
	public DefaultMeasurer(int metricOrdinal) {
		super(metricOrdinal);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.measurers.Measurer#measure(boolean, long[])
	 */
	@Override
	public long measure(boolean open, long[] values) {
//		values[metricOrdinal] = -1L;
//		return -1L;
		return 0;
	}


}
