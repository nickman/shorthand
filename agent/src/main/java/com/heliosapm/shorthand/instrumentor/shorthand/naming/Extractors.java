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
package com.heliosapm.shorthand.instrumentor.shorthand.naming;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import com.heliosapm.shorthand.util.JSExpressionEvaluator;
import com.heliosapm.shorthand.util.StringHelper;


/**
 * <p>Title: Extractors</p>
 * <p>Description: Contains various {@link ValueExtractor} classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.naming.Extractors</code></p>
 * TODO:  Need to add checks for synchronized calls and print warnings
 */

public class Extractors {
	
	/** A package name splitter */
	public static final Pattern DOT_SPLITTER = Pattern.compile("\\.");
	/** A white space cleaner */
	public static final Pattern WS_CLEANER = Pattern.compile("\\s+");
	
	/**
	 * Determines if there is a named attribute method (i.e. no params) with a non void return type in the passed class
	 * @param clazz The class to inspect
	 * @param methodName The name of the attribute method
	 * @return true if one was found, false otherwise
	 */
	public static boolean isMethod(Class<?> clazz, String methodName) {
		Method method = null;
		try {
			try {
				method = clazz.getDeclaredMethod(methodName);
			} catch (NoSuchMethodException e) {
				method = clazz.getMethod(methodName);
			}
			if(method==null) return false;
			return method.getReturnType()!=void.class && method.getReturnType()!=Void.class; 
		} catch (Exception ex) {
			return false;
		}
	}
	
	/**
	 * Returns the first annotation instance found on the method where the annotation's class name or simple class name equals the passed name.
	 * If one is not found, the the class is searched for the same
	 * @param name The name we're searching for
	 * @param member The method or constructor to inspect
	 * @return a [possibly null] annotation
	 */
	public static Annotation getAnnotation(String name, Member member) {
		Annotation annotation = null;
		AnnotatedElement annotatedElement = (AnnotatedElement)member;
		for(Annotation ann: annotatedElement.getAnnotations()) {
			if(ann.annotationType().getName().equals(name) || ann.annotationType().getSimpleName().equals(name)) {
				annotation = ann;
				break;
			}
		}
		if(annotation==null) {
			for(Annotation ann: member.getDeclaringClass().getAnnotations()) {
				if(ann.annotationType().getName().equals(name) || ann.annotationType().getSimpleName().equals(name)) {
					annotation = ann;
					break;
				}
			}			
		}
		return annotation;
	}
	
	/**
	 * Determines if there is a named field in the passed class
	 * @param clazz The class to inspect
	 * @param fieldName The name of the field
	 * @return true if one was found, false otherwise
	 */
	public static boolean isField(Class<?> clazz, String fieldName) {
		Field field = null;
		try {
			try {
				field = clazz.getDeclaredField(fieldName);
			} catch (NoSuchFieldException e) {			 
				field = clazz.getField(fieldName);
			}
			return field!=null;
		} catch (Exception ex) {
			return false;
		}
	}
	
			
	
