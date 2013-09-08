/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.instrumentor.helper;


/**
 * <p>Title: ShorthandHelper</p>
 * <p>Description: Runtime instrumentation helper along the lines of Byteman's</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.ShorthandHelper</code></p>
 */

public class ShorthandHelper {
//	
//	
//	
//	
//	/** A long counter for thread local arbitrarilly keyed bindings */
//	protected static final ThreadLocal<Map<Object, long[]>> threadCounter = new ThreadLocal<Map<Object, long[]>>() {
//		@Override
//		protected Map<Object, long[]> initialValue() {			
//			return new HashMap<Object, long[]>();
//		}
//	};
//	
//	
//	/** A map of shared long counters keyed by an arbitrary object */
//	protected static final ConcurrentHashMap<Object, AtomicLong> longCounterMap = new ConcurrentHashMap<Object, AtomicLong>(128, 0.75f, 16); 
//	/** A map of shared int counters keyed by an arbitrary object */
//	protected static final ConcurrentHashMap<Object, AtomicInteger> intCounterMap = new ConcurrentHashMap<Object, AtomicInteger>(128, 0.75f, 16); 
//	/** A map of shared boolean flags keyed by an arbitrary object */
//	protected static final ConcurrentHashMap<Object, AtomicBoolean> flagMap = new ConcurrentHashMap<Object, AtomicBoolean>(128, 0.75f, 16); 
//
//	protected ShorthandDirective rule = null;
//	
//	/**
//	 * Creates a new ShorthandHelper
//	 * @param rule The rule this helper instance is created for
//	 */
//	public ShorthandHelper(ShorthandDirective rule) {
//		this.rule = rule;
//	}
//	
//	/**
//	 * Logs a formatted string
//	 * @param msg the message to print
//	 */
//	public static void log(Object msg) {
//		System.out.println(String.format("[%s][%s] ShorthandHelper:%s", Thread.currentThread().getName(), new Date(), msg));
//	}
//	
//	// =====================================================================================================
//	//  Rule lifecycle methods
//	// =====================================================================================================	
//	
//    public static void activated() {
//    	log("\n\tACTIVATED");
////        if (Transformer.isDebug()) {
////            System.out.println("Default helper activated");
////        }
//    }
//
//    public static void deactivated() {
//    	log("\n\tDEACTIVATED");
////        if (Transformer.isDebug()) {
////            System.out.println("Default helper deactivated");
////        }
//    }
//
//    public static void installed(ShorthandDirective rule) {
//    	//log("\n\tRULE INSTALLED [" + rule.getName() + "]");
////        if (Transformer.isDebug()) {
////            System.out.println("Installed rule using default helper : " + rule.getName());
////        }
//    }
//
//    public static void uninstalled(ShorthandDirective rule) {
//    	//log("\n\tRULE REMOVED [" + rule.getName() + "]");
////        if (Transformer.isDebug()) {
////            System.out.println("Uninstalled rule using default helper : " + rule.getName());
////        }
//    }
//	
//	
//	// =====================================================================================================
//	//  Methods called directly by the compiled rules
//	// =====================================================================================================
//    
//    private static final Object[] EMPTY_ARGS = {};
//    
//    public static void afterRuleExecute() {
//    	log("-------------------->after Rule Exec"); 
//    }
//	
////	public long[] snapshot(int bitMask, String classSignature, String methodSignature) {
////		String key = String.format("%s.%s", classSignature, methodSignature);
////		log("-------------------->snapshot(" + bitMask + ",\"" + key + "\")");
////		long[] snapshot = MetricCollection.baseline(bitMask, -1L);
////		threadCounter.get().put(key, snapshot);
////		return null;
////	}
////	
////	public long[] resolve(String metricName, String classSignature, String methodSignature, Object...args) {
////		String key = String.format("%s.%s", classSignature, methodSignature);
////		long[] baseline = threadCounter.get().get(key);
////		long[] snapshot = MetricCollection.baseline(baseline);
////		log("####################\nREPORT:" + MetricCollection.report(snapshot));
////		log("-------------------->resolve(" + metricName + ",\"" + key + "\"," + Arrays.toString(args) + ")");
////		return null;
////	}
//	
////	public long[] resolve(String metricName, String classSignature, String methodSignature) {		
////		return resolve(metricName, classSignature, methodSignature,  EMPTY_ARGS);
////	}
//	
//	
//	public long[] handleException(String metricName, String classSignature, String methodSignature, Object...args) {
//		log("-------------------->handleException(" + metricName + ",\"" + String.format("%s.%s", classSignature, methodSignature) + "\"," + Arrays.toString(args) + ")");
//		return null;
//	}
//	
//	public long[] handleException(String metricName, String classSignature, String methodSignature) {
//		return handleException(metricName, classSignature, methodSignature, EMPTY_ARGS);
//	}	
//	
//	
//	
//	/**
//	 * Increments the value of the thread local counter identified by the passed key
//	 * @param key The identifier of the counter
//	 * @return the new value of the counter after the increment
//	 */
//	public static long incrementLocalCounter(Object key) {
//		Map<Object, long[]> map = threadCounter.get();
//		long[] cntr = null;
//		if(map.isEmpty() || (cntr = map.get(key))==null) {
//			map.put(key, new long[]{0});
//			return 0;
//		} else {
//			return ++cntr[0];
//		}		
//	}
//	
//	/**
//	 * Decrements the value of the thread local counter identified by the passed key
//	 * @param key The identifier of the counter
//	 * @return the new value of the counter after the decrement
//	 */
//	public static long decrementLocalCounter(Object key) {
//		Map<Object, long[]> map = threadCounter.get();
//		long[] cntr = null;
//		if(map.isEmpty() || (cntr = map.get(key))==null) {
//			map.put(key, new long[]{0});
//			return 0;
//		} else {
//			return --cntr[0];
//		}		
//	}
//	
//
//
//    /**
//     * Create a shared long counter keyed by the given object
//     * @param o an identifier used to refer to the counter in future
//     * @param increment The amount to increment the counter by when it is acquired. Ignored if equal to zero.
//     * @return true if a new counter was created and false if one already existed under the given identifier
//     */
//    public static boolean createSharedLongCounter(Object o, long increment) {
//    	AtomicLong counter = longCounterMap.get(o);
//    	if(counter==null) {
//    		synchronized(longCounterMap) {
//    			counter = longCounterMap.get(o);
//    	    	if(counter==null) {
//    	    		counter = new AtomicLong();
//    	    		longCounterMap.put(o, counter);
//    	    		return true;
//    	    	}
//    		}
//    	}
//    	if(increment!=0) counter.addAndGet(increment);
//    	return false;
//    }
//    
//    /**
//     * Create a shared long counter keyed by the given object
//     * @param o an identifier used to refer to the counter in future
//     * @return true if a new counter was created and false if one already existed under the given identifier
//     */
//    public static boolean createSharedLongCounter(Object o) {
//    	return createSharedLongCounter(o, 0);
//    }
//	
//    /**
//     * Create a shared int counter keyed by the given object
//     * @param o an identifier used to refer to the counter in future
//     * @param increment The amount to increment the counter by when it is acquired. Ignored if equal to zero.
//     * @return true if a new counter was created and false if one already existed under the given identifier
//     */
//    public static boolean createSharedIntCounter(Object o, int increment) {
//    	AtomicInteger counter = intCounterMap.get(o);
//    	if(counter==null) {
//    		synchronized(intCounterMap) {
//    			counter = intCounterMap.get(o);
//    	    	if(counter==null) {
//    	    		counter = new AtomicInteger();
//    	    		intCounterMap.put(o, counter);
//    	    		return true;
//    	    	}
//    		}
//    	}
//    	if(increment!=0) counter.addAndGet(increment);
//    	return false;
//    }
//    
//    /**
//     * Create a shared int counter keyed by the given object
//     * @param o an identifier used to refer to the counter in future
//     * @return true if a new counter was created and false if one already existed under the given identifier
//     */
//    public static boolean createSharedIntCounter(Object o) {
//    	return createSharedLongCounter(o, 0);
//    }
//    
//    /**
//     * Create a shared boolean flag keyed by the given object
//     * @param o an identifier used to refer to the flag in future
//     * @param state The state to set the flag to
//     * @param overwrite If true, the state will be set whether the flag is new or not, otherwise, it is only set if the flag is new.
//     * @return true if a new flag was created and false if one already existed under the given identifier
//     */
//    public static boolean createSharedFlag(Object o, boolean state, boolean overwrite) {
//    	AtomicBoolean flag = flagMap.get(o);
//    	if(flag==null) {
//    		synchronized(flagMap) {
//    			flag = flagMap.get(o);
//    	    	if(flag==null) {
//    	    		flag = new AtomicBoolean(state);
//    	    		flagMap.put(o, flag);
//    	    		return true;
//    	    	}
//    		}
//    	}
//    	if(overwrite) flag.set(state);
//    	return false;
//    }
//    
//    /**
//     * Create a shared boolean flag keyed by the given object
//     * @param o an identifier used to refer to the flag in future
//     * @param state The state to set the flag to if the flag is new. Ignored otherwise.
//     * @return true if a new flag was created and false if one already existed under the given identifier
//     */
//    public static boolean createSharedFlag(Object o, boolean state) {
//    	return createSharedFlag(o, state, false);
//    }
//	
//	public static void main(String[] args) {
//		log("Helper Test");
//		log("Initial LocalCounter:" + incrementLocalCounter("Foo"));
//		log("Next LocalCounter:" + incrementLocalCounter("Foo"));
//		log("Prior LocalCounter:" + decrementLocalCounter("Foo"));
//				
//	}
//	
//

}
