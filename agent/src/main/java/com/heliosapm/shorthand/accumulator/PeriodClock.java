
package com.heliosapm.shorthand.accumulator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.heliosapm.shorthand.util.OrderedShutdownService;

/**
 * <p>Title: PeriodClock</p>
 * <p>Description: A clock that provides the current start/end time of the current shorthand period, and 
 * emits period events to registered listeners.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.accumulator.PeriodClock</code></p>
 */

public class PeriodClock implements ThreadFactory, Thread.UncaughtExceptionHandler, RejectedExecutionHandler, PeriodEventListener {
	/** The system property that defines the shorthand period in ms. */
	public static final String PERIOD_PROP = "shorthand.period";
	/** The default shorthand period in ms, which is 15000 */
	public static final long DEFAULT_PERIOD = 15000;
	
	/** The thread mxbean */
	protected static final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
	
	/** The singleton instance */
	private static volatile PeriodClock instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** A threadpool to execute listener callbacks on */
	private final ThreadPoolExecutor threadPool;
	/** The period scheduler */
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, this);
	/** Provides serial numbers for created threads */
	private final AtomicInteger serial = new AtomicInteger();
	/** The period in ms. */
	public final long periodMs;
	/** The shutdown hook */
	private final Thread shutdownHook;
	/** A map of Runnable wrapped period listeners keyed by the registered listener */
	private final Map<PeriodEventListener, PeriodEventListenerWraper> periodListeners = new ConcurrentHashMap<PeriodEventListener, PeriodEventListenerWraper>();
	/** Synch queue to hold the worker thread executing the flushes so the scheduler only has to calc the period and drop the barrier */
	private final SynchronousQueue<long[]> periodHandOff = new SynchronousQueue<long[]>(true);  
	
	/**
	 * Acquires the PeriodClock singleton instance
	 * @return the PeriodClock singleton instance
	 */
	public static PeriodClock getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new PeriodClock();
				}
			}
		}
		return instance;
	}
	
	
	
	/**
	 * Registers a new period event listener
	 * @param listener the lisener to register
	 */
	public void registerListener(final PeriodEventListener listener) {
		if(listener!=null) {
			periodListeners.put(listener, new PeriodEventListenerWraper(listener));
		}
	}
	
	/**
	 * Unregisters a period event listener
	 * @param listener the lisener to unregister
	 */
	public void removeListener(final PeriodEventListener listener) {
		if(listener!=null) {
			periodListeners.remove(listener);
		}
	}
	
	/**
	 * Returns the current period
	 * @return a long array with <b><code>{newStartTime, newEndTime, priorStartTime, priorEndTime}</code></b>
	 */
	public long[] getCurrentPeriod() {
		long now = System.currentTimeMillis();		
		long newStartTime = now - (now%periodMs)+1000;
		long newEndTime = newStartTime+periodMs-1000;
		long priorStartTime = newStartTime-periodMs;
		long priorEndTime = newEndTime-periodMs;
		return new long[]{newStartTime, newEndTime, priorStartTime, priorEndTime, now};
	}
	
	private static final int NEW_START = 0;
	private static final int NEW_END = 1;
	private static final int PRIOR_START = 2;
	private static final int PRIOR_END = 3;
	private static final int TS = 4;
	
	/**
	 * Creates a new PeriodClock
	 */
	private PeriodClock() {
		final int nonDaemonThreadCount = tmx.getThreadCount()-tmx.getDaemonThreadCount();
		shutdownHook = new Thread("PeriodClockShutdownHook") {
			public void run() {
				try { scheduler.shutdownNow(); } catch (Exception ex) {/* No Op */}
				try { threadPool.shutdown(); } catch (Exception ex) {/* No Op */}
				try { 
					if(threadPool.awaitTermination(periodMs/4, TimeUnit.MILLISECONDS)) {
						log("Threadpool Clean Shutdown");
					} else {
						log("Threadpool terminated with [%s] tasks in flight", threadPool.shutdownNow().size());
					}
				} catch (Exception ex) {/* No Op */}
			}
		};
		
		periodMs = getPeriod();
		int cores = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		threadPool = new ThreadPoolExecutor(2,cores,(periodMs*2), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(10, true), this, this);
		
		
		//registerListener(this);
		OrderedShutdownService.getInstance().add(shutdownHook);
		
		long[] periodData = getCurrentPeriod();
		final AtomicReference<ScheduledFuture> scheduleHandle = new AtomicReference<ScheduledFuture>(null);
		scheduleHandle.set(scheduler.scheduleAtFixedRate(new Runnable(){public void run() {
			int nonDaemons = tmx.getThreadCount()-tmx.getDaemonThreadCount();
			//log("NON DAEMONS: [%s / %s]", nonDaemons, nonDaemonThreadCount);
			if(nonDaemons==nonDaemonThreadCount-1) {
				Thread t = new Thread("PeriodHandOffSniper") {
					public void run() {
						log("PeriodClock is last non-daemon thread. Killing it.");
						
						scheduleHandle.get().cancel(true);						
					}
				};
				t.setDaemon(true);
				t.start();
			}
			periodHandOff.offer(getCurrentPeriod());
		}}, Math.abs(System.currentTimeMillis()-periodData[NEW_END]), periodMs, TimeUnit.MILLISECONDS));
		threadPool.prestartAllCoreThreads();
		threadPool.execute(new Runnable(){
			public void run() {
				while(true) {
					try {
						long[] periods = periodHandOff.take();
						for(PeriodEventListener listener: periodListeners.values()) {
							listener.onNewPeriod(periods[0], periods[1], periods[2], periods[3]);
						}
					} catch (Exception ex) {/* No Op */}
				}
			}
		});
		log("Period Clock Created. \n\tPeriod is [%s] ms. \n\tCurrent Time: [%s] \n\tCurrent Period Start: [%s] \n\tCurrent Period End: [%s]", periodMs, new Date(periodData[TS]), new Date(periodData[NEW_START]), new Date(periodData[NEW_END]));
		
	}
	
	public static void main(String[] args) {
		getInstance();
		try { Thread.sleep(60000); } catch (Exception ex) {};
		System.exit(0);
	}
	
	/**
	 * Low class out logger
	 * @param fmt The format of the message
	 * @param args The values to embedd
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format("[PeriodClock]" + fmt, args));
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread.UncaughtExceptionHandler#uncaughtException(java.lang.Thread, java.lang.Throwable)
	 */
	@Override
	public void uncaughtException(Thread t, Throwable e) {
		System.err.println("[PeriodClock] Uncaught exception in PeriodListener callback on thread [" + t.getName() + "]. Stack trace follows:");
		e.printStackTrace(System.err);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.RejectedExecutionHandler#rejectedExecution(java.lang.Runnable, java.util.concurrent.ThreadPoolExecutor)
	 */
	@Override
	public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
		System.err.println("[PeriodClock] Rejected execution of period callback [" + r.toString() + "]");
	}
	

	private boolean hasSchedulerThreadBeenAssigned = false;
	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = null;
		if(!hasSchedulerThreadBeenAssigned) {
			hasSchedulerThreadBeenAssigned = true;
			t = new Thread(r, "PeriodClockSchedulerThread#" + serial.incrementAndGet());
			t.setDaemon(false);
		} else {
			t = new Thread(r, "PeriodClockWorkerThread#" + serial.incrementAndGet());
			t.setDaemon(true);
		}
		t.setUncaughtExceptionHandler(this);
		t.setPriority(Thread.MAX_PRIORITY);
		return t;
	}

	/**
	 * <p>Title: PeriodEventListenerWraper</p>
	 * <p>Description: A runnable wrapper for registered {@link PeriodEventListener}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.accumulator.PeriodClock.PeriodEventListenerWraper</code></p>
	 */
	private class PeriodEventListenerWraper implements PeriodEventListener, Runnable {
		/** The delegate listener */
		private final PeriodEventListener delegate;
		/** the invocation values */
		private long[] values = null;
		/**
		 * Creates a new PeriodEventListenerWraper
		 * @param delegate the delegate listener
		 */
		public PeriodEventListenerWraper(PeriodEventListener delegate) {
			this.delegate = delegate;
		}
		/**
		 * {@inheritDoc}
		 * @see java.lang.Runnable#run()
		 */
		@Override
		public void run() {
			delegate.onNewPeriod(values[0], values[1], values[2], values[3]);
			
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.accumulator.PeriodEventListener#onNewPeriod(long, long, long, long)
		 */
		@Override
		public void onNewPeriod(long newStartTime, long newEndTime, long priorStartTime, long priorEndTime) {
			values = new long[]{newStartTime, newEndTime, priorStartTime, priorEndTime};
			threadPool.execute(this);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.accumulator.PeriodEventListener#onNewPeriod(long, long, long, long)
	 */
	@Override
	public void onNewPeriod(long newStartTime, long newEndTime, long priorStartTime, long priorEndTime) {
		long drift = System.currentTimeMillis()-newStartTime;
		log("\n\tPeriod Switch: Drift: [%s]\n\t\tPrior Period: [%s --to--> %s]\n\t\tNew Period:   [%s --to--> %s]\n", drift,
				new Date(priorStartTime), new Date(priorEndTime),
				new Date(newStartTime), new Date(newEndTime)
		);
		
	}
	
	/**
	 * Determines the period from system props
	 * @return the period in ms.
	 */
	private static long getPeriod() {
		long p = -1;
		try {
			p = Long.parseLong(System.getProperty(PERIOD_PROP, "" + DEFAULT_PERIOD));
			if(p < 1000) {
				p = DEFAULT_PERIOD;
			}
			
		} catch (Exception ex) {
			p = DEFAULT_PERIOD;
		}	
		return p;
	}


}
