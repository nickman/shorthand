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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.heliosapm.shorthand.util.URLHelper;

/**
 * <p>Title: ShorthandSourceProcessor</p>
 * <p>Description: A processor that accepts shorthand source text in various forms, pre-processes it and sequentially passes parsed out scripts to the compiler.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandSourceProcessor</code></p>
 */

public class ShorthandSourceProcessor {
	/** The whitespace cleaner */
	public static final Pattern WH_CLEANER = Pattern.compile("\\s+");
	/** The set statement pattern */
	public static final Pattern SET_DIRECTIVE = Pattern.compile("set:\\s+(.*?)\\s+=\\s+(.*?)$");
	/** The clear statement pattern */
	public static final Pattern CLEAR_DIRECTIVE = Pattern.compile("clear:\\s+(.*?)$");

	/** An empty set for no result processes */
	protected static final Set<ShorthandScript> EMPTY_SET = Collections.unmodifiableSet(new HashSet<ShorthandScript>(0));
	
	/**
	 * Processes each line in the passed reader, setting the pre-defs as they appear, 
	 * parsing the shorthand scripts and initializing the background pollers.
	 * @param input The reader containing a series of shorthand statements
	 * @return a set of parsed shorthand scripts
	 */
	public static Set<ShorthandScript> process(Reader input) {
		if(input==null) return EMPTY_SET;
		Set<ShorthandScript> scripts = new HashSet<ShorthandScript>();		
		BufferedReader reader = new BufferedReader(input);
		String line  = null;
		final Map<String, String> setStatements = new HashMap<String, String>();
		try {
			while((line = reader.readLine())!=null) {
				line = line.trim();
				if(line.isEmpty()) continue;
				log("Processing [%s]", line);
				if(line.toLowerCase().startsWith("set:")) {
					String setStatement = WH_CLEANER.matcher(line).replaceAll("");
					Matcher m = SET_DIRECTIVE.matcher(setStatement);
					if(!m.matches()) {
						loge("Failed to match pattern of directive starting with \"set:\" [%s]", setStatement);
						continue;
					}
					setStatements.put(m.group(1), m.group(2));
				} else if(line.toLowerCase().equals("clear")) {
					setStatements.clear();
					log("Cleared All");
				} else if(line.toLowerCase().startsWith("clear:")) {
					String clearStatement = WH_CLEANER.matcher(line).replaceAll("");
					Matcher m = SET_DIRECTIVE.matcher(clearStatement);
					if(!m.matches()) {
						loge("Failed to match pattern of directive starting with \"clear:\" [%s]", clearStatement);
						continue;
					}
					try {
						Pattern p = Pattern.compile(m.group(1));
						for(String key: setStatements.keySet().toArray(new String[setStatements.size()])) {
							if(p.matcher(key).matches()) {
								setStatements.remove(key);
								log("Cleared var [%s]", key);
							}
						}
					} catch (Exception ex) {
						setStatements.remove(m.group(1));
						log("Cleared var [%s]", m.group(1));
					}						
				} else if(line.toLowerCase().startsWith("poller:")) {
					// TODO
				} else {
					try {
						scripts.add(ShorthandScript.parse(line, setStatements));
						log("Parsed [%s]", line);
					} catch (Exception ex) {
						loge("Failed to parse Shorthand Script [%s]", ex, line);
					}
				}
			}
		} catch (Exception ex) {
			loge("Shorthand script inputstream processing failed", ex);
		}
		return scripts;
	}
	
	/**
	 * Processes each line in the passed input stream, setting the pre-defs as they appear, 
	 * parsing the shorthand scripts and initializing the background pollers.
	 * @param is The input stream containing a series of shorthand statements
	 * @return a set of parsed shorthand scripts
	 */
	public static Set<ShorthandScript> process(InputStream is) {
		if(is==null) return EMPTY_SET;
		return process(new InputStreamReader(is));
	}
	
	/**
	 * Processes each line in the passed file, setting the pre-defs as they appear, 
	 * parsing the shorthand scripts and initializing the background pollers.
	 * @param file The file containing a series of shorthand statements
	 * @return a set of parsed shorthand scripts
	 */
	public static Set<ShorthandScript> process(File file) {
		if(file==null || file.canRead()) return EMPTY_SET;
		FileInputStream fis = null;		
		try {
			fis = new FileInputStream(file);
			return process(fis);
		} catch (Exception ex) {
			return EMPTY_SET;
		} finally {
			if(fis!=null) try { fis.close(); } catch (Exception ex) {}
		}
	}
	
	/**
	 * Processes each line read from the passed URL, setting the pre-defs as they appear, 
	 * parsing the shorthand scripts and initializing the background pollers.
	 * @param url The url referencing a resource containing a series of shorthand statements
	 * @return a set of parsed shorthand scripts
	 */
	public static Set<ShorthandScript> process(URL url) {
		if(url==null) return EMPTY_SET;
		InputStream is = null;
		try {
			is = url.openStream();
			return process(is);
		} catch (Exception ex) {
			return EMPTY_SET;
		} finally {
			if(is!=null) try { is.close(); } catch (Exception ex) {}
		}		
	}
	
	/**
	 * Attempts to determine if the passed string value represents a file or a URL and converts.
	 * Processes each line read from the passed resource, setting the pre-defs as they appear, 
	 * parsing the shorthand scripts and initializing the background pollers.
	 * @param source The name of a resource referencing a resource containing a series of shorthand statements
	 * @return a set of parsed shorthand scripts
	 */
	public static Set<ShorthandScript> process(CharSequence source) {
		if(source==null || source.toString().trim().isEmpty()) return EMPTY_SET;
		String srcName = source.toString().trim();
		if(URLHelper.isValidURL(srcName)) {
			return process(URLHelper.toURL(srcName));
		} else if(new File(srcName).canRead()) {
			return process(new File(srcName));
		} else {
			return EMPTY_SET;
		}
	}
	
	/**
	 * Converts the passed string value to an inputstream and processes.
	 * @param source The source code to process
	 * @return a set of parsed shorthand scripts
	 */
	public static Set<ShorthandScript> processSource(CharSequence source) {
		if(source==null || source.toString().trim().isEmpty()) return EMPTY_SET;
		return process(new StringReader(source.toString()));
	}
	
	private static final String CN = "[ShorthandSourceProcessor]";
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(CN + String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(CN + String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(CN + String.format(fmt, args));
		t.printStackTrace(System.err);
	}
	
	
}
