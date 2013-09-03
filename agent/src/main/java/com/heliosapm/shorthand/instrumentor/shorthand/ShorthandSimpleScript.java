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
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

/**
 * <p>Title: ShorthandSimpleScript</p>
 * <p>Description: A simple text (non-JSON) syntax for shorthand scripts</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandSimpleScript</code></p>
 * <h4><pre>
		 [@]<ClassName>[+] [(Method Attributes)] [@]<MethodName>[<Signature>] [Invocation Options] <CollectorName>[<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
	</pre></h4>
 */

public class ShorthandSimpleScript extends ShorthandScript {
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

	/** The index of the target class annotation indicator */
	public static final int IND_TARGETCLASS_ANNOT = 0;
	/** The index of the target class */
	public static final int IND_TARGETCLASS = 1;
	/** The index of the inherritance indicator */
	public static final int IND_INHERRIT = 2;
	/** The index of the method attributes */
	public static final int IND_ATTRS= 3;
	/** The index of the target method annotation indicator */
	public static final int IND_METHOD_ANNOT = 4;	
	/** The index of the target method name or expression */
	public static final int IND_METHOD = 5;
	/** The index of the target method signature */
	public static final int IND_SIGNATURE = 6;
	/** The index of the instrumentation options */
	public static final int IND_INSTOPTIONS = 7;
	/** The index of the collector name */
	public static final int IND_COLLECTORNAME = 8;
	/** The index of the instrumentation bit mask */
	public static final int IND_BITMASK = 9;
	/** The index of the instrumentation generated metric name */
	public static final int IND_METRICNAME = 10;

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


	/**
	 * Creates a new ShorthandSimpleScript
	 */
	public ShorthandSimpleScript() {
		// TODO Auto-generated constructor stub
	}
	
	public static void main(String[] args) {
		StringBuilder b = new StringBuilder("keys = [");
		for(Field f: ShorthandSimpleScript.class.getDeclaredFields()) {
			if(f.getName().startsWith("IND")) {
				try {
					int i = ((Integer)f.get(null));
					b.append("\n\t").append(i).append(" : ").append("\"").append(f.getName()).append("\",");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		b.append("\n];");
		System.out.println(b);
	}

}

//
//import java.util.regex.*;
///*
//
//    [@]<ClassName>[+] [(Method Attributes)] [@]<MethodName>[<Signature>] [Invocation Options] <CollectorName>[<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
//
//*/
//keys = [
//    0 : "IND_TARGETCLASS_ANNOT",
//    1 : "IND_TARGETCLASS",
//    2 : "IND_INHERRIT",
//    3 : "IND_ATTRS",
//    4 : "IND_METHOD_ANNOT",
//    5 : "IND_METHOD",
//    6 : "IND_SIGNATURE",
//    7 : "IND_INSTOPTIONS",
//    8 : "IND_COLLECTORNAME",
//    9 : "IND_BITMASK",
//    10 : "IND_METRICNAME",
//];
//WH_CLEANER = Pattern.compile("\\s+");
//SH_PATTERN = Pattern.compile(
//        "(@)?" +                         // The class annotation indicator
//        "(.*?)" +                         // The classname
//        "(\\+)?" +                         // The classname options (+ for inherritance) 
//        "\\s(?:\\((.*?)\\)\\s)?" +         // The optional method accessibilities. Defaults to "pub"
//        "(@)?" +                         // The method annotation indicator
//        "(.*?)" +                         // The method name expression, wrapped in "[ ]" if a regular expression
//        "(?:\\((.*)\\))?" +            // The optional method signature 
//        "\\s" +                             // spacer
//        "(?:\\-(\\w+))?" +                 // The method instrumentation options (-dr)
//        "(.*?)" +                         // The collector name
//        "(?:\\[(.*)\\])?" +         // The bitmask option. [] is mandatory. It may contain the bitmask int, or comma separated MetricCollection names
//        "\\s" +                            // spacer
//        "(?:'(.*)')"                    // The metric name format        
//    );
//    
//script = "java.lang.Object equals MethodInterceptor[0] 'java/lang/Object'";
////script = "java.lang.Object+ equals MethodInterceptor[0] 'java/lang/Object'";
//script = WH_CLEANER.matcher(script).replaceAll(" ")
//m = SH_PATTERN.matcher(script);
//if(!m.matches()) {
//    println "NO MATCH";
//} else {
//    println "Match. Parsing....";
//    for(x in 1..m.groupCount()) {
//        println "\t${keys.get(x-1)} [${m.group(x)}]";
//    }
//}
