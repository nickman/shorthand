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

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.regex.Pattern;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.instrumentor.shorthand.gson.GsonProvider;

/**
 * <p>Title: ShorthandScript</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript</code></p>
 * <pre>
 		<ClassName>[+] [(Method Attributes)] <MethodName>[<Signature>] [Invocation Options] <CollectorName>[<BitMask>|<CollectionNames>] <MetricFormat> DISABLED
 * </pre>
 */

public class ShorthandScript implements JsonDeserializer<ShorthandScript> {
	// ===============================================================================================
	//		JSON ser/deser Permanent Fields  (i.e. not transient, for temp values) 
	// ===============================================================================================
	/** The target class containing the methods to be instrumented. */
	@SerializedName("tc") @Expose
	protected String className;
	/** The target method to be instrumented */
	@SerializedName("meth") @Expose
	protected String methodName;
	/** An optional expression that if provided filters the target methods by matching against the method signature using a regular expression  */
	@SerializedName("sig") @Expose	
	protected String methodSignature;
	/** An optional bitmask of enum members in {@link MethodAttribute} that filters method by {@link Modifier} attributes. Defaults to {@link MethodAttribute#DEFAULT_METHOD_MASK} */
	@SerializedName("mod") @Expose	
	protected int methodMod = MethodAttribute.DEFAULT_METHOD_MASK;
	/** The enum collector simple class name (if the package has been declared in {@link ShorthandProperties#ENUM_COLLECTOR_PACKAGES_PROP} or the fully qualified name */
	@SerializedName("col") @Expose
	protected String collectorName;
	/** The bitmask of the enabled metric members in the declared enum collector in {@link #collectorName} */
	@SerializedName("bitmask") @Expose
	protected int bitMask;
	/** An expression from which the metric name generated a runtime will be derived from */
	@SerializedName("exp")
	@Expose
	protected String metricExpression;
	/** Indicates if injected instrumentation should remain active when the method is called recursively. Defaults to false. */
	@SerializedName("rec") @Expose
	protected boolean allowRecursion = false;
	/** Indicates if all instrumentation should be disabled until the intrumented method completes. Defaults to false. */
	@SerializedName("dis") @Expose
	protected boolean disableOnCall = false;
	// ===============================================================================================
	//		Permanent Fields  (i.e. not transient, for temp values) 
	//		derrived from the deser values and inserted when serialized  
	// ===============================================================================================
	/** Indicates that the name in {@link #className} is a type level annotation and tha target is any classes annotated as such */
	protected boolean annotatedClass = false;
	/** Indicates that the name in {@link #className} is an interface implying that the target classes are ones that implement this interface */
	protected boolean iface = false;
	/** Indicates that the target classes are one that inherrit from the class named in {@link #className} */
	protected boolean inherritance = false;
	/** Indicates that the name in {@link #methodName} is a method level annotation and the target is any method annotated as such */
	protected boolean annotatedMethod;
	
	// ===============================================================================================
	//		Transient Fields  (i.e. temp values read in by Gson, but then discarded) 
	// ===============================================================================================
	/** The names or ordinals of the enabled metric members in the declared enum collector in {@link #collectorName}. Will be parsed and converted into the bitmask  */
	@SerializedName("cmbrs")
	@Expose(serialize=false)
	protected String collectorMembers;

	/** Alternate expression syntax to specify the {@link ShorthandScript#methodMod} as comma separated names or ordinals of the members in {@link MethodAttribute} */
	@SerializedName("mbrs")
	@Expose(serialize=false)
	protected String methodModMembers;


	
	/**
	 * Parses a shorthand json script and returns an array of script objects
	 * @param scriptText The json script
	 * @return an array of script objects
	 */
	public static ShorthandScript[] parse(String scriptText) {
		JsonElement element = new JsonParser().parse(scriptText);
		if(element.isJsonArray()) {
			return GsonProvider.getInstance().getGson().fromJson(scriptText, ShorthandScript[].class);
		}
		return new ShorthandScript[]{GsonProvider.getInstance().getGson().fromJson(scriptText, ShorthandScript.class)};
	}

