/**
 * Helios Development Group LLC, 2010
 */
package com.heliosapm.shorthand.instrumentor;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.*;
import java.util.*;
 
/**
 * <p>Title: ObjectSizer</p>
 * <p>Description: Captures the deep size of an Object</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Dr. Heinz M. Kabutz (heinz@javaspecialists.eu) 
 * @author Whitehead
 * @version $LastChangedRevision$
 * <p><code>com.heliosapm.shorthand.instrumentor.ObjectSizer</code></p>
 */
public class ObjectSizer {
    /** The agent's instrumentation instance */
    private Instrumentation instrumentation;
 
    /**
     * Creates a new ObjectSizer
     * @param instrumentation The agent's instrumentation instance
     */
    ObjectSizer(Instrumentation instrumentation) {
        if (instrumentation == null) {
            throw new IllegalStateException(
                    "Instrumentation environment not initialised.");
        }         
        this.instrumentation = instrumentation;
    }
     
    /**
     * Returns the object "shallow" size
     * @param obj The object to size
     * @return the size of the object
     */
    public long sizeOf(Object obj) {
        if(obj==null) return 0L;
        if (isSharedFlyweight(obj)) {
            return 0;
        }
        return instrumentation.getObjectSize(obj);
    }
 
    /**
     * Returns the "deep" size of the object
     * @param obj the object to size
     * @return the "deep" size of the object
     */
    public long deepSizeOf(Object obj) {
        if(obj==null) return 0L;
        Map<Object, Object> visited = new IdentityHashMap<Object, Object>();
        Stack<Object> stack = new Stack<Object>();
        stack.push(obj);
 
        long result = 0;
        do {
            result += internalSizeOf(stack.pop(), stack, visited);
        } while (!stack.isEmpty());
        return result;
    }
 
    /**
     * Returns true if this is a well-known shared flyweight. For example, interned Strings, Booleans and Number objects 
     * @param obj the object to test
     * @return true of the passed object is a well-known shared flyweight
     */
    private boolean isSharedFlyweight(Object obj) {
        // optimization - all of our flyweights are Comparable
        if (obj instanceof Comparable) {
            if (obj instanceof Enum) {
                return true;
            } else if (obj instanceof String) {
                return (obj == ((String) obj).intern());
            } else if (obj instanceof Boolean) {
                return (obj == Boolean.TRUE || obj == Boolean.FALSE);
            } else if (obj instanceof Integer) {
                return (obj == Integer.valueOf((Integer) obj));
            } else if (obj instanceof Short) {
                return (obj == Short.valueOf((Short) obj));
            } else if (obj instanceof Byte) {
                return (obj == Byte.valueOf((Byte) obj));
            } else if (obj instanceof Long) {
                return (obj == Long.valueOf((Long) obj));
            } else if (obj instanceof Character) {
                return (obj == Character.valueOf((Character) obj));
            }
        }
        return false;
    }
 
    /**
     * Determines if the visit to an object
     * @param obj The object to skip
     * @param visited the visited object map
     * @return true if the object should be skipped
     */
    private boolean skipObject(Object obj, Map<Object, Object> visited) {
        return obj == null || visited.containsKey(obj)
                || isSharedFlyweight(obj);
    }
 
    /**
     * Returns the cumulative size of all the fields in an object
     * @param obj The object to size
     * @param stack the tracking stack
     * @param visited the visited object map
     * @return the internal size of the passed object
     */
    private long internalSizeOf(Object obj, Stack<Object> stack, Map<Object, Object> visited) {
        if (skipObject(obj, visited)) {
            return 0;
        }
 
        Class<?> clazz = obj.getClass();
        if (clazz.isArray()) {
            addArrayElementsToStack(clazz, obj, stack);
        } else {
            // add all non-primitive fields to the stack
            while (clazz != null) {
                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (!Modifier.isStatic(field.getModifiers())
                            && !field.getType().isPrimitive()) {
                        field.setAccessible(true);
                        try {
                            stack.add(field.get(obj));
                        } catch (IllegalAccessException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
        }
        visited.put(obj, null);
        return sizeOf(obj);
    }
 
    /**
     * Adds an array item to the tracking stack
     * @param clazz The class type of the array
     * @param obj the array instance
     * @param stack the tracking stack
     */
    private void addArrayElementsToStack(Class<?> clazz, Object obj, Stack<Object> stack) {
        if (!clazz.getComponentType().isPrimitive()) {
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i++) {
                stack.add(Array.get(obj, i));
            }
        }
    }
} 