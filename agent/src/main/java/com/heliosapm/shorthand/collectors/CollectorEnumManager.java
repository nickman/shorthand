
package com.heliosapm.shorthand.collectors;

import java.util.HashSet;
import java.util.Set;


/**
 * <p>Title: CollectorEnumManager</p>
 * <p>Description: An aggregating wrapper for {@link ICollector} enum instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.helper.CollectorEnumManager</code></p>
 */

public class CollectorEnumManager {
	protected Class<? extends Enum<? extends ICollector>> enumClass = MethodInterceptor.class;
	protected Enum<? extends ICollector> enumInstance = MethodInterceptor.SYS_CPU;
	
	protected final Set<Enum<? extends ICollector>> collectors = new HashSet<Enum<? extends ICollector>>();
	
	public void foo() {
		try {
			collectors.add(enumClass.newInstance());
			
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
