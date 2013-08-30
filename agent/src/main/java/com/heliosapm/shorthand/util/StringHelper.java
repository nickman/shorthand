/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.util;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

/**
 * <p>Title: StringHelper</p>
 * <p>Description: String helper utility class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.apmrouter.jmx.StringHelper</code></p>
 */
public class StringHelper {
	
	/** The ThreadMXBean */
	protected static final ThreadMXBean tmx = ManagementFactory.getThreadMXBean();
	
	
	public static String initCap(CharSequence cs) {
		char[] chars = cs.toString().trim().toCharArray();
		chars[0] = new String(new char[]{chars[0]}).toUpperCase().charAt(0);
		return new String(chars);
	}

	/**
	 * Returns a formatted string representing the thread identified by the passed id
	 * @param id The id of the thread
	 * @return the formatted message
	 */
	public static String formatThreadName(long id) {
		if(id<1) return "[Nobody]";
		ThreadInfo ti = tmx.getThreadInfo(id);		
		if(ti==null)  return String.format("No Such Thread [%s]", id);
		return String.format("[%s/%s]", ti.getThreadName(), ti.getThreadId());
	}
	
	
	/**
	 * Returns a formatted string presenting the passed elapsed time in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @return the formatted message
	 */
	public static String reportTimes(String title, long nanos) {
		StringBuilder b = new StringBuilder(title).append(":  ");
		b.append(nanos).append( " ns.  ");
		b.append(TimeUnit.MICROSECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " \u00b5s.  ");
		b.append(TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " ms.  ");
		b.append(TimeUnit.SECONDS.convert(nanos, TimeUnit.NANOSECONDS)).append( " s.");
		return b.toString();
	}
	
	/**
	 * Returns a formatted string presenting the average elapsed time 
	 * based on the passed time stamp and count of incidents in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @param count The number of incidents, used for calculating an average
	 * @return the formatted message
	 */
	public static String reportAvgs(String title, long nanos, long count) {
		if(nanos==0 || count==0) return reportTimes(title, 0);
		return reportTimes(title, (nanos/count));
	}
	
	/**
	 * Returns a formatted string presenting the total and average elapsed time 
	 * based on the passed time stamp and count of incidents in the native nanos, microseconds, milliseconds and seconds.
	 * @param title The arbitrary name for the timing
	 * @param nanos The elapsed time in nanos
	 * @param count The number of incidents, used for calculating an average
	 * @return the formatted message
	 */
	public static String reportSummary(String title, long nanos, long count) {
		return reportTimes(title, nanos) + 
				"\n" +
				reportAvgs(title + "  AVGS", nanos, count);
	}


	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder() {
		return new StringBuilder();
	}	
	
	/**
	 * Escapes quote characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String escapeQuotes(CharSequence s) {
		return s.toString().replace("\"", "\\\"");
	}
	
	/**
	 * Escapes json characters in the passed string
	 * @param s The string to esacape
	 * @return the escaped string
	 */
	public static String jsonEscape(CharSequence s) {
		return s.toString().replace("\"", "\\\"").replace("[", "\\[").replace("]", "\\]").replace("{", "\\{").replace("}", "\\}");
	}
	
	
	/**
	 * Acquires and truncates the current thread's StringBuilder.
	 * @param size the inited size of the stringbuilder
	 * @return A truncated string builder for use by the current thread.
	 */
	public static StringBuilder getStringBuilder(int size) {
		return new StringBuilder(size);
	}
	
	/**
	 * Concatenates all the passed strings
	 * @param args The strings to concatentate
	 * @return the concatentated string
	 */
	public static String fastConcat(CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		for(CharSequence s: args) {
			if(s==null) continue;
			buff.append(s);
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skipBlanks If true, blank or null items in the passed array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(boolean skipBlanks, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		if(args!=null && args.length > 0) {			
			for(CharSequence s: args) {				
				if(!skipBlanks || (s!=null && s.length()>0)) {
					buff.append(s).append(delimeter);
				}
			}
			if(buff.length()>0) {
				buff.deleteCharAt(buff.length()-1);
			}
		}
		return buff.toString();
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * Blank or zero length items in the array will be skipped.
	 * @param delimeter The delimeter to insert between each item.
	 * @param args The string array to flatten
	 * @return the flattened string
	 */
	public static String fastConcatAndDelim(String delimeter, CharSequence...args) {
		return fastConcatAndDelim(true, delimeter, args);
	}
	
	/**
	 * Accepts an array of strings and returns the array flattened into a single string, optionally delimeted.
	 * @param skip Skip this many
	 * @param delimeter The delimeter
	 * @param args The strings to concat
	 * @return the resulting string
	 */
	public static String fastConcatAndDelim(int skip, String delimeter, CharSequence...args) {
		StringBuilder buff = getStringBuilder();
		int cnt = args.length - skip;
		int i = 0;
		for(; i < cnt; i++) {
			if(args[i] != null && args[i].length() > 0) {
				buff.append(args[i]).append(delimeter);
			}
		}
		StringBuilder b = buff.reverse();
		while(b.subSequence(0, delimeter.length()).equals(delimeter)) {
			b.delete(0, delimeter.length());
		}
		return b.reverse().toString();
	}	

}
