package org.red5.server.service;

/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 * 
 * Copyright (c) 2006-2010 by respective authors (see below). All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or modify it under the 
 * terms of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either version 2.1 of the License, or (at your option) any later 
 * version. 
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along 
 * with this library; if not, write to the Free Software Foundation, Inc., 
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA 
 */

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.apache.commons.beanutils.BeanMap;
//import org.apache.commons.beanutils.BeanUtils;
//import org.apache.commons.beanutils.ConversionException;
import org.red5.server.IConnection;
//import org.red5.server.api.remoting.IRemotingConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Misc utils for conversions
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public class ConversionUtils {

	private static final Logger log = LoggerFactory.getLogger(ConversionUtils.class);

	private static final Class<?>[] PRIMITIVES = { boolean.class, byte.class, char.class, short.class, int.class,
			long.class, float.class, double.class };

	private static final Class<?>[] WRAPPERS = { Boolean.class, Byte.class, Character.class, Short.class,
			Integer.class, Long.class, Float.class, Double.class };

	private static final String NUMERIC_TYPE = "[-]?\\b\\d+\\b|[-]?\\b[0-9]*\\.?[0-9]+(?:[eE][-+]?[0-9]+)?\\b";
	
	/**
	 * Parameter chains
	 */
	private static final Class<?>[][] PARAMETER_CHAINS = { { boolean.class, null }, { byte.class, Short.class },
			{ char.class, Integer.class }, { short.class, Integer.class }, { int.class, Long.class },
			{ long.class, Float.class }, { float.class, Double.class }, { double.class, null } };

	/** Mapping of primitives to wrappers */
	private static Map<Class<?>, Class<?>> primitiveMap = new HashMap<Class<?>, Class<?>>();

	/** Mapping of wrappers to primitives */
	private static Map<Class<?>, Class<?>> wrapperMap = new HashMap<Class<?>, Class<?>>();

	/** 
	 * Mapping from wrapper class to appropriate parameter types (in order) 
	 * Each entry is an array of Classes, the last of which is either null
	 * (for no chaining) or the next class to try
	 */
	private static Map<Class<?>, Class<?>[]> parameterMap = new HashMap<Class<?>, Class<?>[]>();

	static {
		for (int i = 0; i < PRIMITIVES.length; i++) {
			primitiveMap.put(PRIMITIVES[i], WRAPPERS[i]);
			wrapperMap.put(WRAPPERS[i], PRIMITIVES[i]);
			parameterMap.put(WRAPPERS[i], PARAMETER_CHAINS[i]);
		}
	}

	/**
	 * Convert source to given class
	 * @param source         Source object
	 * @param target         Target class
	 * @return               Converted object
	 * @throws ConversionException           If object can't be converted
	 *
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convert(Object source, Class<?> target)  {
		if (target == null) {
//			throw new ConversionException("Unable to perform conversion, target was null");
		}
		if (source == null) {
			if (target.isPrimitive()) {
//				throw new ConversionException(String.format("Unable to convert null to primitive value of %s", target));
			}
			return source;
		} else if ((source instanceof Float && ((Float) source).isNaN())
				|| (source instanceof Double && ((Double) source).isNaN())) {
			// Don't convert NaN values
			return source;
		}

		//log.trace("Source: {} Target: {}", source.getClass(), target);
		if (IConnection.class.isAssignableFrom(source.getClass()) && (!target.equals(IConnection.class))) {
//			throw new ConversionException("IConnection must match exactly");
		}
		if (target.isInstance(source)) {
			return source;
		}
		if (target.isAssignableFrom(source.getClass())) {
			return source;
		}
		if (target.isArray()) {
//			return convertToArray(source, target);
		}
		if (target.equals(String.class)) {
			return source.toString();
		}
		if (target.isPrimitive()) {
			return convertToWrappedPrimitive(source, primitiveMap.get(target));
		}
		if (wrapperMap.containsKey(target)) {
			return convertToWrappedPrimitive(source, target);
		}
		if (target.equals(Map.class)) {
			return convertBeanToMap(source);
		}
		if (target.equals(List.class) || target.equals(Collection.class)) {
			if (source.getClass().equals(LinkedHashMap.class)) {
				return convertMapToList((LinkedHashMap<?, ?>) source);
			} else if (source.getClass().isArray()) {
//				return convertArrayToList((Object[]) source);
			}
		}
		if (target.equals(Set.class) && source.getClass().isArray()) {
			return convertArrayToSet((Object[]) source);
		}
		if (target.equals(Set.class) && source instanceof List) {
			return new HashSet((List) source);
		}
		//Trac #352
		if (source instanceof Map) {
//			return convertMapToBean((Map) source, target);
		}
//		throw new ConversionException(String.format("Unable to preform conversion from %s to %s", source, target));
		return null;
	}

	/**
	 * Convert to array
	 * @param source         Source object
	 * @param target         Target class
	 * @return               Converted object
	 * @throws ConversionException           If object can't be converted
	 */
	public static Object convertToArray(Object source, Class<?> target)  {
		try {
			Class<?> targetType = target.getComponentType();
			if (source.getClass().isArray()) {
				Object targetInstance = Array.newInstance(targetType, Array.getLength(source));
				for (int i = 0; i < Array.getLength(source); i++) {
					Array.set(targetInstance, i, convert(Array.get(source, i), targetType));
				}
				return targetInstance;
			}
			if (source instanceof Collection<?>) {
				Collection<?> sourceCollection = (Collection<?>) source;
				Object targetInstance = Array.newInstance(target.getComponentType(), sourceCollection.size());
				Iterator<?> it = sourceCollection.iterator();
				int i = 0;
				while (it.hasNext()) {
					Array.set(targetInstance, i++, convert(it.next(), targetType));
				}
				return targetInstance;
			}
			//throw new ConversionException("Unable to convert to array");
		} catch (Exception ex) {
			//throw new ConversionException("Error converting to array", ex);
		}
		return null;
	}

	public static List<Object> convertMapToList(Map<?, ?> map) {
		List<Object> list = new ArrayList<Object>(map.size());
		list.addAll(map.values());
		return list;
	}

	/**
	 * Convert to wrapped primitive
	 * @param source            Source object
	 * @param wrapper           Primitive wrapper type
	 * @return                  Converted object
	 */
	public static Object convertToWrappedPrimitive(Object source, Class<?> wrapper) {
		if (source == null || wrapper == null) {
			return null;
		}
		if (wrapper.isInstance(source)) {
			return source;
		}
		if (wrapper.isAssignableFrom(source.getClass())) {
			return source;
		}
		if (source instanceof Number) {
			return convertNumberToWrapper((Number) source, wrapper);
		} else {
			//ensure we dont try to convert text to a number, prevent 
			//NumberFormatException
			if (Number.class.isAssignableFrom(wrapper)) {
				//test for int or fp number
				if (!source.toString().matches(NUMERIC_TYPE)) {					
					//throw new ConversionException(String.format("Unable to convert string %s its not a number type: %s", source, wrapper));
				}
			}
			return convertStringToWrapper(source.toString(), wrapper);
		}
	}

	/**
	 * Convert string to primitive wrapper like Boolean or Float
	 * @param str               String to convert
	 * @param wrapper           Primitive wrapper type
	 * @return                  Converted object
	 */
	public static Object convertStringToWrapper(String str, Class<?> wrapper) {
		log.trace("String: {} to wrapper: {}", str, wrapper);
		if (wrapper.equals(String.class)) {
			return str;
		} else if (wrapper.equals(Boolean.class)) {
			return Boolean.valueOf(str);
		} else if (wrapper.equals(Double.class)) {
			return Double.valueOf(str);
		} else if (wrapper.equals(Long.class)) {
			return Long.valueOf(str);
		} else if (wrapper.equals(Float.class)) {
			return Float.valueOf(str);
		} else if (wrapper.equals(Integer.class)) {
			return Integer.valueOf(str);
		} else if (wrapper.equals(Short.class)) {
			return Short.valueOf(str);
		} else if (wrapper.equals(Byte.class)) {
			return Byte.valueOf(str);
		}
		//throw new ConversionException(String.format("Unable to convert string to: %s", wrapper));
		return null;
	}

	/**
	 * Convert number to primitive wrapper like Boolean or Float
	 * @param num               Number to conver
	 * @param wrapper           Primitive wrapper type
	 * @return                  Converted object
	 */
	public static Object convertNumberToWrapper(Number num, Class<?> wrapper) {
		
		if (wrapper.equals(String.class)) {
			return num.toString();
		} else if (wrapper.equals(Boolean.class)) {
			return Boolean.valueOf(num.intValue() == 1);
		} else if (wrapper.equals(Double.class)) {
			return Double.valueOf(num.doubleValue());
		} else if (wrapper.equals(Long.class)) {
			return Long.valueOf(num.longValue());
		} else if (wrapper.equals(Float.class)) {
			return Float.valueOf(num.floatValue());
		} else if (wrapper.equals(Integer.class)) {
			return Integer.valueOf(num.intValue());
		} else if (wrapper.equals(Short.class)) {
			return Short.valueOf(num.shortValue());
		} else if (wrapper.equals(Byte.class)) {
			return Byte.valueOf(num.byteValue());
		}
		//throw new ConversionException(String.format("Unable to convert number to: %s", wrapper));
		return null;
	}

	/**
	 * Find method by name and number of parameters
	 * @param object            Object to find method on
	 * @param method            Method name
	 * @param numParam          Number of parameters
	 * @return                  List of methods that match by name and number of parameters
	 */
	public static List<Method> findMethodsByNameAndNumParams(Object object, String method, int numParam) {
		LinkedList<Method> list = new LinkedList<Method>();
		Method[] methods = object.getClass().getMethods();
		for (Method m : methods) {
//			log.debug("Method name: {}", m.getName());
			//check parameter length first since this should speed things up
			if (m.getParameterTypes().length != numParam) {
//				log.debug("Param length not the same");
				continue;
			}
			//now try to match the name
			if (!m.getName().equals(method)) {
//				log.debug("Method name not the same");
				continue;
			}
			list.add(m);
		}
		return list;
	}

	/**
	 * Convert parameters using methods of this utility class
	 * @param source                Array of source object
	 * @param target                Array of target classes
	 * @return                      Array of converted objects
	 * @throws ConversionException  If object can't be converted
	 */
	public static Object[] convertParams(Object[] source, Class<?>[] target)  {
		Object[] converted = new Object[target.length];
		for (int i = 0; i < target.length; i++) {
			converted[i] = convert(source[i], target[i]);
		}
		return converted;
	}

	/**
	 * Convert parameters using methods of this utility class
	 * @param source                Array of source object
	 * @return                      Array of converted objects
	 */
	public static Class<?>[] convertParams(Object[] source) {
		Class<?>[] converted = null;
		if (source != null) {
			converted = new Class<?>[source.length];
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null) {
					converted[i] = source[i].getClass();
				} else {
					converted[i] = null;
				}
			}
		} else {
			converted = new Class<?>[0];
		}
		return converted;
	}

	/**
	 *
	 * @param source source arra
	 * @return list
	 * @throws ConversionException on failure
	 */
	public static List<?> convertArrayToList(Object[] source)  {
		List<Object> list = new ArrayList<Object>(source.length);
		for (Object element : source) {
			list.add(element);
		}
		return list;
	}

	/**
	 * Convert map to bean
	 * @param source                Source map
	 * @param target                Target class
	 * @return                      Bean of that class
	 * @throws ConversionException on failure
	 */
	public static Object convertMapToBean(Map<?, ?> source, Class<?> target)  {
		Object bean = newInstance(target.getClass().getName());
		if (bean == null) {
			//try with just the target name as specified in Trac #352
			bean = newInstance(target.getName());
			if (bean == null) {
				//throw new ConversionException("Unable to create bean using empty constructor");
			}
		}
		try {
//			BeanUtils.populate(bean, source);
		} catch (Exception e) {
//			throw new ConversionException("Error populating bean", e);
		}
		return bean;
	}

	/**
	 * Convert bean to map
	 * @param source      Source bean
	 * @return            Converted map
	 */
	public static Map<?, ?> convertBeanToMap(Object source) {
//		return new BeanMap(source);
		return null;
	}

	/**
	 * Convert array to set, removing duplicates
	 * @param source      Source array
	 * @return            Set
	 */
	public static Set<?> convertArrayToSet(Object[] source) {
		Set<Object> set = new HashSet<Object>();
		for (Object element : source) {
			set.add(element);
		}
		return set;
	}

	/**
	 * Create new class instance
	 * @param className   Class name; may not be loaded by JVM yet
	 * @return            Instance of given class
	 */
	protected static Object newInstance(String className) {
		ClassLoader cl =  Thread.currentThread().getContextClassLoader();
		log.debug("Conversion utils classloader: {}", cl);
		Object instance = null;
		try {
			Class<?> clazz = cl.loadClass(className);
			instance = clazz.newInstance();
		} catch (Exception ex) {
			log.error("Error loading class: {}", className, ex);
		}
		return instance;
	}

}
