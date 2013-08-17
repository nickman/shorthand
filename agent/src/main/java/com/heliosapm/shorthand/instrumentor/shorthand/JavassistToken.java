/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * <p>Title: JavassistToken</p>
 * <p>Description: A functional enum representing the value replaced tokens used in a javassist expression that can be used in a formatted metric name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.JavassistToken</code></p>
 */

public enum JavassistToken {
	/** Represents the <b><i><code>this</code></i></b> object instance */
	$THIS(null, "$0"),
	/** Represents the indexed argument to a method. e.g. <b><i><code>$1</code></i></b> is the value of the first argument */
	$ARG(Pattern.compile("\\$([1-9]+)", Pattern.CASE_INSENSITIVE), null),
	/** Represents the all the argument to a methods as a <b><i><code>Object[]</code></i></b> */
	$ARGS(null, "$args"),
	/** Represents the method <b><i><code>cflow</code></i></b> or the reentrancy level by the same thread */
	$CFLOW(Pattern.compile("\\$cflow\\((.*?)\\)", Pattern.CASE_INSENSITIVE), null),
	/** Represents the method return value */
	$RETURN(null, "$_"),
	/** Represents the method signature as a <b><i><code>Class[]</code></i></b> */
	$SIG(null, "$sig"),
	/** Represents the <b><i><code>Class</code></i></b> of the instrumented method */
	$CLASS(null, "$class"),
	/** Represents the <b><i><code>Method</code></i></b> of the instrumented method. Not strictly a javassist token */
	$METHOD(null, "$method");
	
	
	/** A map of non pattern tokens keyed by the actual */
	public static final Map<String, JavassistToken> TOKEN2ENUM;
	/** A set of all non pattern tokens  */
	public static final Set<JavassistToken> NAMED_TOKENS;
	/** A set of all pattern tokens  */
	public static final Set<JavassistToken> PATTERN_TOKENS;
	
	static {
		JavassistToken[] values = JavassistToken.values();
		Map<String, JavassistToken> tmp = new HashMap<String, JavassistToken>(values.length);
		Set<JavassistToken> tmpNamed = EnumSet.noneOf(JavassistToken.class);
		Set<JavassistToken> tmpPattern = EnumSet.noneOf(JavassistToken.class);
		for(JavassistToken jt: values) {
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
	
	private JavassistToken(Pattern pattern, String actual) {
		this.pattern = pattern;
		this.actual = actual;
		if(this.pattern==null && this.actual==null) throw new RuntimeException("JavassistToken had null pattern and null actual. Programmer Error.");
	}
	
	/** The token match pattern. Null if actual symbol exists */
	public final Pattern pattern;
	/** The token actual symbol. Null if pattern exists */
	public final String actual;
	
	/**
	 * Indicates if the passed name is a valid JavassistToken
	 * @param name the name to test
	 * @return true if the passed name is a valid JavassistToken, false otherwise
	 */
	public static boolean isJavassistToken(CharSequence name) {
		if(name==null) return false;
		try {
			String sname = name.toString().trim().toLowerCase();
			if(TOKEN2ENUM.containsKey(sname)) return true;
			for(JavassistToken token: PATTERN_TOKENS) {
				if(token.pattern.matcher(sname).matches()) return true;
			}
			return false;
		} catch (Exception ex) {
			return false;
		}
	}
	
	public static JavassistToken getJavassistTokenOrNull(CharSequence name) {
		if(name==null) return null;
		try {
			String sname = name.toString().trim().toLowerCase();
			JavassistToken token = TOKEN2ENUM.get(sname);
			if(token!=null) return token;
			for(JavassistToken t: PATTERN_TOKENS) {
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
			JavassistToken token = TOKEN2ENUM.get(sname);
			if(token!=null) return token.actual;
			for(JavassistToken t: PATTERN_TOKENS) {
				if(token.pattern.matcher(sname).matches()) return sname;
			}
			return null;
		} catch (Exception ex) {
			return null;
		}
	}
	
	
	
	
	
}
