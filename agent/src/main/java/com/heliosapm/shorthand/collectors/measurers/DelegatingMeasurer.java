/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.collectors.measurers;


/**
 * <p>Title: DelegatingMeasurer</p>
 * <p>Description: A measurer that delegates the collection to another supplied measurer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.DelegatingMeasurer</code></p>
 */

public class DelegatingMeasurer implements Measurer {
	/** The delegate measurer */
	protected Measurer delegate;
	
	/**
	 * Creates a new DelegatingMeasurer
	 * @param delegate The delegate measurer 
	 */
	public DelegatingMeasurer(Measurer delegate) {
		this.delegate = delegate;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.measurers.Measurer#measure(boolean, long[])
	 */
	@Override
	public long measure(boolean open, long[] values) {
		return delegate.measure(open, values);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.measurers.Measurer#getOrdinal()
	 */
	@Override
	public int getOrdinal() {
		return delegate.getOrdinal();
	}

	

}
