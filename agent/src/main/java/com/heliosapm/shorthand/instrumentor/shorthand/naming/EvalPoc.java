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

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import com.heliosapm.shorthand.instrumentor.annotations.Instrumented;


/**
 * <p>Title: EvalPoc</p>
 * <p>Description: Quickie JS eval for metric naming</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.naming.EvalPoc</code></p>
 */

public class EvalPoc {

	/**
	 * Runs a test eval
	 * @param args None
	 */
	public static void main(String[] args) {
		log("EvalPOC");
		ScriptEngineManager sec = new ScriptEngineManager();
		ScriptEngine engine = sec.getEngineByExtension("js");
		try {
			Object annotation = Foo.class.getDeclaredMethod("getBar", int.class).getAnnotation(Instrumented.class);
			log("Annotation:%s --> %s", annotation, ((Instrumented)annotation).lastInstrumented());
			//String expr = "java.util.Arrays.toString(obj.types())"; //  + obj.version()
			String expr = "(obj.lastInstrumented() + obj.version())";
			SimpleBindings bindings = new SimpleBindings();
			bindings.put("obj", annotation);
			engine.eval(expr, bindings);
			Object result = engine.eval(expr, bindings);
			log("Result: [%s] Type:%s", result, result.getClass().getName());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

	}
	
	public static void calc() throws ScriptException, NoSuchMethodException {
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("js");
	     
	    String script = "function sum(a, b) {" +
	                    "   var result = a + b;" +
	                    "   return result;" +
	                    "}";
	    engine.eval(script);
	 
	    Invocable invocableEngine = (Invocable) engine;
	    Number result = 
	            (Number) invocableEngine.invokeFunction("sum", 2, 3);
	    System.out.println(result); 		
	}
	
	class Foo {
		@Instrumented(lastInstrumented=1094, types= {"a", "b"}, version=9)
		public String getBar(int i) {
			return "#" + i;
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
