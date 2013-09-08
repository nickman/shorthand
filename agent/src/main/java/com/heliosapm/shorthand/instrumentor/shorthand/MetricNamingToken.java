/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
	//   Real Javassist Expressions
	// =====================================================================================
	/** Represents the <b><i><code>this</code></i></b> object instance */
	$THIS(null, "$0"),
	/** Represents the indexed argument to a method. e.g. <b><i><code>$1</code></i></b> is the value of the first argument */
	$ARG(Pattern.compile("\\$([1-9]+)", Pattern.CASE_INSENSITIVE), null),
	/** Represents the all the argument to a methods as a <b><i><code>Object[]</code></i></b> */
	$ARGS(null, "$args"),
	/** Represents the method return value */
	$RETURN(null, "$_"),
	
	// =====================================================================================
	//   "Fake" Javassist Expressions
	// =====================================================================================
	
	/** Represents the simple <b><i><code>Class</code></i></b> name of the class containing the instrumented method */
	$CLASS(null, "$class"),
	/** Represents the <b><i><code>Method</code></i></b> name of the instrumented method */
	$METHOD(null, "$method"),
	/** Represents the <b><i><code>Package</code></i></b> name of the class containing the instrumented method */
	$PACKAGE(null, "$class");
	
	
	
	/** A map of non pattern tokens keyed by the actual */
	public static final Map<String, MetricNamingToken> TOKEN2ENUM;
	/** A set of all non pattern tokens  */
	public static final Set<MetricNamingToken> NAMED_TOKENS;
	/** A set of all pattern tokens  */
	public static final Set<MetricNamingToken> PATTERN_TOKENS;
	
	static {
		MetricNamingToken[] values = MetricNamingToken.values();
		Map<String, MetricNamingToken> tmp = new HashMap<String, MetricNamingToken>(values.length);
		Set<MetricNamingToken> tmpNamed = EnumSet.noneOf(MetricNamingToken.class);
		Set<MetricNamingToken> tmpPattern = EnumSet.noneOf(MetricNamingToken.class);
		for(MetricNamingToken jt: values) {
			if(jt.pattern==null) {
				tmpNamed.add(jt);
				tmp.put(jt.actual, jt);
			} else {
				tmpPattern.add(jt);
			}
		}
		TOKEN2ENUM = Collections.unmodifiableMap(tmp);
		NAMED_TOKENS = Collections.unmodifiableSet(tmpNamed);
		PATTERN_TOKENS = Collections.unmodifiableSet(tmpPattern);
	}
	
	private MetricNamingToken(Pattern pattern, String actual) {
		this.pattern = pattern;
		this.actual = actual;
		if(this.pattern==null && this.actual==null) throw new RuntimeException("MetricNamingToken had null pattern and null actual. Programmer Error.");
	}
	
	/** The token match pattern. Null if actual symbol exists */
	public final Pattern pattern;
	/** The token actual symbol. Null if pattern exists */
	public final String actual;
	
	/**
	 * <p>Title: ValueExtractor</p>
	 * <p>Description: Defines a static value extractor for a given MetricNamingToken for a passed class and method</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.MetricNamingToken.ValueExtractor</code></p>
	 */
	interface ValueExtractor {
		/**
		 * Returns a static metric name part for the passed class and method
		 * @param clazz The target class
		 * @param method The target method
		 * @return the static metric name part
		 */
		public String getStaticValue(Class<?> clazz, Method method);
	}
	
	/**
	 * <p>Title: RuntimeValueExtractor</p>
	 * <p>Description: Defines a runtime value extractor for a given MetricNamingToken</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.MetricNamingToken.RuntimeValueExtractor</code></p>
	 */
	interface RuntimeValueExtractor extends ValueExtractor {
		/**
		 * Returns a runtime metric name part for the passed object instance, return value and arguments
		 * @param thisObject The object instance being invoked on
		 * @param returnValue The return value of the invocation
		 * @param args The arguments to the invocation
		 * @return the runtime metric name part
		 */
		public String getRuntimeValue(Object thisObject, Object returnValue, Object...args);
	}
	
	
	/**
	 * Determines if the expression contains runtime tokens
	 * @param expression The expression to test
	 * @return true if the expression contains runtime tokens, false otherwise
	 */
	public static boolean hasRuntimeTokens(CharSequence expression) {
		
	}
	
	/**
	 * Indicates if the passed name is a valid MetricNamingToken
	 * @param name the name to test
	 * @return true if the passed name is a valid MetricNamingToken, false otherwise
	 */
	public static boolean isJavassistToken(CharSequence name) {
		if(name==null) return false;
		try {
			String sname = name.toString().trim().toLowerCase();
			if(TOKEN2ENUM.containsKey(sname)) return true;
			for(MetricNamingToken token: PATTERN_TOKENS) {
				if(token.pattern.matcher(sname).matches()) return true;
			}
			return false;
		} catch (Exception ex) {
			return false;
		}
	}
	
	public static MetricNamingToken getJavassistTokenOrNull(CharSequence name) {
		if(name==null) return null;
		try {
			String sname = name.toString().trim().toLowerCase();
			MetricNamingToken token = TOKEN2ENUM.get(sname);
			if(token!=null) return token;
			for(MetricNamingToken t: PATTERN_TOKENS) {
				if(token.pattern.matcher(sname).matches()) return token;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}
	
	
	public static String getJavassistTokenValue(CharSequence name) {
		if(name==null) return null;
		try {
			String sname = name.toString().trim().toLowerCase();
			MetricNamingToken token = TOKEN2ENUM.get(sname);
			if(token!=null) return token.actual;
			for(MetricNamingToken t: PATTERN_TOKENS) {
				if(token.pattern.matcher(sname).matches()) return sname;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}
	
	
	
	
	
}
