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
	 * Returns the number of written dirty buffers in the last flush
	 * @return the number of written dirty buffers in the last flush
	 */
	public long getDirtyBuffersWrittenLast();
	
	/**
	 * Returns the rolling average number of written dirty buffers during flushes
	 * @return the rolling average number of written dirty buffers during flushes
	 */
	public long getDirtyBuffersWrittenAverage();

	/**
	 * Returns the total elapsed time of the last flush in ms.
	 * @return the total elapsed time of the last flush in ms.
	 */
	public long getFlushLastTime();
	
	/**
	 * Returns the rolling average total elapsed time of the last flush in ms.
	 * @return the rolling average total elapsed time of the last flush in ms.
	 */
	public long getFlushAverageTime();

	/**
	 * Returns the elapsed time of the last stale buffer cleanup in ms.
	 * @return the elapsed time of the last stale buffer cleanup in ms.
	 */
	public long getStaleBufferCleanLastTime();
	
	/**
	 * Returns the rolling average elapsed time of the stale buffer cleanup in ms.
	 * @return the rolling average elapsed time of the stale buffer cleanup in ms.
	 */
	public long getStaleBufferCleanAverageTime();
	
	
	/**
	 * Returns the elapsed time of the last flush dirty buffer write in ms.
	 * @return the elapsed time of the last flush dirty buffer write in ms.
	 */
	public long getDirtyBufferWriteLastTime();
	
	/**
	 * Returns the rolling average elapsed time of the flush dirty buffer write in ms.
	 * @return the rolling average elapsed time of the flush dirty buffer write in ms.
	 */
	public long getDirtyBufferWriteAverageTime();

	
	/**
	 * Returns the elapsed time of the last flush dirty buffer copy in ms.
	 * @return the elapsed time of the last flush dirty buffer copy in ms.
	 */
	public long getDirtyBufferCopyLastTime();
	
	/**
	 * Returns the rolling average elapsed time of the flush dirty buffer copy in ms.
	 * @return the rolling average elapsed time of the flush dirty buffer copy in ms.
	 */
	public long getDirtyBufferCopyAverageTime();
	
	
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
	


	
	
	
}
