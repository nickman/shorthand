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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


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
	 * @param method The method to inspect
	 * @return a [possibly null] annotation
	 */
	public static Annotation getAnnotation(String name, Method method) {
		Annotation annotation = null;
		for(Annotation ann: method.getAnnotations()) {
			if(ann.annotationType().getName().equals(name) || ann.annotationType().getSimpleName().equals(name)) {
				annotation = ann;
				break;
			}
		}
		if(annotation==null) {
			for(Annotation ann: method.getDeclaringClass().getAnnotations()) {
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
	
			
	
	/** Value extractor to return the {@link #toString()} of the "this" object or a field / attribute notation value */
	public static final ValueExtractor THIS = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			if(expr.equals("$0") || expr.equals("$this")) {
				return expr + ".toString()";
			}
			int indexofDot = expr.indexOf('.');
			int indexofBr = expr.indexOf("()");
			if(indexofDot==-1) throw new RuntimeException("Unrecognized $THIS expression [" + expression + "]");
			if(indexofBr!=-1) {
				String methodName = expr.substring(indexofDot+1, indexofBr);
				if(isMethod(clazz, methodName)) {
					return expr + ".toString()";
				}
			}
			String fieldName = expr.substring(indexofDot+1);
			if(isField(clazz, fieldName)) {
				return expr + ".toString()";
			}
			throw new RuntimeException("Unrecognized $THIS expression [" + expression + "]");
		}
	};
	
	/** Value extractor to return the {@link #toString()} of the indexed argument object or a field/attribute notation value */
	public static final ValueExtractor ARG = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Matcher matcher = MetricNamingToken.$ARG.pattern.matcher(expression);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing expression [" + expression + "]");
			int index = -1;
			try {
				index = Integer.parseInt(matcher.group(1));
			} catch (Exception ex) {
				throw new RuntimeException("Failed to extract index from expression [" + expression + "]");
			}
			if(expr.equals("$0")) {
				throw new RuntimeException("Zero index from $ARG expression [" + expression + "]. (Hint: $ARGS use a one based index)");
			}
			Class<?>[] paramTypes = method.getParameterTypes();
			if(index-1 > paramTypes.length) {
				throw new RuntimeException("Invalid $ARG index in expression [" + expression + "]. Method has [" + paramTypes.length + "] parameters.");
			}
			if(expr.equals("$" + index)) {
				return expr + ".toString()";
			}
			int indexofDot = expr.indexOf('.');
			int indexofBr = expr.indexOf("()");
			if(indexofDot==-1) throw new RuntimeException("Unrecognized $ARG expression [" + expression + "]");
			if(indexofBr!=-1) {
				String methodName = expr.substring(indexofDot+1, indexofBr);
				if(isMethod(paramTypes[index-1], methodName)) {
					return expr + ".toString()";
				}
			}
			String fieldName = expr.substring(indexofDot+1);
			if(isField(paramTypes[index-1], fieldName)) {
				return expr + ".toString()";
			}
			throw new RuntimeException("Unrecognized $ARG expression [" + expression + "]");
		}
	};
	
	/** Value extractor to return the {@link #toString()} of the invocation return value object or a field/attribute notation value */
	public static final ValueExtractor RETURN = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Class<?> returnType = method.getReturnType();
			if(returnType==Void.class || returnType==void.class) {
				throw new RuntimeException("Invalid use of $RETURN token since method [" + clazz.getName() + "." + method.getName() + "] has a void return type");
			}
			if(expr.equals("$_")) return expr;
			
			int indexofDot = expr.indexOf('.');
			int indexofBr = expr.indexOf("()");
			if(indexofDot==-1) throw new RuntimeException("Unrecognized $RETURN expression [" + expression + "]");
			if(indexofBr!=-1) {
				String methodName = expr.substring(indexofDot+1, indexofBr);
				if(isMethod(returnType, methodName)) {
					return expr + ".toString()";
				}
			}
			String fieldName = expr.substring(indexofDot+1);
			if(isField(returnType, fieldName)) {
				return expr + ".toString()";
			}
			throw new RuntimeException("Unrecognized $RETURN expression [" + expression + "]");
		}
	};
	
	// \\$annotation\\((.*)?),(.*)?\\)
	
	/** Value extractor to return the {@link #toString()} of the value of the annotation attribute on the method (or failing that, on the class) or a field/attribute notation value */
	public static final ValueExtractor ANNOTATION = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object...qualifiers) {
			String expr = WS_CLEANER.matcher(expression.toString().trim()).replaceAll("");
			Matcher matcher = MetricNamingToken.$ANNOTATION.pattern.matcher(expression);
			if(!matcher.matches()) throw new RuntimeException("Unexpected non-macthing expression [" + expression + "]");
			String annotationName = matcher.group(1);
			String attributeName = matcher.group(2);
			
			Annotation ann = getAnnotation(annotationName, method);
			if(ann==null) {
				throw new RuntimeException("No annotation named [" + annotationName + "] found on  [" + clazz.getName() + "." + method.getName() + "] for expression [" + expression + "]");
			}
			Class<?> annotationType = ann.annotationType();
			
			int indexofDot = attributeName.indexOf('.');
			int indexofBr = attributeName.indexOf("()");
			if(indexofDot==-1) {
				String longName = String.format("$annotation(%s,%s)", annotationType.getName(), attributeName);
				String shortName = String.format("$annotation(%s,%s)", annotationType.getSimpleName(), attributeName);
				if(expr.equals(longName) || expr.equals(shortName)) {
					
				}
				
			}
			
			
			
			if(!isMethod(annotationType, attributeName)) {
				throw new RuntimeException("No attribute named [" + attributeName + "] found for annotation  [" + annotationType.getName() + "] for expression [" + expression + "]");
			}
			
			if(indexofDot==-1) throw new RuntimeException("Unrecognized $RETURN expression [" + expression + "]");
			if(indexofBr!=-1) {
				String methodName = expr.substring(indexofDot+1, indexofBr);
				if(isMethod(annotationType, methodName)) {
					return expr + ".toString()";
				}
			}
			String fieldName = expr.substring(indexofDot+1);
			if(isField(annotationType, fieldName)) {
				return expr + ".toString()";
			}
			throw new RuntimeException("Unrecognized $RETURN expression [" + expression + "]");
		}
	};
	
	/** A package name or member value extractor */
	public static final ValueExtractor PACKAGE = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object... args) {
			if(args==null || args.length==0) return clazz.getPackage().getName().replace('.', '/');
			int index = (Integer)args[0];
			return DOT_SPLITTER.split(clazz.getPackage().getName())[index];
		}
	};
	
	/** A simple class name value extractor */
	public static final ValueExtractor CLASS = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object... args) {
			return clazz.getSimpleName();
		}
	};
	
	/** A method name value extractor */
	public static final ValueExtractor METHOD = new ValueExtractor() {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.instrumentor.shorthand.naming.ValueExtractor#getStaticValue(java.lang.CharSequence, java.lang.Class, java.lang.reflect.Method, java.lang.Object[])
		 */
		@Override
		public String getStaticValue(CharSequence expression, Class<?> clazz, Method method, Object... args) {
			return method.getName();
		}		
	};

	
}
