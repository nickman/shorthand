/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.heliosapm.shorthand.util.URLHelper;
import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: AbstractShorthandScript</p>
 * <p>Description: The abstract base class for shorthand script implementations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.AbstractShorthandScript</code></p>
 */

public class AbstractShorthandScript {
	/** The shorthand expression parser */
	protected static final Pattern SH_PATTERN = Pattern.compile(
	        		"(@)?" +                         	// The class annotation indicator
	                "(.*?)" +                         	// The classname
	                "(\\+)?" +                         	// The classname options (+ for inherritance) 
	                "(?:<\\-(.*?))?" + 					// The optional classloader expression for the target class name
					"\\s" + 							// spacer
	                "(?:\\((.*?)\\)\\s)?" +         	// The optional method accessibilities. Defaults to "pub"
	                "(@)?" +                         	// The method annotation indicator
	                "(.*?)" +                         	// The method name expression, wrapped in "[ ]" if a regular expression
	                "(?:\\((.*)\\))?" +            		// The optional method signature
	                "(?:\\[(.*)\\])?" +         		// The optional method attributes
	                "(?:<\\-(.*?))?" + 					// The optional classloader expression for the method level annotation
	                "\\s" +                             // spacer
	                "(?:\\-(\\w+))?" +                 	// The method instrumentation options (-dr)
	                "(.*?)" +                         	// The collector name
	                "(?:\\[(.*)\\])?" +         		// The bitmask option. [] is mandatory. It may contain the bitmask int, or comma separated MetricCollection names
	                "(?:<\\-(.*?))?" + 					// The optional classloader expression for the enum collector
	                "\\s" +                            	// spacer
	                "(?:'(.*)')"                    	// The metric name format      
	);
	/** The index of the target class annotation indicator */
	public static final int IND_TARGETCLASS_ANNOT = 0;				
	/** The index of the target class */
	public static final int IND_TARGETCLASS = 1;				// MANDATORY
	/** The index of the inherritance indicator */
	public static final int IND_INHERRIT = 2;
	/** The index of the target class classloader expression */
	public static final int IND_TARGETCLASS_CL = 3;	
	/** The index of the method attributes */
	public static final int IND_ATTRS= 4;
	/** The index of the target method annotation indicator */
	public static final int IND_METHOD_ANNOT = 5;	
	/** The index of the target method name or expression */
	public static final int IND_METHOD = 6;						// MANDATORY
	/** The index of the target method signature */
	public static final int IND_SIGNATURE = 7;
	/** The index of the target method attributes */
	public static final int IND_METHOD_ATTRS = 8;
	
	/** The index of the method level annotation classloader expression */
	public static final int IND_METHOD_ANNOT_CL = 9;
	
	/** The index of the instrumentation options */
	public static final int IND_INSTOPTIONS = 10;
	/** The index of the collector name */
	public static final int IND_COLLECTORNAME = 11;				// MANDATORY
	/** The index of the instrumentation bit mask */
	public static final int IND_BITMASK = 12;					
	/** The index of the collector class classloader expression */
	public static final int IND_COLLECTOR_CL = 13;
	
	/** The index of the instrumentation generated metric name */
	public static final int IND_METRICNAME = 14;				
	
	/** The whitespace cleaner */
	public static final Pattern WH_CLEANER = Pattern.compile("\\s+");
	/** The single quote cleaner */
	public static final Pattern SQ_CLEANER = Pattern.compile("'");
	
	/** A comma splitter */
	public static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	
	/** MetricName delimiter splitter */
	public static final Pattern DELIM_SPLITTER = Pattern.compile("/");
	/** Match everyting pattern */
	public static final Pattern MATCH_ALL = Pattern.compile(".*");
	
	/** The shorthand symbol for inherrits or extends */
	public static final String PLUS = "+";
	/** The shorthand symbol indicating the script should be compiled and installed, but disabled */
	public static final String DISABLED = "DISABLED";
	
	/** The predef classloader key for target classes */
	public static final String PREDEF_CL_TARGET = "target";
	/** The predef classloader key for instrumentation classes */
	public static final String PREDEF_CL_INSTR = "collector";

	/** An empty map const. */
	protected static final Map<String, String> EMPTY_CL_MAP = Collections.unmodifiableMap(new HashMap<String, String>(0));
	
	
	/** The symbol for a class ctor */
	public static final String INIT = "<init>";
	
	/** The JVM's end of line character */
	public static final String EOL = System.getProperty("line.separator", "\n");
	
