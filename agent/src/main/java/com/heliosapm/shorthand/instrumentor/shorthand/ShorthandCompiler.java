/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;


/**
 * <p>Title: ShorthandCompiler</p>
 * <p>Description: Complies the static instrumentor class. Calls to this class are injected into target methods.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandCompiler</code></p>
 */

public class ShorthandCompiler {
	

	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}	
}