	/** Value extractor to return the {@link #toString()} of the "this" object or a field / attribute notation value. 
	 * Pattern is <b><code>[&nbsp;\\$\\{this\\}|\\$\\{this:(.*?)\\}&nbsp;]</code></b>  */
	public static final ValueExtractor THIS = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Matcher matcher = MetricNamingToken.$THIS.pattern.matcher(expr);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing $THIS expression [" + expression + "]");
			String matchedPattern = matcher.group(0);
			String codePoint = matcher.group(1);
			String extract = null;
			if(matchedPattern.equals("${this}") || matchedPattern.equals("${this:}") ) {
				extract = "($0==null ? \"\" : $0.toString())";
			} else {
				extract = codePoint;
			}
			validateCodePoint("$THIS", clazz, member, extract + ";");
			return toArray("%s", extract);	
		}
	};
	
	/**
	 * Attempts to compile a snippet to validate a code point
	 * @param name The extractor name
	 * @param clazz The class targetted for instrumentation
	 * @param member The method or constructor targetted for instrumentation
	 * @param codePoint The code point
	 */
	private static void validateCodePoint(String name, Class<?> clazz, Member member, String codePoint) {
		ClassPool cPool = new ClassPool(true);
		CtClass ctClass = null;
		cPool.appendClassPath(new ClassClassPath(clazz));
		try {
			ctClass = cPool.get(clazz.getName());
			CtMethod ctMethod = ctClass.getMethod(member.getName(), StringHelper.getMemberDescriptor(member));
			ctClass.removeMethod(ctMethod);
			ctMethod.insertAfter(codePoint);
			ctClass.addMethod(ctMethod);
			ctClass.writeFile(System.getProperty("java.io.tmpdir") + File.separator + "js");
			ctClass.toBytecode();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to validate " + name + "] codepoint for [" + clazz.getName() + "." + member.getName() + "]. Codepoint was [" + codePoint + "]", ex);
		} finally {
			if(ctClass!=null) ctClass.detach();
		}
	}
	
	/** Value extractor to return the {@link #toString()} of the indexed argument object or a field/attribute notation value 
	 * Pattern is <b><code>[&nbsp;\\$\\{arg\\[(\\d+)\\]\\}|\\$\\{arg:(.*?)\\}&nbsp;]</code></b>
	 * */
	public static final ValueExtractor ARG = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Matcher matcher = MetricNamingToken.$ARG.pattern.matcher(expr);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing $ARG expression [" + expression + "]");
			String matchedPattern = matcher.group(0);
			String matchedIndex = matcher.group(1);
			String codePoint = matcher.group(2);
			int index = -1;
			String extract = null;
			if(codePoint==null || codePoint.trim().isEmpty()) {
				try { index = Integer.parseInt(matchedIndex); } catch (Exception x) {
					throw new RuntimeException("Invalid $ARG expression [" + expression + "]. Neither a code-point or an index were matched");
				}
				Class<?>[] paramTypes = null;
				if(member instanceof Constructor) {
					paramTypes = ((Constructor)member).getParameterTypes(); 
				} else {
					paramTypes = ((Method)member).getParameterTypes();
				}
				Class<?> argType = paramTypes[index]; 
				index++;
				if(argType.isPrimitive()) {
					extract = String.format("(\"\" + $%s)", index);
				} else {
					extract = String.format("($%s==null ? \"\" : $%s.toString())", index, index );
				}
				
				
			} else {
				extract = codePoint;
			}
			log("Extract:[%s]", extract);
			validateCodePoint("$ARG", clazz, member, extract + ";");
			return toArray("%s", extract);			
		}
	};
	
	/** Value extractor to return the {@link #toString()} of the invocation return value object or a field/attribute notation value 
	 *   Pattern is <b><code>[&nbsp;\\$\\{return(?::(.*))?\\}&nbsp;]</code></b>
	 * */
