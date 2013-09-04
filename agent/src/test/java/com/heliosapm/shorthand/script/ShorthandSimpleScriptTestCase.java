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
package com.heliosapm.shorthand.script;

import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import test.com.heliosapm.shorthand.BaseTest;

import com.google.gson.annotations.Since;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript;

/**
 * <p>Title: ShorthandSimpleScriptTestCase</p>
 * <p>Description: Test cases for the simple shorthand script parser</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.script.ShorthandSimpleScriptTestCase</code></p>
 */

public class ShorthandSimpleScriptTestCase extends BaseTest {
	/**
	 * Tests ther parsing of a shorthand script
	 * @param shorthand The shorthand script source
	 * @param targetClass The expected target class
	 * @param targetClassAnnotation The expected target class annotation flag
	 * @param targetClassInterface  The expected is interface enabled flag
	 * @param inherritanceEnabled The expected inherritance enabled flag
	 * @param methodName The expected method name
	 * @param methodNameExpression The expected method name pattern
	 * @param methodSignature  The expected method signature
	 * @param methodSignatureExpression  The expected method signature pattern
	 * @param targetMethodAnnotation The expected target method is annotation flag
	 * @param enumIndex The expected enum collector index
	 * @param bitMask The expected metrics enabled bitmask
	 * @param methodTemplate The expected method template
	 * @param allowReentrant The expected allow reentrancy invocation option flag
	 * @param disableOnTrigger The expected disable on trigger invocation option flag
	 * @param startDisabled The expected start disabled invocation option flag
	 * @throws Exception thrown on any error
	 */
	@Ignore
	protected void test(String shorthand, Class<?> targetClass, boolean targetClassAnnotation,
			boolean targetClassInterface, boolean inherritanceEnabled,
			String methodName, Pattern methodNameExpression,
			String methodSignature, Pattern methodSignatureExpression,
			boolean targetMethodAnnotation, 
			int enumIndex, int bitMask,
			String methodTemplate, boolean allowReentrant,
			boolean disableOnTrigger, boolean startDisabled) throws Exception {
		final ShorthandScript script = ShorthandScript.parse(shorthand);
		
		Assert.assertEquals("Unexpected target class", targetClass, script.getTargetClass());
		Assert.assertEquals("Unexpected target class annot flag", targetClassAnnotation, script.isTargetClassAnnotation());
		// This guy asserts as true since an annotation does qualify as an interface.
		// but that's not what we want.
		//Assert.assertEquals("Unexpected target class iface flag", targetClass.isInterface(), script.isTargetClassInterface());
		Assert.assertEquals("Unexpected target class iface flag", targetClassInterface, script.isTargetClassInterface());
		Assert.assertEquals("Unexpected target class inherritance flag", inherritanceEnabled, script.isInherritanceEnabled());
		
		Assert.assertEquals("Unexpected method name", methodName, script.getMethodName());
		Assert.assertEquals("Unexpected method name expression", methodNameExpression, script.getMethodNameExpression());
		Assert.assertEquals("Unexpected method signature", methodSignature, script.getMethodSignature());
		Assert.assertEquals("Unexpected method signature expression", methodSignatureExpression, script.getMethodSignatureExpression());
		
		Assert.assertEquals("Unexpected target method annot flag", targetMethodAnnotation, script.isTargetMethodAnnotation());
		
		Assert.assertEquals("Unexpected enum index", enumIndex, script.getEnumIndex());
		Assert.assertEquals("Unexpected bitmask", bitMask, script.getBitMask());
		
		Assert.assertEquals("Unexpected method name template", methodTemplate, script.getMethodTemplate());
		
		Assert.assertEquals("Unexpected allow reentrancy inv option", allowReentrant, script.isAllowReentrant());
		Assert.assertEquals("Unexpected disable on trigger inv option", disableOnTrigger, script.isDisableOnTrigger());
		Assert.assertEquals("Unexpected start disabled inv option", startDisabled, script.isStartDisabled());
		
		final int _bitMask = script.getBitMask();
		Assert.assertEquals("Unexpected bitmask", bitMask, _bitMask);
	}
	
	/** Shortcut for match all pattern */
	protected static final Pattern MA = ShorthandScript.MATCH_ALL;
	/** Shortcut and early init for the enum collector index for MethodInterceptor */
	protected static final int MI = EnumCollectors.getInstance().index(MethodInterceptor.class.getName());

	/**
	 * Tests a simple expression with a zero bitmask
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testBasicZeroBitMask() throws Exception {
		test("java.lang.Object equals MethodInterceptor[0] 'java/lang/Object'", 
				Object.class, false,	// target class, target class annot
				false, false,  			// target class iface, target class inherritance
				"equals", null, 		// methodName, methodName pattern
				null, MA, false, 		// method sig, method sig pattern, target method annot 
				MI, 0, 			// enum index, bit mask
				"java/lang/Object",  	// method template
				false, false, false);  	// allow reentrant, disable on trigger, startDisabled		
	}
	
	/**
	 * Tests a simple expression with a zero bitmask and inherritance
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testInherritedBasicZeroBitMask() throws Exception {
		test("java.lang.Object+ equals MethodInterceptor[0] 'java/lang/Object'", 
				Object.class, false,	// target class, target class annot
				false, true,  			// target class iface, target class inherritance
				"equals", null, 		// methodName, methodName pattern
				null, MA, false, 		// method sig, method sig pattern, target method annot 
				MI, 0, 			// enum index, bit mask
				"java/lang/Object",  	// method template
				false, false, false);  	// allow reentrant, disable on trigger, startDisabled
		//test("java.lang.Object equals [0] '$package[0]/$package[1]/$class/$method'", "java.lang.Object", "equals", "java/lang/Object/equals", false, false, 0, 3);
	}
	
	/**
	 * Tests a simple expression with a zero bitmask and inherritance
	 * @throws Exception thrown on any error
	 */
	@Test
	public void testAnnotatedClassBasicZeroBitMask() throws Exception {
		test("@com.google.gson.annotations.Since foo MethodInterceptor[0] 'since/$class/$2'", 
				Since.class, true,		// target class, target class annot
				false, false,  			// target class iface, target class inherritance
				"foo", null, 		// methodName, methodName pattern
				null, MA, false, 		// method sig, method sig pattern, target method annot 
				MI, 0, 					// enum index, bit mask
				"since/$class/$2",  	// method template
				false, false, false);  	// allow reentrant, disable on trigger, startDisabled
		//test("java.lang.Object equals [0] '$package[0]/$package[1]/$class/$method'", "java.lang.Object", "equals", "java/lang/Object/equals", false, false, 0, 3);
	}
	
	
	//@com.google.gson.annotations.Since
	
	
	
}
