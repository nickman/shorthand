/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.accumulator;


/**
 * <p>Title: CopiedAddressProcedure</p>
 * <p>Description: Defines a procedure that is executed with a copied address for a metric name, passed to the procedure for operating on the copied adddress space.
 * This keeps control of the copied address within the accumulator.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.accumulator.CopiedAddressProcedure</code></p>
 * @param <T> The return type of the procedure
 */

public interface CopiedAddressProcedure<T>  {
	/** Empty object array const */
	public static final Object[] EMPTY_ARR = {};
	
	/**
	 * Callback with the metric name represented in the address space and the address of the memory space.
	 * @param metricName The metric name 
	 * @param address the address
	 * @param refs An arbitrary array of objects to pass in the callback
	 * @return the return value of the procedure
	 */
	public T addressSpace(String metricName, long address, Object...refs);
	
	
}
