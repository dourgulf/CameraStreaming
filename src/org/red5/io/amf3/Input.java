package org.red5.io.amf3;

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

import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
//import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

//import org.apache.commons.beanutils.BeanUtils;
//import org.apache.commons.beanutils.BeanUtilsBean;
//import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.amf.AMF;
import org.red5.io.object.DataTypes;
import org.red5.io.object.Deserializer;
import org.red5.io.utils.ArrayUtils;
import org.red5.io.utils.ObjectMap;
import org.red5.io.utils.XMLUtils;
import org.red5.server.service.ConversionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * Input for Red5 data (AMF3) types
 *
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class Input extends org.red5.io.amf.Input implements org.red5.io.object.Input {

//	private static ConvertUtilsBean convertUtilsBean = BeanUtilsBean.getInstance().getConvertUtils();

	/**
	 * Holds informations about already deserialized classes.
	 */
	protected static class ClassReference {
		/** Name of the deserialized class. */
		protected String className;

		/** Type of the class. */
		protected int type;

		/** Names of the attributes of the class. */
		protected List<String> attributeNames;

		/** Create new informations about a class. 
		 * @param className class name
		 * @param type type
		 * @param attributeNames attributes
		 */
		public ClassReference(String className, int type, List<String> attributeNames) {
			this.className = className;
			this.type = type;
			this.attributeNames = attributeNames;
		}
	}

	/**
	 * Dummy class that is stored as reference for objects currently
	 * being deserialized that reference themselves. 
	 */
	protected static class PendingObject {

		static final class PendingProperty {
			Object obj;

			Class<?> klass;

			String name;

			PendingProperty(Object obj, Class<?> klass, String name) {
				this.obj = obj;
				this.klass = klass;
				this.name = name;
			}
		}

		private List<PendingProperty> properties;

		public void addPendingProperty(Object obj, Class<?> klass, String name) {
			if (properties == null) {
				properties = new ArrayList<PendingProperty>();
			}
			properties.add(new PendingProperty(obj, klass, name));
		}

		public void resolveProperties(Object result) {
			if (properties == null)
				// No pending properties
				return;

			for (PendingProperty prop : properties) {
				try {
					prop.klass.getField(prop.name).set(prop.obj, result);
				} catch (Exception e) {
					try {
//						BeanUtils.setProperty(prop.obj, prop.name, result);
					} catch (Exception ex) {
						log.error("Error mapping property: {} ({})", prop.name, result);
					}
				}
			}
			properties.clear();
		}
	}

	/**
	 * Class used to collect AMF3 references.
	 * In AMF3 references should be collected through the whole "body" (across several Input objects).
	 */
	public static class RefStorage {
		private List<ClassReference> classReferences = new ArrayList<ClassReference>();

		private List<String> stringReferences = new ArrayList<String>();

		private Map<Integer, Object> refMap = new HashMap<Integer, Object>();
	}

	/**
	 * Logger
	 */
	protected static Logger log = LoggerFactory.getLogger(Input.class);

	/**
	 * Set to a value above <tt>0</tt> to enforce AMF3 decoding mode.
	 */
	private int amf3_mode;

	/**
	 * List of string values found in the input stream.
	 */
	private List<String> stringReferences;

	/**
	 * Informations about already deserialized classes.
	 */
	private List<ClassReference> classReferences;

	/**
	 * Creates Input object for AMF3 from byte buffer
	 * 
	 * @param buf        Byte buffer
	 */
	public Input(IoBuffer buf) {
		super(buf);
		amf3_mode = 0;
		stringReferences = new ArrayList<String>();
		classReferences = new ArrayList<ClassReference>();
	}

	/**
	 * Creates Input object for AMF3 from byte buffer and initializes references
	 * from passed RefStorage
	 * @param buf buffer
	 * @param refStorage ref storage
	 */
	public Input(IoBuffer buf, RefStorage refStorage) {
		super(buf);
		this.stringReferences = refStorage.stringReferences;
		this.classReferences = refStorage.classReferences;
		this.refMap = refStorage.refMap;
		amf3_mode = 0;
	}

	/**
	 * Force using AMF3 everywhere
	 */
	public void enforceAMF3() {
		amf3_mode++;
	}

	/**
	 * Provide access to raw data.
	 * 
	 * @return IoBuffer
	 */
	protected IoBuffer getBuffer() {
		return buf;
	}

	/**
	 * Reads the data type
	 * 
	 * @return byte      Data type
	 */
	@Override
	public byte readDataType() {
		byte coreType = AMF3.TYPE_UNDEFINED;
		if (buf != null) {
			currentDataType = buf.get();
			log.debug("Current data type: {}", currentDataType);

			if (currentDataType == AMF.TYPE_AMF3_OBJECT) {
				currentDataType = buf.get();
			} else if (amf3_mode == 0) {
				// AMF0 object
				return readDataType(currentDataType);
			}

			log.debug("Current data type (after amf checks): {}", currentDataType);

			switch (currentDataType) {
				case AMF3.TYPE_UNDEFINED:
				case AMF3.TYPE_NULL:
					coreType = DataTypes.CORE_NULL;
					break;
				case AMF3.TYPE_INTEGER:
				case AMF3.TYPE_NUMBER:
					coreType = DataTypes.CORE_NUMBER;
					break;

				case AMF3.TYPE_BOOLEAN_TRUE:
				case AMF3.TYPE_BOOLEAN_FALSE:
					coreType = DataTypes.CORE_BOOLEAN;
					break;

				case AMF3.TYPE_STRING:
					coreType = DataTypes.CORE_STRING;
					break;
				// TODO check XML_SPECIAL
				case AMF3.TYPE_XML:
				case AMF3.TYPE_XML_DOCUMENT:
					coreType = DataTypes.CORE_XML;
					break;
				case AMF3.TYPE_OBJECT:
					coreType = DataTypes.CORE_OBJECT;
					break;

				case AMF3.TYPE_ARRAY:
					// should we map this to list or array?
					coreType = DataTypes.CORE_ARRAY;
					break;

				case AMF3.TYPE_DATE:
					coreType = DataTypes.CORE_DATE;
					break;

				case AMF3.TYPE_BYTEARRAY:
					coreType = DataTypes.CORE_BYTEARRAY;
					break;

				case AMF3.TYPE_VECTOR_INT:
					coreType = DataTypes.CORE_VECTOR_INT;
					break;
				case AMF3.TYPE_VECTOR_UINT:
					coreType = DataTypes.CORE_VECTOR_UINT;
					break;
				case AMF3.TYPE_VECTOR_NUMBER:
					coreType = DataTypes.CORE_VECTOR_NUMBER;
					break;
				case AMF3.TYPE_VECTOR_OBJECT:
					coreType = DataTypes.CORE_VECTOR_OBJECT;
					break;

				default:
					log.info("Unknown datatype: {}", currentDataType);
					// End of object, and anything else lets just skip
					coreType = DataTypes.CORE_SKIP;
					break;
			}
			log.debug("Core type: {}", coreType);
		} else {
			log.error("Why is buf null?");
		}
		return coreType;
	}

	// Basic

	/**
	 * Reads a null (value)
	 * 
	 * @return Object    null
	 */
	@Override
	public Object readNull(Type target) {
		return null;
	}

	/**
	 * Reads a boolean
	 * 
	 * @return boolean     Boolean value
	 */
	@Override
	public Boolean readBoolean(Type target) {
		return (currentDataType == AMF3.TYPE_BOOLEAN_TRUE) ? Boolean.TRUE : Boolean.FALSE;
	}

	/**
	 * Reads a Number
	 * 
	 * @return Number      Number
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Number readNumber(Type target) {
		Number v;

		if (currentDataType == AMF3.TYPE_NUMBER) {
			v = buf.getDouble();
		} else {
			// we are decoding an int
			v = readAMF3Integer();
		}

		if (target instanceof Class && Number.class.isAssignableFrom((Class<?>) target)) {
			Class cls = (Class) target;
			if (!cls.isAssignableFrom(v.getClass())) {
//				v = (Number) convertUtilsBean.convert(v.toString(), cls);
			}
		}

		return v;
	}

	/**
	 * Reads a string
	 * 
	 * @return String       String
	 */
	@Override
	public String readString(Type target) {
		int len = readAMF3Integer();
		log.debug("readString - length: {}", len);
		if (len == 1) {
			// Empty string
			return "";
		}
		if ((len & 1) == 0) {
			//if the refs are empty an IndexOutOfBoundsEx will be thrown
			if (stringReferences.isEmpty()) {
				log.debug("String reference list is empty");
			}
			// Reference
			return stringReferences.get(len >> 1);
		}
		len >>= 1;
		log.debug("readString - new length: {}", len);
		int limit = buf.limit();
		log.debug("readString - limit: {}", limit);
		final ByteBuffer strBuf = buf.buf();
		strBuf.limit(strBuf.position() + len);
		final String string = AMF3.CHARSET.decode(strBuf).toString();
		log.debug("String: {}", string);
		buf.limit(limit); // Reset the limit
		stringReferences.add(string);
		return string;
	}

	/**
	 * Reads a string of a set length. This does not use
	 * the string reference table.
	 * 
	 * @param length the length of the string
	 * @return String
	 */
	public String readString(int length) {
		log.debug("readString - length: {}", length);
		int limit = buf.limit();
		final ByteBuffer strBuf = buf.buf();
		strBuf.limit(strBuf.position() + length);
		final String string = AMF3.CHARSET.decode(strBuf).toString();
		log.debug("String: {}", string);
		buf.limit(limit);
		//check for null termination
		byte b = buf.get();
		if (b != 0) {
			buf.position(buf.position() - 1);
		}
		return string;
	}

	public String getString() {
		return readString(String.class);
	}

	/**
	 * Returns a date
	 * 
	 * @return Date        Date object
	 */
	@Override
	public Date readDate(Type target) {
		int ref = readAMF3Integer();
		if ((ref & 1) == 0) {
			// Reference to previously found date
			return (Date) getReference(ref >> 1);
		}

		long ms = (long) buf.getDouble();
		Date date = new Date(ms);
		storeReference(date);
		return date;
	}

	// Array

	/**
	 * Returns an array
	 * 
	 * @return int        Length of array
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object readArray(Deserializer deserializer, Type target) {
		int count = readAMF3Integer();
		log.debug("Count: {} and {} ref {}", new Object[] { count, (count & 1), (count >> 1) });
		if ((count & 1) == 0) {
			//Reference
			Object ref = getReference(count >> 1);
			if (ref != null) {
				return ref;
			}
		}

		count = (count >> 1);
		String key = readString(String.class);
		amf3_mode += 1;
		Object result;
		if (key.equals("")) {
			Class<?> nested = Object.class;
			Class<?> collection = Collection.class;
			Collection resultCollection;

			if (target instanceof ParameterizedType) {
				ParameterizedType t = (ParameterizedType) target;
				Type[] actualTypeArguments = t.getActualTypeArguments();
				if (actualTypeArguments.length == 1) {
					nested = (Class<?>) actualTypeArguments[0];
				}
				target = t.getRawType();
			}

			if (target instanceof Class) {
				collection = (Class) target;
			}

			if (collection.isArray()) {
				nested = ArrayUtils.getGenericType(collection.getComponentType());
				result = Array.newInstance(nested, count);
				storeReference(result);
				for (int i = 0; i < count; i++) {
					final Object value = deserializer.deserialize(this, nested);
					Array.set(result, i, value);
				}
			} else {
				if (SortedSet.class.isAssignableFrom(collection)) {
					resultCollection = new TreeSet();
				} else if (Set.class.isAssignableFrom(collection)) {
					resultCollection = new HashSet(count);
				} else {
					resultCollection = new ArrayList(count);
				}

				result = resultCollection;
				storeReference(result);

				for (int i = 0; i < count; i++) {
					final Object value = deserializer.deserialize(this, nested);
					resultCollection.add(value);
				}

			}
		} else {
			Class<?> k = Object.class;
			Class<?> v = Object.class;
			Class<?> collection = Collection.class;

			if (target instanceof ParameterizedType) {
				ParameterizedType t = (ParameterizedType) target;
				Type[] actualTypeArguments = t.getActualTypeArguments();
				if (actualTypeArguments.length == 2) {
					k = (Class<?>) actualTypeArguments[0];
					v = (Class<?>) actualTypeArguments[1];
				}
				target = t.getRawType();
			}

			if (target instanceof Class) {
				collection = (Class) target;
			}

			if (SortedMap.class.isAssignableFrom(collection)) {
				collection = TreeMap.class;
			} else {
				collection = HashMap.class;
			}

			Map resultMap;

			try {
				resultMap = (Map) collection.newInstance();
			} catch (Exception e) {
				resultMap = new HashMap(count);
			}

			// associative array
			storeReference(resultMap);
			while (!key.equals("")) {
				final Object value = deserializer.deserialize(this, v);
				resultMap.put(key, value);
				key = readString(k);
			}
			for (int i = 0; i < count; i++) {
				final Object value = deserializer.deserialize(this, v);
				resultMap.put(i, value);
			}
			result = resultMap;
		}
		amf3_mode -= 1;
		return result;
	}

	public Object readMap(Deserializer deserializer, Type target) {
		throw new RuntimeException("AMF3 doesn't support maps.");
	}

	// Object

	@SuppressWarnings({ "unchecked", "rawtypes", "serial" })
	public Object readObject(Deserializer deserializer, Type target) {
		int type = readAMF3Integer();
		log.debug("Type: {} and {} ref {}", new Object[] { type, (type & 1), (type >> 1) });
		if ((type & 1) == 0) {
			//Reference
			Object ref = getReference(type >> 1);
			if (ref != null) {
				return ref;
			}
			byte b = buf.get();
			if (b == 7) {
				log.debug("BEL: {}", b); //7			
			} else {
				log.debug("Non-BEL byte: {}", b);
				log.debug("Extra byte: {}", buf.get());
			}
		}

		type >>= 1;
		List<String> attributes = null;
		String className;
		Object result = null;
		boolean inlineClass = (type & 1) == 1;
		log.debug("Class is in-line? {}", inlineClass);
		if (!inlineClass) {
			ClassReference info = classReferences.get(type >> 1);
			className = info.className;
			attributes = info.attributeNames;
			type = info.type;
			if (attributes != null) {
				type |= attributes.size() << 2;
			}
		} else {
			type >>= 1;
			className = readString(String.class);
			log.debug("Type: {} classname: {}", type, className);
			//check for flex class alias since these wont be detected as externalizable
			if (classAliases.containsKey(className)) {
				//make sure type is externalizable
				type = 1;
			} else if (className.startsWith("flex")) {
				//set the attributes for messaging classes
				if (className.endsWith("CommandMessage")) {
					attributes = new LinkedList<String>() {
						{
							add("timestamp");
							add("headers");
							add("operation");
							add("body");
							add("correlationId");
							add("messageId");
							add("timeToLive");
							add("clientId");
							add("destination");
						}
					};
				} else {
					log.debug("Attributes for {} were not set", className);
				}
			}
		}
		amf3_mode += 1;
		Object instance = newInstance(className);
		Map<String, Object> properties = null;
		PendingObject pending = new PendingObject();
		int tempRefId = storeReference(pending);
		log.debug("Object type: {}", (type & 0x03));
		switch (type & 0x03) {
			case AMF3.TYPE_OBJECT_PROPERTY:
				log.debug("Detected: Object property type");
				// Load object properties into map
				int count = type >> 2;
				log.debug("Count: {}", count);
				if (attributes == null) {
					attributes = new ArrayList<String>(count);
					for (int i = 0; i < count; i++) {
						attributes.add(readString(String.class));
					}
					classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_PROPERTY, attributes));
				}
				properties = new ObjectMap<String, Object>();
				for (int i = 0; i < count; i++) {
					String name = attributes.get(i);
					properties.put(name, deserializer.deserialize(this, getPropertyType(instance, name)));
				}
				break;
			case AMF3.TYPE_OBJECT_EXTERNALIZABLE:
				log.debug("Detected: Externalizable type");
				// Use custom class to deserialize the object
				if ("".equals(className)) {
					throw new RuntimeException("Classname is required to load an Externalizable object");
				}
				log.debug("Externalizable class: {}", className);
				if (className.length() == 3) {
					//check for special DS class aliases
					className = classAliases.get(className);
				}
				result = newInstance(className);
				if (result == null) {
					throw new RuntimeException(String.format("Could not instantiate class: %s", className));
				}
				if (!(result instanceof IExternalizable)) {
					throw new RuntimeException(String.format("Class must implement the IExternalizable interface: %s",
							className));
				}
				classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_EXTERNALIZABLE, null));
				storeReference(tempRefId, result);
				((IExternalizable) result).readExternal(new DataInput(this, deserializer));
				break;
			case AMF3.TYPE_OBJECT_VALUE:
				log.debug("Detected: Object value type");
				// First, we should read typed (non-dynamic) properties ("sealed traits" according to AMF3 specification).
				// Property names are stored in the beginning, then values are stored.
				count = type >> 2;
				log.debug("Count: {}", count);
				if (attributes == null) {
					attributes = new ArrayList<String>(count);
					for (int i = 0; i < count; i++) {
						attributes.add(readString(String.class));
					}
					classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_VALUE, attributes));
				}
				//use the size of the attributes if we have no count
				if (count == 0 && attributes != null) {
					count = attributes.size();
					log.debug("Using class attribute size for property count: {}", count);
					//read the attributes from the stream and log if count doesnt match
					List<String> tmpAttributes = new ArrayList<String>(count);
					for (int i = 0; i < count; i++) {
						tmpAttributes.add(readString(String.class));
					}
					if (count != tmpAttributes.size()) {
						log.debug("Count and attributes length does not match!");
					}
					classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_VALUE, attributes));
				}

				properties = new ObjectMap<String, Object>();
				for (String key : attributes) {
					log.debug("Looking for property: {}", key);
					Object value = deserializer.deserialize(this, getPropertyType(instance, key));
					log.debug("Key: {} Value: {}", key, value);
					properties.put(key, value);
				}

				log.trace("Buffer - position: {} limit: {}", buf.position(), buf.limit());
				//no more items to read if we are at the end of the buffer
				if (buf.position() < buf.limit()) {
					// Now we should read dynamic properties which are stored as name-value pairs.
					// Dynamic properties are NOT remembered in 'classReferences'.
					String key = readString(String.class);
					while (!"".equals(key)) {
						Object value = deserializer.deserialize(this, getPropertyType(instance, key));
						properties.put(key, value);
						key = readString(String.class);
					}
				}
				break;
			default:
			case AMF3.TYPE_OBJECT_PROXY:
				log.debug("Detected: Object proxy type");
				if ("".equals(className)) {
					throw new RuntimeException("Classname is required to load an Externalizable object");
				}
				log.debug("Externalizable class: {}", className);
				result = newInstance(className);
				if (result == null) {
					throw new RuntimeException(String.format("Could not instantiate class: %s", className));
				}
				if (!(result instanceof IExternalizable)) {
					throw new RuntimeException(String.format("Class must implement the IExternalizable interface: %s",
							className));
				}
				classReferences.add(new ClassReference(className, AMF3.TYPE_OBJECT_PROXY, null));
				storeReference(tempRefId, result);
				((IExternalizable) result).readExternal(new DataInput(this, deserializer));
		}
		amf3_mode -= 1;

		if (result == null) {
			// Create result object based on classname
			if ("".equals(className)) {
				// "anonymous" object, load as Map
				// Resolve circular references
				for (Map.Entry<String, Object> entry : properties.entrySet()) {
					if (entry.getValue() == pending) {
						entry.setValue(properties);
					}
				}

				storeReference(tempRefId, properties);
				result = properties;
			} else if ("RecordSet".equals(className)) {
				// TODO: how are RecordSet objects encoded?
				throw new RuntimeException("Objects of type RecordSet not supported yet.");
			} else if ("RecordSetPage".equals(className)) {
				// TODO: how are RecordSetPage objects encoded?
				throw new RuntimeException("Objects of type RecordSetPage not supported yet.");
			} else {
				// Apply properties to object
				result = newInstance(className);
				if (result != null) {
					storeReference(tempRefId, result);
					Class resultClass = result.getClass();
					pending.resolveProperties(result);
					for (Map.Entry<String, Object> entry : properties.entrySet()) {
						// Resolve circular references
						final String key = entry.getKey();
						Object value = entry.getValue();
						if (value == pending) {
							value = result;
						}

						if (value instanceof PendingObject) {
							// Defer setting of value until real object is created
							((PendingObject) value).addPendingProperty(result, resultClass, key);
							continue;
						}

						if (value != null) {
							try {
								final Field field = resultClass.getField(key);
								final Class fieldType = field.getType();
								if (!fieldType.isAssignableFrom(value.getClass())) {
									value = ConversionUtils.convert(value, fieldType);
								} else if (value instanceof Enum) {
									value = Enum.valueOf(fieldType, value.toString());
								}
								field.set(result, value);
							} catch (Exception e) {
//								try {
////									BeanUtils.setProperty(result, key, value);
////								} catch (IllegalAccessException ex) {
////									log.warn("Error mapping key: {} value: {}", key, value);
//								} catch (InvocationTargetException ex) {
//									log.warn("Error mapping key: {} value: {}", key, value);
//								}
							}
						} else {
							log.debug("Skipping null property: {}", key);
						}
					}
				} // else fall through
			}
		}
		return result;
	}

	public ByteArray readByteArray(Type target) {
		int type = readAMF3Integer();
		if ((type & 1) == 0) {
			// Reference
			return (ByteArray) getReference(type >> 1);
		}

		type >>= 1;
		ByteArray result = new ByteArray(buf, type);
		storeReference(result);
		return result;
	}

	@SuppressWarnings("unchecked")
	public List<Integer> readVectorInt() {
		int type = readAMF3Integer();
		if ((type & 1) == 0) {
			return (List<Integer>) getReference(type >> 1);
		}

		int len = type >> 1;
		List<Integer> array = new ArrayList<Integer>(len);

		storeReference(array);

		@SuppressWarnings("unused")
		int ref2 = readAMF3Integer();
		for (int j = 0; j < len; ++j) {
			array.add(buf.getInt());
		}

		return array;
	}

	@SuppressWarnings("unchecked")
	public List<Long> readVectorUInt() {
		int type = readAMF3Integer();
		if ((type & 1) == 0) {
			return (List<Long>) getReference(type >> 1);
		}

		int len = type >> 1;
		List<Long> array = new ArrayList<Long>(len);

		storeReference(array);

		@SuppressWarnings("unused")
		int ref2 = readAMF3Integer();
		for (int j = 0; j < len; ++j) {
			long value = (buf.get() & 0xff) << 24L;
			value += (buf.get() & 0xff) << 16L;
			value += (buf.get() & 0xff) << 8L;
			value += (buf.get() & 0xff);
			array.add(value);
		}

		return array;
	}

	@SuppressWarnings("unchecked")
	public List<Double> readVectorNumber() {
		int type = readAMF3Integer();
		if ((type & 1) == 0) {
			return (List<Double>) getReference(type >> 1);
		}

		int len = type >> 1;
		List<Double> array = new ArrayList<Double>(len);

		storeReference(array);

		@SuppressWarnings("unused")
		int ref2 = readAMF3Integer();
		for (int j = 0; j < len; ++j) {
			array.add(buf.getDouble());
		}

		return array;
	}

	@SuppressWarnings("unchecked")
	public List<Object> readVectorObject() {
		int type = readAMF3Integer();
		if ((type & 1) == 0) {
			return (List<Object>) getReference(type >> 1);
		}

		int len = type >> 1;
		List<Object> array = new ArrayList<Object>(len);

		storeReference(array);
		
		Deserializer deserializer = new Deserializer();
		Object object;

		for (int j = 0; j < len; ++j) {
			object = readObject(deserializer, null);
			array.add(object);
		}

		return array;
	}	
	
	// Others

	/**
	 * Reads Custom
	 * 
	 * @return Object     Custom type object
	 */
	@Override
	public Object readCustom(Type target) {
		// Return null for now
		return null;
	}

	/** {@inheritDoc} */
	public Object readReference(Type target) {
		throw new RuntimeException("AMF3 doesn't support direct references.");
	}

	/**
	 * Resets map
	 */
	@Override
	public void reset() {
		super.reset();
		stringReferences.clear();
	}

	/**
	 * Parser of AMF3 "compressed" integer data type
	 * 
	 * @return a converted integer value
	 * @see <a href="http://osflash.org/amf3/parsing_integers">parsing AMF3
	 *      integers (external)</a>
	 */
	private int readAMF3Integer() {
		int n = 0;
		int b = buf.get();
		int result = 0;

		while ((b & 0x80) != 0 && n < 3) {
			result <<= 7;
			result |= (b & 0x7f);
			b = buf.get();
			n++;
		}
		if (n < 3) {
			result <<= 7;
			result |= b;
		} else {
			/* Use all 8 bits from the 4th byte */
			result <<= 8;
			result |= b & 0x0ff;

			/* Check if the integer should be negative */
			if ((result & 0x10000000) != 0) {
				/* and extend the sign bit */
				result |= 0xe0000000;
			}
		}

		return result;
	}
	
	//Read UInt29 style
	@SuppressWarnings("unused")
	private int readAMF3IntegerNew() {
		int b = buf.get() & 0xFF;
		if (b < 128) {
			return b;
		}
		int value = (b & 0x7F) << 7;
		b = buf.get() & 0xFF;
		if (b < 128) {
			return (value | b);
		}
		value = (value | b & 0x7F) << 7;
		b = buf.get() & 0xFF;
		if (b < 128) {
			return (value | b);
		}
		value = (value | b & 0x7F) << 8;
		b = buf.get() & 0xFF;
		return (value | b);
	}	

	/** {@inheritDoc} */
	public Document readXML(Type target) {
		int len = readAMF3Integer();
		if (len == 1) {
			// Empty string, should not happen
			return null;
		}
		if ((len & 1) == 0) {
			// Reference
			return (Document) getReference(len >> 1);
		}
		len >>= 1;
		int limit = buf.limit();
		final ByteBuffer strBuf = buf.buf();
		strBuf.limit(strBuf.position() + len);
		final String xmlString = AMF3.CHARSET.decode(strBuf).toString();
		buf.limit(limit); // Reset the limit
		Document doc = null;
		try {
			doc = XMLUtils.stringToDoc(xmlString);
		} catch (IOException ioex) {
			log.error("IOException converting xml to dom", ioex);
		}
		storeReference(doc);
		return doc;
	}

}
