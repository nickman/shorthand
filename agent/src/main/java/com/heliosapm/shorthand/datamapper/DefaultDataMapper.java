/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.datamapper;

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TObjectLongProcedure;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.heliosapm.shorthand.accumulator.CopiedAddressProcedure;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.collectors.DataStruct;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.store.IStore;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: DefaultDataMapper</p>
 * <p>Description: A default non-compiled {@link IDataMapper} </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.DefaultDataMapper</code></p>
 */

public class DefaultDataMapper<T extends Enum<T> & ICollector<T>> implements IDataMapper<T> {
	/** A shareable instance */
	@SuppressWarnings("rawtypes")
	public static final DefaultDataMapper INSTANCE = new DefaultDataMapper(); 
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#reset(long, gnu.trove.map.hash.TObjectLongHashMap)
	 */
	@Override
	public void reset(final long address, TObjectLongHashMap<T> offsets) {
		offsets.forEachEntry(new TObjectLongProcedure<ICollector<?>>() {
			@Override
			public boolean execute(ICollector<?> collector, long offset) {
				DataStruct ds = collector.getDataStruct();
				UnsafeAdapter.copyMemory(ds.defaultValues, ds.type.addressOffset, null, address+offset, ds.byteSize);
				return true;
			}			
		});
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#put(long, gnu.trove.map.hash.TObjectLongHashMap, long[])
	 */
	@Override
	public void put(final long address, TObjectLongHashMap<T> offsets, final long[] data) {
		if(offsets.isEmpty()) return;				
		offsets.forEachEntry(new TObjectLongProcedure<ICollector<?>>() {
			@Override
			public boolean execute(ICollector<?> collector, long offset) {
				if(!collector.isPreApply()) {
					collector.apply(address+offset, data);
				}
				return true;
			}
		});		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#prePut(int, long, gnu.trove.map.hash.TObjectLongHashMap, long[])
	 */
	@Override
	public void prePut(int bitMask, long address, TObjectLongHashMap<T> offsets, long[] data) {
		if(offsets.isEmpty()) return;
		ICollector<?>[] preApplies = (ICollector<?>[]) offsets.iterator().key().getPreApplies(bitMask);
		if(preApplies.length==0) return;
		for(ICollector<?> collector: preApplies) {
			collector.apply(address+offsets.get(collector), data);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return DefaultDataMapper.class.getSimpleName();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#get(long)
	 */
	@Override
	public Map<T, TIntLongHashMap> get(final long address) {
		int enumIndex = UnsafeAdapter.getInt(address + MetricSnapshotAccumulator.HeaderOffsets.EnumIndex.offset); 
		int bitmask = UnsafeAdapter.getInt(address + MetricSnapshotAccumulator.HeaderOffsets.BitMask.offset);
		Class<T> enumClass = (Class<T>) EnumCollectors.getInstance().type(enumIndex);
		Set<T> enabled = (Set<T>) EnumCollectors.getInstance().enabledMembersForIndex(enumIndex, bitmask);
		TObjectLongHashMap<T> offsets = (TObjectLongHashMap<T>) EnumCollectors.getInstance().offsets(enumIndex, bitmask);
		final Map<T, TIntLongHashMap> map = new EnumMap<T, TIntLongHashMap>(enumClass);		
		offsets.forEachEntry(new TObjectLongProcedure<T>() {
			@Override
			public boolean execute(T collector, long offset) {				
				long[] values = new long[collector.getDataStruct().size];
				UnsafeAdapter.copyMemory(null, address + offset, values, UnsafeAdapter.LONG_ARRAY_OFFSET, values.length*8);					
				TIntLongHashMap vmap = new TIntLongHashMap();															
				for(int i = 0; i < values.length; i++) {
					vmap.put(i, values[i]);
				}
				map.put(collector, vmap);
				return true;					
			}
		});
		return map;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#preFlush(long)
	 */
	@Override
	public void preFlush(long address) {
		int enumIndex = UnsafeAdapter.getInt(address + MetricSnapshotAccumulator.HeaderOffsets.EnumIndex.offset); 
		int bitmask = UnsafeAdapter.getInt(address + MetricSnapshotAccumulator.HeaderOffsets.BitMask.offset);
		EnumCollectors.getInstance().enabledMembersForIndex(enumIndex, bitmask).iterator().next().preFlush(address, bitmask);
	}
	
	/**
	 * Returns the long values of the passed map in ascewnding order of the keys
	 * @param map The map to get the values from
	 * @return an array of the map's values
	 */
	public static long[] keyOrderedArray(TIntLongHashMap map) {
		long[] values = new long[map.size()];
		int[] keys = map.keys();
		Arrays.sort(keys);
		for(int i = 0; i < keys.length; i++) {
			values[i] = map.get(keys[i]);
		}
		return values;
	}
	
	

	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#flush(long, com.heliosapm.shorthand.store.IStore, long, long)
	 */
	@Override
	public long[] flush(long address, IStore<T> store, long periodStart, long periodEnd) {
		return null; //store.updatePeriod(MetricSnapshotAccumulator.HeaderOffsets.NameIndex.get(address), periodStart, periodEnd);
	}

	
	

}
