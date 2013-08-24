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

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.Arrays;
import java.util.Map;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.datamapper.DefaultDataMapper;
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
	
	private final long address;

	/**
	 * Creates a new MemSpaceAccessor
	 */
	private MemSpaceAccessor(long address) {
		this.address = address;
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
		long pos = address;
		UnsafeAdapter.putLong(address, 0);   	// Lock
		pos += UnsafeAdapter.LONG_SIZE;
		UnsafeAdapter.putInt(pos, bitMask);		// BitMask
		pos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.putInt(pos, enumIndex);	// EnumIndex
		pos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.putInt(pos, memorySize);	// Mem Size
		pos += UnsafeAdapter.INT_SIZE;				
		UnsafeAdapter.putLong(pos,nameIndex);	// Name Index
		pos += UnsafeAdapter.LONG_SIZE;
	}
	
	
	/**
	 * Returns the bitmask
	 * @return the bitmask
	 */
	public int getBitMask() {
		return (int)MetricSnapshotAccumulator.HeaderOffsets.BitMask.get(address);
	}

	/**
	 * Returns the collector enum index
	 * @return the collector enum index
	 */
	public int getEnumIndex() {
		return (int)MetricSnapshotAccumulator.HeaderOffsets.EnumIndex.get(address);
	}
	
	/**
	 * Returns the size of the memory space allocated in bytes
	 * @return the size of the memory space allocated
	 */
	public int getMemSize() {
		return (int)MetricSnapshotAccumulator.HeaderOffsets.MemSize.get(address);
	}
	
	/**
	 * Returns the index of the metric in the store name index
	 * @return the index of the metric in the store name index
	 */
	public long getNameIndex() {
		return MetricSnapshotAccumulator.HeaderOffsets.NameIndex.get(address);
	}
	
	/**
	 * Returns the datapoints as an array of longs keyed in the sequence of the enabled metrics.
	 * @return the datapoints
	 */
	public long[][] getDataPoints() {		
		IDataMapper dataMapper = getDataMapper();
		Map<T, TIntLongHashMap> dataMap = dataMapper.get(address);		
		final long[][] datapoints = new long[dataMap.size()][];
		int cnt = 0;
		for(Map.Entry<T, TIntLongHashMap> entry: dataMap.entrySet()) {
			long values[] = new long[entry.getKey().getSubMetricNames().length];
			for(int i = 0; i < values.length; i++) {
				values[i] = entry.getValue().get(i);
			}
			datapoints[cnt] = DefaultDataMapper.keyOrderedArray(entry.getValue());
			cnt++;
		}
		return datapoints;
	}
	
	public IDataMapper<T> getDataMapper() {
		return (IDataMapper<T>) EnumCollectors.getInstance().dataMapper(getEnumIndex(), getBitMask());
	}
	
	public void preFlush() {
		getDataMapper().preFlush(address);
	}
	
	public void reset() {
		getDataMapper().reset(address, (TObjectLongHashMap<T>) EnumCollectors.getInstance().offsets(getEnumIndex(), getBitMask()));
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Map<T, TIntLongHashMap> dataMap = getDataMapper().get(address);
		Class<T> t = (Class<T>) EnumCollectors.getInstance().type(getEnumIndex());
		StringBuilder b = new StringBuilder("[").append(EnumCollectors.getInstance().type(getEnumIndex()).getSimpleName()).append("]");
		b.append(" enumindex:").append(getEnumIndex());
		b.append(" bitmask:").append(getBitMask());
		b.append(" nameindex:").append(getNameIndex());
		b.append(" memsize:").append(getMemSize());

		for(Map.Entry<T, TIntLongHashMap> entry: dataMap.entrySet()) {
			b.append("\n\t").append(entry.getKey().name()).append(":").append(Arrays.toString(DefaultDataMapper.keyOrderedArray(entry.getValue())));
		}
		return b.toString();
	}
	

	

}
