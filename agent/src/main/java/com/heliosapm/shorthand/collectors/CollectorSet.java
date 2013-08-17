
package com.heliosapm.shorthand.collectors;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.EnumSet;
import java.util.Set;

import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.datamapper.DataMapperBuilder;
import com.heliosapm.shorthand.datamapper.DefaultDataMapper;
import com.heliosapm.shorthand.datamapper.IDataMapper;

/**
 * <p>Title: CollectorSet</p>
 * <p>Description: Wraps a {@link ICollector} implementing enum</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.collectors.CollectorSet</code></p>
 * @param <T> The metric collector enum type
 */

public class CollectorSet<T extends Enum<T> & ICollector<T>>  {
	/** The reference icollector we initialized with */
	private final T t;
	/** A set of all the collectors enables */
	private final Set<T> icollectors;
	/** The bitmask of the enabled metrics */
	private final int bitMask;
	/** The total memory allocation required for this collector set */
	private final long totalAllocation;
	/** A map of address offsets keyed by the collector for which the space is allocated */
	private final TObjectLongHashMap<T> offsets; 
	/** The data mapper that will execute the memory space updates */
	private final IDataMapper dataMapper;
	/** Indicates if the data mapper is compiled or the default */
	private final boolean compiledDataMapper;
	/**
	 * Returns the enabled collectors 
	 * @return the icollectors
	 */
	public Set<T> getICollectors() {
		return icollectors;
	}

	/**
	 * Returns the enabled collector's offsets
	 * @return the offsets
	 */
	public TObjectLongHashMap<T> getOffsets() {
		return new TObjectLongHashMap<T>(offsets);
	}

	

	/**
	 * Creates a new CollectorSet
	 * @param t An icollector instance to derive the typing from
	 * @param bitMask the bitmask in effect
	 */
	public CollectorSet(T t, int bitMask) {
		this(t.getDeclaringClass(), bitMask);		
	}

	/**
	 * Creates a new CollectorSet
	 * @param clazz the type of the icollection metric set
	 * @param bitMask the bit mask in effect
	 */
	public CollectorSet(Class<T> clazz, int bitMask) {
		icollectors = EnumSet.allOf(clazz);
		t = icollectors.iterator().next();
		icollectors.retainAll(t.getEnabledCollectors(bitMask));
		this.bitMask = bitMask;
		totalAllocation = t.getAllocationFor(bitMask);
		offsets = new TObjectLongHashMap<T>(icollectors.size(), 0.2f);
		long offset = 0;
		for(T t: icollectors) {
			offsets.put(t, offset + MetricSnapshotAccumulator.HEADER_SIZE);
			offset += t.getDataStruct().byteSize;
		}
		dataMapper = DataMapperBuilder.getInstance(clazz).getIDataMapper(t.getDeclaringClass(), bitMask);
		compiledDataMapper = !dataMapper.getClass().equals(DefaultDataMapper.class);
	}
	
	/**
	 * Creates a new CollectorSet
	 * @param className the type name of the icollection metric set
	 * @param bitMask the bit mask in effect
	 */
	public CollectorSet(String className, int bitMask) {
		this((Class<T>)get(className), bitMask);
	}

	private static Class<?> get(String clazzName) {
		try {			
			return Class.forName(clazzName);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create CollectorSet for class name [" + clazzName + "]", ex);
		}		
	}
	
	public static void main(String[] args) {
		log("CollectorSet Test");
		CollectorSet<MethodInterceptor> cs = new CollectorSet<MethodInterceptor>
			(MethodInterceptor.class, MethodInterceptor.allMetricsMask);
		log(cs.dataMapper);
		cs = new CollectorSet<MethodInterceptor>
		(MethodInterceptor.class, MethodInterceptor.defaultMetricsMask);
		log(cs.dataMapper);
	}
	
	/**
	 * Initializes or resets the address space 
	 * @param address the address
	 */
	public void reset(long address) {
		dataMapper.reset(address, getOffsets());
	}
	
	/**
	 * Applies the collected data to the metric's address space
	 * @param address The base address for the metric
	 * @param collectedValues the collected values
	 */
	public void put(long address, long[] collectedValues) {
//		TObjectLongHashMap<T> offsets = getOffsets();
//		log("Offsets:" + offsets);
//		log(dataMapper.toString());
		dataMapper.put(address, compiledDataMapper ? null : getOffsets(), collectedValues);
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

	public int getBitMaskIndex() {
		return t.getBitMaskIndex();
	}
	
	/**
	 * Returns the total allocation for the passed bitmask
	 * @return the totalAllocation
	 */
	public long getTotalAllocation(int bitMask) {
		return t.getAllocationFor(bitMask);
	}

	/**
	 * Returns 
	 * @return the bitMask
	 */
	public int getBitMask() {
		return bitMask;
	}

	/**
	 * Returns 
	 * @return the totalAllocation
	 */
	public long getTotalAllocation() {
		return totalAllocation;
	}

	/**
	 * Returns the 
	 * @return the dataMapper
	 */
	public IDataMapper getDataMapper() {
		return dataMapper;
	}

	/**
	 * Returns the reference collector for this collector set
	 * @return the the reference collector for this collector set
	 */
	public T getReferenceCollector() {
		return t;
	}
	
}
