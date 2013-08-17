/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.collectors;

/**
 * <p>Title: OffsetDataStruct</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.collectors.OffsetDataStruct</code></p>
 */

public class OffsetDataStruct extends DataStruct {
	protected final String[] subNames;
	/**
	 * Creates a new OffsetDataStruct
	 * @param The parent struct with the static layouts and sizes
	 */
	public OffsetDataStruct(DataStruct dataTyping, String...subNames) {
		super(dataTyping.type, dataTyping.size);
		this.subNames = subNames;
	}

}
