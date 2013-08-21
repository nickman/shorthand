/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.util.unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.ProtectionDomain;

import sun.misc.Unsafe;

/**
 * <p>Title: UnsafeAdapter</p>
 * <p>Description: Adapter for {@link sun.misc.Unsafe} that detects the version and provides adapter methods for
 * the different supported signatures.</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.unsafe.UnsafeAdapter</code></p>
 */
@SuppressWarnings({"javadoc"})
public class UnsafeAdapter {
    /** The unsafe instance */    
	public static final Unsafe UNSAFE;
    /** The address size */
    public static final int ADDRESS_SIZE;
    /** Byte array offset */
    public static final int BYTES_OFFSET;
    /** Object array offset */
    public static final long OBJECTS_OFFSET;
    
    /** Indicates if the 5 param copy memory is supported */
    public static final boolean FIVE_COPY;
    /** Indicates if the 4 param set memory is supported */
    public static final boolean FOUR_SET;

    /** The size of an <b><code>int</code></b>  */
    public final static long INT_SIZE = 4;
    /** The size of an <b><code>int[]</code></b> array offset */
    public final static long INT_ARRAY_OFFSET;
    /** The size of a <b><code>long</code></b>  */
    public final static long LONG_SIZE = 8;    
    /** The size of a <b><code>long[]</code></b> array offset */
    public final static long LONG_ARRAY_OFFSET;
    /** The size of a <b><code>byte[]</code></b> array offset */
    public final static long BYTE_ARRAY_OFFSET;
    
    /** A map of SAFE memory segments */
    private static ByteBuffer allocatedMemory;
    
    /** Indicates if unsafe should be used */
    public static final boolean UNSAFE_MODE;
    
    /** System property indicating that Unsafe should be used */
    public static final String UNSAFE_MODE_PROP = "shorthand.unsafe";
    
    
    
    private UnsafeAdapter() {}
    
    private static synchronized ByteBuffer getMemory() {
    	return allocatedMemory;
    }
    
    private static synchronized ByteBuffer setMemory(long newSize) {
    	if(allocatedMemory==null) {
    		allocatedMemory = ByteBuffer.allocateDirect((int)newSize);
    	} else {
    		ByteBuffer tmp = ByteBuffer.allocateDirect((int)(allocatedMemory.capacity() + newSize));
    		allocatedMemory.position(0);
    		tmp.put(allocatedMemory);
    		allocatedMemory = tmp;    		
    	}
    	allocatedMemory.flip();
    	return allocatedMemory;
    }
    

