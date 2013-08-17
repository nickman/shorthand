/**
 * Helios Development Group LLC, 2013
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>Title: InvocationOption</p>
 * <p>Description: Functional enum for shorthand method invocation options</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.InvocationOption</code></p>
 */

public enum InvocationOption {
	/** Allows the method injected instrumentation to be invoked reentrantly instead of being disabled if the cflow > 1 */
	ALLOW_REENTRANT("r"),
	/** Disables all instrumentation on the current thread when the instrumented method is invoked */
	DISABLE_ON_TRIGGER("d"),
	/** Creates but does not install the instrumentation */
	START_DISABLED("s");
	
	/** A map of the method attribute enums keyed by the lower name and aliases */
	public static final Map<String, InvocationOption> ALIAS2ENUM;
	
	static {
		InvocationOption[] values = InvocationOption.values();
		Map<String, InvocationOption> tmp = new HashMap<String, InvocationOption>(values.length*2);
		for(InvocationOption ma: values) {
			tmp.put(ma.name().toLowerCase(), ma);
			for(String name: ma.aliases) {
				if(tmp.put(name, ma)!=null) {
					throw new RuntimeException("Duplicate alias [" + name + "]. Programmer error");
				}
			}
		}
		ALIAS2ENUM = Collections.unmodifiableMap(tmp);
	}

	
	
	private InvocationOption(String...aliases) {
		if(aliases==null || aliases.length==0) {
			this.aliases = Collections.unmodifiableSet(new HashSet<String>(0));
		} else {
			Set<String> s = new HashSet<String>(aliases.length);
			for(String alias: aliases) {
				if(alias==null || alias.trim().isEmpty()) continue;
				s.add(alias.trim().toLowerCase());
			}
			this.aliases = Collections.unmodifiableSet(s);
		}		
	}
	
	/** A set of aliases for this option */
	public final Set<String> aliases;

	/**
	 * Returns a set containing all of the InvocationOptions represented by the passed names, ignoring invalid options.
	 * @param names The names to build the set with
	 * @return the built set
	 */
	public static Set<InvocationOption> getEnabled(CharSequence...names) {
		return getEnabled(true, names);
	}
	
	
	/**
	 * Returns a set containing all of the InvocationOptions represented by the passed names
	 * @param ignoreInvalids if true, invalid or null names will be ignored
	 * @param names The names to build the set with
	 * @return the built set
	 */
	public static Set<InvocationOption> getEnabled(boolean ignoreInvalids, CharSequence...names) {
		Set<InvocationOption> set = EnumSet.noneOf(InvocationOption.class);
		if(names==null || names.length==0) return set; 
		for(CharSequence cs: names) {
			if(names==null || names.toString().isEmpty()) return set;
			char[] chars = cs.toString().replace(" ", "").toCharArray();
			for(char c: chars) {
				String name = new String(new char[]{c});
				InvocationOption ma = forNameOrNull(name);
				if(ma==null) {
					if(!ignoreInvalids) throw new IllegalArgumentException("The option [" + name + "] is invalid");
					continue;
				}
				set.add(ma);				
			}
		}
		return set;
	}
	
	/**
	 * Indicates if the passed name is a valid InvocationOption
	 * @param name the name to test
	 * @return true if the passed name is a valid InvocationOption, false otherwise
	 */
	public static boolean isInvocationOption(CharSequence name) {
		if(name==null) return false;
		try {
			InvocationOption.valueOf(name.toString().trim().toUpperCase());
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

		

	
	/**
	 * Returns the InvocationOption for the passed name
	 * @param name The name to get the attribute for 
	 * @return the decoded InvocationOption 
	 */
	public static InvocationOption forName(CharSequence name) {
		if(name==null) throw new IllegalArgumentException("The passed name was null");
		try {
			return InvocationOption.valueOf(name.toString().trim().toUpperCase());
		} catch (Exception ex) {
			throw new IllegalArgumentException("The passed name [" + name + "] is not a valid InvocationOption");
		}		
	}
	
	/**
	 * Returns the InvocationOption for the passed name or null if not a valid name
	 * @param name The name to get the attribute for 
	 * @return the decoded InvocationOption or null if not a valid name
	 */
	public static InvocationOption forNameOrNull(CharSequence name) {
		try {
			return forName(name);
		} catch (Exception ex) {
			return null;
		}		
	}	

}
