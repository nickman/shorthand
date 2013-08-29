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

import java.util.Arrays;
import java.util.Set;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.datamapper.AbstractDataMapper;
import com.heliosapm.shorthand.datamapper.IDataMapper;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MemSpaceAccessor</p>
 * <p>Description: Pojo style accessor for the accumulator memspace</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.MemSpaceAccessor</code></p>
 * @param <T> The enum collector type
 */

public class MemSpaceAccessor<T extends Enum<T> & ICollector<T>>  {
//	/** The address for the mem-space in focus for this thread */
//	private static final ThreadLocal<long[]> _address = new ThreadLocal<long[]>() {
//		@Override
//		protected long[] initialValue() {
//			return new long[1];
//		}
//	};
//	/** The single accessor instance */
//	private static final MemSpaceAccessor INSTANCE = new MemSpaceAccessor();
	
	/** The reset and untouched flag value */
	public static final byte UNTOUCHED = 0; 
	
	
	private long address;

	/**
	 * Creates a new MemSpaceAccessor
	 */
	private MemSpaceAccessor(long address) {
		this.address = address;
	}
	
	public void setAddress(long address) {
		this.address =address;
	}
	
	/**
	 * Indicates if this mem-space has been invalidated
	 * @return true if this mem-space has been invalidated, false otherwise
	 */
	public boolean isInvalidated() {
		return UnsafeAdapter.getLong(address + UnsafeAdapter.LONG_SIZE)==-1L;
	}
	
	/**
	 * Deletes the mem-space associated to this accessor.
	 * SHOULD ONLY BE CALLED ON AN INVALIDATED ACCESSOR !!.
	 */
	public void delete() {
		UnsafeAdapter.freeMemory(address);
		address = -1L;
	}
	

	/**
	 * Sets the address for the current thread
	 * @param address The address of the memspace to access
	 * @return a MemSpaceAccessor set to the passed address for the current thread
	 */
	public static MemSpaceAccessor get(long address) {
		return new MemSpaceAccessor(address);
	}
	
	/**
	 * Initializes the header of the memory space allocated for a new metric
	 * @param memorySize The amount of memory allocated
	 * @param nameIndex The name index of the new metric
	 * @param bitMask The enabled bitmask of the new metric
	 * @param enumIndex The enum collector index
	 */
	public void initializeHeader(int memorySize, long nameIndex, int bitMask, int enumIndex) {
		HeaderOffset.initializeHeader(address, memorySize, nameIndex, bitMask, enumIndex);
	}
	
	/**
	 * Determines if this mem-space has been touched since the last reset
	 * @return true if this mem-space has been touched since the last reset, false otherwise
	 */
	public boolean isTouched() {
		return UnsafeAdapter.getByte(address + HeaderOffset.Touch.offset)>0;
	}
	
	public long copy() {
		long addr = UnsafeAdapter.allocateMemory(getMemSize());
		UnsafeAdapter.copyMemory(address, addr, getMemSize());
		long currentAddress = address;
		setAddress(addr);
		reset();
		address = currentAddress;
		return addr;
				
	}
	
	
	/**
	 * Returns the bitmask
	 * @return the bitmask
	 */
	public int getBitMask() {
		return (int)HeaderOffset.BitMask.get(address);
	}

	/**
	 * Returns the collector enum index
	 * @return the collector enum index
	 */
	public int getEnumIndex() {
		return (int)HeaderOffset.EnumIndex.get(address);
	}
	
	/**
	 * Returns the size of the memory space allocated in bytes
	 * @return the size of the memory space allocated
	 */
	public int getMemSize() {
		return (int)HeaderOffset.MemSize.get(address);
	}
	
	/**
	 * Returns the index of the metric in the store name index
	 * @return the index of the metric in the store name index
	 */
	public long getNameIndex() {
		return HeaderOffset.NameIndex.get(address);
	}
	
	/**
	 * Returns the datapoints as an array of longs keyed in the sequence of the enabled metrics.
	 * @return the datapoints
	 */
	public long[][] getDataPoints() {		
		return getDataMapper().getDataPoints(address);
	}
	
	public IDataMapper<T> getDataMapper() {
		return (IDataMapper<T>) EnumCollectors.getInstance().dataMapper(getEnumIndex(), getBitMask());
	}
	
	public void preFlush() {
		getDataMapper().preFlush(address);
	}
	
	public void reset() {
		getDataMapper().reset(address);		
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		long[][] datapoints = getDataMapper().getDataPoints(address);
		Set<T> enabled = (Set<T>) EnumCollectors.getInstance().enabledMembersForIndex(getEnumIndex(), getBitMask());
		StringBuilder b = new StringBuilder("[").append(EnumCollectors.getInstance().type(getEnumIndex()).getSimpleName()).append("]");
		b.append(" enumindex:").append(getEnumIndex());
		b.append(" bitmask:").append(getBitMask());
		b.append(" nameindex:").append(getNameIndex());
		b.append(" memsize:").append(getMemSize());
		int index = 0;
		for(T t: enabled) {
			b.append("\n\t").append(t.name()).append(":").append(Arrays.toString(datapoints[index]));
			index++;
		}
		return b.toString();
	}
	

	

}
