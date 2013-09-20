/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.util;

import java.lang.management.ManagementFactory;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: OrderedShutdownService</p>
 * <p>Description: A service that provides ordered shutdown events in a single registered shutdown hook</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.OrderedShutdownService</code></p>
 */

public class OrderedShutdownService extends Thread implements OrderedShutdownServiceMBean, Comparator<Thread>, NotificationBroadcaster {
	/** The singleton instance */
	private static volatile OrderedShutdownService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** A map of registered shutdown hooks */
	private volatile IdentityHashMap<Thread, Thread> hooks = new IdentityHashMap<Thread, Thread>(); 
	/** The notification broadcaster delegate */
	private final NotificationBroadcasterSupport notificationBroadcaster = new NotificationBroadcasterSupport(new MBeanNotificationInfo[]{
		new MBeanNotificationInfo(new String[] {SHUTDOWN_NOTIFICATION}, Notification.class.getName(), "Notification issued when a JVM shutdown is requested")
	});
	
	


	/**
	 * Acquires the OrderedShutdownService singleton instance
	 * @return the OrderedShutdownService singleton instance
	 */
	public static OrderedShutdownService getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new OrderedShutdownService();
				}
			}
		}
		return instance;
	}
	/** The JVM's Runtime Name */
	public static final String RUNTIME_NAME = ManagementFactory.getRuntimeMXBean().getName();
	/** The JVM's process ID */
	public static final long PID = Long.parseLong(RUNTIME_NAME.split("@")[0]);
	
	/**
	 * <p>Runs all the registered shutdown hooks in order of priority
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		final long timeout = ConfigurationHelper.getLongSystemThenEnvProperty(ShorthandProperties.AGENT_SHUTDOWN_NOTIFICATION_TIMEOUT_PROP, ShorthandProperties.DEFAULT_AGENT_SHUTDOWN_NOTIFICATION_TIMEOUT);
		final CountDownLatch latch = new CountDownLatch(1);
		Thread notifThread = new Thread(new Runnable(){
			public void run() {
				sendNotification(new Notification(SHUTDOWN_NOTIFICATION, OBJECT_NAME, PID, System.currentTimeMillis(), "Shutdown:" + RUNTIME_NAME));				
				latch.countDown();
			}
		}, "JVMShutdownNotificationThread");
				
		
		notifThread.setDaemon(false);
		notifThread.setPriority(MAX_PRIORITY);
		notifThread.start();
		System.err.println("===[ Sending JVM Shutdown Notifications (timeout:" + timeout + " ms.)");
		try {
			if(latch.await(timeout, TimeUnit.MILLISECONDS)) {
				System.err.println("===[ JVM Shutdown Notifications Complete");
			} else {
				if(notifThread.isAlive()) {
					notifThread.interrupt();
				}
				System.err.println("===[ JVM Shutdown Notifications Timed Out after [" + timeout + "] ms. Shutdown Proceeding");
			}
		} catch (Exception ex) { /* No Op */ }
		IdentityHashMap<Thread, Thread> _hooks = hooks;
		hooks = null;
		log("[OrderedShutdownService] Starting ordered shutdown");
		Set<Thread> orderedThreads = new TreeSet<Thread>(this);
		orderedThreads.addAll(_hooks.keySet());
		_hooks.clear();
		for(Thread t: orderedThreads) {
			log("Shutdown Hook [%s]  Priority [%s]", t.toString(), t.getPriority());
			t.run();
		}
	}
	
	/**
	 * Performs an ordered shutdown of the JVM
	 * @param exitCode The exit code to return
	 */
	public void shutdown(int exitCode) {
		System.exit(exitCode);
	}
	
	/**
	 * Halts the JVM
	 * @param exitCode The exit code to return
	 */
	public void halt(int exitCode) {
		Runtime.getRuntime().halt(exitCode);
	}
	
    /**
     * Add a new shutdown hook.  Checks the shutdown state and the hook itself, but does not do any security checks.
     * @param hook The shutdown hook to add
     */
    public synchronized void add(Thread hook) {
        if(hooks == null)
            throw new IllegalStateException("Shutdown in progress");

        if (hook.isAlive())
            throw new IllegalArgumentException("Hook already running");

        if (hooks.containsKey(hook))
            throw new IllegalArgumentException("Hook previously registered");

        hooks.put(hook, hook);
    }
    
    /**
     * Removes a shutdown hook
     * @param hook The shutdown hook to remove
     */
    public synchronized void remove(Thread hook) {
        if(hooks == null)
            throw new IllegalStateException("Shutdown in progress");

        if (hook.isAlive())
            throw new IllegalArgumentException("Hook already running");

        hooks.remove(hook);    	
    }
    
	/**
	 * Returns the number of registered shutdown hooks
	 * @return the number of registered shutdown hooks
	 */
    @Override
    public synchronized int getShutdownHookCount() {
    	return hooks.size();
    }
	
	/**
	 * Creates a new OrderedShutdownService
	 */
	private OrderedShutdownService() {
		Runtime.getRuntime().addShutdownHook(this);
		JMXHelper.registerMBean(this, OBJECT_NAME);
	}
	
	/**
	 * Low class out logger
	 * @param fmt The format of the message
	 * @param args The values to embedd
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}

	/**
	 * <p>Compares the two threads for order by descending priority. 
	 * Returns a negative integer, zero, or a positive integer as the first thread has lower equal, or higher priority than the second.</p>
	 * {@inheritDoc}
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Thread t1, Thread t2) {
		int p1 = t1.getPriority(), p2 = t2.getPriority();
		if(p1==p2) return t1.getId()<t2.getId() ? -1 : 1;
		return p1<p2 ? -1 : 1;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#addNotificationListener(javax.management.NotificationListener, javax.management.NotificationFilter, java.lang.Object)
	 */
	@Override
	public void addNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws IllegalArgumentException {
		notificationBroadcaster.addNotificationListener(listener, filter, handback);
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#getNotificationInfo()
	 */
	@Override
	public MBeanNotificationInfo[] getNotificationInfo() { 		
		return notificationBroadcaster.getNotificationInfo();
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationBroadcaster#removeNotificationListener(javax.management.NotificationListener)
	 */
	@Override
	public void removeNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener);
	}

	/**
	 * Removes the matching notification listener
	 * @param listener The listener to remove
	 * @param filter The filter that was registered with the listener
	 * @param handback The handback object that was registered with the listener
	 * @throws ListenerNotFoundException Thrown if a matching listener cannot be found
	 */
	public void removeNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException {
		notificationBroadcaster.removeNotificationListener(listener, filter, handback);
	}

	/**
	 * Sends a JMX notification to all registered listeners
	 * @param notification The notification to send
	 */
	public void sendNotification(Notification notification) {
		notificationBroadcaster.sendNotification(notification);
	}

}
