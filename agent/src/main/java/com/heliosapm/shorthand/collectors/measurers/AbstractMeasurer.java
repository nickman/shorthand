/**
 * Helios Development Group LLC, 2010
 */
package com.heliosapm.shorthand.collectors.measurers;



/**
 * <p>Title: AbstractMeasurer</p>
 * <p>Description: Base class for concrete measurers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.AbstractMeasurer</code></p>
 */
public abstract class AbstractMeasurer implements Measurer {
	/** Valid int powers of 2 */
	protected static final int[] POWS_OF_TWO  = new int[]{2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608,16777216,33554432,67108864,134217728,268435456,536870912,1073741824};
	/** The metric ordinal this measurer reads and writes */
	protected final int metricOrdinal;
	/** The bit mask that represents this measurer */
	protected final int bitMask;

	/**
	 * Creates a new AbstractMeasurer
	 * @param metricOrdinal The metric ordinal this measurer reads and writes
	 */
	public AbstractMeasurer(int metricOrdinal) {
		this.metricOrdinal = metricOrdinal;
		this.bitMask = metricOrdinal<0 ? -1 : POWS_OF_TWO[metricOrdinal];
	} 
	

	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.measurers.Measurer#getOrdinal()
	 */
	@Override
	public int getOrdinal() {
		return metricOrdinal;
	}
	
	
	
}