    static {
    	UNSAFE_MODE = System.getProperty("shorthand.unsafe", "true").trim().toLowerCase().equals("true");    	   
        try {        	
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            UNSAFE = (Unsafe) theUnsafe.get(null);
            ADDRESS_SIZE = UNSAFE.addressSize();
            BYTES_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            OBJECTS_OFFSET = UNSAFE.arrayBaseOffset(Object[].class);
            INT_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(int[].class);
            LONG_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(long[].class);
            BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
            int copyMemCount = 0;
            int setMemCount = 0;
            for(Method method: Unsafe.class.getDeclaredMethods()) {
            	if("copyMemory".equals(method.getName())) copyMemCount++;
            	if("setMemory".equals(method.getName())) setMemCount++;
            }
            FIVE_COPY = copyMemCount>1;
            FOUR_SET = setMemCount>1;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }
    

    
    /**
     * Sets all bytes in a given block of memory to a fixed value
     * @param obj The target object
     * @param offset  The target object offset
     * @param bytes The numer of bytes to set
     * @param value The value to set the bytes to
     */
    public static void setMemory(Object obj, long offset, long bytes, byte value) {
    	if(!UNSAFE_MODE) { //throw new UnsupportedOperationException("setMemory(Object, long, long, byte)  is not supported in SAFE mode");
    		throw new UnsupportedOperationException("setMemory(Object, long, long, byte) where obj is not null is not supported in SAFE mode");
    	}
    	if(FOUR_SET) {
    		UNSAFE.setMemory(obj, offset, bytes, value);
    	} else {
    		UNSAFE.setMemory(offset + getAddressOf(obj), bytes, value);
    	}
    }
    
    /**
     * Returns the address of the passed object
     * @param obj The object to get the address of 
     * @return the address of the passed object or zero if the passed object is null
     */
    public static long getAddressOf(Object obj) {
    	if(!UNSAFE_MODE) throw new UnsupportedOperationException("getAddressOf(Object) is not supported in SAFE mode");
    	if(obj==null) return 0;
    	Object[] array = new Object[] {obj};
    	return ADDRESS_SIZE==4 ? UNSAFE.getInt(array, OBJECTS_OFFSET) : UNSAFE.getLong(array, OBJECTS_OFFSET);
    }

	/**
	 * @return
	 * @see sun.misc.Unsafe#addressSize()
	 */
	public static int addressSize() {
		return UNSAFE.addressSize();
	}

	/**
	 * Creates an instance of a class
	 * @param clazz The class to allocate
	 * @return the instantiated object
	 * @throws InstantiationException
	 * @see sun.misc.Unsafe#allocateInstance(java.lang.Class)
	 */
	public static Object allocateInstance(Class<?> clazz) throws InstantiationException {
		if(!UNSAFE_MODE) {
			try {
				return clazz.newInstance();
			} catch (IllegalAccessException e) {
				throw new InstantiationException("Failed to instantiate ["+ clazz.getName() + "]");
			}
		}
		return UNSAFE.allocateInstance(clazz);
	}

	/**
	 * Allocates a chunk of memory and returns its address
	 * @param size The number of bytes to allocate
	 * @return The address of the allocated memory
	 * @see sun.misc.Unsafe#allocateMemory(long)
	 */
	public static long allocateMemory(long size) {
		if(!UNSAFE_MODE) {
			throw new IllegalArgumentException("Invalid memory size request [" + size + "]");
//			long address = addressFactory.incrementAndGet();
//			ByteBuffer bb = ByteBuffer.allocateDirect((int)size);
//			memorySegments.put(address, bb);
//			return address;
		}
		return UNSAFE.allocateMemory(size);
	}

	/**
	 * Report the offset of the first element in the storage allocation of a 
	 * given array class.  If #arrayIndexScale  returns a non-zero value 
	 * for the same class, you may use that scale factor, together with this 
	 * base offset, to form new offsets to access elements of arrays of the given class.
	 * @param clazz The component type of an array class
	 * @return the base offset
	 * @see sun.misc.Unsafe#arrayBaseOffset(java.lang.Class)
	 */
	public static int arrayBaseOffset(Class<?> clazz) {
		return UNSAFE.arrayBaseOffset(clazz);
	}

	/**
	 * Report the scale factor for addressing elements in the storage allocation of a given array class.  
	 * However, arrays of "narrow" types 
	 * will generally not work properly with accessors like #getByte(Object, int) , 
	 * so the scale factor for such classes is reported as zero.
	 * @param clazz
	 * @return the index scale
	 * @see sun.misc.Unsafe#arrayIndexScale(java.lang.Class)
	 */
	public static int arrayIndexScale(Class<?> clazz) {
		return UNSAFE.arrayIndexScale(clazz);
	}

	/**
	 * Atomically update Java variable or address to x if it is currently holding expected.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expect The expected value to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapInt(java.lang.Object, long, int, int)
	 */
	public static final boolean compareAndSwapInt(Object object, long offset, int expect, int value) {
		if(!UNSAFE_MODE) {
			throw new UnsupportedOperationException("compareAndSwapInt(Object, long, int, int) with a non-null object is not supported in SAFE mode");
//			ByteBuffer bb = memorySegments.get(offset);
//			if(bb==null) throw new RuntimeException("No memory allocated at address [" + offset + "]");
//			synchronized(bb) {
//				
//			}
		}
		
		return UNSAFE.compareAndSwapInt(object, offset, expect, value);
	}

	/**
	 * Atomically update Java variable or address to x if it is currently holding expected.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expect The expected value to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapLong(java.lang.Object, long, long, long)
	 */
	public static final boolean compareAndSwapLong(Object object, long offset, long expect, long value) {
		return UNSAFE.compareAndSwapLong(object, offset, expect, value);
	}
	
	/**
	 * Atomically update Java variable or address to x if it is currently holding any of the expecteds.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expects The expected values to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapLong(java.lang.Object, long, long, long)
	 */
	public static final boolean compareMultiAndSwapLong(Object object, long offset, long value, long...expects) {
		if(expects==null || expects.length==0) return false;
		for(long expect: expects) {
			if(UNSAFE.compareAndSwapLong(object, offset, expect, value)) return true;
		}
		return false;
	}


	/**
	 * Atomically update Java variable or address to x if it is currently holding expected.
	 * @param object The object within which the variable is to be updated. Null if updating pure address.
	 * @param offset The offset from the base address at which the variable is to be updated, or the address to update the value at (if object is null) 
	 * @param expect The expected value to find in the variable or at the address
	 * @param value The value to set into the variable or address
	 * @return true if succeeded, false otherwise
	 * @see sun.misc.Unsafe#compareAndSwapObject(java.lang.Object, long, java.lang.Object, java.lang.Object)
	 */
	public static final boolean compareAndSwapObject(Object object, long offset, Object expect, Object value) {
		return UNSAFE.compareAndSwapObject(object, offset, expect, value);
	}

	/**
	 * Sets all bytes in a given block of memory to a copy of another block.
	 * Equivalent to {@code copyMemory(null, srcAddress, null, destAddress, bytes)}.
	 * @param srcAddress The address of the source
	 * @param targetAddress The address of the target
	 * @param numberOfBytes The number of bytes to copy
	 * @see sun.misc.Unsafe#copyMemory(long, long, long)
	 */
	public static void copyMemory(long srcAddress, long targetAddress, long numberOfBytes) {
		UNSAFE.copyMemory(srcAddress, targetAddress, numberOfBytes);
	}
	
    /**
     * Sets all bytes in a given block of memory to a copy of another block
     * @param srcBase The source object
     * @param srcOffset The source object offset
     * @param destBase The destination object
     * @param destOffset The destination object offset
     * @param bytes The byte count to copy
     */
    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
    	if(FIVE_COPY) {
    		UNSAFE.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    	} else {
    		UNSAFE.copyMemory(srcOffset + getAddressOf(srcBase), destOffset + getAddressOf(destBase), bytes);
    	}
    }	

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @return
	 * @see sun.misc.Unsafe#defineAnonymousClass(java.lang.Class, byte[], java.lang.Object[])
	 */
	public static Class<?> defineAnonymousClass(Class<?> arg0, byte[] arg1, Object[] arg2) {
		return UNSAFE.defineAnonymousClass(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @param arg4
	 * @param arg5
	 * @return
	 * @see sun.misc.Unsafe#defineClass(java.lang.String, byte[], int, int, java.lang.ClassLoader, java.security.ProtectionDomain)
	 */
	public static Class<?> defineClass(String arg0, byte[] arg1, int arg2, int arg3,
			ClassLoader arg4, ProtectionDomain arg5) {
		return UNSAFE.defineClass(arg0, arg1, arg2, arg3, arg4, arg5);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @param arg3
	 * @return
	 * @see sun.misc.Unsafe#defineClass(java.lang.String, byte[], int, int)
	 */
	public static Class<?> defineClass(String arg0, byte[] arg1, int arg2, int arg3) {
		return UNSAFE.defineClass(arg0, arg1, arg2, arg3);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#ensureClassInitialized(java.lang.Class)
	 */
	public static void ensureClassInitialized(Class<?> arg0) {
		UNSAFE.ensureClassInitialized(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#fieldOffset(java.lang.reflect.Field)
	 */
	public static int fieldOffset(Field arg0) {
		return UNSAFE.fieldOffset(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#freeMemory(long)
	 */
	public static void freeMemory(long arg0) {
		UNSAFE.freeMemory(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getAddress(long)
	 */
	public static long getAddress(long arg0) {
		return UNSAFE.getAddress(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getBoolean(java.lang.Object, int)
	 */
	public static boolean getBoolean(Object arg0, int arg1) {
		return UNSAFE.getBoolean(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getBoolean(java.lang.Object, long)
	 */
	public static boolean getBoolean(Object arg0, long arg1) {
		return UNSAFE.getBoolean(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getBooleanVolatile(java.lang.Object, long)
	 */
	public static boolean getBooleanVolatile(Object arg0, long arg1) {
		return UNSAFE.getBooleanVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getByte(long)
	 */
	public static byte getByte(long arg0) {
		return UNSAFE.getByte(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getByte(java.lang.Object, int)
	 */
	public static byte getByte(Object arg0, int arg1) {
		return UNSAFE.getByte(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getByte(java.lang.Object, long)
	 */
	public static byte getByte(Object arg0, long arg1) {
		return UNSAFE.getByte(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getByteVolatile(java.lang.Object, long)
	 */
	public static byte getByteVolatile(Object arg0, long arg1) {
		return UNSAFE.getByteVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getChar(long)
	 */
	public static char getChar(long arg0) {
		return UNSAFE.getChar(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getChar(java.lang.Object, int)
	 */
	public static char getChar(Object arg0, int arg1) {
		return UNSAFE.getChar(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getChar(java.lang.Object, long)
	 */
	public static char getChar(Object arg0, long arg1) {
		return UNSAFE.getChar(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getCharVolatile(java.lang.Object, long)
	 */
	public static char getCharVolatile(Object arg0, long arg1) {
		return UNSAFE.getCharVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getDouble(long)
	 */
	public static double getDouble(long arg0) {
		return UNSAFE.getDouble(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getDouble(java.lang.Object, int)
	 */
	public static double getDouble(Object arg0, int arg1) {
		return UNSAFE.getDouble(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getDouble(java.lang.Object, long)
	 */
	public static double getDouble(Object arg0, long arg1) {
		return UNSAFE.getDouble(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getDoubleVolatile(java.lang.Object, long)
	 */
	public static double getDoubleVolatile(Object arg0, long arg1) {
		return UNSAFE.getDoubleVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getFloat(long)
	 */
	public static float getFloat(long arg0) {
		return UNSAFE.getFloat(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getFloat(java.lang.Object, int)
	 */
	public static float getFloat(Object arg0, int arg1) {
		return UNSAFE.getFloat(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getFloat(java.lang.Object, long)
	 */
	public static float getFloat(Object arg0, long arg1) {
		return UNSAFE.getFloat(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getFloatVolatile(java.lang.Object, long)
	 */
	public static float getFloatVolatile(Object arg0, long arg1) {
		return UNSAFE.getFloatVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getInt(long)
	 */
	public static int getInt(long arg0) {
		return UNSAFE.getInt(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getInt(java.lang.Object, int)
	 */
	public static int getInt(Object arg0, int arg1) {
		return UNSAFE.getInt(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getInt(java.lang.Object, long)
	 */
	public static int getInt(Object arg0, long arg1) {
		return UNSAFE.getInt(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getIntVolatile(java.lang.Object, long)
	 */
	public static int getIntVolatile(Object arg0, long arg1) {
		return UNSAFE.getIntVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getLoadAverage(double[], int)
	 */
	public static int getLoadAverage(double[] arg0, int arg1) {
		return UNSAFE.getLoadAverage(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getLong(long)
	 */
	public static long getLong(long arg0) {
		return UNSAFE.getLong(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getLong(java.lang.Object, int)
	 */
	public static long getLong(Object arg0, int arg1) {
		return UNSAFE.getLong(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getLong(java.lang.Object, long)
	 */
	public static long getLong(Object arg0, long arg1) {
		return UNSAFE.getLong(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getLongVolatile(java.lang.Object, long)
	 */
	public static long getLongVolatile(Object arg0, long arg1) {
		return UNSAFE.getLongVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getObject(java.lang.Object, int)
	 */
	public static Object getObject(Object arg0, int arg1) {
		return UNSAFE.getObject(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getObject(java.lang.Object, long)
	 */
	public static Object getObject(Object arg0, long arg1) {
		return UNSAFE.getObject(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getObjectVolatile(java.lang.Object, long)
	 */
	public static Object getObjectVolatile(Object arg0, long arg1) {
		return UNSAFE.getObjectVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#getShort(long)
	 */
	public static short getShort(long arg0) {
		return UNSAFE.getShort(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#getShort(java.lang.Object, int)
	 */	
	public static short getShort(Object arg0, int arg1) {
		return UNSAFE.getShort(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getShort(java.lang.Object, long)
	 */
	public static short getShort(Object arg0, long arg1) {
		return UNSAFE.getShort(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#getShortVolatile(java.lang.Object, long)
	 */
	public static short getShortVolatile(Object arg0, long arg1) {
		return UNSAFE.getShortVolatile(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#monitorEnter(java.lang.Object)
	 */
	public static void monitorEnter(Object arg0) {
		UNSAFE.monitorEnter(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#monitorExit(java.lang.Object)
	 */
	public static void monitorExit(Object arg0) {
		UNSAFE.monitorExit(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#objectFieldOffset(java.lang.reflect.Field)
	 */
	public static long objectFieldOffset(Field arg0) {
		return UNSAFE.objectFieldOffset(arg0);
	}

	/**
	 * @return
	 * @see sun.misc.Unsafe#pageSize()
	 */
	public static int pageSize() {
		return UNSAFE.pageSize();
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#park(boolean, long)
	 */
	public static void park(boolean arg0, long arg1) {
		UNSAFE.park(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putAddress(long, long)
	 */
	public static void putAddress(long arg0, long arg1) {
		UNSAFE.putAddress(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putBoolean(java.lang.Object, int, boolean)
	 */
	public static void putBoolean(Object arg0, int arg1, boolean arg2) {
		UNSAFE.putBoolean(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putBoolean(java.lang.Object, long, boolean)
	 */
	public static void putBoolean(Object arg0, long arg1, boolean arg2) {
		UNSAFE.putBoolean(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putBooleanVolatile(java.lang.Object, long, boolean)
	 */
	public static void putBooleanVolatile(Object arg0, long arg1, boolean arg2) {
		UNSAFE.putBooleanVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the byte value at the specified address
	 * @param address The address of the target put
	 * @param value The value to put
	 * @see sun.misc.Unsafe#putByte(long, byte)
	 */
	public static void putByte(long address, byte value) {
		UNSAFE.putByte(address, value);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putByte(java.lang.Object, int, byte)
	 */
	public static void putByte(Object arg0, int arg1, byte arg2) {
		UNSAFE.putByte(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putByte(java.lang.Object, long, byte)
	 */
	public static void putByte(Object arg0, long arg1, byte arg2) {
		UNSAFE.putByte(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putByteVolatile(java.lang.Object, long, byte)
	 */
	public static void putByteVolatile(Object arg0, long arg1, byte arg2) {
		UNSAFE.putByteVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putChar(long, char)
	 */
	public static void putChar(long arg0, char arg1) {
		UNSAFE.putChar(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putChar(java.lang.Object, int, char)
	 */
	public static void putChar(Object arg0, int arg1, char arg2) {
		UNSAFE.putChar(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putChar(java.lang.Object, long, char)
	 */
	public static void putChar(Object arg0, long arg1, char arg2) {
		UNSAFE.putChar(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putCharVolatile(java.lang.Object, long, char)
	 */
	public static void putCharVolatile(Object arg0, long arg1, char arg2) {
		UNSAFE.putCharVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putDouble(long, double)
	 */
	public static void putDouble(long arg0, double arg1) {
		UNSAFE.putDouble(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putDouble(java.lang.Object, int, double)
	 */
	public static void putDouble(Object arg0, int arg1, double arg2) {
		UNSAFE.putDouble(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putDouble(java.lang.Object, long, double)
	 */
	public static void putDouble(Object arg0, long arg1, double arg2) {
		UNSAFE.putDouble(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putDoubleVolatile(java.lang.Object, long, double)
	 */
	public static void putDoubleVolatile(Object arg0, long arg1, double arg2) {
		UNSAFE.putDoubleVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putFloat(long, float)
	 */
	public static void putFloat(long arg0, float arg1) {
		UNSAFE.putFloat(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putFloat(java.lang.Object, int, float)
	 */
	public static void putFloat(Object arg0, int arg1, float arg2) {
		UNSAFE.putFloat(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putFloat(java.lang.Object, long, float)
	 */
	public static void putFloat(Object arg0, long arg1, float arg2) {
		UNSAFE.putFloat(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putFloatVolatile(java.lang.Object, long, float)
	 */
	public static void putFloatVolatile(Object arg0, long arg1, float arg2) {
		UNSAFE.putFloatVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the int value at the specified address
	 * @param address The address of the target put
	 * @param value The value to put
	 * @see sun.misc.Unsafe#putInt(long, int)
	 */
	public static void putInt(long address, int value) {
		UNSAFE.putInt(address, value);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putInt(java.lang.Object, int, int)
	 */
	public static void putInt(Object arg0, int arg1, int arg2) {
		UNSAFE.putInt(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putInt(java.lang.Object, long, int)
	 */
	public static void putInt(Object arg0, long arg1, int arg2) {
		UNSAFE.putInt(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putIntVolatile(java.lang.Object, long, int)
	 */
	public static void putIntVolatile(Object arg0, long arg1, int arg2) {
		UNSAFE.putIntVolatile(arg0, arg1, arg2);
	}

	/**
	 * Sets the long value at the specified address
	 * @param address The address of the target put
	 * @param value The value to put
	 * @see sun.misc.Unsafe#putLong(long, long)
	 */
	public static void putLong(long address, long value) {
		UNSAFE.putLong(address, value);
	}
	
	/**
	 * Sets the long values in the array starting at at the specified address
	 * @param address The address of the target put
	 * @param values The values to put
	 * @see sun.misc.Unsafe#putLong(long, long)
	 */
	public static void putLongs(long address, long[] values) {
		copyMemory(values, LONG_ARRAY_OFFSET, null, address, values.length*LONG_SIZE);		
	}
	

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putLong(java.lang.Object, int, long)
	 */
	public static void putLong(Object arg0, int arg1, long arg2) {
		UNSAFE.putLong(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putLong(java.lang.Object, long, long)
	 */
	public static void putLong(Object arg0, long arg1, long arg2) {
		UNSAFE.putLong(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putLongVolatile(java.lang.Object, long, long)
	 */
	public static void putLongVolatile(Object arg0, long arg1, long arg2) {
		UNSAFE.putLongVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putObject(java.lang.Object, int, java.lang.Object)
	 */
	public static void putObject(Object arg0, int arg1, Object arg2) {
		UNSAFE.putObject(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putObject(java.lang.Object, long, java.lang.Object)
	 */
	public static void putObject(Object arg0, long arg1, Object arg2) {
		UNSAFE.putObject(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putObjectVolatile(java.lang.Object, long, java.lang.Object)
	 */
	public static void putObjectVolatile(Object arg0, long arg1, Object arg2) {
		UNSAFE.putObjectVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putOrderedInt(java.lang.Object, long, int)
	 */
	public static void putOrderedInt(Object arg0, long arg1, int arg2) {
		UNSAFE.putOrderedInt(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putOrderedLong(java.lang.Object, long, long)
	 */
	public static void putOrderedLong(Object arg0, long arg1, long arg2) {
		UNSAFE.putOrderedLong(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putOrderedObject(java.lang.Object, long, java.lang.Object)
	 */
	public static void putOrderedObject(Object arg0, long arg1, Object arg2) {
		UNSAFE.putOrderedObject(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @see sun.misc.Unsafe#putShort(long, short)
	 */
	public static void putShort(long arg0, short arg1) {
		UNSAFE.putShort(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @deprecated
	 * @see sun.misc.Unsafe#putShort(java.lang.Object, int, short)
	 */
	public static void putShort(Object arg0, int arg1, short arg2) {
		UNSAFE.putShort(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putShort(java.lang.Object, long, short)
	 */
	public static void putShort(Object arg0, long arg1, short arg2) {
		UNSAFE.putShort(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#putShortVolatile(java.lang.Object, long, short)
	 */
	public static void putShortVolatile(Object arg0, long arg1, short arg2) {
		UNSAFE.putShortVolatile(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @return
	 * @see sun.misc.Unsafe#reallocateMemory(long, long)
	 */
	public static long reallocateMemory(long arg0, long arg1) {
		return UNSAFE.reallocateMemory(arg0, arg1);
	}

	/**
	 * @param arg0
	 * @param arg1
	 * @param arg2
	 * @see sun.misc.Unsafe#setMemory(long, long, byte)
	 */
	public static void setMemory(long arg0, long arg1, byte arg2) {
		UNSAFE.setMemory(arg0, arg1, arg2);
	}

	/**
	 * @param arg0
	 * @return
	 * @deprecated
	 * @see sun.misc.Unsafe#staticFieldBase(java.lang.Class)
	 */
	public static Object staticFieldBase(Class<?> arg0) {
		return UNSAFE.staticFieldBase(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#staticFieldBase(java.lang.reflect.Field)
	 */
	public static Object staticFieldBase(Field arg0) {
		return UNSAFE.staticFieldBase(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#staticFieldOffset(java.lang.reflect.Field)
	 */
	public static long staticFieldOffset(Field arg0) {
		return UNSAFE.staticFieldOffset(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#throwException(java.lang.Throwable)
	 */
	public static void throwException(Throwable arg0) {
		UNSAFE.throwException(arg0);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "UnsafeAdapter";
	}

	/**
	 * @param arg0
	 * @return
	 * @see sun.misc.Unsafe#tryMonitorEnter(java.lang.Object)
	 */
	public static boolean tryMonitorEnter(Object arg0) {
		return UNSAFE.tryMonitorEnter(arg0);
	}

	/**
	 * @param arg0
	 * @see sun.misc.Unsafe#unpark(java.lang.Object)
	 */
	public static void unpark(Object arg0) {
		UNSAFE.unpark(arg0);
	}
    
    
    

}
