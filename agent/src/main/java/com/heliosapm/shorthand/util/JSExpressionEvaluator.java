/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package com.heliosapm.shorthand.util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;

/**
 * <p>Title: JSExpressionEvaluator</p>
 * <p>Description: Utility singleton class to evaluate javascript snippets instead of coding the raw reflection.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.JSExpressionEvaluator</code></p>
 */

public class JSExpressionEvaluator {
	/** The singleton instance */
	private static volatile JSExpressionEvaluator instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	

	
	/** The script engine manager */
	private final ScriptEngineManager sec = new ScriptEngineManager();
	/** The shared JS script engine */
	private final ScriptEngine engine = sec.getEngineByExtension("js");
	/** A binding serial number */
	private final AtomicLong serial  = new AtomicLong(0L);
	
	
	/**
	 * Acquires the singleton JSExpressionEvaluator instance
	 * @return the singleton JSExpressionEvaluator instance
	 */
	public static JSExpressionEvaluator getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new JSExpressionEvaluator();
				}
			}
		}
		return instance;
	}
	
	private JSExpressionEvaluator() {}

	
	/**
	 * Evaluates the passed JS expression
	 * @param expression The expression to evaluate
	 * @param binds Objects to be bound into the engine for evaluation. Tokens named <b><code>#</i>n</i>#</code></b> will be replaced with service generated unique tokens.
	 * @return the value the expression resolves to 
	 */
	public Object evaluate(CharSequence expression, Object...binds) {
		String script = expression.toString();
		SimpleBindings bindings = new SimpleBindings();		
		if(binds!=null) {			
			for(int i = 0; i < binds.length; i++) {
				String token = String.format("##%s##", i);
				String bind = String.format("__%s__", serial.incrementAndGet());
				script = script.replace(token, bind);
				bindings.put(bind, binds[i]);				
			}
		}
		try {
			return engine.eval(script, bindings);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to evaluate expression [" + script + "]", ex);
		} 		
	}
	
	/**
	 * Quickies tester
	 * @param args None
	 */
	public static void main(String[] args) {
		log("JSExpressionEvaluator Test");
		JSExpressionEvaluator jse = JSExpressionEvaluator.getInstance();
		for(int i = 0; i < 10; i++) {
			for(int x = 3; x > 0; x--) {
				log("Eval of %s + %s: %s", i, x, jse.evaluate("##0## + ##1##", i, x));
			}
		}
		log("Engine Scope:%s", jse.engine.getBindings(ScriptContext.ENGINE_SCOPE).size());
		log("Global Scope:%s", jse.engine.getBindings(ScriptContext.GLOBAL_SCOPE).size());
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
