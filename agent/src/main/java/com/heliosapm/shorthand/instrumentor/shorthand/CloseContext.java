/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

/**
 * <p>Title: CloseContext</p>
 * <p>Description: A container class for all the dynamic [late bound] method close tokens that may be used in the metric name formatter</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.CloseContext</code></p>
 */

public class CloseContext {
	/** Empty object array constant */
	protected static final Object[] EMPTY_ARGS = {};
	
	/** The method arguments of the closing method context */
	protected static final ThreadLocal<Object[]> methodArguments = new ThreadLocal<Object[]>() {
		@Override
		protected Object[] initialValue() {
			return EMPTY_ARGS;
		}
	};
	
	/** The target object instance the closing invoked method is being executed on. Null if method is static. */
	protected static final ThreadLocal<Object> targetInstance = new ThreadLocal<Object>();
	
	/**
	 * All statics. No ctor here.
	 */
	private CloseContext() {
	}

}
