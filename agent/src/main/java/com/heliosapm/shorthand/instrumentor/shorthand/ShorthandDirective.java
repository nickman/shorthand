/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;


/**
 * <p>Title: ShorthandDirective</p>
 * <p>Description: A type checked container for a compiled shorthand expression</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandDirective</code></p>
 * <pre>
 		<ClassName>[+] [(Method Attributes)] <MethodName>[<Signature>] [Invocation Options] <CollectorName>[<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
 * </pre>
 */

public class ShorthandDirective {
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(String.format(fmt, args));
		t.printStackTrace(System.err);
	}
	


}
