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

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

/**
 * <p>Title: MetricNameCompiler</p>
 * <p>Description: The javassist compiler to convert a metric template expression to a metric name</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.naming.MetricNameCompiler</code></p>
 */

public class MetricNameCompiler {
	
	private static final AtomicLong classSerial = new AtomicLong();
			
	private static final RemovalListener<Class<?>, Cache<Method, Cache<String, MetricNameProvider>>> classRemovalListener
	 = new RemovalListener<Class<?>, Cache<Method, Cache<String, MetricNameProvider>>>() {
			@Override
			public void onRemoval(RemovalNotification<Class<?>, Cache<Method, Cache<String, MetricNameProvider>>> notification) {
				log("MetricNameProviders Expiration: Class:[%s] Cause:[%s]", notification.getKey().getName(), notification.getCause().name());
			}
		};
		
	private static final RemovalListener<Method, Cache<String, MetricNameProvider>> methodRemovalListener
	 = new RemovalListener<Method, Cache<String, MetricNameProvider>>() {
			@Override
			public void onRemoval(RemovalNotification<Method, Cache<String, MetricNameProvider>> notification) {
				log("MetricNameProviders Expiration: Method:[%s] Cause:[%s]", notification.getKey().toGenericString(), notification.getCause().name());
			}
		};
		
	private static final RemovalListener<String, MetricNameProvider> providerRemovalListener
	 = new RemovalListener<String, MetricNameProvider>() {
			@Override
			public void onRemoval(RemovalNotification<String, MetricNameProvider> notification) {
				log("MetricNameProviders Expiration: Expression:[%s] Cause:[%s]", notification.getKey(), notification.getCause().name());
			}
		};
	
	private static final Cache<Class<?>, Cache<Method, Cache<String, MetricNameProvider>>> pCache = CacheBuilder.newBuilder()
			.weakKeys().removalListener(classRemovalListener).build();
	
	private static final CtClass[] EMPTY_ARR = {};
	
	public static String[] getMetricNameCodePoints(Class<?> clazz, Member member, String metricNameExpression) {
		String[] codePoints = null;
		StringBuffer b = new StringBuffer();
		Matcher matcher = MetricNamingToken.ALL_PATTERNS.matcher(metricNameExpression);
		StringBuilder javassistExpressions = new StringBuilder();
		while(matcher.find()) {
			String matchedPattern = matcher.group(0);
			MetricNamingToken token = MetricNamingToken.matchToken(matcher.group(0));
			String[] replacers = token.extractor.getStringReplacement(matchedPattern, clazz, member);
			log("Replacers %s", Arrays.toString(replacers));
			matcher.appendReplacement(b, replacers[0]);
			if(token.runtime) {
				javassistExpressions.append(replacers[1]).append(",");
			}				
		}
		matcher.appendTail(b);
		if(javassistExpressions.length()>0) {
			codePoints = new String[2];
			javassistExpressions.deleteCharAt(javassistExpressions.length()-1);
			codePoints[1] = javassistExpressions.toString(); 
		} else {
			codePoints = new String[1];
		}
		codePoints[0] = b.toString();
		return codePoints;
	}
	
	
	private static MetricNameProvider compile(Class<?> clazz, Method method, String metricNameExpression) {
		ClassPool classPool = new ClassPool(true);
		String className = clazz.getName() + "." + method.getName() + "MetricNameProvider" + classSerial.incrementAndGet();
		try {
			CtClass ctClass = classPool.makeClass(className);
			ctClass.addInterface(classPool.get(MetricNameProvider.class.getName()));
			CtMethod ctMethod = new CtMethod(classPool.get(String.class.getName()), "getMetricName", EMPTY_ARR, ctClass);
			StringBuffer b = new StringBuffer();
			Matcher matcher = MetricNamingToken.ALL_PATTERNS.matcher(metricNameExpression);
			List<String> javassistExpressions = new ArrayList<String>();
			while(matcher.find()) {
				String matchedPattern = matcher.group(0);
				log("Matched Pattern [%s]", matchedPattern);
				MetricNamingToken token = MetricNamingToken.matchToken(matcher.group(0));
				String[] replacers = token.extractor.getStringReplacement(matchedPattern, clazz, method);
				log("Replacers %s", Arrays.toString(replacers));
				matcher.appendReplacement(b, replacers[0]);
				if(token.runtime) {
					javassistExpressions.add(replacers[1]);
				}				
			}
			matcher.appendTail(b);
			if(javassistExpressions.isEmpty()) {
				ctMethod.setBody(String.format("{return \"%s\";}", b.toString()));
				log("Static Source: [%s]", b);
			} else {
				StringBuilder src = new StringBuilder("{ return String.format(\"").append(b.toString()).append("\",");
				for(String x: javassistExpressions) {
					src.append(x).append(",");
				}
				src.deleteCharAt(src.length()-1);
				src.append(");}");
				log("Runtime Source: [%s]", src);
				ctMethod.setBody(src.toString());
			}
			ctClass.addMethod(ctMethod);
			return (MetricNameProvider)ctClass.toClass().newInstance();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to compile [" + className + "]", ex);
		}
		
	}
	
	/**
	 * Attempts to locate the MetricNameProvider for the passed class, method and expression
	 * @param clazz The target class
	 * @param method The target method
	 * @param metricNameExpression The metric name expression
	 * @return the matching MetricNameProvider or null if one was not found
	 */
	public static MetricNameProvider getMetricNameProvider(final Class<?> clazz, final Method method, final String metricNameExpression) {
		try {
			return pCache.get(clazz, new Callable<Cache<Method,Cache<String,MetricNameProvider>>>() {
				public Cache<Method,Cache<String,MetricNameProvider>> call() {
					return CacheBuilder.newBuilder().weakKeys().removalListener(methodRemovalListener).build();
				}
			}).get(method, new Callable<Cache<String,MetricNameProvider>>() {
				public Cache<String, MetricNameProvider> call() {
					return CacheBuilder.newBuilder().weakKeys().removalListener(providerRemovalListener).build();
				}			
			}).get(metricNameExpression, new Callable<MetricNameProvider>() {
				@Override
				public MetricNameProvider call() throws Exception {
					return compile(clazz, method, metricNameExpression);
				}
			});
		} catch (ExecutionException eex) {
			throw new RuntimeException("Failed to acquire MetricNameProvider", eex);
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
}
