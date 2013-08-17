/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.collectors;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: DataStruct</p>
 * <p>Description: An encapsulated definition of a metric's field type and size</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.collectors.DataStruct</code></p>
 */

public class DataStruct {
	/** The type of the struct */
	public final Primitive type;
	/** The size of the struct (the number of primitives) */
	public final int size;
	/** The byte size of the struct (the number of bytes an instance will occupy) */
	public final int byteSize;
	/** The default values for the data struct when allocated */
	public final Object defaultValues;
	
	/** The toString value */
	public final String ts;
	
	/** A map of all created datastructs keyed by the type and size */
	private static final Map<String, DataStruct> structs = new ConcurrentHashMap<String, DataStruct>();
	
	/**
	 * Returns a datastruct for the passed type and size
	 * @param type The type of the struct
	 * @param size The number of type instances in the struct
	 * @param defaultValues The default values for this data struct
	 * @return the struct
	 */
	public static DataStruct getInstance(Primitive type, int size, Number...defaultValues) {
		if(defaultValues.length != size) throw new IllegalArgumentException("Size and number of default values were not equal");
		String key = type.name() + size + Arrays.toString(defaultValues);
		DataStruct ds = structs.get(key);
		if(ds==null) {
			synchronized(structs) {
				ds = structs.get(key);
				if(ds==null) {
					ds = new DataStruct(type, size, defaultValues);
					structs.put(key, ds);
				}
			}
		}
		return ds;
	}
	
	/**
	 * Creates a new DataStruct
	 * @param type The type of the struct
	 * @param size The size of the struct (the number of primitives)
	 * @param defaultValues The default values for this DataStruct to initialize the memory space
	 */
	protected DataStruct(Primitive type, int size, Number...defaultValues) {		
		this.type = type;
		this.size = size;
		byteSize = size * type.size;
		ts = String.format("DataStruct[type:%s size:%s byteSize:%s]", type.name().toLowerCase(), size, byteSize);
		this.defaultValues = Primitive.toPrimitiveArray(type, defaultValues);
	}
	
	public Number[] readMemorySpace(long address, long offset) {
		Object array = type.allocateArray(size);
		UnsafeAdapter.copyMemory(null, address+offset, array, type.addressOffset, size*type.size);
		return type.convert(array);
	}
	
	public static void main(String[] args) {
		log("Test readMemorySpace");
		long address = -1;
		try {
			int alloc = 3*8;
			address = UnsafeAdapter.allocateMemory(alloc);
			for(int i = 0; i < alloc; i+=8) {
				UnsafeAdapter.putLong(address+i, i);
			}
			log("Allocated");
			DataStruct ds = new DataStruct(Primitive.LONG, 3);
			Number[] nums = ds.readMemorySpace(address, 0);
			log("Values:" + Arrays.toString(nums));
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		} finally {
			if(address!=-1) UnsafeAdapter.freeMemory(address);
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return ts;
	}
	
	/**
	 * Returns the toString value with the default values appended
	 * @return the toString value with the default values appended
	 */
	public String dump() {
		StringBuilder b = new StringBuilder(" Defaults: [");
		for(int i = 0; i < size; i++) {
			b.append(Array.get(defaultValues, i)).append(",");
		}
		b.deleteCharAt(b.length()-1).append("]");
		return ts + b.toString();
	}
	
	/**
	 * Returns the byte size of the struct (the number of bytes an instance will occupy)
	 * @return the byteSize
	 */
	public int getByteSize() {
		return byteSize;
	}

	
	/**
	 * Returns The type of the struct
	 * @return the type
	 */
	public Primitive getType() {
		return type;
	}
	
	/**
	 * Returns The size of the struct (the number of primitives) 
	 * @return the size
	 */
	public int getSize() {
		return size;
	}

	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + size;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DataStruct other = (DataStruct) obj;
		if (size != other.size) {
			return false;
		}
		if (type != other.type) {
			return false;
		}
		return true;
	}


	
	
}
