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
package com.heliosapm.shorthand.store;

import com.heliosapm.shorthand.accumulator.PeriodClock;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: ChronicleOffset</p>
 * <p>Description: A functional enumeration of Chronicle Name Index Offsets</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.ChronicleOffset</code></p>
 */

public enum ChronicleOffset {
	/** The entry  lock */
	Lock(0, UnsafeAdapter.BYTE_SIZE),											// Offset 0									 		
	/** The delete indicator */
	Delete(Lock.size + Lock.offset, UnsafeAdapter.BYTE_SIZE),					// Offset 1
	/** The enum index */
	EnumIndex(Delete.size + Delete.offset, UnsafeAdapter.INT_SIZE),				// Offset 2
	/** The bitmask index */
	BitMask(EnumIndex.size + EnumIndex.offset, UnsafeAdapter.INT_SIZE),			// Offset 6
	/** The creation time  */
	CreateTime(BitMask.size + BitMask.offset, UnsafeAdapter.LONG_SIZE),			// Offset 10
	/** The period start time  */
	PeriodStart(CreateTime.size + CreateTime.offset, UnsafeAdapter.LONG_SIZE),	// Offset 18
	/** The period end time  */
	PeriodEnd(PeriodStart.size + PeriodStart.offset, UnsafeAdapter.LONG_SIZE),	// Offset 26 
	/** The metric name size  */
	NameSize(PeriodEnd.size + PeriodEnd.offset, UnsafeAdapter.INT_SIZE), 		// Offset 34
	/** The number of data indexes */
	Enabled(NameSize.size + NameSize.offset, UnsafeAdapter.INT_SIZE);			// Offset 38
	
	// tier1indexes start at offset 42
	// 
	
	public static long writeNewNameIndex(int enumIndex, int bitMask, String metricName, Excerpt ex) {
		final boolean closeEx = ex==null;
		long[] periods = PeriodClock.getInstance().getCurrentPeriod();
		if(ex==null) {
			ex = ChronicleStore.getInstance().nameIndex.createExcerpt();			
		}
		try {
			int dataIndexCount = EnumCollectors.getInstance().type(enumIndex).getEnumConstants().length;
			ex.startExcerpt(HEADER_SIZE + metricName.getBytes().length + 1 + (dataIndexCount << 3));
			ex.writeByte(0);							// the lock
			ex.writeByte(0);							// the delete indicator
			ex.writeInt(enumIndex);						// the enum index (i.e. which enum it is)
			ex.writeInt(bitMask);						// the enabled bitmask			
			ex.writeLong(System.currentTimeMillis());	// the creation timestamp
			ex.writeLong(periods[0]);					// the period start time.
			ex.writeLong(periods[1]);					// the period end time.
			ex.writeInt(metricName.getBytes().length);	// the number of bytes in the metric name
			ex.writeInt(dataIndexCount);				// the number of data indexes
			ex.write(metricName.getBytes());			// the metric name bytes
			for(int i = 0; i < dataIndexCount; i++) {	// the data index place holders
				ex.writeLong(-1L);
			}
			ex.finish();
			long nameIndex = ex.index();
			
			return nameIndex;
		} finally {
			if(closeEx) ex.close();
		}
	}
	
	
	
	private ChronicleOffset(int offset, int size) {
		this.offset = offset;
		this.size = size;			
	}
	
	
	
	/** The offset of this chronicle field */
	public final int offset;
	/** The size of this chronicle field */
	public final int size;
	
	/** The length of the known part of the entry in bytes */
	public static final int HEADER_SIZE;

	/** The name index chronicle store */
	private final ChronicleStore<?> chronicleStore  = ChronicleStore.getInstance();

	/** The name index chronicle */
	private final IndexedChronicle chronicle = chronicleStore.nameIndex;
	
	static {
		int offset = 0;
		for(ChronicleOffset off: ChronicleOffset.values()) {
			offset += off.size;
		}
		HEADER_SIZE = offset;
	}
	
	/**
	 * Return the chronicle field from the passed index
	 * @param index the name index chronicle index
	 * @return the value as a long (it may be an int)
	 */
	public long get(long index) {
		return get(index, null);
	}
	
	
	/**
	 * Return the chronicle field from the passed index
	 * @param index the name index chronicle index
	 * @param ex The excerpt to read from. If null, one will be created and closed
	 * @return the value as a long (it may be an int)
	 */
	public long get(long index, Excerpt ex) {
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = chronicle.createExcerpt();
		}		
		try {
			ex.index(index);
			if(size==1) {
				return ex.readByte(offset);
			} else if(size==4) {
				return ex.readInt(offset);
			}
			return ex.readLong(offset);
		} finally {
			if(closeEx) ex.close();
		}
	}
	
	/**
	 * Writes a value to a chronicle field
	 * @param value The value to write
	 * @param index The index of the chronicle to write to
	 */
	public void set(long value, long index) {
		set(value, index, null);
	}
	
	/**
	 * Writes a value to a chronicle field
	 * @param value The value to write
	 * @param index The index of the chronicle to write to
	 * @param ex The excerpt to read from. If null, one will be created and closed
	 */
	public void set(long value, long index, Excerpt ex) {
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = chronicle.createExcerpt();
		}		
		try {
			ex.index(index);
			if(size==1) {
				ex.write(offset, (byte)value);				
			} else if(size==4) {
				ex.writeInt(offset, (int)value);
			} else {
				ex.writeLong(offset, value);
			}
			ex.toEnd();
			ex.finish();
		} finally {
			if(closeEx) ex.close();
		}
		
	}
	
	/**
	 * Returns the tier1 indexes for the name index entry at the passed address
	 * @param index The index of the target name index entry
	 * @return the tier 1 indexes
	 */
	public long[] getTier1Indexes(long index) {
		return getTier1Indexes(index, null);
	}
	
	
	/**
	 * Returns the tier1 indexes for the name index entry at the passed address
	 * @param index The index of the target name index entry
	 * @param ex The excerpt to read from. If null, one will be created and closed
	 * @return the tier 1 indexes
	 */
	public long[] getTier1Indexes(long index, Excerpt ex) {
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = chronicle.createExcerpt();
		}		
		try {
			ex.index(index);
			int indexCount = (int)Enabled.get(index, ex);			
			ex.position(HEADER_SIZE);
			return chronicleStore.readLongArray(ex, indexCount);
		} finally {
			if(closeEx) ex.close();
		}				
	}
	
	/**
	 * Returns the metric name for the name index entry at the passed address
	 * @param index The index of the target name index entry
	 * @param ex The excerpt to read from. If null, one will be created and closed
	 * @return the metric name
	 */
	public String getName(long index, Excerpt ex) {
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = chronicle.createExcerpt();
		}		
		try {
			ex.index(index);
			int indexCount = (int)Enabled.get(index, ex);
			ex.skip(indexCount << 3);
			return ex.readByteString();
		} finally {
			if(closeEx) ex.close();
		}		
	}
	
	/**
	 * Returns the metric name for the name index entry at the passed address
	 * @param index The index of the target name index entry
	 * @return the metric name
	 */
	public String getName(long index) {
		return getName(index, null);
	}	

	
	public static void main(String[] args) {
		log("Chronicle Offsets");
		for(ChronicleOffset off: ChronicleOffset.values()) {
			log(String.format("\t[%s] Offset:%s  Size:%s", off.name(), off.offset, off.size));
		}
		log("Total Header Size:" + HEADER_SIZE);
	}
	
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	


}
