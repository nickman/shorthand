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

import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;
import com.higherfrequencytrading.chronicle.Excerpt;
import com.higherfrequencytrading.chronicle.impl.IndexedChronicle;

/**
 * <p>Title: ChronicleDataOffset</p>
 * <p>Description: A functional enumeration of Chronicle Data Index Offsets</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.ChronicleDataOffset</code></p>
 */

public enum ChronicleDataOffset {
	/** The delete indicator */
	Delete(0, UnsafeAdapter.BYTE_SIZE),											// Offset 0									 		
	/** The name index */
	NameIndex(Delete.size + Delete.offset, UnsafeAdapter.LONG_SIZE),			// Offset 1
	/** The enum collector member ordinal */
	EnumOrdinal(NameIndex.size + NameIndex.offset, UnsafeAdapter.INT_SIZE),		// Offset 9
	/** The number of data values for the current enum ordinal */
	SubCount(EnumOrdinal.size + EnumOrdinal.offset, UnsafeAdapter.INT_SIZE);	// Offset 13
	
	// body starts at 17
	

	private ChronicleDataOffset(int offset, int size) {
		this.offset = offset;
		this.size = size;			
	}
	
	
	
	/** The offset of this chronicle field */
	public final int offset;
	/** The size of this chronicle field */
	public final int size;
	
	/** The length of the known part of the entry in bytes */
	public static final int HEADER_SIZE;
	
	/** The chronicle store */
	private final ChronicleStore<?> chronicleStore  = ChronicleStore.getInstance();

	/** The data index chronicle */
	private final IndexedChronicle chronicle = chronicleStore.tier1Data;
	

	static {
		int offset = 0;
		for(ChronicleDataOffset off: ChronicleDataOffset.values()) {
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
	 * @param index the data index chronicle index
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
	 * Returns the data points for the data index entry at the passed address
	 * @param index The index of the target data index entry
	 * @return the data points (e.g. Count, or Min,Max,Avg)
	 */
	public long[] getDataPoints(long index) {
		return getDataPoints(index, null);
	}
	
	
	/**
	 * Returns the data points for the data index entry at the passed address
	 * @param index The index of the target data index entry
	 * @param ex The excerpt to read from. If null, one will be created and closed
	 * @return the data points (e.g. Count, or Min,Max,Avg)
	 */
	public long[] getDataPoints(long index, Excerpt ex) {
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = chronicle.createExcerpt();
		}		
		try {
			ex.index(index);
			int pointCount = (int)SubCount.get(index, ex);			
			ex.position(HEADER_SIZE);
			return chronicleStore.readLongArray(ex, pointCount);
		} finally {
			if(closeEx) ex.close();
		}				
	}
	
	/**
	 * Updates the datapoints in a data index
	 * @param index The chronicle index to update
	 * @param values The values to write
	 */
	public static void updateDataIndex(long index, long[] values) {
		updateDataIndex(index, values, null);
	}
	
	
	/**
	 * Updates the datapoints in a data index
	 * @param index The chronicle index to update
	 * @param values The values to write
	 * @param ex The excerpt to write with. If null, will create a new one and close it on completion
	 */
	public static void updateDataIndex(long index, long[] values, Excerpt ex) {		
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = ChronicleStore.getInstance().tier1Data.createExcerpt();
		}		
		try {
			ex.index(index);
			ex.position(HEADER_SIZE);
			for(long v: values) {
				ex.writeLong(v);
			}
			ex.finish();
		} finally {
			if(closeEx) ex.close();
		}						
	}
		
	/**
	 * Writes a new data entry into the tier1data chronicle
	 * @param nameIndex The chronicle index of the parent name index
	 * @param ordinal The enum collector member ordinal
	 * @param defaultValues The default values for this ordinal
	 * @return the index of the new data index
	 */
	public static long writeNewDataIndex(long nameIndex, int ordinal, long[] defaultValues) {
		return writeNewDataIndex(nameIndex, ordinal, defaultValues, null);
	}
 
	
	/**
	 * Writes a new data entry into the tier1data chronicle
	 * @param nameIndex The chronicle index of the parent name index
	 * @param ordinal The enum collector member ordinal
	 * @param defaultValues The default values for this ordinal
	 * @param ex The excerpt to write with. If null, will create a new one and close it on completion
	 * @return the index of the new data index
	 */
	public static long writeNewDataIndex(long nameIndex, int ordinal, long[] defaultValues, Excerpt ex) {
		final boolean closeEx = ex==null;
		if(ex==null) {
			ex = ChronicleStore.getInstance().nameIndex.createExcerpt();			
		}
		try {
			ex.startExcerpt(HEADER_SIZE + (defaultValues.length << 3));
			ex.writeByte(0);							// the delete indicator
			ex.writeLong(nameIndex);					// the name index
			ex.writeInt(ordinal);						// the enum collector member ordinal
			ex.writeInt(defaultValues.length);			// the number of data points
			for(long v: defaultValues) {				// the default datapoint values
				ex.writeLong(v);
			}
			ex.finish();
			return ex.index();
		} finally {
			if(closeEx) ex.close();
		}
	}
	

	public static void main(String[] args) {
		log("Chronicle Data Offsets");
		for(ChronicleDataOffset off: ChronicleDataOffset.values()) {
			log(String.format("\t[%s] Offset:%s  Size:%s", off.name(), off.offset, off.size));
		}
		log("Total Header Size:" + HEADER_SIZE);
	}
	
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}


}



