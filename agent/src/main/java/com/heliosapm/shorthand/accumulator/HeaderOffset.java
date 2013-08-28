/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.shorthand.accumulator;


import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: HeaderOffset</p>
 * <p>Description: Enumeration of memory space header elements and their relative offsets</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.HeaderOffset</code></p>
 */
public enum HeaderOffset {
	/** The touch flag */
	Touch(0, UnsafeAdapter.BYTE_SIZE),									// At offset 0 		
	/** The metric bitmask */
	BitMask(Touch.size + Touch.offset, UnsafeAdapter.INT_SIZE),			// At offset 1
	/** The enum index */
	EnumIndex(BitMask.size + BitMask.offset, UnsafeAdapter.INT_SIZE),	// At offset 5
	/** The total memory size of this allocation */
	MemSize(EnumIndex.size + EnumIndex.offset, UnsafeAdapter.INT_SIZE),	// At offset 9
	/** The name index */
	NameIndex(MemSize.size + MemSize.offset, UnsafeAdapter.LONG_SIZE); 	// At offset 13
	
	// body starts at offset 21
	
	private HeaderOffset(int offset, int size) {
		this.offset = offset;
		this.size = size;			
	}
	
	public static void main(String[] args) {
		log("Header Offsets");
		for(HeaderOffset off: HeaderOffset.values()) {
			log(String.format("[%s] Offset:%s  Size:%s", off.name(), off.offset, off.size));
		}
		log("Total Header Size:" + HEADER_SIZE);
	}
	
	/** The reset and untouched flag value */
	public static final byte UNTOUCHED = 0; 

	
	/**
	 * Initializes the header of the memory space allocated for a new metric
	 * @param address The address of the memory space
	 * @param memorySize The amount of memory allocated
	 * @param nameIndex The name index of the new metric
	 * @param bitMask The enabled bitmask of the new metric
	 */
	public static void initializeHeader(long address, int memorySize, long nameIndex, int bitMask, int enumIndex) {		
		long pos = address;
		UnsafeAdapter.putByte(address, UNTOUCHED);   	// Touch Flag
		pos += UnsafeAdapter.BYTE_SIZE;
		UnsafeAdapter.putInt(pos, bitMask);				// BitMask
		pos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.putInt(pos, enumIndex);			// EnumIndex
		pos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.putInt(pos, memorySize);			// Mem Size
		pos += UnsafeAdapter.INT_SIZE;				
		UnsafeAdapter.putLong(pos,nameIndex);			// Name Index
		pos += UnsafeAdapter.LONG_SIZE;
		assert pos==HEADER_SIZE;
	}		
			
	
	/** The length of the header in bytes */
	public static final int HEADER_SIZE;
	
	static {
		int offset = 0;
		for(HeaderOffset off: HeaderOffset.values()) {
			offset += off.size;
		}
		HEADER_SIZE = offset;
	}
	
	/** The offset of this header element */
	public final int offset;
	/** The size of this header element */
	public final int size;
	
	
	/**
	 * Return the header value at the passed address
	 * @param address The address
	 * @return the value as a long (it may be an int)
	 */
	public long get(long address) {
		if(size==1) {
			return UnsafeAdapter.getByte(address + offset);
		} else if(size==4) {
			return UnsafeAdapter.getInt(address + offset);
		}
		return UnsafeAdapter.getLong(address + offset);
	}
	
	/**
	 * Sets the value of this header at the passed address
	 * @param address The starting address for these headers
	 * @param value The value to set the header to
	 */
	public void set(long address, long value) {
		if(size==1) {
			UnsafeAdapter.putByte(address + offset, (byte)value);
		} else if(size==4) {
			UnsafeAdapter.putInt(address + offset, (int)value);
		} else {
			UnsafeAdapter.putLong(address + offset, value);
		}
	}
	
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
}