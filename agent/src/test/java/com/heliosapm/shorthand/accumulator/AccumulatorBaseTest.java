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

import java.util.Arrays;
import java.util.Date;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;

import test.com.heliosapm.shorthand.BaseTest;

import com.heliosapm.shorthand.store.ChronicleStore;
import com.heliosapm.shorthand.store.IStore;

/**
 * <p>Title: AccumulatorBaseTest</p>
 * <p>Description: Base test class for accumulator test cases</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.AccumulatorBaseTest</code></p>
 */
@Ignore
public class AccumulatorBaseTest extends BaseTest {
	/** The period clock */
	protected final PeriodClock PERIOD_CLOCK = PeriodClock.getInstance();
	/** The accumulator */
	protected final MetricSnapshotAccumulator ACCUMULATOR = MetricSnapshotAccumulator.getInstance(); 
	/** The accumulator store */
	protected final IStore STORE = ChronicleStore.getInstance();
	
	
	
	/**
	 * Trigger a period flush
	 * @param currentTime The time to submit the flush as
	 * @return The created period
	 */
	protected long[] periodFlush(long currentTime) {
		long[] period = PERIOD_CLOCK.getCurrentPeriod(currentTime);
		PERIOD_CLOCK.triggerFlush(period);
		log("Flushed: %s", periodFormatter(period));
		return period;
	}
	
	/**
	 * Trigger a period flush for right now
	 * @return The created period
	 */
	protected long[] periodFlush() {
		return periodFlush(System.currentTimeMillis());
	}
	
	
	 
	
	
	/**
	 * Formats a period into a human readable string
	 * @param period The period
	 * @return a formatted period
	 */
	public String periodFormatter(long[] period) {
		if(period==null) return "Null period";
		if(period.length!=5) return "Invalid period: " + Arrays.toString(period);
		return String.format("Period: Prior Period: [%s --to--> %s] New Period: [%s --to--> %s]", 
				new Date(period[2]), new Date(period[3]),
				new Date(period[0]), new Date(period[1])
		);
		
	}
}
