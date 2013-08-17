/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

/**
 * <p>Title: ShorthandDirective</p>
 * <p>Description: A type checked container for a compiled shorthand expression</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandDirective</code></p>
 * <pre>
 		<ClassName>[+] <MethodName>[<Signature>] [<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
 * </pre>
 */

public class ShorthandDirective {
	protected String className;
	protected boolean iface;
	protected boolean inherritance;
	protected String methodName;
	protected String methodSignature;
	protected int bitMask;
	protected String metricExpression;
	protected String closeExpression;
	protected boolean recursive;
	protected boolean disabled;
	
	
	
	/**
	 * Creates a new ShorthandDirective
	 * @param className
	 * @param iface
	 * @param inherritance
	 * @param methodName
	 * @param methodSignature
	 * @param bitMask
	 * @param metricExpression
	 * @param closeExpression 
	 * @param recursive
	 * @param disabled
	 */
	public ShorthandDirective(String className, boolean iface,
			boolean inherritance, String methodName, String methodSignature,
			int bitMask, String metricExpression, String closeExpression, boolean recursive, boolean disabled) {
		super();
		this.className = className;
		this.iface = iface;
		this.inherritance = inherritance;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
		this.bitMask = bitMask;
		this.metricExpression = metricExpression;
		this.closeExpression = closeExpression;
		this.recursive = recursive;
		this.disabled = disabled;
	}



	/**
	 * Creates a new ShorthandDirective
	 */
	public ShorthandDirective() {
		// TODO Auto-generated constructor stub
	}

}
