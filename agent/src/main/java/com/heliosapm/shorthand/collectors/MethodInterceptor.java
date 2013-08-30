/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.collectors;

import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.heliosapm.shorthand.accumulator.HeaderOffset;
import com.heliosapm.shorthand.collectors.measurers.AbstractDeltaMeasurer;
import com.heliosapm.shorthand.collectors.measurers.DefaultMeasurer;
import com.heliosapm.shorthand.collectors.measurers.DelegatingMeasurer;
import com.heliosapm.shorthand.collectors.measurers.InvocationMeasurer;
import com.heliosapm.shorthand.collectors.measurers.Measurer;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MethodInterceptor</p>
 * <p>Description: An {@link ICollector} that collects a set of metrics about method invocations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.MethodInterceptor</code></p>
 */

public enum MethodInterceptor implements ICollector<MethodInterceptor> {
	/** The elapsed system cpu time in microseconds */
	SYS_CPU(seed.next(), false, true, "CPU Time (\u00b5s)", "syscpu", "CPU Thread Execution Time", new DefaultSysCpuMeasurer(0), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The elapsed user mode cpu time in microseconds */
	USER_CPU(seed.next(), false, true, "CPU Time (\u00b5s)", "usercpu", "CPU Thread Execution Time In User Mode", new DefaultUserCpuMeasurer(1), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The number of thread waits on locks or other concurrent barriers */
	WAIT_COUNT(seed.next(), false, true, "Thread Waits", "waits", "Thread Waiting On Notification Count", new DefaultWaitCountMeasurer(2), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The thread wait time on locks or other concurrent barriers */
	WAIT_TIME(seed.next(), false, true, "Thread Wait Time (ms)", "waittime", "Thread Waiting On Notification Time", new DefaultWaitTimeMeasurer(3), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The number of thread waits on synchronization monitors */
	BLOCK_COUNT(seed.next(), false, true, "Thread Blocks", "blocks", "Thread Waiting On Monitor Entry Count", new DefaultBlockCountMeasurer(4), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The thread wait time on synchronization monitors  */
	BLOCK_TIME(seed.next(), false, true, "Thread Block Time (ms)", "blocktime", "Thread Waiting On Monitor Entry Time", new DefaultBlockTimeMeasurer(5), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The elapsed wall clock time in ns. */
	ELAPSED(seed.next(), true, false, "Elapsed Time (ns)", "elapsed", "Elapsed Execution Time", new DefaultElapsedTimeMeasurer(6), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The elapsed compilation time in ms */
	COMPILATION(seed.next(), false, false, "Elapsed Compilation Time (ms)", "compilation", "Elapsed Compilation Time", new DefaultCompilationTimeMeasurer(7), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The number of threads concurrently passing through the instrumented joinpoint  */
	METHOD_CONCURRENCY(seed.next(), false, false, "Method Concurrent Thread Execution Count", "methconcurrent", "Number Of Threads Executing Concurrently In The Same Method", new DelegatingMeasurer(new DefaultMeasurer(8)), DataStruct.getInstance(Primitive.LONG, 3, Long.MAX_VALUE, Long.MIN_VALUE, -1L), "Min", "Max", "Avg"),
	/** The total number of invocations of the instrumented joinpoint  */
	INVOCATION_COUNT(seed.next(), true, false, "Method Invocations", "invcount", "Method Invocation Count", new InvocationMeasurer(9), DataStruct.getInstance(Primitive.LONG, 1, 0), "Count"),
	/** The total number of successful invocations of the instrumented method */
	RETURN_COUNT(seed.next(), false, false, "Method Returns", "retcount", "Method Return Count", new DelegatingMeasurer(new DefaultMeasurer(10)), DataStruct.getInstance(Primitive.LONG, 1, 0), "Count"),
	/** The total number of invocations of the instrumented method that terminated on an exception */
	EXCEPTION_COUNT(seed.next(), false, false, "Method Invocation Exceptions ", "exccount", "Method Invocation Exception Count", new DelegatingMeasurer(new DefaultMeasurer(11)), DataStruct.getInstance(Primitive.LONG, 1, 0), "Count");
	
	
	private static final TIntObjectHashMap<MethodInterceptor> ORD2ENUM;
	
	
	/**
	 * The private enum ctor
	 * @param baseMask the seed 
	 * @param defaultOn If true, this metric is turned on by default
	 * @param isRequiresTI indicates if the metric's measurer will require a ThreadInfo instance
	 * @param unit the unit of the metric
	 * @param shortName a short name for the metric
	 * @param description the description of the metric
	 * @param measurer the measurement capturing procedure for this metric
	 * @param ds The data struct describing the memory allocation required for collected metrics
	 * @param dependencies The ordinals of this collector's dependencies
	 * @param subNames The metric sub names
	 */
	private MethodInterceptor(int baseMask, boolean defaultOn, boolean isRequiresTI, 
			String unit, String shortName, String description, Measurer measurer, DataStruct ds, int[] dependencies, String...subNames) {
		this.baseMask = baseMask;
		this.defaultOn = defaultOn;
		this.isRequiresTI = isRequiresTI;
		this.unit = unit;
		this.shortName = shortName;
		this.description = description;
		this.measurer = measurer;
		this.ds = ds;
		this.subNames = subNames;
		this.dependencies = dependencies;
		if(ds.size != subNames.length) {
			throw new IllegalArgumentException("DataStruct Size [" + ds.size + "] was not the same as sub names length [" + subNames.length + "] for [" + name() + "]. Programmer Error.");
		}
	}
	
	/**
	 * The private enum ctor with no dependencies
	 * @param baseMask the seed 
	 * @param defaultOn If true, this metric is turned on by default
	 * @param isRequiresTI indicates if the metric's measurer will require a ThreadInfo instance
	 * @param unit the unit of the metric
	 * @param shortName a short name for the metric
	 * @param description the description of the metric
	 * @param measurer the measurement capturing procedure for this metric
	 * @param ds The data struct describing the memory allocation required for collected metrics
	 * @param subNames The metric sub names
	 */
	private MethodInterceptor(int baseMask, boolean defaultOn, boolean isRequiresTI, 
			String unit, String shortName, String description, Measurer measurer, DataStruct ds, String...subNames) {
		this(baseMask, defaultOn, isRequiresTI, unit, shortName, description, measurer, ds, new int[0], subNames);
	}
	
	
	/**
	 * Returns a bitmask enabled for all the passed MethodInterceptor 
	 * @param mis the MethodInterceptors to enable for
	 * @return a bitmask
	 */
	public static int getBitMaskFor(MethodInterceptor...mis) {
		int mask = 0;
		for(MethodInterceptor mi: mis) {
			mask = mi.enable(mask);
		}
		return mask;
	}
	
	public static void main(String[] args) {
		for(MethodInterceptor mi: MethodInterceptor.values()) {
			log(mi.name() + " (" + mi.ordinal() + "/" + mi.baseMask + ")  DataStruct:" + mi.ds.dump());
		}
		log("Default BitMask:" + defaultMetricsMask);
		log("All BitMask:" + allMetricsMask);
		log("Item Count:" + values().length);
		
		Map<MethodInterceptor, Long> offsets = (Map<MethodInterceptor, Long>) EnumCollectors.getInstance().offsets(MethodInterceptor.class.getName(), allMetricsMask);
		log("Offsets:" + offsets.size());
		for(MethodInterceptor mi: offsets.keySet()) {
			log("\t" + mi.name() + " : " + offsets.get(mi));
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
	/** The total number of collectors */
	public static final int itemCount;
	/** The location in the values array where the bit mask can be found */
	public static final int bitMaskIndex;
	/** The location in the open close indicator can be found */
	public static final int openCloseIndex;
	
	/** The bitmask for all metrics enabled */
	public static final int allMetricsMask;
	/** The bitmask for default metrics enabled */
	public static final int defaultMetricsMask;
	/** The pre-apply collectors, needed to be done before the others,  in the order specified */
	public static final Set<MethodInterceptor> preApplies = EnumSet.of(MethodInterceptor.INVOCATION_COUNT);
	/** an array of bitMasks for metrics that require a ThreadInfo for measurement */
	private static final int[] threadInfoRequiredMasks;
	/** the current measurement ThreadInfo */
	private static final ThreadLocal<ThreadInfo> currentThreadInfo = new ThreadLocal<ThreadInfo>();
	/** the JVM ThreadMXBean */
	private static final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
	

	
	

	
	
	static {
		 
		
		MethodInterceptor[] values = MethodInterceptor.values();
		itemCount = values.length;
		bitMaskIndex = values.length;
		openCloseIndex = bitMaskIndex +1; 
		TIntHashSet tirs = new TIntHashSet(values.length);
		int _allMaskIndex = 0, _defaultMaskIndex = 0;
		ORD2ENUM = new TIntObjectHashMap<MethodInterceptor>(values.length, 0.1f); 
		for(MethodInterceptor mi: values) {
			ORD2ENUM.put(mi.ordinal(), mi);
			_allMaskIndex = mi.enable(_allMaskIndex);
			if(mi.isDefaultOn()) _defaultMaskIndex = mi.enable(_defaultMaskIndex);
			if(mi.isRequiresTI) tirs.add(mi.ordinal());
		}
		allMetricsMask = _allMaskIndex;
		defaultMetricsMask = _defaultMaskIndex;
		threadInfoRequiredMasks = tirs.toArray();
	}

	
	/** The internal code */
	public final int baseMask;
	/** indicates if this metric is turned on by default */
	public final boolean defaultOn;
	/** indicates if the metric's measurer will require a ThreadInfo instance */
	private final boolean isRequiresTI;
	/** The metric unit */
	private final String unit;	
	/** The metric short name */
	private final String shortName;
	/** The metric description */
	private final String description;
	/** the measurer */
	private final Measurer measurer;
	/** The data struct */
	public final DataStruct ds;
	/** The metric sub names */
	private final String[] subNames;
	/** The ordinals of this collector's dependencies */
	private final int[] dependencies;
	
	
	/**
	 * Returns the total allocation for the passed bitmask
	 * @param bitMask The bitmask to calculate total allocation for
	 * @return the total number of bytes needed for the passed bitmask
	 */
	public static int getTotalAllocation(int bitMask) {
		int total = 0;
		for(MethodInterceptor mi: MethodInterceptor.values()) {
			if(mi.isEnabled(bitMask)) {
				total += mi.ds.byteSize;
			}
		}
		return total + HeaderOffset.HEADER_SIZE;
	}
	
	
	/**
	 * Returns an array of the MethodInterceptor names enabled for the passed bit mask
	 * @param bitMask the enabled metric bit mask
	 * @return an array of MethodInterceptor names
	 */
	public static String[] enabledNames(int bitMask) {
		Set<MethodInterceptor> enabled = enabledCollectors(bitMask);
		String[] names = new String[enabled.size()];
		int cnt = 0;
		for(MethodInterceptor mi: enabled) {
			names[cnt] = mi.name();
			cnt++;
		}
		return names;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getEnabledNames(int)
	 */
	@Override
	public String[] getEnabledNames(int bitMask) {
		return enabledNames(bitMask);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getAllocationFor(int)
	 */
	@Override
	public long getAllocationFor(int bitMask) {
		return getTotalAllocation(bitMask);
	}
	
	/**
	 * Returns a set of the MethodInterceptors enabled for the passed bit mask
	 * @param bitMask the enabled metric bit mask
	 * @return a set of MethodInterceptors
	 */
	public static Set<MethodInterceptor> enabledCollectors(int bitMask) {
		Set<MethodInterceptor> enabled = EnumSet.noneOf(MethodInterceptor.class);		
		for(MethodInterceptor mi: MethodInterceptor.values()) {
			if(mi.isEnabled(bitMask)) {
				enabled.add(mi);
			}
		}
		return enabled;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getEnabledCollectors(int)
	 */
	@Override
	public Set<MethodInterceptor> getEnabledCollectors(int bitMask) {
		return enabledCollectors(bitMask);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getMeasurer()
	 */
	@Override
	public Measurer getMeasurer() {
		return measurer;
	}

	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#isDefaultOn()
	 */
	@Override
	public boolean isDefaultOn() {
		return defaultOn;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#isEnabled(int)
	 */
	@Override
	public boolean isEnabled(int bitMask) {
		return (bitMask & baseMask)==baseMask;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#enable(int)
	 */
	@Override
	public int enable(int bitMask) {
		return (bitMask | baseMask);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getMask()
	 */
	@Override
	public int getMask() {
		return baseMask;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getUnit()
	 */
	@Override
	public String getUnit() {
		return unit;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getShortName()
	 */
	@Override
	public String getShortName() {
		return shortName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#collectors()
	 */
	@Override
	public MethodInterceptor[] collectors() {
		return MethodInterceptor.values();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getDataStruct()
	 */
	@Override
	public DataStruct getDataStruct() {
		return ds;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getSubMetricNames()
	 */
	@Override
	public String[] getSubMetricNames() {
		return subNames;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getBitMaskIndex()
	 */
	@Override
	public int getBitMaskIndex() {
		return bitMaskIndex;
	}
	
	/** An empty method interceptor array const. */
	private static final MethodInterceptor[] EMPTY_INTERCEPTOR_ARR = {};
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#getPreApplies(int)
	 */
	@Override
	public MethodInterceptor[] getPreApplies(int bitmask) {		
		Set<MethodInterceptor> copy = EnumSet.copyOf(preApplies);
		for(Iterator<MethodInterceptor> iter = copy.iterator(); iter.hasNext();) {
			if(!iter.next().isEnabled(bitmask)) iter.remove(); 
		}
		if(copy.isEmpty()) return EMPTY_INTERCEPTOR_ARR;
		return copy.toArray(new MethodInterceptor[copy.size()]);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#isPreApply()
	 */
	@Override
	public boolean isPreApply() {
		return preApplies.contains(this);
	}
	
	/**
	 * Returns the offsets for the passed bitMask
	 * @param bitMask The bitmask to get offsets for
	 * @return a map of offsets keyed by the associated method interceptor
	 */
	@Override
	public Map<MethodInterceptor, Long> getOffsets(int bitMask) {
		final Set<MethodInterceptor> icollectors = getEnabledCollectors(bitMask);
		final Map<MethodInterceptor, Long> offsets = new EnumMap(MethodInterceptor.class);
		long offset = 0;
		for(MethodInterceptor t: icollectors) {
			offsets.put(t, offset + HeaderOffset.HEADER_SIZE);
			offset += t.getDataStruct().byteSize;
		}
		return offsets;
		
	}
	

	/** The memory size of a MinMaxAvg triplet */
	public static final long MIN_MAX_AVG_SIZE = 3*UnsafeAdapter.LONG_SIZE;
	
	protected final AtomicInteger applyConcurrency = new AtomicInteger(0); 
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#apply(long, long[])
	 * FIXME:  Only works for longs
	 */
	@Override
	public void apply(long address, long[] collectedValues) {
		long v = collectedValues[ordinal()];
		if(this.ds.size==1) {
			long base = UnsafeAdapter.getLong(address) + v;
			UnsafeAdapter.putLong(address, base);
//			if(this.isPreApply()) {
//				collectedValues[ordinal()] = v;
//			}
		} else {			
			if(collectedValues[openCloseIndex]!=0) {
				System.err.println("Unclosed snap " + Arrays.toString(collectedValues));
			}
			long offset = address;
//			long[] state = new long[3];
//			UnsafeAdapter.copyMemory(null, address, state, UnsafeAdapter.LONG_ARRAY_OFFSET, MIN_MAX_AVG_SIZE);
//			if(v < state[0]) state[0] = v;
//			if(v > state[1]) state[1] = v;
//			state[2] = rollingAvg(v, state[2], collectedValues[INVOCATION_COUNT.ordinal()]);
//			UnsafeAdapter.copyMemory(state, UnsafeAdapter.LONG_ARRAY_OFFSET, null, address, MIN_MAX_AVG_SIZE);		
			
			if(v < UnsafeAdapter.getLong(offset)) {
				UnsafeAdapter.putLong(offset, v);
			}
			offset += 8;
			if(v > UnsafeAdapter.getLong(offset)) {
				UnsafeAdapter.putLong(offset, v);				
			}
			offset += 8;
			// ===============
			// Changing to capture total and deferring Avg calc to flush 
			// ===============
			if(UnsafeAdapter.getLong(offset)==-1L) {
				UnsafeAdapter.putLong(offset, v);
			} else {
				UnsafeAdapter.putLong(offset, UnsafeAdapter.getLong(offset) + v);
			}
			//UnsafeAdapter.putLong(offset, rollingAvg(v, UnsafeAdapter.getLong(offset), collectedValues[INVOCATION_COUNT.ordinal()]));
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#preFlush(long, int)
	 */
	@Override
	public void preFlush(long address, int bitMask) {
		if(!INVOCATION_COUNT.isEnabled(bitMask)) return;
		Map<MethodInterceptor, Long> offsets =  getOffsets(bitMask);
		long invCount = UnsafeAdapter.getLong(address + offsets.get(INVOCATION_COUNT));
		if(invCount<1) return;
		long offset = address + HeaderOffset.HEADER_SIZE;
		for(MethodInterceptor mi: getEnabledCollectors(bitMask)) {
			if(mi.getDataStruct().size!=3) {
				offset += UnsafeAdapter.LONG_SIZE;				
			} else {
				offset += (UnsafeAdapter.LONG_SIZE*2);
				UnsafeAdapter.putLong(offset, UnsafeAdapter.getLong(offset)/invCount);
				offset += UnsafeAdapter.LONG_SIZE;
			}			
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#resetMemSpace(long, int)
	 */
	@Override
	public void resetMemSpace(long address, int bitmask) {
		long offset = address + HeaderOffset.HEADER_SIZE;
		for(MethodInterceptor mi: getEnabledCollectors(bitmask)) {
			UnsafeAdapter.putLongs(offset, (long[])mi.getDataStruct().defaultValues);
			offset += (UnsafeAdapter.LONG_SIZE * mi.getDataStruct().size); 
		}		
	}



	/**
	 * Returns the default [reset] values for a collector's mem-space
	 * @param bitMask The bitmask of all enabled metrics
	 * @return an array of default values for each enabled metric
	 */
	public long[][] getDefaultValues(int bitMask) {
		Set<MethodInterceptor> enabled = getEnabledCollectors(bitMask);
		long[][] defValues = new long[enabled.size()][];
		int index = 0;
		for(MethodInterceptor mi: enabled) {
			defValues[index] = (long[])mi.getDataStruct().defaultValues;
			index++;
		}
		return defValues;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.collectors.ICollector#preApply(long, long[])
	 */
	@Override
	public void preApply(long address, long[] collectedValues) {
		UnsafeAdapter.putLong(address, UnsafeAdapter.getLong(address) + collectedValues[ordinal()]);
	}
	
	
	// ==================================================================================================
	// ==================================================================================================
	//  FIXME:  methodEnter and methodExit should be compiled
	// ==================================================================================================
	// ==================================================================================================
	
	/**
	 * Captures method entry metric baselines.
	 * @param bitMask the bitMask indicating which metrics are enabled.
	 * @return and array of thread stat baseline values.
	 */
	public static long[] methodEnter(int bitMask) {
		long[] values = new long[itemCount+2];
		values[bitMaskIndex] = bitMask;
		values[openCloseIndex] = 1;
		if(isRequiresTI(bitMask)) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MethodInterceptor m: MethodInterceptor.values()) {
			if(m.isEnabled(bitMask)) {
				values = executeMeasurement(values, m, true);
			}
		}
		currentThreadInfo.remove();
		return values;		
	}
	
	/**
	 * Captures method exit metrics.
	 * @param values The method entry caputed baseline
	 * @return and array of thread stat baseline values.
	 */
	public static long[] methodExit(long[] values) {
		int bitMask = (int)values[bitMaskIndex];
		if(isRequiresTI(bitMask)) {
			currentThreadInfo.set(threadMXBean.getThreadInfo(Thread.currentThread().getId()));
		}			
		for(MethodInterceptor m: MethodInterceptor.values()) {
			if(m.isEnabled(bitMask)) {
				values = executeMeasurement(values, m, false);
			}
		}
		currentThreadInfo.remove();
		values[openCloseIndex] = 0;
		return values;				
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
	
	/**
	 * Executes the actual measurement and updates the values array accordingly.
	 * If the measurement returned is -1, the measurement failed and the bitMask at
	 * the end of the values array will be updated to turn off the failed metric.
	 * @param values The values array
	 * @param m the MetricCollection being measured
	 * @param isStart true if the collection is starting, false if it is stopping.
	 * @return the updated values array
	 */
	private static long[] executeMeasurement(final long[] values, MethodInterceptor m, boolean isStart) {
		m.measurer.measure(isStart, values);
		return values;
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
		//private static final long startTime;
		
		static {
			long min = Long.MAX_VALUE;
			long max = Long.MIN_VALUE;
			long last = -1;
			for(int i = 0; i < 10000; i++) {
				long start = System.nanoTime();
				long elapsed = System.nanoTime()-start;
				if(elapsed<min) min = elapsed;
				if(elapsed>max) max = elapsed;
				last = elapsed;
			}
			log("HIGH REZ Clock Warmup. Min:" + min + "  Max:" + max + "  Last:" + last);
		}
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