	/** An executor service to execute classpath scans in parallel */
	private static final ExecutorService scanExecutor = Executors.newCachedThreadPool(new ThreadFactory(){
		private final AtomicInteger serial = new AtomicInteger();
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "ReflectionsScanningThread#" + serial.incrementAndGet());
			t.setDaemon(true);
			return t;
		}
	});
	
	static {
		((ThreadPoolExecutor)scanExecutor).prestartCoreThread();
		((ThreadPoolExecutor)scanExecutor).prestartCoreThread();
	}
	
	//==============================================================================================
	//		Instrumentation Attributes
	//==============================================================================================
	/** The method invocation options (from {@link InvocationOption}) */
	protected int methodInvocationOption = 0;
	/** The enum collector class index */
	protected int enumIndex = -1;
	/** The enum collector class classloader */
	protected ClassLoader enumCollectorClassLoader = null;
	/** The enum collector enabled metric bitmask */
	protected int bitMask = -1;
	/** The metric name template */
	protected String metricNameTemplate = null;
	/** Indicates if the instrumented method should have the instrumentation enabled when the method is called reentrantly (i.e. self-calls) */
	protected boolean allowReentrant = false;
	/** Indicates if all instrumentation on the current thread should be disabled when the method is invoked */
	protected boolean disableOnTrigger = false;
	/** Indicates if the instrumentation should be disabled at start time (and require intervention to activate) */
	protected boolean startDisabled = false;
	/** Indicates if the instrumentation should batch transform (see {@link InvocationOption#TRANSFORMER_BATCH}) */
	protected boolean batchTransform = false;
	/** Indicates if the instrumentation's classfile transformer should stay resident (see {@link InvocationOption#TRANSFORMER_RESIDENT}) */
	protected boolean residentTransformer = false;
	
	/**
	 * Attempts to derive a classloader from the passed object.
	 * @param obj The object to derive a classloader from
	 * @return a classloader
	 */
	protected ClassLoader classLoaderFrom(Object obj) {
		if(obj==null) {
			return ClassLoader.getSystemClassLoader();
		} else if(obj instanceof ClassLoader) {
			return (ClassLoader)obj;
		} else if(obj instanceof Class) {
			return ((Class<?>)obj).getClassLoader();
		} else if(obj instanceof URL) {
			return new URLClassLoader(new URL[]{(URL)obj}); 
		} else if(URLHelper.isValidURL(obj.toString())) {
			URL url = URLHelper.toURL(obj.toString());
			return new URLClassLoader(new URL[]{url});
		} else if(obj instanceof ObjectName) {
			return getClassLoader((ObjectName)obj);
		} else if(JMXHelper.isObjectName(obj.toString())) {
			return getClassLoader(JMXHelper.objectName(obj.toString()));
		} else if(obj instanceof File) {
			File f = (File)obj;
			return new URLClassLoader(new URL[]{URLHelper.toURL(f)});
		} else if(new File(obj.toString()).canRead()) {
			File f = new File(obj.toString());
			return new URLClassLoader(new URL[]{URLHelper.toURL(f)});			
		} else {
			return obj.getClass().getClassLoader();
		}
		
	}
	
	/**
	 * Returns the classloader represented by the passed ObjectName
	 * @param on The ObjectName to resolve the classloader from
	 * @return a classloader
	 */
	protected ClassLoader getClassLoader(ObjectName on) {
		try {
			MBeanServer server = JMXHelper.getHeliosMBeanServer();
			if(server.isInstanceOf(on, ClassLoader.class.getName())) {
				return server.getClassLoader(on);
			}
			return server.getClassLoaderFor(on);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get classloader for object name [" + on + "]", ex);
		}
	}
	
	/**
	 * Validates that the passed string value is an int
	 * @param s The string value to check
	 * @return true if the passed string value is an int, false otherwise
	 */
	private static boolean isNumber(CharSequence s) {
		try {
			Integer.parseInt(s.toString().trim());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}	
	

	/**
	 * Validates that the mandatory fields are not null or empty
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param fields The fields to validate
	 */
	protected void validateMandatoryFields(String source, String[] fields) {
		if(fields==null || fields.length < 11) throw new ShorthandParseFailureException("Invalid parsed field count [" + (fields==null ? 0 : fields.length) + "]", source);
		if(fields[IND_TARGETCLASS]==null || fields[IND_TARGETCLASS].trim().isEmpty()) throw new ShorthandParseFailureException("Mandatory field for TARGET_CLASS was null or empty", source);
		//if(fields[IND_METHOD]==null || fields[IND_METHOD].trim().isEmpty()) throw new ShorthandParseFailureException("Mandatory field for TARGET_METHOD was null or empty", source);
		if(fields[IND_COLLECTORNAME]==null || fields[IND_COLLECTORNAME].trim().isEmpty()) throw new ShorthandParseFailureException("Mandatory field for COLLECTORNAME was null or empty", source);
		//if(fields[IND_BITMASK]==null || fields[IND_BITMASK].trim().isEmpty()) throw new ShorthandParseFailureException("Mandatory field for BITMASK was null or empty", source);
		if(fields[IND_METRICNAME]==null || fields[IND_METRICNAME].trim().isEmpty()) throw new ShorthandParseFailureException("Mandatory field for METRICNAME was null or empty", source);
	}



	

}
