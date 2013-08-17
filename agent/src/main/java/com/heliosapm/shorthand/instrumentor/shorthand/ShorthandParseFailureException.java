/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

/**
 * <p>Title: ShorthandParseFailureException</p>
 * <p>Description: An exception thrown when the shorthand compiler cannot parse an expression</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandParseFailureException</code></p>
 */

public class ShorthandParseFailureException extends ShorthandException {
	/**  */
	private static final long serialVersionUID = -5855310122745557706L;

	/**
	 * Creates a new ShorthandParseFailureException
	 * @param message The failure message
	 * @param expression The expression that failed
	 * @param cause The underlying cause
	 */
	public ShorthandParseFailureException(String message, String expression, Throwable cause) {
		super(message, expression, cause);
	}
	
	/**
	 * Creates a new ShorthandParseFailureException
	 * @param message The failure message
	 * @param expression The expression that failed
	 */
	public ShorthandParseFailureException(String message, String expression) {
		super(message, expression);
	}

}
