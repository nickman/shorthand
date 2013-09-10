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
package com.heliosapm.shorthand.instrumentor;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtNewMethod;

/**
 * <p>Title: AccessorGenerator</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.AccessorGenerator</code></p>
 */

public class AccessorGenerator {
	
	public interface PropertyAccessor<S, V> {
		   public void set(S source, V value);
		   public V get(S source);
		}	

    private final ClassPool pool;

    public AccessorGenerator() {
        pool = new ClassPool();
        pool.appendSystemPath();
    }

    public Map<String, PropertyAccessor> createAccessors(Class<?> klazz) throws Exception {
        Field[] fields = klazz.getDeclaredFields();

        Map<String, PropertyAccessor> temp = new HashMap<String, PropertyAccessor>();
        for (Field field : fields) {
            PropertyAccessor accessor = createAccessor(klazz, field);
            temp.put(field.getName(), accessor);
        }

        return Collections.unmodifiableMap(temp);
    }

    private PropertyAccessor createAccessor(Class<?> klazz, Field field) throws Exception {
        final String classTemplate = "%s_%s_accessor";
        final String getTemplate = "public Object get(Object source) { return ((%s)source).%s; }";
        final String setTemplate = "public void set(Object dest, Object value) { return ((%s)dest).%s = (%s) value; }";

        final String getMethod = String.format(getTemplate, 
                                               klazz.getName(),
                                               field.getName());
        final String setMethod = String.format(setTemplate, 
                                               klazz.getName(), 
                                               field.getName(), 
                                               field.getType().getName());

        final String className = String.format(classTemplate, klazz.getName(), field.getName());

        CtClass ctClass = pool.makeClass(className);
        ctClass.addMethod(CtNewMethod.make(getMethod, ctClass));
        ctClass.addMethod(CtNewMethod.make(setMethod, ctClass));
        ctClass.setInterfaces(new CtClass[] { pool.get(PropertyAccessor.class.getName()) });
        Class<?> generated = ctClass.toClass();
        return (PropertyAccessor) generated.newInstance();
    }

    public static void main(String[] args) throws Exception {
        AccessorGenerator generator = new AccessorGenerator();

        Map<String, PropertyAccessor> accessorsByName = generator.createAccessors(PurchaseOrder.class);

        PurchaseOrder purchaseOrder = new PurchaseOrder("foo", new Customer());

        accessorsByName.get("name").set(purchaseOrder, "bar");
        String name = (String) accessorsByName.get("name").get(purchaseOrder);
        System.out.println(name);
    }
}