/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.collectors;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator.HeaderOffsets;
import com.heliosapm.shorthand.datamapper.DataMapperBuilder;
import com.heliosapm.shorthand.datamapper.IDataMapper;

/**
 * <p>Title: EnumCollectors</p>
 * <p>Description: A repository to centralize cross-references of known enum collectors</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.collectors.EnumCollectors</code></p>
 * @param <T> The enum collector type
 */

public class EnumCollectors<T extends Enum<T> & ICollector<T>> {
	/** The singleton instance */
	private static volatile EnumCollectors<?> instance = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	/** A sequence generator to assign a local arbitrary id to an enum collector instance */
	private static final AtomicInteger INDEX_SEQ = new AtomicInteger();

	/** The enum collector types indexed by class-name */
	private final Map<String, Class<T>> indexByName = new NonBlockingHashMap<String, Class<T>>();
	/** The enum collector types indexed by assigned index */
	private final BiMap<Integer, Class<T>> indexByIndex = HashBiMap.create();
	/** The reference enum collector instance keyed by enum index */
	private final Map<Integer, T> refByIndex = new NonBlockingHashMap<Integer, T>();
	/** The data-mappers keyed by enum index and bit-mask as a string */
	private final Map<String, IDataMapper<T>> dataMappers = new NonBlockingHashMap<String, IDataMapper<T>>();
	/** A map of relative offset maps keyed by the enum index and bit-mask as a string */
	private final Map<String, TObjectLongHashMap<T>> offsets = new NonBlockingHashMap<String, TObjectLongHashMap<T>>();
	 
