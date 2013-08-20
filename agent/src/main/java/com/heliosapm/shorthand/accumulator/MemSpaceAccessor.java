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

import java.util.Arrays;
import java.util.Map;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.datamapper.DataMapperBuilder;
import com.heliosapm.shorthand.datamapper.DefaultDataMapper;
import com.heliosapm.shorthand.datamapper.IDataMapper;

/**
 * <p>Title: MemSpaceAccessor</p>
 * <p>Description: Pojo style ccessor for the accumulator memspace</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.MemSpaceAccessor</code></p>
 * @param <T> The enum collector type
 */

public class MemSpaceAccessor<T extends Enum<T> & ICollector<T>>  {
	/** The accumulator memspace address */
	private long address;
	/** The data mapper for the specified address */
	private IDataMapper<T> dataMapper;

	/**
	 * Creates a new MemSpaceAccessor
	 */
	public MemSpaceAccessor() {
	}
	
	/**
	 * Creates a new MemSpaceAccessor
	 * @param address The accumulator memspace address
	 */
	public MemSpaceAccessor(long address) {
		setAddress(address);		
	}
	
	/**
	 * Updates the address and the data mapper
	 * @param address The address of the mem space
	 */
	public void setAddress(long address) {
		this.address = address;
		Class<T> type = (Class<T>) EnumCollectors.getInstance().type(getEnumIndex());
		dataMapper = DataMapperBuilder.getInstance().getIDataMapper(type.getName(), getBitMask()); 
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
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		Map<T, TIntLongHashMap> dataMap = dataMapper.get(address);
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
