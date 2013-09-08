/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand.naming;

/**
 * <p>Title: MetricNameFragment</p>
 * <p>Description: A functional enumeration for all the supported static [precompiled] metric name substitution tokens</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.MetricNameFragment</code></p>
 */

public enum MetricNameFragment {
	/** The package name */
	packagename,
	/** The simple class name */
	classname,
	/** The method name */
	methodname;
}