	/**
	 * Acquires the singleton instance
	 * @return the singleton instance
	 */
	@SuppressWarnings("rawtypes")
	public static EnumCollectors<?> getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new EnumCollectors();
				}
			}
		}
		return instance;
	}
	
	
	/**
	 * Returns a map of offsets keyed by the enum collector member for the passed enum collector index
	 * @param enumIndex The enum collector index
	 * @param bitMask The bitmask
	 * @return a map of offsets keyed by the enum collector member
	 */
	public Map<T, Long> offsets(int enumIndex, int bitMask) {
		return ref(enumIndex).getOffsets(bitMask);
	}
	
	/**
	 * Returns the data mapper for the passed key or null if it is not found
	 * @param key The data mapper key which is a string as the <b><code>[enum collector type name]/[bitMask]</code></b>
	 * @return the data mapper or null if not found
	 */
	public IDataMapper<T> dataMapper(String key) {
		return dataMappers.get(key);
	}
	
	public IDataMapper<T> dataMapper(int enumIndex, int bitMask) {
		final String key = String.format("%s/%s", type(enumIndex).getName(), bitMask);
		IDataMapper<T> dataMapper = dataMappers.get(key);
		if(dataMapper==null) {
			dataMapper = (IDataMapper<T>) DataMapperBuilder.getInstance().getIDataMapper(type(enumIndex).getName(), bitMask);
		}
		return dataMapper;
	}

	/**
	 * Returns the enum collection type for the passed name
	 * @param className The name of the enum collection type  to get
	 * @return the enum collection class
	 */
	@SuppressWarnings("unchecked")
	public Class<T> typeForName(String className) {
		Class<T> ct = indexByName.get(className);
		if(ct==null) {
			synchronized(indexByName) {
				ct = indexByName.get(className);
				if(ct==null) {
					try {
						int index = INDEX_SEQ.incrementAndGet();
						ct = (Class<T>) Class.forName(className);
						indexByName.put(className, ct);
						indexByIndex.put(index, ct);
						refByIndex.put(index, ct.getEnumConstants()[0]);
					} catch (Exception ex) {
						throw new RuntimeException("Failed to get type for name [" + className + "]", ex);
					}
				}
			}
		}
		return ct;
	}
	
	/**
	 * Returns the first enum collector member for the passed class name
	 * @param className The name of the enum collection type  to get
	 * @return the first enum collector member
	 */
	public T memberForName(String className) {
		Class<T> ct = typeForName(className);
		return ct.getEnumConstants()[0];
	}

	/**
	 * Returns the reference enum collector instance for the passed index
	 * @param index The enum collector index
	 * @return the reference enum collector 
	 */
	public T ref(int index) {
		return refByIndex.get(index);
	}
	
	/**
	 * Returns a set of all enum collector members for the passed class name
	 * @param className The name of the enum collection type  to get
	 * @return a set of all enum collector members
	 */
	public Set<T> allMembersForName(String className) {
		Class<T> ct = typeForName(className);
		return EnumSet.allOf(ct);
	}
	
	/**
	 * Returns a set of all enum collector members for the passed class name that are enabled for the passed bit mask
	 * @param className The name of the enum collection type 
	 * @param bitMask The bitmask
	 * @return a set of enabled enum collector members
	 */
	public Set<T> enabledMembersForName(String className, int bitMask) {
		T t = memberForName(className);
		return t.getEnabledCollectors(bitMask);
	}
	
	/**
	 * Returns a set of all enum collector members for the enum collector type identified by the passed index
	 * @param index The index of the enum collection type 
	 * @return a set of all enum collector members
	 */
	public Set<T> allMembersForIndex(int index) {
		Class<T> ct = indexByIndex.get(index);
		if(ct==null) throw new RuntimeException("No enum collection type for [" + index + "]");
		return allMembersForName(ct.getName());
	}
	
	/**
	 * Returns a set of enabled enum collector members for the enum collector type identified by the passed index
	 * @param index The index of the enum collection type 
	 * @param bitMask The bitmask
	 * @return a set of enabled enum collector members
	 */
	public Set<T> enabledMembersForIndex(int index, int bitMask) {
		Class<T> ct = indexByIndex.get(index);
		if(ct==null) throw new RuntimeException("No enum collection type for [" + index + "]");
		return enabledMembersForName(ct.getName(), bitMask);
	}

	
	
	/**
	 * Returns the index for the passed enum collector type name
	 * @param className the enum collector type name
	 * @return the index 
	 */
	public int indexForName(String className) {
		Class<T> ct = typeForName(className);
		return indexByIndex.inverse().get(ct);
	}
	
	/**
	 * Returns the index for the passed enum type
	 * @param enumType The enum type to get the index for
	 * @return the index for the passed enum type
	 */
	public int index(Class<T> enumType) {
		if(!indexByName.containsKey(enumType.getName())) {
			synchronized(indexByName) {
				if(!indexByName.containsKey(enumType.getName())) {
					indexByName.put(enumType.getName(), enumType);
					int index = INDEX_SEQ.incrementAndGet();
					indexByIndex.put(INDEX_SEQ.incrementAndGet(), enumType);
					return index;
				}
			}
		}
		return indexByIndex.inverse().get(enumType);
	}
	
	/**
	 * Returns the index for the passed enum member
	 * @param t The enum member to get the index for
	 * @return the index for the passed enum member
	 */
	public int index(T t) {
		return index(t.getDeclaringClass());
	}
	
	/**
	 * Returns the index for the passed enum collector name
	 * @param className The enum member member to get the index for
	 * @return the index for the passed enum member name
	 */
	public int index(String className) {
		return index(typeForName(className));
	}
	
	/**
	 * Returns the name for the passed index
	 * @param index The index
	 * @return the name
	 */
	public String name(int index) {
		Class<T> ct = indexByIndex.get(index);
		if(ct==null) throw new RuntimeException("No enum collection type for [" + index + "]");
		return ct.getName();
	}
	
	/**
	 * Returns the type for the passed index
	 * @param index The index
	 * @return the type
	 */
	public Class<T> type(int index) {
		Class<T> ct = indexByIndex.get(index);
		if(ct==null) throw new RuntimeException("No enum collection type for [" + index + "]");
		return ct;		
	}
	
	/**
	 * Returns a map of offsets keyed by the enum collector member for the passed enum collector class 
	 * @param enumType The enum collector class
	 * @param bitMask The bitmask
	 * @return a map of offsets keyed by the enum collector member 
	 */
	public Map<T, Long> offsets(Class<T> enumType, int bitMask) {
		return offsets(enumType.getName(), bitMask);
	}
	
	/**
	 * Returns a map of offsets keyed by the enum collector member for the passed enum collector member 
	 * @param t The enum collector member
	 * @param bitMask The bitmask
	 * @return a map of offsets keyed by the enum collector member 
	 */
	public Map<T, Long> offsets(T t, int bitMask) {
		return offsets(t.getDeclaringClass().getName(), bitMask);
	}
	
	
	/**
	 * Returns a map of offsets keyed by the enum collector member for the passed enum collector class name 
	 * @param className The enum collector class name
	 * @param bitMask The bitmask
	 * @return a map of offsets keyed by the enum collector member 
	 */
	public Map<T, Long> offsets(String className, int bitMask) {
		Set<T> enabled = enabledMembersForName(className, bitMask);
		Map<T, Long> offsets = new EnumMap(enabled.iterator().next().getDeclaringClass());
		long offset = HeaderOffsets.HEADER_SIZE;
		for(T t: enabled) {
			offsets.put(t, offset);
			offset += t.getDataStruct().byteSize;
		}
		return offsets;
	}
	
	
	/**
	 * Creates a new EnumCollectors
	 */
	private EnumCollectors() {
	}
	

}
