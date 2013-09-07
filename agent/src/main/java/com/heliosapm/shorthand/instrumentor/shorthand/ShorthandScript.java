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
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.StringHelper;
import com.heliosapm.shorthand.util.URLHelper;
import com.heliosapm.shorthand.util.enums.EnumHelper;
import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: ShorthandScript</p>
 * <p>Description: A simple plain text syntax for shorthand scripts. We should really write a proper parser for this.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript</code></p>
 * <h4><pre>
		 [@]<ClassName>[+] [(Method Attributes)] [@]<MethodName>[<Signature>] [Invocation Options] <CollectorName>[<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
	</pre></h4>
 */

public class ShorthandScript  {
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
	//		Target Class Attributes
	//==============================================================================================
	/** The target class for instrumentation */
	protected Class<?> targetClass = null;
	/** Indicates if the target class is an annotation */
	protected boolean targetClassAnnotation = false;
	/** The target class classloader */
	protected ClassLoader targetClassLoader = null;
	/** Indicates if the target class is an interface */
	protected boolean targetClassInterface = false;	
	/** Indicates if inherritance off the target class is enabled */
	protected boolean inherritanceEnabled = false;
	
	//==============================================================================================
	//		Target Method Attributes
	//==============================================================================================
	/** The target method name, null if expr is used */
	protected String methodName = null;
	/** The target method name expression, null if name is used */
	protected Pattern methodNameExpression = null;
	/** The target method signature, null if expr is used */
	protected String methodSignature = null;
	/** The target method signature expression, null if signature is used */
	protected Pattern methodSignatureExpression = null;
	/** Indicates if the target method is an annotation */
	protected boolean targetMethodAnnotation = false;
	/** The target method level annotation class */
	protected Class<? extends Annotation> methodAnnotationClass = null;	
	/** The target method level annotation classloader */
	protected ClassLoader methodAnnotationClassLoader = null;
	/** The method attributes (from {@link MethodAttribute}) */
	protected int methodAttribute = MethodAttribute.DEFAULT_METHOD_MASK;

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
	


	
	
	//==============================================================================================
	



	/**
	 * Returns a parsed ShorthandScript instance for the passed source
	 * @param source The source to parse
	 * @return a parsed ShorthandScript instance 
	 */
	public static ShorthandScript parse(CharSequence source) {
		if(source==null || source.toString().trim().isEmpty()) throw new ShorthandParseFailureException("The passed source was null or empty", "<null>");
		return new ShorthandScript(source.toString().trim());
	}

	
	/**
	 * Creates a new ShorthandScript
	 * @param source The source to parse
	 * @param classLoader An optional classloader
	 */
	private ShorthandScript(String source) {
		String whiteSpaceCleanedSource = WH_CLEANER.matcher(source).replaceAll(" ");
		Matcher matcher = SH_PATTERN.matcher(whiteSpaceCleanedSource);
		if(!matcher.matches()) {
			throw new ShorthandParseFailureException("Shorthand script regex pattern not recognized", whiteSpaceCleanedSource);
		}
		final int fieldCount = matcher.groupCount();
		String[] parsedFields = new String[fieldCount];
		for(int i = 1; i <= fieldCount; i++ ) {
			parsedFields[i-1] = matcher.group(i);
		}
		log("Parsed values: %s", Arrays.toString(parsedFields));
		validateMandatoryFields(whiteSpaceCleanedSource, parsedFields);
		validateTargetClass(whiteSpaceCleanedSource, parsedFields[IND_TARGETCLASS], parsedFields[IND_TARGETCLASS_CL], parsedFields[IND_TARGETCLASS_ANNOT], parsedFields[IND_INHERRIT]);
		validateTargetMethod(whiteSpaceCleanedSource, parsedFields[IND_METHOD], parsedFields[IND_METHOD_ANNOT_CL], parsedFields[IND_METHOD_ANNOT], parsedFields[IND_SIGNATURE], parsedFields[IND_INSTOPTIONS]);
		validateTargetMethodAttributes(whiteSpaceCleanedSource, parsedFields[IND_METHOD_ANNOT_CL]); 
		validateMethodSignature(whiteSpaceCleanedSource, parsedFields[IND_SIGNATURE]);
		validateMethodInvocationOptions(whiteSpaceCleanedSource, parsedFields[IND_INSTOPTIONS]);
		validateMethodInstrumentation(whiteSpaceCleanedSource, parsedFields[IND_COLLECTORNAME], parsedFields[IND_COLLECTOR_CL], parsedFields[IND_BITMASK]);		
		metricNameTemplate = parsedFields[IND_METRICNAME].trim();
	}
	
	/**
	 * Validates, loads and configures the target method instrumentation collector and configuration
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param collectorName The partial or full collector name
	 * @param parsedClassLoader The classloader expression for the enum collector class
	 * @param bitMaskOptions The bitmask or comma separated collector member names to enable
	 */
	protected void validateMethodInstrumentation(String source, String collectorName, String parsedClassLoader, String bitMaskOptions) {
		String _collectorName = collectorName.trim();
		ClassLoader classLoader = null;
		if(parsedClassLoader!=null && !parsedClassLoader.trim().isEmpty()) {
			classLoader = classLoaderFrom(parsedClassLoader.trim());
		} else {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		Class<? extends ICollector<?>> clazz = null;
		try {
			clazz = (Class<? extends ICollector<?>>) Class.forName(_collectorName, true, classLoader);
		} catch (Exception ex) {/* No Op */}
		if(clazz==null) {
			for(String packageName: ShorthandProperties.getEnumCollectorPackages()) {
				try {
					clazz = (Class<? extends ICollector<?>>) Class.forName(packageName + "." + _collectorName, true, classLoader);
					break;
				} catch (Exception ex) {
					continue;
				}				
			}
		}
		if(clazz==null) throw new ShorthandParseFailureException("Failed to locate collector class [" + collectorName + "]", source);
		EnumCollectors.getInstance().typeForName(clazz.getName(), classLoader);
		enumIndex = EnumCollectors.getInstance().index(clazz.getName());
		if(isNumber(bitMaskOptions.trim())) {
			bitMask = Integer.parseInt(bitMaskOptions.trim());
		} else {			
			if("*".equals(bitMaskOptions.trim())) {
				// all members
				bitMask = EnumHelper.getEnabledBitMask(true, EnumHelper.castToIntBitMaskedEnum(clazz), COMMA_SPLITTER.split(bitMaskOptions.trim()));
			} else {
				bitMask = EnumHelper.getEnabledBitMask(true, EnumHelper.castToIntBitMaskedEnum(clazz), COMMA_SPLITTER.split(bitMaskOptions.trim()));
			}
		}		
	}
	
	/**
	 * Returns a map of sets of members (methods and constructors) targetted for instrumentation, keyed by the classes they are declared in.
	 * @return a map of sets of members keyed by the declaring class
	 */
	public Map<Class<?>, Set<Member>> getTargetMembers() {
		Set<Class<?>> targetClasses = getTargetClasses();
		Map<Class<?>, Set<Member>> targetMembers = new HashMap<Class<?>, Set<Member>>(targetClasses.size());
		for(Class<?> clazz: targetClasses) {
			targetMembers.put(clazz, new HashSet<Member>());
		}
		Class<? extends Annotation> annotationClass = null;
		if(targetMethodAnnotation) {
			annotationClass = (Class<? extends Annotation>) methodAnnotationClass;
		}
		for(Map.Entry<Class<?>, Set<Member>> entry: targetMembers.entrySet()) {
			for(Method m: entry.getKey().getDeclaredMethods()) {
				if(targetMethodAnnotation) {
					if(m.getAnnotation(annotationClass)!=null) {
						if(isMatchingSignature(m) && isMatchingAttribute(m)) {
							entry.getValue().add(m);
						}
					}
				} else if(methodName!=null && methodName.equals(m.getName())) {
					if(isMatchingSignature(m) && isMatchingAttribute(m)) {
						entry.getValue().add(m);
					}
				} else if(methodNameExpression!=null && methodNameExpression.matcher(m.getName()).matches()) {
					if(isMatchingSignature(m) && isMatchingAttribute(m)) entry.getValue().add(m);
				}
			}
		}
		return targetMembers;
	}
	
	
	/**
	 * Determines if the passed member matches either the defined signature or the signature expression
	 * @param member The member to test 
	 * @return true for a match, false otherwise
	 */
	protected boolean isMatchingSignature(Member member) {
		String desc = StringHelper.getMemberDescriptor(member);
		if(methodSignature!=null) {
			return methodSignature.equals(desc);
		}
		return methodSignatureExpression.matcher(desc).matches();
	}
	
	/**
	 * Determines if the passed member's modifiers match the method attribute defined in the script
	 * @param member The member to test
	 * @return true for a match, false otherwise
	 */
	protected boolean isMatchingAttribute(Member member) {
		//log("isMatchingAttribute:  %s [%s]  ma [%s]", member.getName(), member.getModifiers(), methodAttribute);
		return (methodAttribute & member.getModifiers())==member.getModifiers(); 
	}
	
	

	/**
	 * Locates the targetted classes and returns them in a set
	 * @return a set of target classes
	 */
	public Set<Class<?>> getTargetClasses() {
		ConfigurationBuilder cb = new ConfigurationBuilder()
			.addClassLoader(targetClassLoader)
			.addScanners(new SubTypesScanner());
		
		if(targetClassAnnotation) {
			cb.addScanners(new TypeAnnotationsScanner());
		}
		cb.setExecutorService(scanExecutor);
		Reflections reflections = new Reflections(cb.build());
		if(targetClassAnnotation) {
			return reflections.getTypesAnnotatedWith((Class<? extends Annotation>) targetClass, inherritanceEnabled);
		} else if(inherritanceEnabled) {
			Set<?> subTypes = reflections.getSubTypesOf(targetClass);
			Set<Class<?>> results = new HashSet<Class<?>>((Collection<? extends Class<?>>) subTypes);
		}
		Set<Class<?>> results  = new HashSet<Class<?>>(Arrays.asList(targetClass));
		return results;
	}
	
	/**
	 * Validates, loads and configures the target method invocation options
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param parsedInvocationOptions The method option characters
	 */
	protected void validateMethodInvocationOptions(String source, String parsedInvocationOptions) {
		allowReentrant = InvocationOption.isAllowReentrant(parsedInvocationOptions);
		disableOnTrigger = InvocationOption.isDisableOnTrigger(parsedInvocationOptions);
		startDisabled = InvocationOption.isStartDisabled(parsedInvocationOptions);
		// ==========================
		batchTransform = InvocationOption.isBatchTransform(parsedInvocationOptions);
		residentTransformer = InvocationOption.isResidentTransformer(parsedInvocationOptions);
		if(!batchTransform && !residentTransformer) {
			residentTransformer = true;
		}
	}

	
	/**
	 * Validates, loads and configures the target method[s]
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param parsedMethodSignature The method signature or pattern
	 */
	protected void validateMethodSignature(String source, String parsedMethodSignature) {
		if(parsedMethodSignature!=null && !parsedMethodSignature.trim().isEmpty()) {
			methodSignature = parsedMethodSignature.trim(); 
			boolean patternStart = methodSignature.startsWith("[");
			boolean patternEnd = methodSignature.endsWith("]");
			if((patternStart && patternEnd) || (!patternStart && !patternEnd)) {
				if(patternStart && patternEnd) {
					try {
						this.methodSignatureExpression = Pattern.compile(methodSignature.substring(1, methodSignature.length()-1));
						this.methodSignature = null;
					} catch (Exception ex) {
						throw new ShorthandParseFailureException("Failed to compile method signature pattern " + methodSignature, source);
					}
				}
			} else {
				throw new ShorthandParseFailureException("Method signature [" + methodSignature + "] seemed to want to be an expression but was missing an opener or closer", source);
			}
		} else {
			methodSignature = null;
			methodSignatureExpression = MATCH_ALL;
		}

	}
	
	/**
	 * Validates, loads and configures the target method attributes
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param parsedMethodAttributes The method attributes (from {@link MethodAttribute})
	 */
	protected void validateTargetMethodAttributes(String source, String parsedMethodAttributes) {
		if(parsedMethodAttributes!=null && !parsedMethodAttributes.trim().isEmpty()) {
			String[] attrs = COMMA_SPLITTER.split(parsedMethodAttributes.trim());
			methodAttribute = EnumHelper.getEnabledBitMask(true, MethodAttribute.class, attrs);
		}
	}
	
	/**
	 * Validates, loads and configures the target method[s]
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param parsedMethodName The method name or pattern
	 * @param parsedClassLoader The classloader expression for the method level annotation class
	 * @param parsedMethodAnnotation The method annotation indicator
	 * @param parsedMethodSignature The method signature or pattern
	 * @param parsedMethodInvOptions The method invocation options (from {@link InvocationOption})
	 */
	protected void validateTargetMethod(String source, String parsedMethodName, String parsedClassLoader, String parsedMethodAnnotation, String parsedMethodSignature, String parsedMethodInvOptions) {
		ClassLoader classLoader = null;
		if(parsedClassLoader!=null && !parsedClassLoader.trim().isEmpty()) {
			classLoader = classLoaderFrom(parsedClassLoader.trim());
		} else {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		
		if(parsedMethodAnnotation!=null) {
			targetMethodAnnotation = "@".equals(parsedMethodAnnotation.trim());
		} else {
			targetMethodAnnotation = false;
		}		
		if(parsedMethodName!=null && !parsedMethodName.trim().isEmpty()) {
			methodName = parsedMethodName.trim(); 
			boolean patternStart = methodName.startsWith("[");
			boolean patternEnd = methodName.endsWith("]");
			if((patternStart && patternEnd) || (!patternStart && !patternEnd)) {
				
				if(patternStart && patternEnd) {
					// This means we're looking at a method expression
					if(targetMethodAnnotation) throw new ShorthandParseFailureException("Cannot combine method annotation and method name expression", source);
					try {
						this.methodNameExpression = Pattern.compile(methodName.substring(1, methodName.length()-1));
						this.methodName = null;
					} catch (Exception ex) {
						throw new ShorthandParseFailureException("Failed to compile method name pattern " + methodName, source);
					}
				} else {
					// This means we're NOT looking at a method expression, so it's either an annotation or a simple method name
					this.methodNameExpression = null;
					if(targetMethodAnnotation) {
						// It's a method annotation
						this.methodName = null;
						try {
							methodAnnotationClass = (Class<? extends Annotation>)Class.forName(methodName, true, classLoader);
						} catch (Exception ex) {
							throw new ShorthandParseFailureException("Failed to load method level annotation class [" + methodName + "]", source, ex);
						}
					} else {
						// It's a simple method name
						// unless it's "*"
						if(methodName.equals("*")) {
							this.methodNameExpression = MATCH_ALL;
							this.methodName = null;
						} else {
							methodAnnotationClass = null;
						}
					}
					
				}
			} else {
				throw new ShorthandParseFailureException("Method name [" + methodName + "] seemed to want to be an expression but was missing an opener or closer", source);
			}
		} else {
			this.methodName = null;
			this.methodNameExpression = MATCH_ALL;
		}
		
		
	}
	
	
	
	/**
	 * Validates, loads and configures the target class[es]
	 * @param source The source (for reporting in any ecxeption thrown)
	 * @param parsedClassName The target class name
	 * @param parsedClassLoader The classloader expression for the target class
	 * @param parsedAnnotationIndicator The parsed annotation indicator
	 * @param inherritanceIndicator The parsed inherritance indicator
	 */
	protected void validateTargetClass(String source, String parsedClassName, String parsedClassLoader, String parsedAnnotationIndicator, String inherritanceIndicator) {
		String className = parsedClassName.trim();
		ClassLoader classLoader = null;
		if(parsedClassLoader!=null && !parsedClassLoader.trim().isEmpty()) {
			classLoader = classLoaderFrom(parsedClassLoader.trim());
		} else {
			classLoader = Thread.currentThread().getContextClassLoader();
		}
		if(parsedAnnotationIndicator!=null) {
			targetClassAnnotation = "@".equals(parsedAnnotationIndicator.trim());
		} else {
			targetClassAnnotation = false;
		}
		if(inherritanceIndicator!=null) {
			inherritanceEnabled = "+".equals(inherritanceIndicator.trim());
		} else {
			inherritanceEnabled = false;
		}
		if(targetClassAnnotation && inherritanceEnabled) {
			loge("WARNING: Target class was marked as an annotation and for inherritance.");
		}
		try {
			targetClass = Class.forName(className, true, classLoader);
			// If the class is an annotation, we don't want to mark is an interface,
			// although the JVM considers it to be. We want them to be mutually exclusive.
			if(targetClass.isAnnotation()) {
				targetClassAnnotation = true;
				inherritanceEnabled = false;
				targetClassInterface = false;
			} else if(targetClass.isInterface()) {
				targetClassInterface = true;
				inherritanceEnabled = true;
				targetClassAnnotation = false;
			}
		} catch (Exception ex) {
			throw new ShorthandParseFailureException("Failed to locate target class [" + className + "]", source, ex);
		}
	}
	
//	/**
//	 * Resolves the passed metric bitmask expression, determines the applicable metric instances and returns a bitmask enabled for them
//	 * @param bitmaskStr The expression to evaluate
//	 * @return a bitmask for the metrics to enable
//	 * @throws ShorthandInvalidBitMaskException thrown if the expression cannot be interpreted
//	 */
//	public static int resolveBitMask(String bitmaskStr) throws ShorthandInvalidBitMaskException {
//		try {
//			if(bitmaskStr==null || bitmaskStr.isEmpty()) return MetricCollection.getDefaultBitMask();
//			if("*".equalsIgnoreCase(bitmaskStr.trim())) return MetricCollection.getAllEnabledBitMask();
//			if(isNumber(bitmaskStr)) return Integer.parseInt(bitmaskStr);
//			if(bitmaskStr.indexOf(',')!=-1) {
//				try {
//					return MetricCollection.enableFor((Object[])COMMA_SPLITTER.split(bitmaskStr));
//				} catch (Exception ex) {
//					throw new ShorthandInvalidBitMaskException("Invalid bitmask", bitmaskStr, ex);
//				}
//			} 
////			ICollector mc = MetricCollection.forValueOrNull(bitmaskStr);
////			if(mc!=null) return mc.getMask();
////			throw new ShorthandInvalidBitMaskException("Invalid bitmask", bitmaskStr);
//			return -1;
//		} catch (Exception ex) {
//			throw new ShorthandInvalidBitMaskException("Unexpected error interpreting bitmask", bitmaskStr, ex);
//		}
//	}	
	
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
	 * Returns the 
	 * @return the targetClass
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	/**
	 * Returns the 
	 * @return the targetClassIsAnnotation
	 */
	public boolean isTargetClassAnnotation() {
		return targetClassAnnotation;
	}

	/**
	 * Returns the 
	 * @return the inherritanceEnabled
	 */
	public boolean isInherritanceEnabled() {
		return inherritanceEnabled;
	}

	/**
	 * Returns the 
	 * @return the methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * Returns the 
	 * @return the methodNameExpression
	 */
	public Pattern getMethodNameExpression() {
		return methodNameExpression;
	}

	/**
	 * Returns the 
	 * @return the methodSignature
	 */
	public String getMethodSignature() {
		return methodSignature;
	}

	/**
	 * Returns the 
	 * @return the methodSignatureExpression
	 */
	public Pattern getMethodSignatureExpression() {
		return methodSignatureExpression;
	}

	/**
	 * Returns the 
	 * @return the targetMethodIsAnnotation
	 */
	public boolean isTargetMethodAnnotation() {
		return targetMethodAnnotation;
	}

	/**
	 * Returns the 
	 * @return the methodInvocationOption
	 */
	public int getMethodInvocationOption() {
		return methodInvocationOption;
	}

	/**
	 * Returns the 
	 * @return the methodAttribute
	 */
	public int getMethodAttribute() {
		return methodAttribute;
	}

	/**
	 * Returns the 
	 * @return the enumIndex
	 */
	public int getEnumIndex() {
		return enumIndex;
	}

	/**
	 * Returns the 
	 * @return the bitMask
	 */
	public int getBitMask() {
		return bitMask;
	}

	/**
	 * Returns the template to build the metric name from
	 * @return the methodTemplate
	 */
	public String getMetricNameTemplate() {
		return metricNameTemplate;
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(String.format(fmt, args));
		t.printStackTrace(System.err);
	}

	/**
	 * Returns the 
	 * @return the targetClassInterface
	 */
	public boolean isTargetClassInterface() {
		return targetClassInterface;
	}

	/**
	 * Indicates if the instrumentation injected into this method will remain active during reentrant calls
	 * @return true if the instrumentation remains active, false otherwise
	 */
	public boolean isAllowReentrant() {
		return allowReentrant;
	}
	
	/**
	 * Indicates if the instrumentation should batch transform (see {@link InvocationOption#TRANSFORMER_BATCH}) 
	 * @return true for batch transform, false otherwise
	 */
	public boolean isBatchTransform() {
		return batchTransform;
	}


	/**
	 * Indicates if the instrumentation's classfile transformer should stay resident (see {@link InvocationOption#TRANSFORMER_RESIDENT})
	 * @return true for resident, false otherwise
	 */
	public boolean isResidentTransformer() {
		return residentTransformer;
	}
	

	/**
	 * Indicates if all intrumentation should be disabled for the current thread until this method exits
	 * @return true to disable instrumentation for the current thread until this method exits, false otherwise
	 */
	public boolean isDisableOnTrigger() {
		return disableOnTrigger;
	}

	/**
	 * Indicates if the instrumentation for this method shold start disabled 
	 * @return true if the instrumentation for this method shold start disabled
	 */
	public boolean isStartDisabled() {
		return startDisabled;
	}

	/**
	 * Returns the target class classloader
	 * @return the target class classloader
	 */
	public ClassLoader getTargetClassLoader() {
		return targetClassLoader;
	}

	/**
	 * Returns the method level annotation classloader
	 * @return the method level annotation classloader
	 */
	public ClassLoader getMethodAnnotationClassLoader() {
		return methodAnnotationClassLoader;
	}

	/**
	 * Returns the enum collector class classloader
	 * @return the enum collector class classloader
	 */
	public ClassLoader getEnumCollectorClassLoader() {
		return enumCollectorClassLoader;
	}


	/**
	 * Returns the target method level annotation class
	 * @return the target method level annotation class
	 */
	public Class<? extends Annotation> getMethodAnnotation() {
		return methodAnnotationClass;
	}

	

}

//
//import java.util.regex.*;
///*
//
//    [@]<ClassName>[+] [(Method Attributes)] [@]<MethodName>[<Signature>] [Invocation Options] <CollectorName>[<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
//
//*/
//keys = [
//    0 : "IND_TARGETCLASS_ANNOT",
//    1 : "IND_TARGETCLASS",
//    2 : "IND_INHERRIT",
//    3 : "IND_ATTRS",
//    4 : "IND_METHOD_ANNOT",
//    5 : "IND_METHOD",
//    6 : "IND_SIGNATURE",
//    7 : "IND_INSTOPTIONS",
//    8 : "IND_COLLECTORNAME",
//    9 : "IND_BITMASK",
//    10 : "IND_METRICNAME",
//];
//WH_CLEANER = Pattern.compile("\\s+");
//SH_PATTERN = Pattern.compile(
//        "(@)?" +                         // The class annotation indicator
//        "(.*?)" +                         // The classname
//        "(\\+)?" +                         // The classname options (+ for inherritance) 
//        "\\s(?:\\((.*?)\\)\\s)?" +         // The optional method accessibilities. Defaults to "pub"
//        "(@)?" +                         // The method annotation indicator
//        "(.*?)" +                         // The method name expression, wrapped in "[ ]" if a regular expression
//        "(?:\\((.*)\\))?" +            // The optional method signature 
//        "\\s" +                             // spacer
//        "(?:\\-(\\w+))?" +                 // The method instrumentation options (-dr)
//        "(.*?)" +                         // The collector name
//        "(?:\\[(.*)\\])?" +         // The bitmask option. [] is mandatory. It may contain the bitmask int, or comma separated MetricCollection names
//        "\\s" +                            // spacer
//        "(?:'(.*)')"                    // The metric name format        
//    );
//    
//script = "java.lang.Object equals MethodInterceptor[0] 'java/lang/Object'";
////script = "java.lang.Object+ equals MethodInterceptor[0] 'java/lang/Object'";
//script = WH_CLEANER.matcher(script).replaceAll(" ")
//m = SH_PATTERN.matcher(script);
//if(!m.matches()) {
//    println "NO MATCH";
//} else {
//    println "Match. Parsing....";
//    for(x in 1..m.groupCount()) {
//        println "\t${keys.get(x-1)} [${m.group(x)}]";
//    }
//}

