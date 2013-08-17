/**
 * Helios Development Group LLC, 2010
 */
package com.heliosapm.shorthand.instrumentor.helper;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import com.heliosapm.shorthand.collectors.measurers.AbstractDeltaMeasurer;
import com.heliosapm.shorthand.collectors.measurers.DefaultMeasurer;
import com.heliosapm.shorthand.collectors.measurers.DelegatingMeasurer;
import com.heliosapm.shorthand.collectors.measurers.InvocationMeasurer;
import com.heliosapm.shorthand.collectors.measurers.Measurer;


/**
 * <p>Title: MetricCollection</p>
 * <p>Description: An articulated enum listing different metric collection points and supporting functionality for each</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead
 * @version $LastChangedRevision$
 * <p><code>com.heliosapm.jmx.metrics.MetricCollection</code></p>
 */
public enum MetricCollection implements MC {
	/** The elapsed system cpu time in microseconds */
	SYS_CPU(seed.next(), true, false, false, "CPU Time (\u00b5s)", "syscpu", "CPU Thread Execution Time", new DefaultSysCpuMeasurer(0)),
	/** The elapsed user mode cpu time in microseconds */
	USER_CPU(seed.next(), true, false, false, "CPU Time (\u00b5s)", "usercpu", "CPU Thread Execution Time In User Mode", new DefaultUserCpuMeasurer(1)),
	/** The number of thread waits on locks or other concurrent barriers */
	WAIT_COUNT(seed.next(), true, false, true, "Thread Waits", "waits", "Thread Waiting On Notification Count", new DefaultWaitCountMeasurer(2)),
	/** The thread wait time on locks or other concurrent barriers */
	WAIT_TIME(seed.next(), true, false, true, "Thread Wait Time (ms)", "waittime", "Thread Waiting On Notification Time", new DefaultWaitTimeMeasurer(3)),
	/** The number of thread waits on synchronization monitors */
	BLOCK_COUNT(seed.next(), true, false, true, "Thread Blocks", "blocks", "Thread Waiting On Monitor Entry Count", new DefaultBlockCountMeasurer(4)),
	/** The thread wait time on synchronization monitors  */
	BLOCK_TIME(seed.next(), true, false, true, "Thread Block Time (ms)", "blocktime", "Thread Waiting On Monitor Entry Time", new DefaultBlockTimeMeasurer(5)),
	/** The elapsed wall clock time in ns. */
	ELAPSED(seed.next(), true, true, false, "Elapsed Time (ns)", "elapsed", "Elapsed Execution Time", new DefaultElapsedTimeMeasurer(6)),
	/** The elapsed compilation time in ms */
	COMPILATION(seed.next(), true, false, false, "Elapsed Compilation Time (ms)", "compilation", "Elapsed Compilation Time", new DefaultCompilationTimeMeasurer(7)),
	/** The number of threads concurrently passing through the instrumented joinpoint  */
	METHOD_CONCURRENCY(seed.next(), true, false, false, "Method Concurrent Thread Execution Count", "methconcurrent", "Number Of Threads Executing Concurrently In The Same Method", new DelegatingMeasurer(new DefaultMeasurer(8))),
	/** The total number of invocations of the instrumented joinpoint  */
	INVOCATION_COUNT(seed.next(), false, true, false, "Method Invocations", "invcount", "Method Invocation Count", new InvocationMeasurer(9)),
	/** The total number of successful invocations of the instrumented method */
	RETURN_COUNT(seed.next(), false, false, false, "Method Returns", "retcount", "Method Return Count", new DelegatingMeasurer(new DefaultMeasurer(10))),
	/** The total number of invocations of the instrumented method that terminated on an exception */
	EXCEPTION_COUNT(seed.next(), false, true, false, "Method Invocation Exceptions ", "exccount", "Method Invocation Exception Count", new DelegatingMeasurer(new DefaultMeasurer(11)));
//	/** The total number of transaction starts during invocation of the instrumented method */
//	TX_ACTIVITY(seed.next(), true, false, false, "Transactions", "tx", "Transaction Activity During Invocation", null);


	/** A map of MetricCollections keyed by ordinal */
	private static final Map<Integer, MetricCollection> ORDINAL_MAP = new HashMap<Integer, MetricCollection>(MetricCollection.values().length);
	/** A map of MetricCollections keyed by short name */
	private static final Map<String, MetricCollection> SHORTNAME_MAP = new HashMap<String, MetricCollection>(MetricCollection.values().length);
	/** a set of bitMasks for metrics that require a ThreadInfo for measurement */
	private static final int[] threadInfoRequiredMasks;

