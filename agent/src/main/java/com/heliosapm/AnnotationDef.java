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
package com.heliosapm;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AnnotationNode;

/**
 * <p>Title: AnnotationDef</p>
 * <p>Description: Simple pojo to contain the ASM visited definitions of annotations.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.AnnotationDef</code></p>
 */

public class AnnotationDef {
	/** The annotation desc */
	final String annotationName;
	/** The annotation simple values keyed by the name */
	final Map<String, Object> values = new LinkedHashMap<String, Object>();
	/** The annotation array values keyed by the name */	
	final Map<String, Object[]> arrays = new LinkedHashMap<String, Object[]>();
	
	
	/**
	 * Creates a new AnnotationDef 
	 * @param annotationNode The annotation node to build the def from
	 */
	public AnnotationDef(final AnnotationNode annotationNode) {
		if(annotationNode==null) throw new IllegalArgumentException("The passed annotation node was null");
		annotationName = annotationNode.desc;
		if(annotationNode.values!=null && !annotationNode.values.isEmpty()) {
			String key = null;
			Object value = null;
			for(int i = 0; i < annotationNode.values.size(); i++) {
				if(i%2==0) {
					// we're on a name
					key = annotationNode.values.get(i).toString();
				} else {
					// we got the name, this is the value
					value = annotationNode.values.get(i);
					if(value.getClass().isArray()) {
						arrays.put(key, (Object[]) value);
					} else {
						values.put(key, value);
					}
				}
			}
		}
//		log("Created annotation: [%s]", this.annotationName);
	}

	
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("AnnotationDef: [");
		b.append("\n\tType: ").append(annotationName);
		b.append("\n\tValues: ");
		for(Map.Entry<String, Object> ve: values.entrySet()) {
			b.append("\n\t\t").append(ve.getKey()).append(":").append(ve.getValue()).append(" [").append(ve.getValue().getClass().getName()).append("]");
		}
		b.append("\n\tArrays: ");
		for(Map.Entry<String, Object[]> ve: arrays.entrySet()) {
			b.append("\n\t\t").append(ve.getKey()).append("[");
			for(Object o: ve.getValue()) {
				b.append("\n\t\t\t").append(o).append(" [").append(o.getClass().getName()).append("]");
			}
			b.append("\n\t\t]");
		}						
		return b.append("\n]").toString();
	}
	
}

