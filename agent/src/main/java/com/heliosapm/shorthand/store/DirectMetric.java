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

import java.util.Date;
import java.util.Map;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;
import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * <p>Title: DirectMetric</p>
 * <p>Description: An IMetric implementation that reads directly from the underlying chronicle</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.DirectMetric</code></p>
 * @param <T> The expected enum collector type
 */

public class DirectMetric<T extends Enum<T> & ICollector<T>> implements IMetric<T> {
	/** The name index of the metric */
	protected final long nameIndex;
	/** The allocated direct address for this metric */
	protected final long address;
	
	/**
	 * Creates a new DirectMetric
	 * @param nameIndex The name index of the metric
	 * @param nameIndexEx An excerpt to read from the name index
	 * @param dataPointsEx An excerpt to read from the data points
	 */
	public DirectMetric(long nameIndex, Excerpt nameIndexEx, Excerpt dataPointsEx) {
		this.nameIndex = nameIndex;
		nameIndexEx.index(nameIndex);
		int bytesForHeader = walkForSize(nameIndexEx);
		address = UnsafeAdapter.allocateMemory(bytesForHeader);
		long directPos = address;
		nameIndexEx.position(2); 	// skip the the lock byte and the deleted indicator byte
		
		UnsafeAdapter.putInt(directPos, nameIndexEx.readInt());		// the enum index  (0)
		directPos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.putInt(directPos, nameIndexEx.readInt());		// the bitmask 	(4)
		directPos += UnsafeAdapter.INT_SIZE;

		byte[] metricName = nameIndexEx.readByteString().getBytes();// read the name		

		
		UnsafeAdapter.putLong(directPos, nameIndexEx.readLong());		// the create timestamp (8)
		directPos += UnsafeAdapter.LONG_SIZE;
		UnsafeAdapter.putLong(directPos, nameIndexEx.readLong());		// the period start timestamp  (16)
		directPos += UnsafeAdapter.LONG_SIZE;		
		UnsafeAdapter.putLong(directPos, nameIndexEx.readLong());		// the period end timestamp  (24)
		directPos += UnsafeAdapter.LONG_SIZE;
		
		UnsafeAdapter.putInt(directPos, metricName.length);			// the size of the name		(32)
		directPos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.copyMemory(metricName, UnsafeAdapter.BYTE_ARRAY_OFFSET, null, directPos, metricName.length);  // write the name bytes  (36)
//		directPos += metricName.length;
		
		
//		nameIndexEx.writeInt(tier1AddressCount);// (int) the number of tier addresses
//		final int pos = nameIndexEx.position(); 
//		for(int i = 0; i < tier1AddressCount; i++) {
//			nameIndexEx.writeLong(-3L);
//		}
	}
	
	/** The known portion length of a name index entry */
	public static final int NAME_ENTRY_SIZE = 
			4 + 				// (int) collector enum index
			4 + 				// (int) the enabled bitmask
			4 +					// (int) the number of bytes in te metric name
			8 + 				// (long) the created timestamp
			8 + 				// (long) the period start time
			8;	 				// (long) the period end time

	/** The position to jump to when reading the name */
	public static final int NAME_START_POS =
			1 + 				// (byte)) The lock byte
			1 + 				// (byte)) The deleted indicator byte
			4 + 				// (int) collector enum index
			4; 					// (int) the enabled bitmask

	
	
	private int walkForSize(Excerpt nameIndexEx) {		
		nameIndexEx.position(NAME_START_POS);
		String name = nameIndexEx.readByteString();	// read the metric name
		return 
				name.getBytes().length + 			// the byte count of the name
				NAME_ENTRY_SIZE; 					// the known size of the rest of the allocation
		
		
		
	}
	
	
	/**
	 * Returns the enum index
	 * @return the enum index
	 */
	public int getEnumIndex() {
		return UnsafeAdapter.getInt(address);
	}
	
	/**
	 * Returns the collector name
	 * @return the collector name
	 */
	public String getCollectorTypeName() {
		return EnumCollectors.getInstance().type(getEnumIndex()).getSimpleName();
	}
	
	/**
	 * Returns the number of bytes in the metric name
	 * @return the number of bytes in the metric name
	 */
	public int getMetricNameSize() {
		return UnsafeAdapter.getInt(address + 32);
	}
	
	/**
	 * Returns the creation timestamp 
	 * @return the creation timestamp 
	 */
	public long getCreateTime() {
		return UnsafeAdapter.getLong(address + 8);
	}
	
	/**
	 * Returns the period start timestamp 
	 * @return the period start timestamp 
	 */
	public long getPeriodStart() {
		return UnsafeAdapter.getLong(address + 16);
	}
	/**
	 * Returns the period end timestamp 
	 * @return the period endtimestamp 
	 */
	public long getPeriodEnd() {
		return UnsafeAdapter.getLong(address + 24);
	}
	
	/**
	 * Returns the creation date 
	 * @return the creation date 
	 */
	public Date getCreateDate() {
		return new Date(getCreateTime());
	}
	
	/**
	 * Returns the period start date 
	 * @return the period start date 
	 */
	public Date getPeriodStartDate() {
		return new Date(getPeriodStart());
	}
	
	
	/**
	 * Returns the period end timestamp 
	 * @return the period endtimestamp 
	 */
	public Date getPeriodEndDate() {
		return new Date(getPeriodEnd());
	}
	
	
	
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	public String getName() {
		byte[] bytes = new byte[getMetricNameSize()];
		UnsafeAdapter.copyMemory(null, address + 36, bytes, UnsafeAdapter.BYTE_ARRAY_OFFSET, bytes.length);
		return new String(bytes);
	}
	
	/**
	 * Returns the bitmask
	 * @return the bitmask
	 */
	public int getBitMask() {
		return UnsafeAdapter.getInt(address + UnsafeAdapter.INT_SIZE);
	}
	
	public void finalize() throws Throwable {
		UnsafeAdapter.freeMemory(address);
		super.finalize();
	}
	
	
	public String toString() {
		StringBuilder b = new StringBuilder("[").append(getName()).append(" (").append(getBitMask()).append(") ]");
		b.append("\n\t").append("Collector:").append(getCollectorTypeName());
		b.append("\n\t").append("Period Start:").append(getPeriodStartDate());
		b.append("\n\t").append("Period End:").append(getPeriodEndDate());
		
		
		return b.toString();
	}
	

	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getMetricDataPoints()
	 */
	@Override
	public Map<T, IMetricDataPoint<T>> getMetricDataPoints() {
		// TODO Auto-generated method stub
		return null;
	}

}
