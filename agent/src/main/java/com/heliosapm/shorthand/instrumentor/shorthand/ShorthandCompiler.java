/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Title: ShorthandCompiler</p>
 * <p>Description: A byteman script shorthand compiler</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandCompiler</code></p>
 * <h4><pre>
		 <ClassName>[+] [(Method Attributes)] <MethodName>[<Signature>] [Invocation Options] [<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
	</pre></h4>
 */

public class ShorthandCompiler {
	
	/** The shorthand expression parser */
	protected static final Pattern SH_PATTERN = Pattern.compile(
		"(.*?)" + 						// The classname
		"(\\+)?" + 						// The classname options (+ for inherritance) 
		"\\s(?:\\((.*?)\\)\\s)?" + 		// The optional method accessibilities. Defaults to "pub"
		"(.*?)" + 						// The method name expression, wrapped in "[ ]" if a regular expression
		"(?:\\((.*)\\)\\s)?" +			// The optional method signature 
		"(?:\\-(\\w+))?" + 				// The method instrumentation options (-dr)
		"(?:\\s\\[(.*)\\])?\\s" + 		// The bitmask option. [] is mandatory. It may contain the bitmask int, or comma separated MetricCollection names
		"(?:'(.*)')"					// The metric name format		
	);

	/** The index of the target class */
	public static final int IND_TARGETCLASS = 0;
	/** The index of the inherritance indicator */
	public static final int IND_INHERRIT = 1;
	/** The index of the method attributes */
	public static final int IND_ATTRS= 2;	
	/** The index of the target method name or expression */
	public static final int IND_METHOD = 3;
	/** The index of the target method signature */
	public static final int IND_SIGNATURE = 4;
	/** The index of the instrumentation options */
	public static final int IND_INSTOPTIONS = 5;
	/** The index of the instrumentation bit mask */
	public static final int IND_BITMASK = 6;
	/** The index of the instrumentation generated metric name */
	public static final int IND_METRICNAME = 7;
	
	
	
	// p = Pattern.compile("(.*?)(\+)?\s(?:\((.*?)\)\s)?(.*?)(?:\((.*)\)\s)?(?:\-(\w+))?(?:\s\[(.*)\])?\s(?:'(.*)')");
	
	/*
	 * Still need:
	 * allow recursive
	 * disable triggering
	 * 
Match on [java.lang.Object+ (pub,pri) equals(Object) [3] 'java/lang/Object']
============================================================================
#1: [java.lang.Object]
#2: [+]
#3: [pub,pri]
#4: [equals]
#5: [Object]
#6: [3]
#7: [java/lang/Object]

	 */
	
	
	/** The whitespace cleaner */
	protected static final Pattern WH_CLEANER = Pattern.compile("\\s+");
	/** The single quote cleaner */
	protected static final Pattern SQ_CLEANER = Pattern.compile("'");
	
	/** A comma splitter */
	protected static final Pattern COMMA_SPLITTER = Pattern.compile(",");
	
	/** MetricName delimiter splitter */
	protected static final Pattern DELIM_SPLITTER = Pattern.compile("/");
	
	/** The shorthand symbol for inherrits or extends */
	public static final String PLUS = "+";
	/** The shorthand symbol indicating the script should be compiled and installed, but disabled */
	public static final String DISABLED = "DISABLED";
	
	/** Default rule enablement expression */
	public static final String IF_TRUE = "IF TRUE";
	
	
	/** The byteman symbol for inherrits or extends */
	public static final String INHR = "^";
	/** The byteman symbol for a target class */
	public static final String CLASS = "CLASS";
	/** The byteman symbol for a class ctor */
	public static final String INIT = "<init>";
	
	/** The byteman symbol a target interface */
	public static final String INTERFACE = "INTERFACE";
	/** The JVM's end of line character */
	public static final String EOL = System.getProperty("line.separator", "\n");


	
	/** The opener rule location */
	public static final String OPEN_LOC = "AT ENTRY";
	/** The closer rule location */
	public static final String CLOSE_LOC = "AT EXIT";
	/** The exception handler rule location */
	public static final String EXC_LOC = "AT THROW ALL";
	
	
	/** The rule name prefix for the method opener */
	public static final String OPEN_NAME = "shorthand-open";
	/** The rule name prefix for the method closer */
	public static final String CLOSE_NAME = "shorthand-close";
	/** The rule name prefix for the method exception handler */
	public static final String EXC_NAME = "shorthand-exception";

	
	

	
	/** The class name token replacer */
	protected static final Pattern CLASS_NAME_REPLACER = Pattern.compile("\\$class", Pattern.CASE_INSENSITIVE);
	/** The method name token replacer */
	protected static final Pattern METHOD_NAME_REPLACER = Pattern.compile("\\$method", Pattern.CASE_INSENSITIVE);
	/** The package name splitter */
	protected static final Pattern DOT_SPLITTER = Pattern.compile("\\.");
	/** The package fragment parser */
	protected static final Pattern PACKAGE_PARSER = Pattern.compile("(\\$package)(?:\\[(\\d+)\\])?", Pattern.CASE_INSENSITIVE);
	/** The dynamic token formatter */
	protected static final Pattern DYNAMIC_TOKEN_REPLACER = Pattern.compile("(\\$\\d+|\\$this|\\$!|\\$\\^)", Pattern.CASE_INSENSITIVE);
	

	/**
	 * Cleans all multiwhitespace segments into a single white space
	 * @param source The source to clean
	 * @return the cleaned source
	 */
	public static String cleanWhiteSpace(CharSequence source) {
		if(source==null) throw new IllegalArgumentException("The passed source was null");
		return WH_CLEANER.matcher(source).replaceAll(" ");
	}
	
	/**
	 * Strips all whitespace segments
	 * @param source The source to strip
	 * @return the stripped source
	 */
	public static String stripWhiteSpace(CharSequence source) {
		if(source==null) throw new IllegalArgumentException("The passed source was null");
		return WH_CLEANER.matcher(source).replaceAll("");
	}
	
	
	/**
	 * Validates the passed method name
	 * @param methodName The method name to validate
	 * @param clazz The class the method is purportedly in. Can be null if NONSTRICT.
	 * @param inherritance Indicates if class inherritance should be considered
	 * @return Null for no match, a pattern for a regex and a string for a straight method name
	 * FIXME: Need to do more validation upfront
	 */
	public static Object validateMethodName(String methodName, Class<?> clazz, boolean inherritance) {
		if(methodName==null || methodName.trim().isEmpty()) return null;
		if(methodName.startsWith("[") && methodName.endsWith("]")) {
			methodName = methodName.substring(1, methodName.length()-1);
			if(methodName.isEmpty()) return null;
			return methodName.isEmpty() ? null : Pattern.compile(methodName);
		}
		return methodName;
		
	}
	
	/**
	 * Compiles the passed shorthand expression into a pair of byteman scripts, returining them in a string.
	 * @param source The shorthand expression
	 * @return the byteman source
	 * @throws ShorthandException Thrown if the expression cannot be parsed
	 */
	public static ShorthandDirective compile(CharSequence source) throws ShorthandException {
		String _source = null;
		try {
			if(source==null) throw new ShorthandException("The passed source was null", "<null>");
			_source = cleanWhiteSpace(source);
			String[] fragments = parse(_source);
			
			// ==================================================
			// vars to be loaded into dec
			// ==================================================
			Class<?> clazz = null;
			boolean inherrit = false;
			int methodAttributeBitMask = 0;
			String methodName = null;
			Pattern methodNamePattern = null;
			String signature = null;
			Set<InvocationOption> options = null;
			int bitmask = 0;
			String metricExpression = null;
			// ==================================================
			
			// ==================================================
			// Validating parsed fields
			// ==================================================
			
			try {
				clazz = Class.forName(fragments[IND_TARGETCLASS]);
			} catch (Exception ex) {
				throw new ShorthandException("Failed to load class [" + fragments[IND_TARGETCLASS] + "]" , _source, ex);
			}
			inherrit = clazz.isInterface() || PLUS.equals(fragments[IND_INHERRIT]);
			methodAttributeBitMask = MethodAttribute.enableFor(fragments[IND_ATTRS]);
			Object mname = validateMethodName(fragments[IND_METHOD], clazz, inherrit);
			if(mname==null) throw new ShorthandException("Failed to validate method name [" + fragments[IND_METHOD] + "]" , _source);
			if(mname instanceof Pattern) {
				methodNamePattern = (Pattern)mname;
				methodName = methodNamePattern.toString();
			} else {
				methodName = (String)mname;
			}
			signature = fragments[IND_SIGNATURE];  // FIXME: We need to break this out better so the classfiletransformer can efficiently filter
			options = InvocationOption.getEnabled(true, fragments[IND_INSTOPTIONS]);
			bitmask = resolveBitMask(fragments[IND_BITMASK]);
			metricExpression = fragments[IND_METRICNAME];
			if(metricExpression==null || metricExpression.trim().isEmpty()) throw new ShorthandException("Invalid metric name expression [" + metricExpression + "]" , _source);
			int tokenCount = 0;
			for(String s: DELIM_SPLITTER.split(metricExpression)) {
				JavassistToken t = JavassistToken.getJavassistTokenOrNull(s); 
				if(t!=null) {
					tokenCount++;
					if(t==JavassistToken.$METHOD && methodNamePattern==null) {
						metricExpression = metricExpression.replace(t.actual, methodName);
					}
				}
			}
			if(tokenCount==0) {
				throw new ShorthandException("No tokens in [" + metricExpression + "]" , _source);
			}
	
			// ==================================================
			// Create declaration
			// ==================================================
			
			
			ShorthandDirective sd = new ShorthandDirective(clazz.getName(), clazz.isInterface(),
					inherrit, methodName, signature,
					bitmask, metricExpression, "", false, false); 
				
				
				return sd;
		} catch (Exception ex) {
			throw new ShorthandException("Unexpected compilation error", (_source==null ? source.toString() : _source), ex);
		}		
	}
	
	
	public static void main(String[] args) {
		try {
			log("Resolve MetricName Test");
			String p = "(.*?)(\\+)?\\s(?:\\((.*?)\\)\\s)?(.*?)(?:\\((.*)\\)\\s)?(?:\\-(\\w+))?(?:\\s\\[(.*)\\])?\\s(?:'(.*)')";
			log("Match:" + p.equals(SH_PATTERN.toString()));
			log(SH_PATTERN.matcher("").groupCount());
//			String mn = "AAA"; 
//			log(Arrays.toString(resolveMetricName(mn, Object.class, "equals")));
//			mn = "AA/$package/$method";
//			log(Arrays.toString(resolveMetricName(mn, Object.class, "equals")));
//			mn = "AA/$package/$method/$this/$!";
//			log(Arrays.toString(resolveMetricName(mn, Object.class, "equals")));

		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	
	public static String[] resolveMetricName(String template, String packageName, String simpleClassName, String methodName) {
		String _template = template.trim();
		_template = CLASS_NAME_REPLACER.matcher(_template).replaceAll(simpleClassName);
		_template = METHOD_NAME_REPLACER.matcher(_template).replaceAll(methodName);
		if(_template.toLowerCase().indexOf("$package")!=-1) {
			final String[] packageFragments = DOT_SPLITTER.split(packageName);
			StringBuffer b = new StringBuffer();
			Matcher m = PACKAGE_PARSER.matcher(_template);
			while(m.find()) {
				String index = m.group(2);
				m.appendReplacement(b, packageExtract(index, packageName, packageFragments));
			}
			m.appendTail(b);
			_template = b.toString();
		}
		StringBuffer b = new StringBuffer();
		Matcher m = DYNAMIC_TOKEN_REPLACER.matcher(_template);
		StringBuilder argSig = new StringBuilder();
		while(m.find()) {
			String token = m.group(1);			
			argSig.append(",").append(token);			
		}		
		return new String[]{_template, "classSig", "methodSig", argSig.toString()};
	}
	
	public static String[] resolveMetricName(String template, Class<?> clazz, String method) {
		return resolveMetricName(template, clazz.getPackage().getName(), clazz.getSimpleName(), method);
	}
	
	public static String packageExtract(String index, String packageName, String[] packageFragments) {
		if(index==null) return packageName;
		int ind = Integer.parseInt(index.trim());
		return packageFragments[ind];
	}
	
	/**
	 * Resolves the passed metric bitmask expression, determines the applicable metric instances and returns a bitmask enabled for them
	 * @param bitmaskStr The expression to evaluate
	 * @return a bitmask for the metrics to enable
	 * @throws ShorthandInvalidBitMaskException thrown if the expression cannot be interpreted
	 */
	public static int resolveBitMask(String bitmaskStr) throws ShorthandInvalidBitMaskException {
		try {
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
			return -1;
		} catch (Exception ex) {
			throw new ShorthandInvalidBitMaskException("Unexpected error interpreting bitmask", bitmaskStr, ex);
		}
	}
	
	private static boolean isNumber(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}
	
	/**
	 * Parses the shorthand into its known fragments
	 * @param source The source to parse
	 * @return the shorthand fragments
	 * @throws ShorthandParseFailureException Thrown if the expression cannot be parsed
	 */
	public static String[] parse(CharSequence source) throws ShorthandParseFailureException {		
		Matcher matcher = SH_PATTERN.matcher(source);
		if(!matcher.matches()) {
			throw new ShorthandParseFailureException("No pattern match on shorthand expression", source.toString());
		}				
		String[] fragments = new String[]{matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4), matcher.group(5), matcher.group(6)};
		for(int i = 0; i < fragments.length; i++) {
			if(fragments[i]==null || fragments[i].trim().isEmpty()) {
				fragments[i]=null;
			} else {
				fragments[i] = stripWhiteSpace(fragments[i]);
			}
		}		
		return fragments;
	}

	public static void log(Object msg) {
		System.out.println(msg);
	}

	
}