//	public static final ValueExtractor RETURN = new ValueExtractor() {
//		/**
//		 * {@inheritDoc}
//		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
//		 */
//		@Override
//		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Method method, Object...qualifiers) {
//			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
//			Class<?> returnType = method.getReturnType();
//			if(returnType==Void.class || returnType==void.class) {
//				throw new RuntimeException("Invalid use of $RETURN token since method [" + clazz.getName() + "." + method.getName() + "] has a void return type");
//			}
//			String extract = null;
//			if(expr.equals("${return}") || expr.equals("${return:}")) {
//				if(returnType.isPrimitive()) {
//					extract = "$_";
//				} else {
//					extract = "($_==null ? \"\" : $_.toString())";
//				}
//			} else {
//				Matcher matcher = MetricNamingToken.$RETURN.pattern.matcher(expr);
//				if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing $RETURN expression [" + expression + "]");
//				extract = matcher.group(1);
//			}
//			validateCodePoint("$RETURN", clazz, method, extract + ";");
//			return toArray("%s", extract);			
//		}
//	};
	
	


	/** Value extractor to validate and return the code point of a freeform $JAVA naming token 
	 *   Pattern is <b><code>[&nbsp;\\$\\{java(?::(.*))?\\}&nbsp;]</code></b>
	 * */
	public static final ValueExtractor JAVA = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Matcher matcher = MetricNamingToken.$JAVA.pattern.matcher(expr);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing $JAVAexpression [" + expression + "]");
			String extract = String.format("(\"\" + (%s))",  matcher.group(1));
			validateCodePoint("$JAVA", clazz, member, extract + ";");
			return toArray("%s", extract);			
		}
	};
	
	/** Value extractor to return the {@link #toString()} of the value of the annotation attribute on the method (or failing that, on the class) or a field/attribute notation value.
	  Pattern is <b><code>[&nbsp;\\$\\{(.*?)@\\((.*?)\\)(.*?)\\}&nbsp;]</code></b>  */
	public static final ValueExtractor ANNOTATION = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Matcher matcher = MetricNamingToken.$ANNOTATION.pattern.matcher(expr);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing $ANNOTATION expression [" + expr + "]");
			String preAnnotationCode = matcher.group(1);
			String annotationName = matcher.group(2);
			String postAnnotationCode = matcher.group(3);
			
			Annotation ann = getAnnotation(annotationName, member);
			if(ann==null) {
				throw new RuntimeException("No annotation named [" + annotationName + "] found on  [" + clazz.getName() + "." + member.getName() + "] for expression [" + expr + "]");
			}
			
			if(preAnnotationCode==null) preAnnotationCode=""; else preAnnotationCode = preAnnotationCode.trim();
			if(postAnnotationCode==null) postAnnotationCode=""; else postAnnotationCode = postAnnotationCode.trim();
			
			Object result = null;
			try {
				result = JSExpressionEvaluator.getInstance().evaluate(String.format("%s##0##%s", preAnnotationCode, postAnnotationCode), ann);
			} catch (Exception ex) {
				throw new RuntimeException("Failed to evaluate $ANNOTATION expression [" + expr + "]", ex);
			}
			
			if(result==null) throw new RuntimeException("$ANNOTATION expression [" + expr + "] returned null");
			
			return toArray(result.toString());
		}
			
	};
	
	/** A package name or member value extractor. Pattern is <b><code>[&nbsp;\\$\\{package(?:\\[(\\d+)\\])?\\}&nbsp;]</code></b> */
	public static final ValueExtractor PACKAGE = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object... args) {
			Matcher matcher = MetricNamingToken.$PACKAGE.pattern.matcher(expression);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing $PACKAGE expression [" + expression + "]");
			String strIndex = matcher.group(1); 
			if(strIndex==null || strIndex.trim().isEmpty()) {
				return toArray(clazz.getPackage().getName().replace('.', '/'));
			}
			int index = -1;
			try {
				index = Integer.parseInt(matcher.group(1));
			} catch (Exception ex) {
				throw new RuntimeException("Failed to extract index from  $PACKAGE expression [" + expression + "]", ex);
			}
			return toArray(DOT_SPLITTER.split(clazz.getPackage().getName())[index]);
		}
	};
	
	/** A simple class name value extractor */
	public static final ValueExtractor CLASS = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object... args) {
			return toArray(clazz.getSimpleName());
		}
	};
	
	/** A method name value extractor */
	public static final ValueExtractor METHOD = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStringReplacement(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Member member, Object... args) {
			return toArray(member.getName());
		}		
	};
	
	/**
	 * Convenience method to return a varg as a first class array
	 * @param strings The strings to return as an array
	 * @return a string array
	 */
	public static String[] toArray(String...strings) {
		return strings;
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}

	
}


/*
	Some Test Groovy for these patterns
*/

/*
==================================================================================
	$PACKAGE
==================================================================================
import java.util.regex.*;
p = Pattern.compile('\\$\\{package(?:\\[(\\d+)\\])?\\}');
v = 'foo.bar.snafu';
expr = '${package[0]}';

m = p.matcher(expr);
if(!m.matches()) {
    println "No Match";
} else {
    ind = m.group(1);
    if(ind==null || ind.trim().isEmpty()) {
        println "--->[${v.replace('.', '/')}]";
    } else {
        int index = Integer.parseInt(ind.trim());
        println "--->[${v.split("\\.")[index]}]";
        
    }
}



*/