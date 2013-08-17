/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

/**
 * <p>Title: ShorthandInvalidBitMaskException</p>
 * <p>Description: An exception thrown when the bit mask syntax cannot be interpreted</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandInvalidBitMaskException</code></p>
 */

public class ShorthandInvalidBitMaskException extends ShorthandException {

	/**  */
	private static final long serialVersionUID = 110103188239305220L;

	/**
	 * Creates a new ShorthandInvalidBitMaskException
	 * @param message The failure message
	 * @param expression The expression that failed
	 * @param cause The underlying cause
	 */
	public ShorthandInvalidBitMaskException(String message, String expression,
			Throwable cause) {
		super(message, expression, cause);
	}

	/**
	 * Creates a new ShorthandInvalidBitMaskException
	 * @param message The failure message
	 * @param expression The expression that failed
	 */
	public ShorthandInvalidBitMaskException(String message, String expression) {
		super(message, expression);
	}

}
