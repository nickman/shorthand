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

import org.junit.Test;
import org.junit.Assert;

import test.com.heliosapm.shorthand.BaseTest;

/**
 * <p>Title: ShorthandSimpleScriptTestCase</p>
 * <p>Description: Test cases for the simple shorthand script parser</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.script.ShorthandSimpleScriptTestCase</code></p>
 */

public class ShorthandSimpleScriptTestCase extends BaseTest {
	/**
	 * Encapsulates a shorthand compilation test
	 * @param shorthand The shorthand expression to test
	 * @param className The expected class name
	 * @param methodName The expected method name (and signature)
	 * @param expectedMetricName The expected metric name
	 * @param isIface true if an interface, false otherwise
	 * @param isInherrited true if implements/extends, false otherwise
	 * @param expectedBitMask The expected bit mask
	 * @param expectedRuleCount The expected rule script and rule count
	 * @throws Exception thrown on any error
	 */
	protected void test(String shorthand, String className, String methodName, String expectedMetricName, boolean isIface, boolean isInherrited, int expectedBitMask, int expectedRuleCount) throws Exception {
		final String btm = ShorthandCompiler.compile(shorthand);
		final int bitMask = getBitMaskFromRule(btm);
		Assert.assertEquals("Unexpected bitmask", expectedBitMask, bitMask);
		
		Map<String, RuleScript> ruleScripts = compileRules(btm);
		Assert.assertEquals("Unexpected RuleScript Count", expectedRuleCount, ruleScripts.size());
		List<Rule> rules = new ArrayList<Rule>();
		for(RuleScript rs: ruleScripts.values()) {
			rules.add(Rule.create(rs, ClassLoader.getSystemClassLoader(), HM));
		}
		Assert.assertEquals("Unexpected RuleScript Count", expectedRuleCount, rules.size());
		int index = 0;
		for(Rule rule: rules) {						 
			RuleScript rs = ruleScripts.get(rule.getName());			
			
			if(index!=0) {
				final String metricName = extractMetricName(rs.getRuleText());
				Assert.assertEquals("Unexpected metricName", expectedMetricName, metricName);
			}
			
			
			Assert.assertEquals("Rule and RuleScript names not equal", rs.getName(), rule.getName());
			Assert.assertEquals("Unexpected rule name in compiled rule", RULE_NAME_PREFIXES[index] + " " + shorthand, rule.getName());
			// The rule instance does not have the helper class yet
			Assert.assertEquals("Unexpected Helper Class Name in rule script", ICEHelper.class.getName(), rs.getTargetHelper());			
			Assert.assertEquals("Unexpected method name", methodName, rule.getTargetMethod());
			Assert.assertEquals("Unexpected method name", className, rule.getTargetClass());
			Assert.assertEquals("Unexpected class interface", isIface, rule.isInterface());
			Assert.assertEquals("Unexpected class inherritance", isInherrited, rule.isOverride());
			Location location = rule.getTargetLocation();
			Action action = rule.getAction();
			switch(index) {
			case 0: // open
				Assert.assertEquals("Unexpected location", ShorthandCompiler.OPEN_LOC, location.toString());				
				Assert.assertEquals("Unexpected action", SPACE_AND_SEMIC.matcher(String.format(ShorthandCompiler.OPEN_ACTION, bitMask, "classSig", "methodSig")).replaceAll(""), SPACE_AND_SEMIC.matcher(action.toString()).replaceAll(""));				
				break;
			case 1: // close
				Assert.assertEquals("Unexpected location", ShorthandCompiler.CLOSE_LOC, location.toString());
				//Assert.assertEquals("Unexpected action", SPACE_AND_SEMIC.matcher(String.format(ShorthandCompiler.CLOSE_ACTION, metricName)).replaceAll(""), SPACE_AND_SEMIC.matcher(action.toString()).replaceAll(""));
				break;					
			case 2: // exception
				Assert.assertEquals("Unexpected location", ShorthandCompiler.EXC_LOC, location.toString());
				//Assert.assertEquals("Unexpected action", SPACE_AND_SEMIC.matcher(String.format(ShorthandCompiler.EXC_ACTION, metricName)).replaceAll(""), SPACE_AND_SEMIC.matcher(action.toString()).replaceAll(""));
			}
			index++;
		}
	}
	
}