	/** the item count */
	public static final int itemCount;
	/** the index of the snapshot bitmask */
	public static final int bitMaskIndex;
	/** the index of the snapshot id */
	public static final int snapshotIdIndex;
	/** the number of MinMaxAvg counters */
	public static final int minMaxAvgCount;
	/** the number of accumulator counters */
	public static final int accumulatorCount;
	
	
	/** the default bitMask */
	public static final int defaultBitMask;
	/** the all enabled bitMask */
	public static final int allBitMask;
	
	private static final SortedSet<MetricCollection> minMaxAvgMetrics; 
	private static final SortedSet<MetricCollection> accumulatorMetrics;
	private static final Set<MetricCollection> orderedMetrics;

	/** the current measurement ThreadInfo */
	private static final ThreadLocal<ThreadInfo> currentThreadInfo = new ThreadLocal<ThreadInfo>();
	
	
	
	static {
		Set<Integer> tirs = new HashSet<Integer>();
		int _defaultBitMask = 0;
		int _minMaxAvgCount=0, _accumulatorCount=0, _allBitMask = 0;
		Set<MetricCollection> _orderedMetrics = new LinkedHashSet<MetricCollection>(MetricCollection.values().length);
		for(MetricCollection m: MetricCollection.values()) {
			ORDINAL_MAP.put(m.ordinal(), m);
			SHORTNAME_MAP.put(m.getShortName(), m);
			if(m.isRequiresTI()) {
				tirs.add(m.getMask());
			}
			if(m.isDefaultOn()) {
				_defaultBitMask = _defaultBitMask | m.getMask();
			}
			_allBitMask = m.enable(_allBitMask);
			if(m.isMinMaxAvg()) _minMaxAvgCount++;
			else _accumulatorCount++;
		}
		minMaxAvgCount = _minMaxAvgCount;
		accumulatorCount = _accumulatorCount;
		TreeSet<MetricCollection> _minMaxAvgMetrics = new TreeSet<MetricCollection>(); 
		TreeSet<MetricCollection> _accumulatorMetrics = new TreeSet<MetricCollection>();

		for(MetricCollection m: MetricCollection.values()) {
			if(m.isMinMaxAvg())  {
				_minMaxAvgMetrics.add(m);				
			} else {
				_accumulatorMetrics.add(m);
			}
		}
		
		minMaxAvgMetrics = Collections.unmodifiableSortedSet(_minMaxAvgMetrics);
		accumulatorMetrics = Collections.unmodifiableSortedSet(_accumulatorMetrics);
		_orderedMetrics.addAll(minMaxAvgMetrics);
		_orderedMetrics.addAll(accumulatorMetrics);
		orderedMetrics = Collections.unmodifiableSet(_orderedMetrics);
		defaultBitMask = _defaultBitMask;
		allBitMask = _allBitMask;
		int size = ORDINAL_MAP.size();
		itemCount = size + 2;
		bitMaskIndex = size;
		snapshotIdIndex = size + 1;
		threadInfoRequiredMasks = new int[tirs.size()];
		int cnt = 0;
		for(Integer i: tirs) {
			threadInfoRequiredMasks[cnt] = i;
			cnt++;
		}
	}
	
	public static Set<MetricCollection> minMaxAvgMetrics() {
		return minMaxAvgMetrics;
	}
	
	public static Set<MetricCollection> accumulatorMetrics() {
		return accumulatorMetrics;
	}
	
	public static Set<MetricCollection> orderedMetrics() {
		return orderedMetrics;
	}
	
	/**
	 * Returns a long array sized for all MinMaxAvg counters
	 * @return a long array sized for all MinMaxAvg counters
	 */
	public static long[] counterArray() {
		return new long[minMaxAvgCount];
	}

	/**
	 * Returns a long array sized for all accumulators
	 * @return a long array sized for all accumulators
	 */
	public static long[] accumulatorArray() {
		return new long[accumulatorCount];
	}
	
	public long getInitMin() {
		return mma ? Long.MAX_VALUE : 0;
	}
	
	public long getInitMax() {
		return mma ? Long.MIN_VALUE : 0;
	}
	
	public long getInitAvg() {
		return 0;
	}
	
