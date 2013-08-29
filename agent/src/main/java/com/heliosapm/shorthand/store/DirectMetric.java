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
import java.util.EnumMap;
import java.util.Map;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.ref.RunnableReferenceQueue;
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
		int bytesForHeader = getMemAllocation(nameIndex, nameIndexEx);
		address = UnsafeAdapter.allocateMemory(bytesForHeader);
		RunnableReferenceQueue.getInstance().buildPhantomReference(this, address);
		long directPos = address;
		
		UnsafeAdapter.putInt(directPos, (int)ChronicleOffset.EnumIndex.get(this.nameIndex, nameIndexEx));		// the enum index  (0)
		directPos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.putInt(directPos, (int)ChronicleOffset.BitMask.get(this.nameIndex, nameIndexEx));		// the bitmask 	(4)
		directPos += UnsafeAdapter.INT_SIZE;

		byte[] metricName = ChronicleOffset.getName(this.nameIndex, nameIndexEx).getBytes();		

		
		
		UnsafeAdapter.putLong(directPos, ChronicleOffset.CreateTime.get(this.nameIndex, nameIndexEx));		// the create timestamp (8)
		directPos += UnsafeAdapter.LONG_SIZE;
		UnsafeAdapter.putLong(directPos, ChronicleOffset.PeriodStart.get(this.nameIndex, nameIndexEx));		// the period start timestamp (8)
		directPos += UnsafeAdapter.LONG_SIZE;		
		UnsafeAdapter.putLong(directPos, ChronicleOffset.PeriodEnd.get(this.nameIndex, nameIndexEx));		// the period end timestamp (8)
		directPos += UnsafeAdapter.LONG_SIZE;
		
		UnsafeAdapter.putInt(directPos, metricName.length);			// the size of the name		(32)
		directPos += UnsafeAdapter.INT_SIZE;
		UnsafeAdapter.copyMemory(metricName, UnsafeAdapter.BYTE_ARRAY_OFFSET, null, directPos, metricName.length);  // write the name bytes  (36)		
	}
	
	/** The size of the known part of the memory allocation */
	public static final int FIXED_SIZE;
	
	static {
		int fs = 0;
		fs += UnsafeAdapter.INT_SIZE;			// (int) the enum index
		fs += UnsafeAdapter.INT_SIZE;			// (int) the enabled bit mask
		fs += UnsafeAdapter.LONG_SIZE;			// (long) the metric creation time
		fs += UnsafeAdapter.LONG_SIZE;			// (long) the period start time
		fs += UnsafeAdapter.LONG_SIZE;			// (long) the period end time
		fs += UnsafeAdapter.INT_SIZE;			// (int) the size of the metric name
		FIXED_SIZE = fs;
	}
	
	/**
	 * Calculates the total memory required to allocate this metric
	 * @param nameIndex The name index of the metric
	 * @param nameIndexEx The excerpt to read from
	 * @return the number of bytes required
	 */
	private int getMemAllocation(long nameIndex, Excerpt nameIndexEx) {		
		return FIXED_SIZE + (int)ChronicleOffset.NameSize.get(nameIndex, nameIndexEx);
	}
	
	
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
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

	
	
	public String toString() {
		StringBuilder b = new StringBuilder("[").append(getName()).append(" (").append(getBitMask()).append(") ]");
		b.append("\n\t").append("Collector:").append(getCollectorTypeName());
		b.append("\n\t").append("Period Start:").append(getPeriodStartDate());
		b.append("\n\t").append("Period End:").append(getPeriodEndDate());
		b.append("\n\t").append("Data Points:");
		for(Map.Entry<T, IMetricDataPoint<T>> entry: getMetricDataPoints().entrySet()) {
			T t = entry.getKey();
			b.append("\n\t\t").append(t.name()).append(" (").append(t.getUnit()).append(") ");
			long[] dataPoints = entry.getValue().getDataPoints();
			String[] names = t.getSubMetricNames();
//			for(int i = 0; i < names.length; i++) {			
			for(int i = 0; i < dataPoints.length; i++) {
				b.append(names[i]).append(": ").append(dataPoints[i]).append(", ");
			}
			b.deleteCharAt(b.length()-1); b.deleteCharAt(b.length()-1);
		}
		
		return b.toString();
	}
	

	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getMetricDataPoints()
	 */
	@Override
	public Map<T, IMetricDataPoint<T>> getMetricDataPoints() {
		Map<T, IMetricDataPoint<T>> dataMap = new EnumMap<T, IMetricDataPoint<T>>((Class<T>) EnumCollectors.getInstance().type(getEnumIndex()));
		long[] dataIndexes = ChronicleOffset.getTier1Indexes(this.nameIndex);
		int dIndex = 0;
		int bitMask = getBitMask();
		for(ICollector<?> collector: EnumCollectors.getInstance().allMembersForIndex(getEnumIndex())) {
			if(collector.isEnabled(bitMask)) {
				dataMap.put((T) collector, new DirectMetricDataPoint(getEnumIndex(), getBitMask(), collector.ordinal(), dataIndexes[dIndex]));
			}
			dIndex++;
		}
		return dataMap;
	}

}