	/** Annotation prefix pattern */
	private static final Pattern ANNOT = Pattern.compile("@");
	/** Inherritance sufffix pattern */
	private static final Pattern PLUS = Pattern.compile("\\+");
	/** Interface sufffix pattern */
	private static final Pattern MULT = Pattern.compile("\\*");
	

	/**
	 * Executed after json deserialization to complete parsing and validate
	 * @param pre The pre-processed script object
	 * @param json The json fragment that was just parsed
	 * @return The validated and post-processed script object
	 * @throws ShorthandParseFailureException thrown on any validation errors
	 */
	protected ShorthandScript postDeserialize(ShorthandScript pre, String json) throws ShorthandParseFailureException {
		if(pre.className==null || pre.className.trim().isEmpty()) throw new ShorthandParseFailureException("Target Class Name was null or empty", json);
		pre.className = pre.className.trim();
		if(pre.className.startsWith("@")) {
			pre.annotatedClass = true;
			pre.className = ANNOT.matcher(pre.className).replaceFirst("");
		}
		if(pre.className.endsWith("*")) {
			pre.inherritance = true;
			pre.iface = true;
			pre.className = MULT.matcher(pre.className).replaceFirst("");
		}		
		if(pre.className.endsWith("+")) {
			if(iface) throw new ShorthandParseFailureException("Class Name inherritance was specified as interface (*) and extends (+)", json); 
			pre.inherritance = true;
			pre.iface = false;
			pre.className = PLUS.matcher(pre.className).replaceFirst("");
		}
		
		return pre;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
	 */
	@Override
	public ShorthandScript deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
		ShorthandScript pre = GsonProvider.getInstance().getNoSerGson().fromJson(json, ShorthandScript.class);
		return postDeserialize(pre, json.toString());
	}

	
	
	public static void main(String[] args) {
		log("Shorthand Script Test");
		ShorthandScript sd = new ShorthandScript();
		//com.theice.aop.icebyteman.test.classes.BaseSampleClass massiveCalc(int) [*] '$class/$method/$1'"
		sd.className = "com.heliosapm.shorthand.testclasses.BaseSampleClass";
		sd.methodName = "massiveCalc";
		sd.methodSignature = "int";
		sd.collectorName = "MethodInterceptor";
		sd.bitMask = 4095;
		sd.metricExpression="$class/$method/$1";
		
		//String json = GsonProvider.getInstance().getPrettyPrinter().toJson(sd);
		String json = GsonProvider.getInstance().getGson().toJson(sd);
		
		log("JSON:\n%s", json);
		
		// {\"tc\":\"com.heliosapm.shorthand.testclasses.BaseSampleClass\",\"meth\":\"massiveCalc\",\"sig\":\"int\",\"col\":\"MethodInterceptor\",\"bitmask\":4095,\"exp\":\"$class/$method/$1\",\"rec\":false,\"dis\":false}
		
		ShorthandScript[] scripts = parse(json);
		log(Arrays.deepToString(scripts));
		
	}
	
	
	/**
	 * Creates a new ShorthandScript
	 */
	public ShorthandScript() {
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append("ShorthandScript [");
		b.append("\n\t");
		if(annotatedClass) b.append("@");
		b.append(className);
		if(iface && inherritance) b.append("* ");
		if(!iface && inherritance) b.append("+ ");
		
		b.append("\n\tmethodName=").append(methodName);

		
		if (methodSignature != null) {
			b.append("\n\tmethodSignature=[");
			b.append(methodSignature).append("]");
		}
		
		b.append("\n\tmethodMod=").append(methodMod); //FIXME: Decode to member names
		b.append("\n\tannotatedMethod=").append(annotatedMethod);
		
		b.append("\n\tcollectorName=").append(collectorName);

		b.append("\n\tbitMask=").append(bitMask);
		
		b.append("\n\tmetricExpression=[").append(metricExpression).append("]");

		b.append("\n\tallowRecursion=").append(allowRecursion);
		
		b.append("\n\tdisableOnCall=").append(disableOnCall);
		
		
		b.append("\n]");
		return b.toString();
	}
	
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(String.format(fmt, args));
		t.printStackTrace(System.err);
	}









}