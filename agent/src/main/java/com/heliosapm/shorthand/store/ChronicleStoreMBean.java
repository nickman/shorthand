/**
 * Helios, OpenSource Monitoring
 *
 */
package com.heliosapm.shorthand.store;

/**
 * <p>Title: ChronicleStoreMBean</p>
 * <p>Description: Management and monitoring interface for {@link ChronicleStore}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.ChronicleStoreMBean</code></p>
 */

public interface ChronicleStoreMBean {
	/**
	 * Returns the number of registered enum collectors
	 * @return the number of registered enum collectors
	 */
	public int getEnumCollectorCount();
	/**
	 * Returns the number of registered metric names
	 * @return the number of registered metric names
	 */
	public long getMetricNameCount();
	/**
	 * Returns the number of registered metric data points
	 * @return the number of registered metric data points
	 */
	public long getMetricDataPointCount();
	
	/**
	 * Returns the size of the metric name chronicle in bytes
	 * @return the size of the metric name chronicle in bytes
	 */
	public long getMetricNameSize();
	/**
	 * Returns the size of the metric data point chronicle in bytes
	 * @return the size of the metric data point chronicle in bytes
	 */
	public long getMetricDataPointSize();
	
	/**
	 * Returns the elapsed time of the last first phase flush in ns.
	 * @return the elapsed time of the last first phase flush in ns.
	 */
	public long getFirstPhaseLastFlushElapsedNs();
	
	/**
	 * Returns the elapsed time of the first phase last flush in ms.
	 * @return the elapsed time of the first phase last flush in ms.
	 */
	public long getFirstPhaseLastFlushElapsedMs();
	
	/**
	 * Returns the rolling average of first phase flush times in ns.
	 * @return the rolling average of first phase flush times in ns.
	 */
	public long getFirstPhaseAverageFlushElapsedNs();
	
	/**
	 * Returns the rolling average of first phase flush times in ms.
	 * @return the rolling average of first phase flush times in ms.
	 */
	public long getFirstPhaseAverageFlushElapsedMs();
	
	/**
	 * Returns the elapsed time of the last second phase flush in ns.
	 * @return the elapsed time of the last second phase flush in ns.
	 */
	public long getSecondPhaseLastFlushElapsedNs();
	
	/**
	 * Returns the elapsed time of the second phase last flush in ms.
	 * @return the elapsed time of the second phase last flush in ms.
	 */
	public long getSecondPhaseLastFlushElapsedMs();
	
	/**
	 * Returns the rolling average of second phase flush times in ns.
	 * @return the rolling average of second phase flush times in ns.
	 */
	public long getSecondPhaseAverageFlushElapsedNs();
	
	/**
	 * Returns the rolling average of second phase flush times in ms.
	 * @return the rolling average of second phase flush times in ms.
	 */
	public long getSecondPhaseAverageFlushElapsedMs();
	
	/**
	 * Get and clear the current count of reprobes on the pending deallocate map
	 * @return the total number of reprobes since the last reset
	 */
	public long getPendingDeallocateReprobes();
	
	/**
	 * Get and clear the current count of reprobes on the name index cache
	 * @return the total number of reprobes since the last reset
	 */
	public long getNameIndexReprobes();
	
	/**
	 * Returns the total number of invalid mem-space releases which occurs when an accumulator thread locks an invalidated mem-space.
	 * @return the total number of invalid mem-space releases
	 */
	public long getReleaseCount();

	
	
	
}