	/**
	 * Computes the applied values for all metric types which are written into the passed "putHere" arrays.
	 * @param invCount The current invocation count (already updated for this call)
	 * @param collectedValues The MetricCollection snapshot array of {@link #itemCount} length
	 * @param accumulatorState An array representing the current state of the accumulator counters. A <code>long[0][accumulatorCount]</code> array.
	 * @param minMaxAvgState An array representing the current state of the minMaxAvg counters. A <code>long[3][minMaxAvgCount]</code> array.
	 * @param putAccumulatorsHere The array to write the applied accumulator values into. A <code>long[accumulatorCount]</code> array.
	 * @param putMinMaxAvgsHere The array to write the applied minMaxAvg values into A <code>long[minMaxAvgCount]</code> array.
	 * @param putFalseHereIfDisabled The array to write a <code>false</code> to at the metric's index if it was disabled. A <code>boolean[minMaxAvgCount + accumulatorCount]</code> array. 
	 */
	public static void apply(long invCount, long[] collectedValues, 
				long[][] accumulatorState, long[][] minMaxAvgState, 
				long[] putAccumulatorsHere, long[] putMinMaxAvgsHere, 
				boolean[] putFalseHereIfDisabled) {
		int ord = -1;
		long[] minMaxAvg = new long[3];
		for(MetricCollection mc: minMaxAvgMetrics) {
			ord = mc.ordinal();
			if(!mc.isEnabled((int)collectedValues[bitMaskIndex])) {
				putFalseHereIfDisabled[ord] = false;
				continue;
			}
			// need to convert minMaxAvgState ([MINMAXAVG][minMaxAvgCount])   to long[3] with minMaxAvg for the ord.
			for(int i = 0; i < 3; i++) {
				minMaxAvg[i] = minMaxAvgState[i][ord];
			}			
			mc.apply(collectedValues[ord], invCount, minMaxAvg, putMinMaxAvgsHere);
		}
		for(MetricCollection mc: accumulatorMetrics) {
			ord = mc.ordinal();
			if(!mc.isEnabled((int)collectedValues[bitMaskIndex])) {
				putFalseHereIfDisabled[ord] = false;
				continue;
			}			
			mc.apply(collectedValues[ord], invCount, accumulatorState[0], putAccumulatorsHere);
		}
		
	}
	
	public void apply(long collectedValue, long invCount, long[] currentState, long[] putValuesInHere) {
		//log("Processing new value [" + name() + "] --:" + collectedValue);
		if(!mma) {
			long newTotal = currentState[0] + collectedValue;
			Arrays.fill(putValuesInHere, newTotal);
		} else {
			if(collectedValue < currentState[0])  putValuesInHere[0]  = collectedValue;
			if(collectedValue > currentState[1])  putValuesInHere[1]  = collectedValue;
			long avg = rollingAvg(collectedValue, currentState[2], invCount);
			//log("Rolling avg [" + collectedValue + "/" + currentState[2] + "/" + invCount + "]:  " + avg);
			putValuesInHere[2] = avg;
		}
	}
	
	private static long rollingAvg(double newVal, double existingVal, long invCount) {		
		if(invCount==1) return (long)newVal;
		double total = newVal + existingVal;
		if(total==0) return 0L;
		double avg = total/2d;		
		return (long)avg;
	}
	
	
	
	/**
	 * Returns a bit mask enabled for the passed metric collections
	 * @param mcs An array of metric collections
	 * @return a bit mask  
	 */
	public static int enableFor(MetricCollection...mcs) {
		int i = 0;
		if(mcs!=null) {
			for(MetricCollection mc: mcs) {
				i = mc.enable(i);
			}
		}
		return i;
	}
	
	
	
	/**
	 * Returns a bit mask enabled for the passed metric collections
	 * @param mcs An array of metric collections
	 * @return a bit mask  
	 */
	public static int enableFor(Object...mcs) {
		int i = 0;
		if(mcs!=null) {
			for(Object o: mcs) {
				MetricCollection mc = forValue(o);
				i = mc.enable(i);
			}
		}
		return i;
	}
	

	/**
	 * The private enum ctor
	 * @param baseMask the seed 
	 * @param mma Indicates if this metric is a period MinMaxAvg counter
	 * @param defaultOn If true, this metric is turned on by default
	 * @param isRequiresTI indicates if the metric's measurer will require a ThreadInfo instance
	 * @param unit the unit of the metric
	 * @param shortName a short name for the metric
	 * @param description the description of the metric
	 * @param measurer the measurement capturing procedure for this metric
	 */
	private MetricCollection(int baseMask, boolean mma, boolean defaultOn, boolean isRequiresTI, String unit, String shortName, String description, Measurer measurer) {
		this.baseMask = baseMask;
		this.mma = mma;
		this.defaultOn = defaultOn;
		this.isRequiresTI = isRequiresTI;
		this.unit = unit;
		this.shortName = shortName;
		this.description = description;
		this.measurer = measurer;
	}
	

	

	
	/** the measurer */
	private final Measurer measurer;
	/** indicates if this metric is turned on by default */
	private final boolean defaultOn;
	/** indicates if this metric is a period MinMaxAvg counter */
	private final boolean mma; 
	
	/** The internal code */
	private final int baseMask;
	/** The metric unit */
	private final String unit;
	/** The metric short name */
	private final String shortName;
	/** The metric description */
	private final String description;
	/** indicates if the metric's measurer will require a ThreadInfo instance */
	private final boolean isRequiresTI;

	/**
	 * Returns this metric's measurer.
	 * @return the measurer
	 */
	public Measurer getMeasurer() {
		return measurer;
	}
	
