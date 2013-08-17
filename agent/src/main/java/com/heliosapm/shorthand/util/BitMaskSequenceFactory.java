
package com.heliosapm.shorthand.util;

/**
 * <p>Title: BitMaskSequenceFactory</p>
 * <p>Description: Generates int based bit masks, starting at one, and raising up by pow(2) each request</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.BitMaskSequenceFactory</code></p>
 */

public class BitMaskSequenceFactory {
	/** The int seed  */
	private int seed = 0;
	/* All the available bit masks for an int */
	private static final int[] BITMASKS = {1,2,4,8,16,32,64,128,256,512,1024,2048,4096,8192,16384,32768,65536,131072,262144,524288,1048576,2097152,4194304,8388608,16777216,33554432,67108864,134217728,268435456,536870912,1073741824,-2147483648};
	
	
	/**
	 * Returns the next seed in the sequence which is 2 X the prior value.
	 * @return the next seed in the sequence
	 */
	public synchronized int next() {
		int ret = BITMASKS[seed];
		seed++;
		return ret;
	}
}
