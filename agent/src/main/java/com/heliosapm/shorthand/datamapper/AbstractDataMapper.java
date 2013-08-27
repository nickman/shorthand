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
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator.HeaderOffsets;
import com.heliosapm.shorthand.collectors.DataStruct;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.store.IStore;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: AbstractDataMapper</p>
 * <p>Description: A default non-compiled {@link IDataMapper} </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.AbstractDataMapper</code></p>
 */

public abstract class AbstractDataMapper<T extends Enum<T> & ICollector<T>> implements IDataMapper<T> {
	
	/**
	 * Returns the enum collector index for this data mapper
	 * @return the enum collector index
	 */
	@Override
	public abstract int getEnumIndex();
	
	/**
	 * Returns the metric bit mask for this data mapper
	 * @return the metric bit mask
	 */
	@Override
	public abstract int getBitMask();
	
	/**
	 * Returns the mem-space body offsets for each enabled metric
	 * @return the mem-space body offsets for each enabled metric
	 */
	@Override
	public abstract Map<T, Long> getOffsets();

	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#reset(long, gnu.trove.map.hash.TObjectLongHashMap)
	 */
	@Override
	public void reset(final long address) {
		HeaderOffsets.Touch.set(address, 0);
		int enumIndex = (int)HeaderOffsets.EnumIndex.get(address);
		int bitMask = (int)HeaderOffsets.BitMask.get(address);
		Map<T, Long> offsets = (Map<T, Long>) EnumCollectors.getInstance().offsets(enumIndex, bitMask);
		for(Map.Entry<T, Long> entry: offsets.entrySet()) {
			DataStruct ds = entry.getKey().getDataStruct();
			UnsafeAdapter.copyMemory(ds.defaultValues, ds.type.addressOffset, null, address+entry.getValue(), ds.byteSize);			
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#put(long, gnu.trove.map.hash.TObjectLongHashMap, long[])
	 */
	@Override
	public void put(final long address, final long[] data) {
		HeaderOffsets.Touch.set(address, 1);
		int enumIndex = (int)HeaderOffsets.EnumIndex.get(address);
		int bitMask = (int)HeaderOffsets.BitMask.get(address);
		TObjectLongHashMap<T> offsets = (TObjectLongHashMap<T>) EnumCollectors.getInstance().offsets(enumIndex, bitMask);
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
	 * @see com.heliosapm.shorthand.datamapper.IDataMapper#prePut(long, long[])
	 */
	@Override
	public void prePut(long address, long[] data) {
		int enumIndex = (int)HeaderOffsets.EnumIndex.get(address);
		int bitMask = (int)HeaderOffsets.BitMask.get(address);
		TObjectLongHashMap<T> offsets = (TObjectLongHashMap<T>) EnumCollectors.getInstance().offsets(enumIndex, bitMask);

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
		return AbstractDataMapper.class.getSimpleName();
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
	 * Returns the long values of the passed map in ascending order of the keys
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