	/**
	 * Returns a bitMask representing all the default metrics turned on.
	 * @return the default bitMask
	 */
	public static int getDefaultBitMask() {
		return defaultBitMask;
	}
	
	/**
	 * Returns the bitmask for all metrics enabled
	 * @return the bitmask for all metrics enabled
	 */
	public static int getAllEnabledBitMask() {
		return allBitMask;
	}
	
	
	
	/**
	 * indicates if the metric's measurer will require a ThreadInfo instance
	 * @return true if the metric's measurer will require a ThreadInfo instance
	 */
	public boolean isRequiresTI() {
		return isRequiresTI;
	}
	
	/**
	 * Indicates if this metric is a MinMaxAvg type counter, or a straight accumulation
	 * @return true if this metric is a MinMaxAvg type counter, false if a straight accumulation
	 */
	public boolean isMinMaxAvg() {
		return mma;
	}
	
	/**
	 * Indicates if this metric is turned on by default
	 * @return true if on by default
	 */
	public boolean isDefaultOn() {
		return defaultOn;
	}
	
	/**
	 * Determines if the passed bitMask requires a ThreadInfo for measurement.
	 * @param bitMask the bitMask to test
	 * @return true if the passed bitMask requires a ThreadInfo for measurement.
	 */
	public static boolean isRequiresTI(int bitMask) {
		for(int m: threadInfoRequiredMasks) {
			if((bitMask & m)==m) return true;
		}
		return false;
	}
	
	public static void main(String[] args) {
//		log("Test Needs TI");
		log("MinMaxAvg\n=========");
		String f = null;
		for(MetricCollection m: MetricCollection.values()) {
			f = Integer.toBinaryString(m.baseMask);
			if(m.isMinMaxAvg()) {
				log("\t" + m.name() + ":" + m.ordinal() + ", default:" + m.isDefaultOn() + ",  tiRequired:" + m.isRequiresTI + ",  shortName:" + m.getShortName() + ",  mask=" + m.baseMask + ", filter[" + f + "] :" + (f.length()) + "--> " + (m.measurer==null ? "NULL" : "Allocated"));
			}
		}
		log("Sum\n=========");
		for(MetricCollection m: MetricCollection.values()) {
			f = Integer.toBinaryString(m.baseMask);
			if(!m.isMinMaxAvg()) {
				log("\t" + m.name() + ":" + m.ordinal() + ", default:" + m.isDefaultOn() + ",  tiRequired:" + m.isRequiresTI + ",  shortName:" + m.getShortName() + ",  mask=" + m.baseMask + ", filter[" + f + "] :" + f.length() + "--> " + (m.measurer==null ? "NULL" : "Allocated"));
			}
		}
		
		log("Enum Count:" + MetricCollection.values().length);
		log("All Item Count:" + MetricCollection.itemCount);
		log("ALL BIT MASK:" + allBitMask + "\n============");
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(allBitMask)) {
				log("\t" + m.name());
			}
		}
		
		
