/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import test.com.heliosapm.shorthand.FlushCompletionBarrier;

import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.store.IMetric;
import com.heliosapm.shorthand.store.IMetricDataPoint;
import com.heliosapm.shorthand.util.ArrayUtils;
import com.heliosapm.shorthand.util.unsafe.collections.ConcurrentLongSlidingWindow;
import com.heliosapm.shorthand.util.unsafe.collections.LongSlidingWindow;

/**
 * <p>Title: MethodInterceptorAccumulatorTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.MethodInterceptorAccumulatorTest</code></p>
 */

public class MethodInterceptorAccumulatorTest extends AccumulatorBaseTest {
	/** A flush completion barrier */
	protected final FlushCompletionBarrier completionBarrier = new FlushCompletionBarrier(5000, TimeUnit.MILLISECONDS); 
	
	/**
	 * Disables the period clock
	 * @throws java.lang.Exception thrown on any error
	 */
	@BeforeClass
	public static void disablePeriodClock() throws Exception {
		Method m = PeriodClock.class.getDeclaredMethod("disablePeriodClock");
		m.setAccessible(true);
		m.invoke(PeriodClock.getInstance());
		log("!!! DISABLED PERIOD CLOCK !!!");
	}

	/**
	 * Re-enables the period clock
	 * @throws java.lang.Exception thrown on any error
	 */
	@AfterClass
	public static void enablePeriodClock() throws Exception {
		Method m = PeriodClock.class.getDeclaredMethod("enablePeriodClock");
		m.setAccessible(true);
		m.invoke(PeriodClock.getInstance());
		log("!!! ENABLED PERIOD CLOCK !!!");
	}
	
	
	/**
	 * Registers the completion barrier
	 * {@inheritDoc}
	 * @see test.com.heliosapm.shorthand.BaseTest#setUp()
	 */
	@Before
	public void setUp() throws Exception {
		super.setUp();
		PERIOD_CLOCK.registerListener(completionBarrier);
	}

	/**
	 * Unregisters the completion barrier
	 * {@inheritDoc}
	 * @see test.com.heliosapm.shorthand.BaseTest#tearDown()
	 */
	@After
	public void tearDown() throws Exception {
		super.tearDown();
		PERIOD_CLOCK.removeListener(completionBarrier);
	}
	
	/**
	 * Tests a handfull of metric interceptor submissions, then calls a manual flush and validates what's in the store.
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testOnePeriodFlush() throws Exception {
		STORE.clear();
		final int bitMask = MethodInterceptor.getBitMaskFor(MethodInterceptor.values());
		CollectorSet<MethodInterceptor> cs = new CollectorSet<MethodInterceptor>(MethodInterceptor.class, bitMask);
		IAssert.assertEquals(true, true);
		final int LOOPS = 1000;
		final String metricName = getClass().getName() + ".testOnePeriodFlush"; 
		final int ITEM_COUNT = MethodInterceptor.values().length;
		final Map<Long, long[]> testValues = new LinkedHashMap<Long, long[]>(LOOPS);
		long[][] values = new long[LOOPS][];
		long startTime = System.nanoTime();
		long periodTime = System.currentTimeMillis();
		for(int i = 0; i < LOOPS; i++) {
			values[i] = new long[ITEM_COUNT+2];
			for(int x = 0; x < ITEM_COUNT; x++) {
				values[i][x] = nextPosInt(10000)+1;
			}
			values[i][ITEM_COUNT] = bitMask;
			values[i][ITEM_COUNT+1] = 0;
			values[i][MethodInterceptor.INVOCATION_COUNT.ordinal()] = nextPosInt(9)+1;
			testValues.put(periodTime + (i*1000), values[i]);			
			ACCUMULATOR.snap(metricName, cs, values[i]);
		}		
		long endTime = System.nanoTime()-startTime;
		log("Completed [%s] snaps in [%s] ns for an average of [%s] ns. per snap", LOOPS, endTime, endTime/LOOPS);
		completionBarrier.reset();
		long[] period = this.periodFlush();
		completionBarrier.waitForCompletion();
		long PERIOD_START = period[2];
		long PERIOD_END = period[3];		
		long[][] pivotedValues = ArrayUtils.pivot(values);		
		IAssert.assertTrue("Pivoted Array Mismatch", Arrays.deepEquals(values, ArrayUtils.pivot(pivotedValues)));
		IMetric<MethodInterceptor> metric = STORE.getMetric(metricName);
		IAssert.assertEquals("Unexpected metric name", metricName, metric.getName());
		IAssert.assertEquals("Unexpected collector type name", MethodInterceptor.class.getSimpleName(), metric.getCollectorTypeName());
		IAssert.assertEquals("Unexpected period start", PERIOD_START, metric.getPeriodStart());
		IAssert.assertEquals("Unexpected period end", PERIOD_END, metric.getPeriodEnd());
		IAssert.assertEquals("Unexpected period start", new Date(PERIOD_START), new Date(metric.getPeriodStart()));
		IAssert.assertEquals("Unexpected period end", new Date(PERIOD_END), new Date(metric.getPeriodEnd()));
		
		Map<MethodInterceptor, IMetricDataPoint<MethodInterceptor>> dataPoints = metric.getMetricDataPoints();
		
		
		for(MethodInterceptor mi: MethodInterceptor.values()) {
			LongSlidingWindow arr = new ConcurrentLongSlidingWindow(LOOPS, pivotedValues[mi.ordinal()]);
			long totalInvocations = new ConcurrentLongSlidingWindow(LOOPS, pivotedValues[MethodInterceptor.INVOCATION_COUNT.ordinal()]).sum();
			IMetricDataPoint<MethodInterceptor> mdp = dataPoints.get(mi);
			IAssert.assertEquals("Unexpected collector", mi, mdp.getCollector());			
			TObjectLongHashMap<String> dp = mdp.getDataPoints();			
			if(mi.getDataStruct().size==1) {
				IAssert.assertEquals("Unexpected period total", arr.sum(), dp.get(mi.getSubMetricNames()[0]));
			} else {
				
				for(int i = 0; i < mi.getDataStruct().size; i++) {
					String subMetricName = mi.getSubMetricNames()[i];
					long storeValue = dp.get(subMetricName);
					switch(i) {
						case 0:
							IAssert.assertEquals("Unexpected sub metric name for index [" + i + "]", "Min", subMetricName);
							IAssert.assertEquals("Unexpected Min Value", arr.min(), storeValue);
							break;
						case 1:
							IAssert.assertEquals("Unexpected sub metric name for index [" + i + "]", "Max", subMetricName);
							IAssert.assertEquals("Unexpected Max Value", arr.max(), storeValue);
							break;
						case 2:
							IAssert.assertEquals("Unexpected sub metric name for index [" + i + "]", "Avg", subMetricName);
							
							IAssert.assertEquals(
									"Unexpected Avg Value for values " + Arrays.toString(arr.asLongArray()) + "\n and invocations:"
									+ Arrays.toString(pivotedValues[MethodInterceptor.INVOCATION_COUNT.ordinal()])
									, arr.sum()/totalInvocations, storeValue);
							break;
						default:
							throw new Exception("Invalid data struct index [" + i + "]");
					}
				}
			}
		}

	}
}
