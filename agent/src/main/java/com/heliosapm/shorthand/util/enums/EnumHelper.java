/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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
package com.heliosapm.shorthand.util.enums;

import java.lang.reflect.Array;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: EnumHelper</p>
 * <p>Description: Static enum helper methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.enums.EnumHelper</code></p>
 */

public class EnumHelper {

	/**
	 * Some quickie tests
	 * @param args None
	 */
	public static void main(String[] args) {
		log("EnumHelper Test");
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
	 * Acquires the combined bitmask of the passed IntBitMaskedEnum enum class members 
	 * where the enabled members are identified by the passed names, each name optionally being a member name
	 * or a member ordinal. Each passed name is trimmed and uppercased, but matched ignoring case. 
	 * @param strict If true, a runtime exception will be thrown if any of the names cannot be matched
	 * @param clazz The IntBitMaskedEnum enum class
	 * @param names The names or ordinals of the members
	 * @return the ffective bit mask
	 */
	public static <T extends Enum<?> & IntBitMaskedEnum> int getEnabledBitMask(boolean strict, Class<T> clazz, String ...names) {
		if(names==null || names.length==0) return 0;
		T[] constants = clazz.getEnumConstants();
		Map<String, T> members = new HashMap<String, T>(constants.length * 2);
		for(T t: constants) {
			members.put(t.name().toUpperCase(), t);
			members.put("" + t.ordinal(), t);
		}
		int bitMask = 0;
		for(String name: names) {
			T t = members.get(name.trim().toUpperCase());
			if(t==null) {
				if(strict) throw new RuntimeException("The name [" + name + "] was not an ordinal or member name of class [" + clazz.getName() + "]");
				continue;
			}
			bitMask = (bitMask | t.getMask());
		}
		return bitMask;
	}
	
	/**
	 * Returns an array of the members activated in the passed bit mask
	 * @param clazz The IntBitMaskedEnum enum class
	 * @param bitMask The bitmask to match
	 * @return an array of enum members
	 */
	public static <T extends Enum<?> & IntBitMaskedEnum> T[]  getEnabledFor(Class<T> clazz, int bitMask) {
		T[] values = clazz.getEnumConstants();		
		Set<T> matches = new HashSet<T>();
		for(T t: values) {
			if((bitMask & t.getMask())==t.getMask()) {
				matches.add(t);
			}
		}
		return matches.toArray((T[])Array.newInstance(clazz, matches.size()));
	}
	
	/**
	 * Returns an array of the names of members activated in the passed bit mask
	 * @param clazz The IntBitMaskedEnum enum class
	 * @param bitMask The bitmask to match
	 * @return an array of enum member names
	 */
	public static <T extends Enum<?> & IntBitMaskedEnum> String[]  getEnabledNamesFor(Class<T> clazz, int bitMask) {
		T[] values = clazz.getEnumConstants();
		Set<String> matches = new HashSet<String>();
		for(T t: values) {
			if((bitMask & t.getMask())==t.getMask()) {
				matches.add(t.name());
			}
		}
		return matches.toArray(new String[matches.size()]);
	}
	
	
	/**
	 * Casts the passed class to an intbitmaskedenum enum class
	 * @param clazz The class to cast
	 * @return the cast class
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Enum<?> & IntBitMaskedEnum> Class<T> castToIntBitMaskedEnum(Class<?> clazz) {
		return (Class<T>)clazz;
	}

}
