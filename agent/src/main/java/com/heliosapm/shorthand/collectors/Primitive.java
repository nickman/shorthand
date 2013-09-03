/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.collectors;

import java.lang.reflect.Array;

import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: Primitive</p>
 * <p>Description: A simple enum of all primitive</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.collectors.Primitive</code></p>
 */

public enum Primitive {
//	/** A void (not really a primitive) */
//	VOID(8, Void.TYPE, Void.class, null),	
	/** A boolean primitive */
	BOOLEAN(8, Boolean.TYPE, Boolean.class, boolean[].class),
	/** A byte primitive */
	BYTE(Byte.SIZE, Byte.TYPE, Byte.class, byte[].class),
	/** A short primitive */
	SHORT(Short.SIZE, Short.TYPE, Short.class, short[].class),
	/** An integer primitive */
	INTEGER(Integer.SIZE, Integer.TYPE, Integer.class, int[].class),
	/** A float primitive */
	FLOAT(Float.SIZE, Float.TYPE, Float.class, float[].class),
	/** A long primitive */
	LONG(Long.SIZE, Long.TYPE, Long.class, long[].class),
	/** A double primitive */
	DOUBLE(Double.SIZE, Double.TYPE, Double.class, double[].class);
	
	
	private Primitive(int size, Class<?> type, Class<?> upcast, Class<?> arrayType) {
		this.size = size/8;
		this.type = type;
		this.upcast = upcast;
		this.arrayType = arrayType;
		addressOffset = UnsafeAdapter.arrayBaseOffset(arrayType); 
	}
	
	/**
	 * Converts the passed numbers to a primitive array for the passed primitive enum member
	 * @param primitive The Primitive enum member
	 * @param defaultValues The numbers to convert
	 * @return a primitive array
	 */
	public static Object toPrimitiveArray(Primitive primitive, Number...defaultValues) {
		Object arr = Array.newInstance(primitive.type, defaultValues.length);
		for(int i = 0; i < defaultValues.length; i++) {
			Array.set(arr, i, defaultValues[i]);
		}
		return arr;
	}
	
	/**
	 * Allocates a primitive array for this primitive type
	 * @param size The size of the array
	 * @return the allocated array
	 */
	public Object allocateArray(int size) {
		return Array.newInstance(type, size);
	}
	
	public Number[] convert(Object array) {
		int size = Array.getLength(array);
		Number[] nums = new Number[size];
		for(int i = 0; i < size; i++) {
			nums[i] = (Number)Array.get(array, i);
		}
		return nums;
	}
	
	
	
	public static void main(String[] args) {
		log("Primitive Test");
		for(Primitive p: Primitive.values()) {
			log(String.format("\t[%s] size:%s type:%s array:%s  array-offset:%s", p.name(), p.size, p.type.getName(), p.arrayType.getName(), p.addressOffset));
		}	
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	
	/** The byte size of this primitive */
	public final int size;
	/** The type of the primitive */
	public final Class<?> type;
	/** The type of the primitive 1 diensional array */
	public final Class<?> arrayType;
	/** The offset size to the data in a primitive 1 diensional array */
	public final long addressOffset;
	public final Class<?> upcast;
	
}
