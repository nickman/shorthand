
package com.heliosapm.shorthand.accumulator;

/**
 * <p>Title: PeriodEventListener</p>
 * <p>Description: Defines a listener that will be notified of period switch events</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.PeriodEventListener</code></p>
 */

public interface PeriodEventListener {
	/**
	 * Callback when a period ends and new period begins
	 * @param newStartTime The start time in ms of the new [current] period
	 * @param newEndTime The end time in ms of the new [current] period
	 * @param priorStartTime The start time in ms of the prior period
	 * @param priorEndTime The end time in ms of the prior period
	 */
	public void onNewPeriod(long newStartTime, long newEndTime ,long priorStartTime, long priorEndTime);
}
