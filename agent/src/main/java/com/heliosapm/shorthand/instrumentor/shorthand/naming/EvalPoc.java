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

import java.lang.reflect.Method;
import java.util.Arrays;

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
		try {
			Method method = Foo.class.getDeclaredMethod("getBar", int.class);
			//    \\$\\{(.*)?@\\((.*)?\\)(.*)?\\}
			String replacement[] = Extractors.ANNOTATION.getStringReplacement("${@(Instrumented).version()}", Foo.class, method);
			log("Replacement:[%s]", Arrays.toString(replacement));
			
			replacement = Extractors.THIS.getStringReplacement("${this}", Foo.class, method);
			log("Replacement:[%s]", Arrays.toString(replacement));
			replacement = Extractors.THIS.getStringReplacement("${this:}", Foo.class, method);
			log("Replacement:[%s]", Arrays.toString(replacement)); 
			replacement = Extractors.THIS.getStringReplacement("${this: $0.toString().toUpperCase()}", Foo.class, method);
			log("Replacement:[%s]", Arrays.toString(replacement));
			
			replacement = Extractors.ARG.getStringReplacement("${arg[0]}", Foo.class, method);
			log("Replacement:[%s]", Arrays.toString(replacement));			
			replacement = Extractors.ARG.getStringReplacement("${arg:(\"\" + ($1 + $1))}", Foo.class, method);
			log("Replacement:[%s]", Arrays.toString(replacement));
			

//			replacement = Extractors.RETURN.getStringReplacement("${return}", Foo.class, method);
//			log("Return Replacement:[%s]", Arrays.toString(replacement));			
//			replacement = Extractors.RETURN.getStringReplacement("${return:$_.toUpperCase()}", Foo.class, method);
//			log("Return Replacement:[%s]", Arrays.toString(replacement));			

			
			replacement = Extractors.JAVA.getStringReplacement("${java:$_ + $1}", Foo.class, method);
			log("JAVA Replacement:[%s]", Arrays.toString(replacement));		
			
			MetricNameProvider mnp = MetricNameCompiler.getMetricNameProvider(Foo.class, method, "${package}/${class}/${method}");


		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

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
