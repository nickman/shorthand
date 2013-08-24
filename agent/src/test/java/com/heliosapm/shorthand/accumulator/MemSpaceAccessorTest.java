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
package com.heliosapm.shorthand.accumulator;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import test.com.heliosapm.shorthand.BaseTest;

import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator.HeaderOffsets;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.datamapper.IDataMapper;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MemSpaceAccessorTest</p>
 * <p>Description: Test case for the basic ops of a {@link MemSpaceAccessor}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.MemSpaceAccessorTest</code></p>
 */

public class MemSpaceAccessorTest extends BaseTest {

	/**
	 * Creates a new MemSpaceAccessorTest
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testMemSpaceAccessor() throws Exception {
		EnumCollectors.getInstance().typeForName(MethodInterceptor.class.getName());
		int enumIndex = EnumCollectors.getInstance().index(MethodInterceptor.class.getName());
		int bitMask = MethodInterceptor.defaultMetricsMask;
		int memsize = MethodInterceptor.getTotalAllocation(bitMask);
		log("Memory to be allocated:%s", memsize);
		long actualSize = HeaderOffsets.HEADER_SIZE;
		for(Object o: EnumCollectors.getInstance().enabledMembersForIndex(enumIndex, bitMask)) {
			MethodInterceptor mi = (MethodInterceptor)o;
			actualSize += mi.getDataStruct().byteSize;			
		}
		Assert.assertEquals("Allocated Memory Size", actualSize, memsize);
		final long address = UnsafeAdapter.allocateMemory(memsize);
		log("Allocating [%s] bytes @ [%s]  [%s]", memsize, address, Long.toHexString(address));
		try {
			
			MemSpaceAccessor msa = MemSpaceAccessor.get(address);			
			log("Enum Index:%s", enumIndex);
			
			final Set<MethodInterceptor> enabledCollectors = (Set<MethodInterceptor>) EnumCollectors.getInstance().enabledMembersForIndex(enumIndex, bitMask);			
			//======================================================================
			// Get the data mapper
			//======================================================================
			IDataMapper dataMapper = EnumCollectors.getInstance().dataMapper(enumIndex, bitMask);
			Assert.assertNotNull("The data mapper", dataMapper);
			//======================================================================
			// Initialize the header
			//======================================================================
			msa.initializeHeader(memsize, 17L, bitMask, enumIndex);
			//======================================================================
			//  Validate the header by API
			//======================================================================
			Assert.assertEquals("Memory Size", memsize, msa.getMemSize());
			Assert.assertEquals("Name Index", 17L, msa.getNameIndex());
			Assert.assertEquals("Bit Mask", bitMask, msa.getBitMask());
			Assert.assertEquals("Enum Index", enumIndex, msa.getEnumIndex());			
			log("Header Size:%s", HeaderOffsets.HEADER_SIZE);
			//======================================================================
			// Reset/Initialize the mem-space body
			//======================================================================
			TObjectLongHashMap<MethodInterceptor>  offsets = (TObjectLongHashMap<MethodInterceptor>) EnumCollectors.getInstance().offsets(enumIndex, bitMask);
			dataMapper.reset(address);
			//======================================================================
			//  Validate the reset mem-space body
			//======================================================================
			for(MethodInterceptor mi: enabledCollectors) {
				long offset = offsets.get(mi);
				long[] defaultValues = (long[])mi.getDataStruct().defaultValues;
				for(int i = 0; i < mi.getDataStruct().size; i++) {					
					Assert.assertEquals("[" + mi.name() + "]-Reset Value Index#" + i, defaultValues[i], UnsafeAdapter.getLong(address + offset));
					offset += UnsafeAdapter.LONG_SIZE;
				}
			}
			log("Validated mem-space body");
			//======================================================================
			// Add some values
			//======================================================================
			long[] values = MethodInterceptor.methodEnter(bitMask);
			Assert.assertEquals("MethodEnter Array Size", MethodInterceptor.itemCount+2 , values.length);

			Assert.assertEquals("Enabled Collectors Size", 2 , enabledCollectors.size());
			// Mark the snap closed
			values[values.length-1] = 1;
			// Load some known values into the values array
			for(MethodInterceptor mi: enabledCollectors) {
				switch(mi) {
					case INVOCATION_COUNT:
						values[MethodInterceptor.INVOCATION_COUNT.ordinal()] = 1;
						break;
					case ELAPSED:
						values[MethodInterceptor.ELAPSED.ordinal()] = 79;
						break;
					default:
						Assert.fail("Unexpected MethodIntereptor [" + mi.name() + "]");
				}
			}
			Assert.assertEquals("Invocation Count", 1, values[MethodInterceptor.INVOCATION_COUNT.ordinal()]);
			Assert.assertEquals("Elapsed Time", 79, values[MethodInterceptor.ELAPSED.ordinal()]);
			Assert.assertEquals("Bit Mask", bitMask, values[MethodInterceptor.bitMaskIndex]);
			//======================================================================
			//  Load the data into the mem-space
			//======================================================================	
			dataMapper.put(address, values);
			//======================================================================
			//  Validate the data set mem-space body using direct references
			//======================================================================
			for(MethodInterceptor mi: enabledCollectors) {
				long offset = address + offsets.get(mi);
				switch(mi) {
					case INVOCATION_COUNT:
						Assert.assertEquals("MemSpace InvocationCount", 1, UnsafeAdapter.getLong(offset));
						break;
					case ELAPSED:
						for(int i = 0; i < mi.getDataStruct().size; i++) {
							Assert.assertEquals("MemSpace Elapsed #" + i, 79, UnsafeAdapter.getLong(offset));
							offset += UnsafeAdapter.LONG_SIZE;
						}
						break;
					default:
						Assert.fail("Unexpected MethodIntereptor [" + mi.name() + "]");
				}
			}
			//======================================================================
			//  Validate the data set mem-space body using the API
			//======================================================================
			long[][] datapoints = msa.getDataPoints();
			Assert.assertEquals("The number of MSA DataPoints", 2, datapoints.length);
			int index = 0;
			for(MethodInterceptor mi: enabledCollectors) {
				switch(mi) {
					case INVOCATION_COUNT:
						Assert.assertEquals("The number of MSA DataPoints for Invocation Count", mi.getDataStruct().size, datapoints[index].length);
						Assert.assertEquals("MemSpaceAccessor InvocationCount", 1, datapoints[index][0]);
						break;
					case ELAPSED:
						Assert.assertEquals("The number of MSA DataPoints for Elapsed Time", mi.getDataStruct().size, datapoints[index].length);
						for(int i = 0; i < mi.getDataStruct().size; i++) {
							Assert.assertEquals("MemSpaceAccessor Elapsed #" + i, 79, datapoints[index][i]);
						}
						
						break;
					default:
						Assert.fail("Unexpected MethodIntereptor [" + mi.name() + "]");
				}
				index++;				
			}
		} finally {
			try {
				log("Freeing address [%s]  [%s]", address, Long.toHexString(address));
				UnsafeAdapter.freeMemory(address);
			} catch (Throwable t) {
				t.printStackTrace(System.err);
			}
		}
	}
	
	/**
     * Finds the next positive power of 2 for the passed value
     * @param value the value to find the next power of 2 for
     * @return the next power of 2
     */
    public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}	
	


}
