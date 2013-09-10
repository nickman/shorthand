/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.instrumentor.shorthand.naming;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * <p>Title: MetricNamingToken</p>
 * <p>Description: A functional enum representing the value replaced tokens used in a metric name template expression that can be used in a formatted metric name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.MetricNamingToken</code></p>
 */

public enum MetricNamingToken {
	// =====================================================================================
	//   Static Tokens
	// =====================================================================================
	/** Represents the simple <b><i><code>Class</code></i></b> name of the class containing the instrumented method */
	$CLASS("\\$\\{class\\}", false, Extractors.CLASS),
	/** Represents the <b><i><code>Method</code></i></b> name of the instrumented method */
	$METHOD("\\$\\{method\\}", false, Extractors.METHOD),
	/** Represents the <b><i><code>Package</code></i></b> name of the class containing the instrumented method */
	$PACKAGE("\\$\\{package(?:\\[(\\d+)\\])?\\}", false, Extractors.PACKAGE),  // Example:  ${package} or ${package[2]}
	/** Represents the <b><i><code>Package</code></i></b> name of the class containing the instrumented method */
	$ANNOTATION("\\$\\{(.*?)@\\((.*?)\\)(.*?)\\}", false, Extractors.ANNOTATION),  // Example: ${@(Instrumented).version()}
	

	// =====================================================================================
	//   Runtime Tokens
	// =====================================================================================	
	/** Represents the <b><i><code>this</code></i></b> object instance */
	$THIS("\\$\\{this\\}|\\$\\{this:(.*?)\\}", true, Extractors.THIS),  // Example:  ${this}   or   ${this: $0.toString().toUpperCase()}
	/** Represents the indexed argument to a method. e.g. <b><i><code>$1</code></i></b> is the value of the first argument */
	$ARG("\\$\\{arg\\[(\\d+)\\]\\}|\\$\\{arg:(.*?)\\}", true, Extractors.ARG),  // Example:  ${arg[2]}   or ${arg:(\"\" + ($1 + $1))}
	/** Represents the return value of the method invocation */
	$RETURN("\\$\\{return(?::(.*))?\\}", true, Extractors.RETURN);
	
	/**
	 * Some quicke helper tests
	 * @param args None
	 */
	public static void main(String[] args) {
		log("======== All Patterns in One ========");
		StringBuilder b = new StringBuilder();
		for(MetricNamingToken mt: MetricNamingToken.values()) {
			b.append("(?:").append(mt.pattern.pattern().replace("\\",  "\\\\")).append(")|");
		}
		b.deleteCharAt(b.length()-1);
		log(b.toString());
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
//	/** Represents the all the argument to a methods as a <b><i><code>Object[]</code></i></b> */
//	$ARGS(null, "$args"),
//	/** Represents the method return value */
//	$RETURN(null, "$_");
	
	
	
	
	
	/** A map of all tokens keyed by the pattern */
	public static final Map<Pattern, MetricNamingToken> PATTERN2ENUM;
	/** A map of runtime tokens keyed by the pattern */
	public static final Map<Pattern, MetricNamingToken> RT_PATTERN2ENUM;
	
	static {
		MetricNamingToken[] values = MetricNamingToken.values();
		Map<Pattern, MetricNamingToken> tmp = new HashMap<Pattern, MetricNamingToken>(values.length);
		Map<Pattern, MetricNamingToken> tmp2 = new HashMap<Pattern, MetricNamingToken>(values.length);
		for(MetricNamingToken jt: values) {
			tmp.put(jt.pattern, jt);
			if(jt.runtime) {
				tmp2.put(jt.pattern, jt);
			}
		}
		PATTERN2ENUM = Collections.unmodifiableMap(tmp);
		RT_PATTERN2ENUM = Collections.unmodifiableMap(tmp2);
	}
	
	private MetricNamingToken(String pattern, boolean runtime, ValueExtractor extractor) {
		this.pattern = Pattern.compile(pattern);
		this.runtime = runtime;
		this.extractor = extractor;
	}
	
	/** The token match pattern. Null if actual symbol exists */
	public final Pattern pattern;
	/** Indicates if the token is a runtime replacement */
	public final boolean runtime;
	/** The value extractor */
	public final ValueExtractor extractor;
	
	
	
	
//	public static final ValueExtractor classExtractor = new ValueExtractor {
//		public String getStaticValue(Class<?> clazz, Method method) {
//			
//		}
//	};
	
	/**
	 * Determines if the expression contains runtime tokens
	 * @param expression The expression to test
	 * @return true if the expression contains runtime tokens, false otherwise
	 */
	public static boolean hasRuntimeTokens(CharSequence expression) {
		for(Pattern p: RT_PATTERN2ENUM.keySet()) {
			if(p.matcher(expression).find()) return true;
		}
		return false;
	}
	
	
	
	
	
}
