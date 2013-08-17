/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.collectors.measurers;


/**
 * <p>Title: AbstractDeltaMeasurer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.AbstractDeltaMeasurer</code></p>
 */

public abstract class AbstractDeltaMeasurer extends AbstractMeasurer {

	/**
	 * Creates a new AbstractDeltaMeasurer
	 * @param metricOrdinal the metric ordinal
	 */
	public AbstractDeltaMeasurer(int metricOrdinal) {
		super(metricOrdinal);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.measurers.Measurer#measure(boolean, long[])
	 */
	@Override
	public long measure(boolean open, long[] values) {
		long v = sample();
		values[metricOrdinal] = open ? v : v-values[metricOrdinal];
		return v;
	}
	
	/**
	 * Collects the opening and/or closing raw metric sample
	 * @return the sampled value
	 */
	protected abstract long sample();

}
