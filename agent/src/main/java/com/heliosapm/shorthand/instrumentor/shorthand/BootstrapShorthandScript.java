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

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.enums.EnumHelper;

/**
 * <p>Title: BootstrapShorthandScript</p>
 * <p>Description: A shorthand script parser intended for use with resident transformers installed at JVM boot-time with a <b><code>-javaagent</code></b> JVM option.
 * The primary difference between this impl and the runtime script is that this script only parses out very general criteria and performs no validation, since the 
 * boot-time environment may not have the classloaders / classpaths available to resolve class references. In other words, this script impl is intended to provide very 
 * generic criteria to determine if the class being presented in a classfile transformer is elligible for instrumentation. As such, any collected class information
 * will not be resolved unless it is related to the agent itself (which is ok since it will be in the boot classpath)</p>
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.BootstrapShorthandScript</code></p>
 */

public class BootstrapShorthandScript extends AbstractShorthandScript {
	
	/** A regex to validate valid class names (FIXME) */
	public static final Pattern JAVA_CLASS_PATTERN = Pattern.compile(".*");
	
	//==============================================================================================
	//		Early Bound Target Class/Method Attributes
	//		i.e. attributes set as soon as the script is parsed
	//==============================================================================================
	/** The target binary class name for instrumentation which may be a class or an interface */
	protected String targetClassName = null;
	/** Indicates if inherritance off the target class is enabled */
	protected boolean inherritanceEnabled = false;
	
	/** The target method name, null if expr is used */
	protected String methodName = null;
	/** The target method name expression, null if name is used */
	protected Pattern methodNameExpression = null;
	/** The target method signature, null if expr is used */
	protected String methodSignature = null;
	/** The target method signature expression, null if signature is used */
	protected Pattern methodSignatureExpression = null;
	/** The method attributes (from {@link MethodAttribute}) */
	protected int methodAttribute = MethodAttribute.DEFAULT_METHOD_MASK;
	

	//==============================================================================================
	//		Late Bound Target Class/Method Attributes
	//		i.e. attributes set when the classfile transformer is invoked and
	//		we have candidate class byte-code to load into a javassist classpool
	//		and we can examine the class meta.
	//==============================================================================================
	/** Indicates if the target class is an annotation */
	protected boolean targetClassAnnotation = false;
	/** Indicates if the target class is an interface */
	protected boolean targetClassInterface = false;	
	/** Indicates if the target method is an annotation */
	protected boolean targetMethodAnnotation = false;
	/** The target method level annotation class name */
	protected String methodAnnotationClassName = null;	
	
	/** The processor supplied classloader pre-defs */
	protected final Map<String, String> classLoaders;


	//==============================================================================================
	//		Late Bound Target Class/Method/Annotation Classloaders
	//==============================================================================================
	/** The target class classloader */
	protected ClassLoader targetClassLoader = null;
	/** The target method level annotation classloader */
	protected ClassLoader methodAnnotationClassLoader = null;

	//==============================================================================================
	
	/**
	 * Returns a parsed BootstrapShorthandScript instance for the passed source
	 * @param source The source to parse
	 * @return a parsed BootstrapShorthandScript instance 
	 */
	public static BootstrapShorthandScript parse(CharSequence source) {
		return parse(source, EMPTY_CL_MAP);
	}
	
	/**
	 * Returns a parsed BootstrapShorthandScript instance for the passed source
	 * @param source The source to parse
	 * @param classLoaders A map of classloader names keyed by the type the classloader is for (i.e. <b>target</b> or <b>collector</b>)
	 * @return a parsed BootstrapShorthandScript instance 
	 */
	public static BootstrapShorthandScript parse(CharSequence source, Map<String, String> classLoaders) {
		if(source==null || source.toString().trim().isEmpty()) throw new ShorthandParseFailureException("The passed source was null or empty", "<null>");
		return new BootstrapShorthandScript(source.toString().trim(), classLoaders);
	}
	

	
	/**
	 * Creates a new BootstrapShorthandScript
	 * @param source The source to parse
	 * @param classLoaders A map of classloader names keyed by the type the classloader is for (i.e. <b>target</b> or <b>collector</b>)
	 */
	private BootstrapShorthandScript(String source, Map<String, String> classLoaders) {
		this.classLoaders = classLoaders;
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
		} else if(classLoaders.containsKey(PREDEF_CL_INSTR)) {
			classLoader = classLoaderFrom(classLoaders.get(PREDEF_CL_INSTR));
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
					if(!targetMethodAnnotation) {
						// It's a simple method name
						// unless it's "*"
						if(methodName.equals("*")) {
							this.methodNameExpression = MATCH_ALL;
							this.methodName = null;
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
		} else if(this.classLoaders.containsKey(PREDEF_CL_TARGET)) {
			classLoader = classLoaderFrom(classLoaders.get(PREDEF_CL_TARGET));
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
}
