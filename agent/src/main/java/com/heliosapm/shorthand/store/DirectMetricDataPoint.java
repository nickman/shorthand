/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package com.heliosapm.shorthand.store;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: DirectMetricDataPoint</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.DirectMetricDataPoint</code></p>
 * @param <T> The enum collector type
 */

public class DirectMetricDataPoint<T extends Enum<T> & ICollector<T>> implements IMetricDataPoint<T> {
	/** The address of the memory allocated for this instance */
	protected final long address;
	
	/** The offset of the enum index */
	public static int ENUM_OFFSET = 0;
	/** The offset of the bitmask */
	public static int BITMASK_OFFSET = ENUM_OFFSET + UnsafeAdapter.INT_SIZE;
	/** The offset of the enum collector member ordinal */
	public static int ORDINAL_OFFSET = BITMASK_OFFSET + UnsafeAdapter.INT_SIZE;
	/** The offset of the chronicle data index */
	public static int INDEX_OFFSET = ORDINAL_OFFSET + UnsafeAdapter.INT_SIZE;
	
	/**
	 * Creates a new DirectMetricDataPoint
	 * @param enumIndex The enum collector index
	 * @param bitMask The enabled metric bitmask
	 * @param ordinal The enum collector member instance ordinal
	 * @param dataIndex The index of the target data chronicle entry
	 */
	public DirectMetricDataPoint(int enumIndex, int bitMask, int ordinal, long dataIndex) {
		address = UnsafeAdapter.allocateMemory((3 << 2) + UnsafeAdapter.LONG_SIZE);
		UnsafeAdapter.putIntArray(address, new int[]{enumIndex, bitMask, ordinal});
		UnsafeAdapter.putLong(address + INDEX_OFFSET, dataIndex);
	}
	
	private int enumIndex() {
		return UnsafeAdapter.getInt(address);
	}
	
	private int bitMask() {
		return UnsafeAdapter.getInt(address + BITMASK_OFFSET);
	}

	private int ordinal() {
		return UnsafeAdapter.getInt(address + ORDINAL_OFFSET);
	}

	private long dIndex() {
		return UnsafeAdapter.getLong(address + INDEX_OFFSET);
	}
	
	/**
	 * <p>Releases the memory allocated for this guy</p>
	 * {@inheritDoc}
	 * @see java.lang.Object#finalize()
	 */
	@Override
	protected void finalize() throws Throwable {
		UnsafeAdapter.freeMemory(address);
		super.finalize();
	}
	
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getCollectorName()
	 */
	@Override
	public String getCollectorName() {
		return EnumCollectors.getInstance().memberForIndex(enumIndex(), ordinal()).name();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getShortName()
	 */
	@Override
	public String getShortName() {		
		return EnumCollectors.getInstance().memberForIndex(enumIndex(), ordinal()).getShortName();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getUnit()
	 */
	@Override
	public String getUnit() {		
		return EnumCollectors.getInstance().memberForIndex(enumIndex(), ordinal()).getUnit();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getSubNames()
	 */
	@Override
	public String[] getSubNames() {
		return EnumCollectors.getInstance().memberForIndex(enumIndex(), ordinal()).getSubMetricNames();
	}


	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getDataPoints()
	 */
	@Override
	public long[] getDataPoints() {
		return ChronicleDataOffset.getDataPoints(dIndex());
	}




}
