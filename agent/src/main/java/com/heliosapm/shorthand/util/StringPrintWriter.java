package com.heliosapm.shorthand.util;
/**
 * Helios Development Group LLC, 2013
 */


import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * <p>Title: StringPrintWriter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.jmx.instrumentor.StringPrintWriter</code></p>
 */

public class StringPrintWriter extends PrintWriter {
	/** The internal string writer */
	private final StringWriter stringWriter;
	/**
	 * Creates a new StringPrintWriter
	 */
	public StringPrintWriter() {
		super(new StringWriter(), true);
		stringWriter = (StringWriter)this.out;
	}
	
	/**
	 * Creates a new StringPrintWriter
	 * @param initialSize The initial buffer for the string writer
	 */
	public StringPrintWriter(int initialSize) {
		super(new StringWriter(initialSize), true);
		stringWriter = (StringWriter)this.out;
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		stringWriter.flush();
		return stringWriter.toString();
	}
}
