/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.util;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.TreeSet;

/**
 * <p>Title: OrderedShutdownService</p>
 * <p>Description: A service that provides ordered shutdown events in a single registered shutdown hook</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.OrderedShutdownService</code></p>
 */

public class OrderedShutdownService extends Thread implements Comparator<Thread> {
	/** The singleton instance */
	private static volatile OrderedShutdownService instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	/** A map of registered shutdown hooks */
	private volatile IdentityHashMap<Thread, Thread> hooks = new IdentityHashMap<Thread, Thread>(); 

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
	
	/**
	 * <p>Runs all the registered shutdown hooks in order of priority
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	public void run() {
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
	 * Creates a new OrderedShutdownService
	 */
	private OrderedShutdownService() {
		Runtime.getRuntime().addShutdownHook(this);
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

}
