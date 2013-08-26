
package com.heliosapm.shorthand.accumulator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.MethodInterceptor;

/**
 * <p>Title: AccumulatorLoad</p>
 * <p>Description: A non-unit load test</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.accumulator.AccumulatorLoad</code></p>
 */

public class AccumulatorLoad implements ThreadFactory, Thread.UncaughtExceptionHandler {
	public static final int THREAD_COUNT = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()/2;
	public static final int WARMUP_LOOPS = 1600;
	public static final int RUN_LOOPS = 1000000;
	public static final long LOOP_RUN_TIME = 1000 * 60 * 15;
	public static final int METRIC_COUNT = 1000;
//	public static final int SLEEP_TIME = 10;
	public static final int BIT_MASK = MethodInterceptor.allMetricsMask & ~MethodInterceptor.USER_CPU.baseMask;
	//public static final int BIT_MASK = MethodInterceptor.defaultMetricsMask;
	//public static final int BIT_MASK = MethodInterceptor.INVOCATION_COUNT.baseMask;
	
	/** Thread id factory */
	public static final AtomicLong serial = new AtomicLong(0);
	public static final Random RANDOM = new Random(System.currentTimeMillis());
	
	/** The start latch */
	private final CountDownLatch startLatch;
	/** The completion latch */
	private final CountDownLatch completionLatch;
	
	private final MetricSnapshotAccumulator accumulator;

	private AccumulatorLoad(int threadCount, int warmupLoops, int runLoops, int metricCount, int bitMask) {
		startLatch = new CountDownLatch(1);
		completionLatch = new CountDownLatch(threadCount);
		String[] metricNames = new String[metricCount];
		String[] warmupMetricNames = new String[metricCount];
		//Iterator<Object> iter = System.getProperties().keySet().iterator();
		for(int i = 0; i < metricCount; i++) { metricNames[i] = UUID.randomUUID().toString();  }
		for(int i = 0; i < metricCount; i++) { warmupMetricNames[i] = UUID.randomUUID().toString(); }
		CollectorSet<MethodInterceptor> cs = new CollectorSet<MethodInterceptor>(MethodInterceptor.class, bitMask);
		log("Data Mapper [%s]", cs.getDataMapper().getClass().getName());
		accumulator = MetricSnapshotAccumulator.getInstance();
		log("Starting Warmup");
		accumulator.setDebug(true);
		long[] snap;
		//accumulator.setDebug(true);
		long start = System.nanoTime();
		for(int i = 0; i < warmupLoops; i++) {
			snap = MethodInterceptor.methodEnter(bitMask);	
			for(String metricName: warmupMetricNames) {				
				accumulator.snap(metricName, cs, MethodInterceptor.methodExit(snap));
			}
		}
		log(AccumulatorThreadStats.report());
		long elapsed = System.nanoTime()-start;
		log("Warmup Complete");
		log(reportSummary("Warmup", elapsed, warmupLoops*metricCount));
//		log("\n\n===================SUMMARY======================\n\n");
//		log(accumulator.summary(warmupMetricNames));
		final long[] elapsedTimes = new long[threadCount];
		for(int i = 0; i < threadCount; i++) {
			newThread(buildLoadRunner(metricNames, runLoops, bitMask, i, elapsedTimes)).start();
			//newThread(buildLoadRunner(warmupMetricNames, runLoops, bitMask)).start();
		}
		accumulator.setDebug(false);
		start = System.nanoTime();
		startLatch.countDown();
		try { completionLatch.await(); } catch (Exception ex) {
			ex.printStackTrace(System.err);
			return;
		}
		elapsed = System.nanoTime()-start;
//		log("Full Run Complete");
		log("==================================");
		long totalRecordings = runLoops*metricCount*threadCount;
		log(reportSummary("Full Run for [" + totalRecordings + "]", elapsed, totalRecordings));
		log("==================================");
		System.exit(1);
//		log("\n\n===================SUMMARY======================\n\n");
//		log(accumulator.summary(metricNames));
	}
	
	private Runnable buildLoadRunner(final String[] metricNames, final int runLoops, final int bitMask, final int slot, final long[] elapsedTimes) {
		return new Runnable() {
			final CollectorSet<MethodInterceptor> cs = new CollectorSet<MethodInterceptor>(MethodInterceptor.class, bitMask);
			public void run() {
				final long startTime = System.currentTimeMillis(), endTime = startTime + LOOP_RUN_TIME;
				while(true) {
					Set<String> _metricNames = new HashSet<String>(metricNames.length);
					Collections.addAll(_metricNames, metricNames.clone());
					//Collections.shuffle(_metricNames, RANDOM);
					long[] snap;
					if(startLatch.getCount()>0) {
						try { startLatch.await(); } catch (Exception ex) {}
					}
					long start = System.nanoTime();
					//log("Starting Thread [%s]...", Thread.currentThread().toString());
					for(int i = 0; i < runLoops; i++) {
						for(String metricName: _metricNames) {
							snap = MethodInterceptor.methodEnter(bitMask);
							accumulator.snap(metricName, cs, MethodInterceptor.methodExit(snap));		
						}
					}
					elapsedTimes[slot] = System.nanoTime()-start;
					//log(reportSummary("Full Boat", elapsed, runLoops*metricNames.length));
					AccumulatorThreadStats.reset();
					//log("Thread [%s] completed. Stats:%s", Thread.currentThread().getName(), AccumulatorThreadStats.report());
					if(System.currentTimeMillis()>=endTime) break;
				}
				completionLatch.countDown();
			}
		};
	}
	
	protected static String report(long[] elapsed) {
		StringBuilder b = new StringBuilder("Full Run Test Summary");
		
		return b.toString();
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Accumulator Load Test");
		ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
		tmx.setThreadContentionMonitoringEnabled(true);
		tmx.setThreadCpuTimeEnabled(true);
		log("BITMASK:" + BIT_MASK);
		int mask = 0;
		for(MethodInterceptor mi: MethodInterceptor.values()) {
			if(mi==MethodInterceptor.USER_CPU) continue;
			mask=  mi.enable(mask);
		}
		log("CALCED:" + mask);
		
		new AccumulatorLoad(THREAD_COUNT, WARMUP_LOOPS, RUN_LOOPS, METRIC_COUNT, BIT_MASK);
		
		
	}

	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	public static String reportTimes(String title, long nanos) {
		StringBuilder b = new StringBuilder(title).append(":  ");
		b.append(nanos).append( " ns.  ");
		b.append(TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " \u00b5s.  ");
		b.append(TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " ms.  ");
		b.append(TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " s.");
		return b.toString();
	}
	
	public static String reportAvgs(String title, long nanos, long count) {
		if(nanos==0 || count==0) return reportTimes(title, 0);
		return reportTimes(title, (nanos/count));
	}
	
	public static String reportSummary(String title, long nanos, long count) {
		return reportTimes(title, nanos) + 
				"\n" +
				reportAvgs(title + "  AVGS", nanos, count);
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "AccumulatorLoadThread#" + serial.incrementAndGet());
		t.setDaemon(true);
		t.setUncaughtExceptionHandler(this);
		return t;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.err.println("Uncaught exception on thread [" + t.getName() + "]. Stack trace follows:");
		e.printStackTrace(System.err);		
	}

}
