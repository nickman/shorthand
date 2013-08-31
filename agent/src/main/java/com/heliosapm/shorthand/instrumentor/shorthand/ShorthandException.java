/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

/**
 * <p>Title: ShorthandException</p>
 * <p>Description: The base shorthand exception class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandException</code></p>
 */

public class ShorthandException extends RuntimeException {
	/**  */
	private static final long serialVersionUID = -3505318391749927410L;
	/** The failed expression */
	protected final String expression;


	/**
	 * Creates a new ShorthandException
	 * @param message The failure message
	 * @param expression The expression that failed
	 * @param cause The underlying cause
	 */
	public ShorthandException(String message, String expression, Throwable cause) {
		super(message, cause);
		this.expression = expression;
	}
	
	/**
	 * Creates a new ShorthandException
	 * @param message The failure message
	 * @param expression The expression that failed
	 */
	public ShorthandException(String message, String expression) {
		super(message);
		this.expression = expression;
	}
	
	/**
	 * Returns the failed expression
	 * @return the failed expression
	 */
	public String getExpression() {
		return expression;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {		
		return new StringBuilder(super.toString()).append("Expression [").append(expression).append("]").toString();
	}
	

}
