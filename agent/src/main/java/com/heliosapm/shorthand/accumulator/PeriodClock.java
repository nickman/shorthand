
package com.heliosapm.shorthand.accumulator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
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

import com.heliosapm.shorthand.util.ConfigurationHelper;
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
	/** The system property that defines if the period clock should be disabled, usually for testing purposes */
	public static final String DISABLE_PERIOD_CLOCK_PROP = "shorthand.period.disabled";
	
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
	/** The flush schedule handle */
	private final AtomicReference<ScheduledFuture<?>> scheduleHandle = new AtomicReference<ScheduledFuture<?>>(null);
	/** Provides serial numbers for created threads */
	private final AtomicInteger serial = new AtomicInteger();
	/** The period in ms. */
	public final long periodMs;
	/** The shutdown hook */
	private final Thread shutdownHook;
	
	/** Indicates if the scheduler thread has been assigned */
	private boolean hasSchedulerThreadBeenAssigned = false;
	/** The thread group that the worker threads are put into */
	private final ThreadGroup periodHandOffThreadGroup = new ThreadGroup("PeriodHandOffThreadGroup");
	
	/** Indicates if the period clock is disabled */
	private final boolean clockDisabled;
	/** A map of Runnable wrapped period listeners keyed by the registered listener */
	private final Map<PeriodEventListener, PeriodEventListenerWrapper> periodListeners = new ConcurrentHashMap<PeriodEventListener, PeriodEventListenerWrapper>();
	/** A map of Runnable wrapped period completion listeners keyed by the registered listener */
	private final Map<PeriodEventCompletionListener, PeriodEventCompletionListenerWrapper> periodCompletionListeners = new ConcurrentHashMap<PeriodEventCompletionListener, PeriodEventCompletionListenerWrapper>();
	
	/** Synch queue to hold the worker thread executing the flushes so the scheduler only has to calc the period and drop the barrier */
	private final SynchronousQueue<long[]> periodHandOff = new SynchronousQueue<long[]>(true);
	
	/**
	 * Disables the period clock
	 */
	private void disablePeriodClock() {
		if(!clockDisabled) {
			periodListeners.clear();
			periodCompletionListeners.clear();
			OrderedShutdownService.getInstance().remove(shutdownHook);
			ScheduledFuture<?> handle = scheduleHandle.get();
			if(handle!=null) {
				handle.cancel(true);
				scheduleHandle.set(null);
			}
			System.setProperty(DISABLE_PERIOD_CLOCK_PROP, "true");
			instance = null;
			getInstance();
		}
		
	}
	
	/**
	 * Enables the period clock 
	 */
	private void enablePeriodClock() {
		if(clockDisabled) {
			periodListeners.clear();
			periodCompletionListeners.clear();
			OrderedShutdownService.getInstance().remove(shutdownHook);
			System.setProperty(DISABLE_PERIOD_CLOCK_PROP, "false");
			instance = null;
			getInstance();
		}
		
	}
	
	
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
			periodListeners.put(listener, new PeriodEventListenerWrapper(listener));
			if(listener instanceof PeriodEventCompletionListener) {
				PeriodEventCompletionListener completionListener = (PeriodEventCompletionListener)listener;
				periodCompletionListeners.put(completionListener, new PeriodEventCompletionListenerWrapper(completionListener));
			}
		}
	}
	
	/**
	 * Unregisters a period event listener
	 * @param listener the lisener to unregister
	 */
	public void removeListener(final PeriodEventListener listener) {
		if(listener!=null) {
			periodListeners.remove(listener);
			periodCompletionListeners.remove(listener);
		}
	}
	
	/**
	 * Returns the current period 
	 * @return a long array with <b><code>{newStartTime, newEndTime, priorStartTime, priorEndTime}</code></b>
	 */
	public long[] getCurrentPeriod() {
		return getCurrentPeriod(System.currentTimeMillis());
	}
	
	
	/**
	 * Returns the current period
	 * @param now The current timestamp in millis
	 * @return a long array with <b><code>{newStartTime, newEndTime, priorStartTime, priorEndTime}</code></b>
	 */
	public long[] getCurrentPeriod(long now) {			
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
	
	private void shutdown() {
		
	}
	
	/**
	 * Creates a new PeriodClock
	 */
	private PeriodClock() {
		final int nonDaemonThreadCount = tmx.getThreadCount()-tmx.getDaemonThreadCount();
		shutdownHook = new Thread("PeriodClockShutdownHook") {
			public void run() {
				try { scheduler.shutdownNow(); } catch (Exception ex) {/* No Op */}
				try { threadPool.shutdown(); } catch (Exception ex) {/* No Op */}
				periodHandOffThreadGroup.interrupt();
				try { 
					if(threadPool.awaitTermination(periodMs/4, TimeUnit.MILLISECONDS)) {
						log("Threadpool Clean Shutdown");
					} else {
						log("Threadpool terminated with [%s] tasks in flight", threadPool.shutdownNow().size());
					}
				} catch (Exception ex) {/* No Op */}
			}
		};
		clockDisabled = ConfigurationHelper.getBooleanSystemThenEnvProperty(DISABLE_PERIOD_CLOCK_PROP, false);
		if(clockDisabled) {
			log("\n\t================================================\n\tPERIOD CLOCK DISABLED\n\t================================================\n");
		}
		periodMs = getPeriod();
		int cores = ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
		threadPool = new ThreadPoolExecutor(2,cores,(periodMs*2), TimeUnit.MILLISECONDS, new ArrayBlockingQueue<Runnable>(10, true), this, this);
		
		
		//registerListener(this);
		OrderedShutdownService.getInstance().add(shutdownHook);
		
		long[] periodData = getCurrentPeriod();
		
		if(!clockDisabled) {
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
		}
		threadPool.prestartAllCoreThreads();
		threadPool.execute(new Runnable(){
			public void run() {				
				while(true) {
					try {
						long[] periods = periodHandOff.take();
						log("Firing Period Event agains [%s] registered listeners", periodListeners.size());
						long startTime = System.nanoTime();
						for(PeriodEventListener listener: periodListeners.values()) {
							listener.onNewPeriod(periods[0], periods[1], periods[2], periods[3]);
						}						
						threadPool.invokeAll(periodListeners.values());
						long elapsedTime = System.nanoTime()-startTime;
						for(PeriodEventCompletionListener listener: periodCompletionListeners.values()) { 
							listener.periodEventComplete(periods, elapsedTime);
						}
					} catch (InterruptedException iex) {
						if(threadPool.isTerminating() || threadPool.isTerminated()) break;
					} catch (Exception ex) {/* No Op */}
				}
			}
		});
		if(!clockDisabled) log("Period Clock Created. \n\tPeriod is [%s] ms. \n\tCurrent Time: [%s] \n\tCurrent Period Start: [%s] \n\tCurrent Period End: [%s]", periodMs, new Date(periodData[TS]), new Date(periodData[NEW_START]), new Date(periodData[NEW_END]));
		
	}
	
	/**
	 * Triggers a flush to the store
	 * @param period The current period (see {@link #getCurrentPeriod()})
	 */
	public void triggerFlush(long[] period) {
		if(!clockDisabled) {
			throw new RuntimeException("The period clock is enabled so manual flushes are not allowed");
		}
		periodHandOff.offer(period);
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
	 * Indicates if the period clock is disabled
	 * @return true if the period clock is disabled, false otherwise
	 */
	public boolean isPeriodClockDisabled() {
		return clockDisabled;
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
			
			t = new Thread(periodHandOffThreadGroup, r, "PeriodClockWorkerThread#" + serial.incrementAndGet());
			t.setDaemon(true);
		}
		t.setUncaughtExceptionHandler(this);
		t.setPriority(Thread.MAX_PRIORITY);
		return t;
	}

	/**
	 * <p>Title: PeriodEventListenerWrapper</p>
	 * <p>Description: A runnable wrapper for registered {@link PeriodEventListener}</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.accumulator.PeriodClock.PeriodEventListenerWrapper</code></p>
	 */
	private class PeriodEventListenerWrapper implements PeriodEventListener, Callable<Void> {
		/** The delegate listener */
		protected final PeriodEventListener delegate;
		/** the invocation values */
		protected long[] values = null;
		/**
		 * Creates a new PeriodEventListenerWraper
		 * @param delegate the delegate listener
		 */
		public PeriodEventListenerWrapper(PeriodEventListener delegate) {
			this.delegate = delegate;
		}

		/**
		 * {@inheritDoc}
		 * @see java.util.concurrent.Callable#call()
		 */
		@Override
		public Void call() {
			delegate.onNewPeriod(values[0], values[1], values[2], values[3]);
			return null;			
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.accumulator.PeriodEventListener#onNewPeriod(long, long, long, long)
		 */
		@Override
		public void onNewPeriod(long newStartTime, long newEndTime, long priorStartTime, long priorEndTime) {
			values = new long[]{newStartTime, newEndTime, priorStartTime, priorEndTime};			
		}
	}
	
	private class PeriodEventCompletionListenerWrapper extends PeriodEventListenerWrapper implements PeriodEventCompletionListener {
		/**
		 * Creates a new PeriodEventCompletionListenerWrapper
		 * @param delegate
		 */
		public PeriodEventCompletionListenerWrapper(PeriodEventCompletionListener delegate) {
			super(delegate);
		}
		
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.accumulator.PeriodClock.PeriodEventListenerWrapper#onNewPeriod(long, long, long, long)
		 */
		@Override
		public void onNewPeriod(long newStartTime, long newEndTime, long priorStartTime, long priorEndTime) {
			super.onNewPeriod(newStartTime, newEndTime, priorStartTime, priorEndTime);
			
		}

		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.accumulator.PeriodEventCompletionListener#periodEventComplete(long[], long)
		 */
		@Override
		public void periodEventComplete(final long[] period, final long elapsedNanos) {
			log("[" + getClass().getSimpleName() + "] Period Event Complete in [%s] nanos", elapsedNanos);
			threadPool.execute(new Runnable() {
				public void run() {
					((PeriodEventCompletionListener)delegate).periodEventComplete(period, elapsedNanos);
				}
			});
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
