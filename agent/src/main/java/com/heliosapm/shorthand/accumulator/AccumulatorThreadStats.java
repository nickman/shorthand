/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.accumulator;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: AccumulatorThreadStats</p>
 * <p>Description: Thread local stats for individual threads passing through the accumulator</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.accumulator.AccumulatorThreadStats</code></p>
 */

public class AccumulatorThreadStats {
	/** Total number of spins on name locks */
	public static final int NAME_LOCK_SPINS = 0;
	/** Total number of spins on global lock */
	public static final int GLOBAL_LOCK_SPINS = 1;
	/** Total amount of time writing new metrics */
	public static final int NEW_METRIC_TIME = 2;
	/** Total number of new metrics */
	public static final int NEW_METRIC_COUNT = 3;
	/** Total amount of time initializing new metrics */
	public static final int INIT_METRIC_TIME = 4;
	/** Total number of initializing metrics */
	public static final int INIT_METRIC_COUNT = 5;
	
	private static final ThreadLocal<long[]> STATS = new ThreadLocal<long[]>() {
		@Override
		protected long[] initialValue() {
			return new long[6];
		}
	};
	
	/**
	 * Resets the current thread's stats
	 */
	public static void reset() {
		Arrays.fill(STATS.get(), 0);
	}
	
	/**
	 * Returns a summary of the accumulated stats for the current thread
	 * @return a summary of the accumulated stats for the current thread
	 */
	public static String report() {
		StringBuilder b = new StringBuilder("[").append(Thread.currentThread().getName()).append("]");
		long[] stats = STATS.get();
		b.append(" nls:").append(stats[NAME_LOCK_SPINS]);
		b.append(" gls:").append(stats[GLOBAL_LOCK_SPINS]);
		b.append(" nmc:").append(stats[NEW_METRIC_COUNT]);
		if(stats[NEW_METRIC_TIME]==0 || stats[NEW_METRIC_COUNT]==0) {
			b.append(" nmt:").append(0);
		} else {
			long avg = stats[NEW_METRIC_TIME]/stats[NEW_METRIC_COUNT];
			b.append(" nmt:").append(avg).append(" ns.");
			b.append(" (").append(TimeUnit.MICROSECONDS.convert(avg, TimeUnit.NANOSECONDS)).append(" \u00b5s.)");
		}
		b.append(" imc:").append(stats[INIT_METRIC_COUNT]);
		if(stats[INIT_METRIC_TIME]==0 || stats[INIT_METRIC_COUNT]==0) {
			b.append(" imt:").append(0);
		} else {
			long avg = stats[INIT_METRIC_TIME]/stats[INIT_METRIC_COUNT];
			b.append(" imt:").append(avg).append(" ns.");
			b.append(" (").append(TimeUnit.MICROSECONDS.convert(avg, TimeUnit.NANOSECONDS)).append(" \u00b5s.)");
		}
		
		return b.toString();
	}
	
	/**
	 * Increments the name lock spin count
	 * @param count the name lock spin count delta
	 */
	public static void incrementNameLockSpins(int count) {
		STATS.get()[NAME_LOCK_SPINS] += count;
	}
	/**
	 * Increments the global lock spin count
	 * @param count the global lock spin count delta
	 */
	public static void incrementGlobalLockSpins(int count) {
		STATS.get()[GLOBAL_LOCK_SPINS] += count;
	}
	/**
	 * Increments the new metric time
	 * @param time the new metric time delta
	 */
	public static void incrementNewMetricTime(long time) {
		STATS.get()[NEW_METRIC_TIME] += time;
		STATS.get()[NEW_METRIC_COUNT]++;
	}
	/**
	 * Increments the init metric time
	 * @param time the init metric time delta
	 */
	public static void incrementInitMetricTime(long time) {
		STATS.get()[INIT_METRIC_TIME] += time;
		STATS.get()[INIT_METRIC_COUNT]++;
	}
	
	

}