//		log("CPU + WAITS:" + isRequiresTI(MetricCollection.SYS_CPU.getMask() | MetricCollection.WAIT_COUNT.getMask()));
//		log("CPU + ELAPSED:" + isRequiresTI(MetricCollection.SYS_CPU.getMask() | MetricCollection.ELAPSED.getMask()));
//		log("Test Base Filter");
//		int i = MetricCollection.builder().add(ELAPSED).add(COMPILATION).add(WAIT_COUNT).add(BLOCK_COUNT).build();
//		log("Standard Filter:" + i);
//		i &= ~SYS_CPU.getMask();
//		log("Standard Filter With SYS_CPU turned off:" + i);
//		i = i | SYS_CPU.getMask();
//		log("Standard Filter With SYS_CPU turned on:" + i);
//		i &= ~SYS_CPU.getMask();
//		log("Standard Filter With SYS_CPU turned off again:" + i);
		
	}
	
	public static void log(Object message) {
		System.out.println(message);
	}

	/** the JVM ThreadMXBean */
	private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	

	/**
	 * Returns the MetricCollection that has the passed ordinal.
	 * @param ordinal the ordinal to get the MetricCollection for
	 * @return a MetricCollection or null if the ordinal is invalid.
	 */
	public static MetricCollection ord(int ordinal) {
		return ORDINAL_MAP.get(ordinal);
	}
	
	
	/**
	 * Determines if this metric is enabled for the passed bitMask
	 * @param bitMask the bitMask to test
	 * @return true if this metric is enabled for the passed bitMask
	 */
	public boolean isEnabled(int bitMask) {
		return (bitMask & baseMask)==baseMask;
	}
	
	/**
	 * Returns an int as the passed bitMask enabled for this metric
	 * @param bitMask the bitmask to enable against
	 * @return the passed bitMask enabled for this metric
	 */
	public int enable(int bitMask) {
		return (bitMask | baseMask);
	}
	
	
	/**
	 * Returns the number of MetricCollection enum entries
	 * @return the number of MetricCollection enum entries
	 */
	public static int getItemCount() {
		return itemCount;
	}
	
	/**
	 * Returns the MetricCollection corresponding to the passed shortname
	 * @param shortName the shortname 
	 * @return the MetricCollection
	 */
	public static MetricCollection shortValueOf(String shortName) {
		if(!isValidShortName(shortName)) throw new RuntimeException("Invalid ShortName [" + shortName + "]");
		return SHORTNAME_MAP.get(shortName.toLowerCase());
	}
	

	
	/**
	 * Returns the metrics enabled bitMask for a comma delimited string of shortNames.
	 * @param shortNames a comma delimited string of short names
	 * @return the resulting bit mask
	 */
	public static int shortNameBitMask(String shortNames) {
		return shortNameBitMask(",", shortNames);
	}
	
	
	/**
	 * Returns the metrics enabled bitMask for a delimited string of shortNames.
	 * @param delim the delimeter
	 * @param shortNames a delimited string of short names
	 * @return the resulting bit mask
	 */
	public static int shortNameBitMask(String delim, String shortNames) {
		if(shortNames==null) return 0;
		return shortNameBitMask(shortNames.split(delim));
	}
	
	/**
	 * Returns the metrics enabled bitMask for an array of shortNames.
	 * @param shortNames an array of short names.
	 * @return the reulting bit mask
	 */
	public static int shortNameBitMask(String...shortNames) {
		if(shortNames==null || shortNames.length < 1) return 0;
		int base = 0;
		for(String s: shortNames) {
			if(isValidShortName(s)) {
				base = base | SHORTNAME_MAP.get(s).getMask();
			}
		}
		return base;
	}
	
	/**
	 * Returns a set of the MetricCollection enum names that are represented by the passed bit mask
	 * @param bitMask the bit mask
	 * @return a set of MetricCollection enum names 
	 */
	public static Set<String> namesForBitMask(int bitMask) {
		Set<String> names = new HashSet<String>(getItemCount(bitMask));
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) names.add(m.name());
		}
		return names;
	}
	
	/**
	 * Determines if the passed value is a valid shortname 
	 * @param shortName the value to test
	 * @return true of the passed value is a valid short name.
	 */
	public static boolean isValidShortName(String shortName) {
		return shortName != null && SHORTNAME_MAP.containsKey(shortName);
	}
	
	/**
	 * Determines if the passed value is a valid name 
	 * @param name the value to test
	 * @return true of the passed value is a valid name.
	 */
	public static boolean isValidName(CharSequence name) {
		try {
			MetricCollection.valueOf(name.toString().trim().toUpperCase());
			return true;
		} catch (Exception ex) {
			return false;
		}		
	}
	
	/**
	 * Translates the passed value into a MetricCollection.
	 * The value may be interpreted as a number in which case the ordinal will be decoded
	 * or as a MetricCollection name (from the toString()) in which case the name will be decoded.
	 * @param name The object to decode
	 * @return The decoded MetricCollection or null
	 */
	public static MetricCollection forValue(Object name) {
		MetricCollection mc = forValueOrNull(name);
		if(mc==null) throw new IllegalArgumentException("Invalid MetricCollection value [" + name + "]");
		return mc;
	}
	
	
	/**
	 * Translates the passed value into a MetricCollection.
	 * The value may be interpreted as a number in which case the ordinal will be decoded
	 * or as a MetricCollection name (from the toString()) in which case the name will be decoded.
	 * @param name The object to decode
	 * @return The decoded MetricCollection or null
	 */
	public static MetricCollection forValueOrNull(Object name) {
		if(name==null) return null;
		if(name instanceof Number || isNumber(name.toString())) {
			return ORDINAL_MAP.get(toInt(name));
		}
		try {
			return MetricCollection.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception ex) {
			return null;
		}		
	}
	
	private static int toInt(Object object) {
		if(object==null) throw new IllegalArgumentException("The passed object was null");
		if(object instanceof Number) {
			return ((Number)object).intValue();
		}
		return new Double(Double.parseDouble(object.toString())).intValue();		
	}
	
	private static boolean isNumber(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}	
	
	
	/**
	 * Returns the number of MetricCollection enum entries enabled for the passed bitMask
	 * @param bitMask the bitMask to get an enabled metric count for
	 * @return the number of enabled metric for the passed bitMask
	 */
	public static int getItemCount(int bitMask) {
		int cnt = 0;
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) cnt++;
		}
		return cnt;
	}
	
	/**
	 * Starts a measurement collection for the metric enabled by the passed bit mask
	 * @param bitMask the bitMask indicating which metrics are enabled.
	 * @param id The thread metric stack consecutive id
	 * @return The baseline measurements
	 */
	public static long[] start(int bitMask, long id) {
		try {
			long[] values = new long[itemCount];
			values[bitMaskIndex] = bitMask;
			values[snapshotIdIndex] = id;
			if(isRequiresTI(bitMask)) {
				currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
			}			
			for(MetricCollection m: MetricCollection.values()) {
				if(m.isEnabled(bitMask)) {
					values = executeMeasurement(values, m, true);
				}
			}			
			return values;
		} finally {
			currentThreadInfo.set(null);
		}
	}
	
	/**
	 * Captures baseline thread metrics.
	 * @param bitMask the bitMask indicating which metrics are enabled.
	 * @param id The thread metric stack consecutive id
	 * @return and array of thread stat baseline values.
	 */
	public static long[] baseline(int bitMask, long id) {
		long[] values = new long[itemCount];
		values[bitMaskIndex] = bitMask;
		values[snapshotIdIndex] = id;
		if(isRequiresTI(bitMask)) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MetricCollection m: MetricCollection.values()) {
			values = executeMeasurement(values, m, true);
		}
		currentThreadInfo.remove();
		return values;		
	}
	
	public static long[] baseline(long[] values) {
		if(isRequiresTI((int)values[bitMaskIndex])) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MetricCollection m: MetricCollection.values()) {
			values = executeMeasurement(values, m, false);
		}
		currentThreadInfo.remove();
		return values;				
	}
	
	public static String report(long[] values) {
		StringBuilder b = new StringBuilder("Metric Collection:");
		final int bitMask = (int)values[bitMaskIndex];
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) {
				b.append("\n\t").append(m.shortName).append("(").append(m.unit).append("):").append(values[m.ordinal()]);
			}
		}
		return b.toString();
	}
	
	/**
	 * Returns a Map of thread metrics as a delta off the baseline keyed by metric name.
	 * @param baseline The previously captured baseline.
	 * @return a Map of thread metrics as a delta off the baseline keyed by metric name.
	 */
	public static Map<String, Long> baselineDelta(long[] baseline) {
		if(baseline==null || baseline.length<1) return Collections.emptyMap();
		int bitMask = (int)baseline[itemCount];
		Map<String, Long> map = new HashMap<String, Long>();
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) {
				if(NS_METRICS.contains(m)) {
					map.put(m.getShortName() + " ms.", TimeUnit.MILLISECONDS.convert(baseline[m.ordinal()], TimeUnit.NANOSECONDS));
				} else {
					map.put(m.getShortName(), baseline[m.ordinal()]);
				}
			}
		}		
		return map;		
	}
	
	/** A set of MetricCollections that report values in nanoseconds. */
	public static final Set<MetricCollection> NS_METRICS = Collections.unmodifiableSet(new HashSet<MetricCollection>(
			Arrays.asList(new MetricCollection[]{
					MetricCollection.ELAPSED, MetricCollection.SYS_CPU, MetricCollection.USER_CPU
			})
	));
	
	
	
	
	/**
	 * Stops a measurement collection for a previously started collection and returns the delta of the current measurements and the starting measurements.
	 * @param baseline the baseline to calculate the deltas off
	 * @return an array of values  
	 */
	public static long[] stop(long[] baseline) {
		try {			
			int bitMask = (int)baseline[bitMaskIndex];
			if(isRequiresTI(bitMask)) {
				currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
			}						
			for(MetricCollection m: MetricCollection.values()) {
				executeMeasurement(baseline, m, false);
			}		
			return baseline;
		} finally {
			currentThreadInfo.set(null);
		}
	}
	

	
	/**
	 * Executes the actual measurement and updates the values array accordingly.
	 * If the measurement returned is -1, the measurement failed and the bitMask at
	 * the end of the values array will be updated to turn off the failed metric.
	 * @param values The values array
	 * @param m the MetricCollection being measured
	 * @param isStart true if the collection is starting, false if it is stopping.
	 * @return the updated values array
	 */
	private static long[] executeMeasurement(final long[] values, MetricCollection m, boolean isStart) {
		int bitMask = (int)values[bitMaskIndex];
		if(m.isEnabled(bitMask)) {
			long msrmnt = m.measurer.measure(isStart, values);
			if(msrmnt==-1) {				
				values[bitMaskIndex] = (bitMask &= ~m.getMask());			
			}
		} else {
			values[bitMaskIndex] = (bitMask &= ~m.getMask());
		}			
		return values;
	}
	
	
	
	/**
	 * Stops a measurement collection for a previously started collection and returns a map of the delta of the current measurements and the starting measurements keyed by short name.
	 * @param baseline The baseline of which to calculate the deltas
	 * @return a map of results   
	 */
	public static Map<String, Long> stopMap(long[] baseline) {
		long[] values = stop(baseline);
		int bitMask = (int)values[bitMaskIndex];
		Map<String, Long> map = new HashMap<String, Long>();
		for(MetricCollection m: MetricCollection.values()) {
			if(m.isEnabled(bitMask)) {
				map.put(m.getShortName(), values[m.ordinal()]);
			}
		}		
		return map;
	}
	
	/**
	 * Returns the metric collection bit mask code.
	 * @return the metric collection bit mask code
	 */
	public int getMask() {
		return baseMask;
	}
	
	/**
	 * Returns the unit of the metric
	 * @return the unit
	 */
	public String getUnit() {
		return unit;
	}

	/**
	 * Returns the short name of the metric
	 * @return the shortName
	 */
	public String getShortName() {
		return shortName;
	}

	/**
	 * Returns the description of the metric
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}
	
	
	/**
	 * Creates a new CollectionBuilder starting with a zero bit mask.
	 * @return a CollectionBuilder.
	 */
	public static CollectionBuilder builder() {
		return new CollectionBuilder();
	}
	
	/**
	 * Creates a new CollectionBuilder starting with the passed bit mask.
	 * @param baseMask the bit mask to start with.
	 * @return a CollectionBuilder.
	 */
	public static CollectionBuilder builder(int baseMask) {
		return new CollectionBuilder(baseMask);
	}
	
	/**
	 * Creates a new CollectionBuilder starting with the all options enabled bit mask.
	 * This is simply more efficient if you want more options enabled than disabled.
	 * @return a CollectionBuilder.
	 */
	public static CollectionBuilder allEnabledBuilder() {
		return new CollectionBuilder(CollectionBuilder.getAllOptionsEnabledMask());
	}


	
	public static class CollectionBuilder {
		/** enabled options */
		private Set<MetricCollection> enabled = new HashSet<MetricCollection>(MetricCollection.itemCount);
		/** current builder working base */
		private int baseMask = 0; 
		/** the bit mask for all options enabled */
		private static int ALL_OPTIONS = 0;
		
		static {
			for(MetricCollection m: MetricCollection.values()) {
				ALL_OPTIONS = ALL_OPTIONS | m.getMask();
			}
		}
		
		/**
		 * Returns the bit mask for all options enabled.
		 * @return the bit mask for all options enabled.
		 */
		public static int getAllOptionsEnabledMask() {
			return ALL_OPTIONS;
		}
		
		/**
		 * Private Ctor. Sets base mask to zero.
		 */
		private CollectionBuilder(){};
		
		/**
		 * Private Ctor. Sets base mask to existing mask.
		 * @param an existing mask to start the builder off with.
		 */
		private CollectionBuilder(int baseMask){this.baseMask = baseMask;};

		
		/**
		 * Compiles the bitmask for the build combination of metrics.
		 * @return the bitmask 
		 */
		public int build() {
			for(MetricCollection mc: enabled) {
				baseMask = baseMask | mc.getMask();
			}
			return baseMask;
		}
		
		/**
		 * Enables a metric collection
		 * @param mc The MetricCollection to enable
		 * @return this builder
		 */
		public CollectionBuilder add(MetricCollection mc) {
			enabled.add(mc);
			return this;
		}
		
		/**
		 * Disables a metric collection
		 * @param mc The MetricCollection to disable
		 * @return this builder
		 */
		public CollectionBuilder remove(MetricCollection mc) {
			enabled.remove(mc);
			return this;
		}
		
		/**
		 * Adds the MetricCollection identified by the passed short name to the builder
		 * @param shortName
		 * @return this builder
		 */
		public CollectionBuilder add(String shortName) {
			if(shortName!=null) {
				if(SHORTNAME_MAP.containsKey(shortName.toLowerCase())) {
					enabled.add(MetricCollection.shortValueOf(shortName.toLowerCase()));
				}			
			}
			return this;
		}
		
		/**
		 * Removes the MetricCollection identified by the passed short name from the builder
		 * @param shortName
		 * @return this builder
		 */
		public CollectionBuilder remove(String shortName) {
			if(shortName!=null) {
				if(SHORTNAME_MAP.containsKey(shortName.toLowerCase())) {
					enabled.remove(MetricCollection.shortValueOf(shortName.toLowerCase()));
				}
			}
			return this;
		}
		
		
	}

	
	/**
	 * <p>Title: DefaultSysCpuMeasurer</p>
	 * <p>Description: Default system cpu time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultSysCpuMeasurer</code></p>
	 */
	public static class DefaultSysCpuMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultSysCpuMeasurer
		 * @param metricOrdinal
		 */
		public DefaultSysCpuMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			try {
				return TimeUnit.MICROSECONDS.convert(threadMXBean.getCurrentThreadCpuTime(), TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				return -1L;
			}			
		}
	}
	/**
	 * <p>Title: DefaultUserCpuMeasurer</p>
	 * <p>Description: Default user cpu time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultUserCpuMeasurer</code></p>
	 */
	public static class DefaultUserCpuMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultUserCpuMeasurer
		 * @param metricOrdinal
		 */
		public DefaultUserCpuMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			try {
				return TimeUnit.MICROSECONDS.convert(threadMXBean.getCurrentThreadUserTime(), TimeUnit.NANOSECONDS);
			} catch (Exception e) {
				return -1L;
			}			
		}
	}
	
	/**
	 * <p>Title: DefaultWaitCountMeasurer</p>
	 * <p>Description: Default wait time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultWaitCountMeasurer</code></p>
	 */
	public static class DefaultWaitCountMeasurer extends AbstractDeltaMeasurer  {
		/**
		 * Creates a new DefaultWaitCountMeasurer
		 * @param metricOrdinal
		 */
		public DefaultWaitCountMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getWaitedCount();
		}
	}
	
	/**
	 * <p>Title: DefaultWaitTimeMeasurer</p>
	 * <p>Description: Default wait time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultWaitTimeMeasurer</code></p>
	 */
	public static class DefaultWaitTimeMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultWaitCountMeasurer
		 * @param metricOrdinal
		 */
		public DefaultWaitTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getWaitedTime();
		}
	}
	/**
	 * <p>Title: DefaultBlockCountMeasurer</p>
	 * <p>Description: Default block count measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultBlockCountMeasurer</code></p>
	 */
	public static class DefaultBlockCountMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultBlockCountMeasurer
		 * @param metricOrdinal
		 */
		public DefaultBlockCountMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getBlockedCount();
		}
	}

	/**
	 * <p>Title: DefaultBlockTimeMeasurer</p>
	 * <p>Description: Default block time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultBlockTimeMeasurer</code></p>
	 */
	public static class DefaultBlockTimeMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultBlockTimeMeasurer
		 * @param metricOrdinal
		 */
		public DefaultBlockTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return currentThreadInfo.get().getBlockedTime();
		}
	}
	
	/**
	 * <p>Title: DefaultElapsedTimeMeasurer</p>
	 * <p>Description: Default elapsed time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 * <p><code>com.heliosapm.jmx.metrics.DefaultElapsedTimeMeasurer</code></p>
	 */
	public static class DefaultElapsedTimeMeasurer extends AbstractDeltaMeasurer {
		/**
		 * Creates a new DefaultBlockTimeMeasurer
		 * @param metricOrdinal
		 */
		public DefaultElapsedTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return System.nanoTime();
		}		
	}
	
	/**
	 * <p>Title: DefaultCompilationTimeMeasurer</p>
	 * <p>Description: Default elapsed compilation time measurer</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead
	 * @version $LastChangedRevision$
	 */
	public static class DefaultCompilationTimeMeasurer  extends AbstractDeltaMeasurer {
		private final CompilationMXBean cmx = ManagementFactory.getCompilationMXBean();
		/**
		 * Creates a new DefaultCompilationTimeMeasurer
		 * @param metricOrdinal
		 */
		public DefaultCompilationTimeMeasurer(int metricOrdinal) {
			super(metricOrdinal);
		}
		@Override
		protected long sample() {
			return cmx.getTotalCompilationTime();
		}
	}
		
	
	
	

}
/**
 * <p>Title: MC</p>
 * <p>Description: Interface to provide a bit mask seed provider for the enum.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead
 * @version $LastChangedRevision$
 * <p><code>com.heliosapm.jmx.metrics.MC</code></p>
 */
interface MC {
	/** The enum bit mask seed */
	final MCBitMaskSeed seed = new MCBitMaskSeed();
}
/**
 * <p>Title: MCBitMaskSeed</p>
 * <p>Description: A bit mask seed provider</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead
 * @version $LastChangedRevision$
 * <p><code>com.heliosapm.jmx.metrics.MetricCollection.MCBitMaskSeed</code></p>
 */
class MCBitMaskSeed {
	/** The int seed  */
	private int seed = 2;
	
	/**
	 * Returns the next seed in the sequence which is 2 X the prior value.
	 * @return the next seed in the sequence
	 */
	public synchronized int next() {
		int ret = seed;
		seed = seed*2;
		return ret;
	}
}





/*
StringBuilder b = new StringBuilder("0000000000");
char one = '1';
char zero = '0';
for(baseMask in 0..9) {
    int index = 9-baseMask;
    b.setCharAt(index, one);    
    println "b:${b}\t${Integer.parseInt(b.toString(), 2)}";
    b.setCharAt(index, zero);
}

int val = 1 | 512;
println "Val:${Integer.toBinaryString(val)}";

println "Is 16 Enabled:${(val & 16)==16}";
*/


