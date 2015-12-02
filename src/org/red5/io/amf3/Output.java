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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
//import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
//import java.util.Set;

//import net.sf.ehcache.Element;

//import org.apache.commons.beanutils.BeanMap;
import org.apache.mina.core.buffer.IoBuffer;
import org.red5.annotations.Anonymous;
//import org.red5.compatibility.flex.ObjectProxy;
import org.red5.io.amf.AMF;
import org.red5.io.object.RecordSet;
import org.red5.io.object.Serializer;
import org.red5.io.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * AMF3 output writer
 *
 * @see  org.red5.io.amf3.AMF3
 * @see  org.red5.io.amf3.Input
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 * @author Harald Radi (harald.radi@nme.at)
 */
public class Output extends org.red5.io.amf.Output implements org.red5.io.object.Output {

	protected static Logger log = LoggerFactory.getLogger(Output.class);

	/**
	 * Set to a value above <tt>0</tt> to disable writing of the AMF3 object tag.
	 */
	private int amf3_mode;

	/**
	 * List of strings already written.
	 * */
	private Map<String, Integer> stringReferences;

	/**
	 * Constructor of AMF3 output.
	 *
	 * @param buf instance of IoBuffer
	 * @see IoBuffer
	 */
	public Output(IoBuffer buf) {
		super(buf);
		amf3_mode = 0;
		stringReferences = new HashMap<String, Integer>();
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

	/** {@inheritDoc} */
	@Override
	public boolean supportsDataType(byte type) {
		return true;
	}

	// Basic Data Types

	protected void writeAMF3() {
		if (amf3_mode == 0) {
			buf.put(AMF.TYPE_AMF3_OBJECT);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void writeBoolean(Boolean bol) {
		writeAMF3();
		buf.put(bol ? AMF3.TYPE_BOOLEAN_TRUE : AMF3.TYPE_BOOLEAN_FALSE);
	}

	/** {@inheritDoc} */
	@Override
	public void writeNull() {
		writeAMF3();
		buf.put(AMF3.TYPE_NULL);
	}

	protected void putInteger(long value) {
		if ((value >= -268435456) && (value <= 268435455)) {
			value &= 536870911;
		}
		if (value < 128) {
			buf.put((byte) value);
		} else if (value < 16384) {
			buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value < 2097152) {
			buf.put((byte) (((value >> 14) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 7) & 0x7F) | 0x80));
			buf.put((byte) (value & 0x7F));
		} else if (value < 1073741824) {
			buf.put((byte) (((value >> 22) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 15) & 0x7F) | 0x80));
			buf.put((byte) (((value >> 8) & 0x7F) | 0x80));
			buf.put((byte) (value & 0xFF));
		} else {
			log.error("Integer out of range: {}", value);
		}
	}

	protected static byte[] encodeString(String string) {
		//Element element = getStringCache().get(string);
		//byte[] encoded = (element == null ? null : (byte[]) element.getObjectValue());
		byte[] encoded = null;
		if (encoded == null) {
			ByteBuffer buf = AMF3.CHARSET.encode(string);
			encoded = new byte[buf.limit()];
			buf.get(encoded);
			//getStringCache().put(new Element(string, encoded));
		}
		return encoded;
	}

	protected void putString(String str, byte[] encoded) {
		final int len = encoded.length;
		Integer pos = stringReferences.get(str);
		if (pos != null) {
			// Reference to existing string
			putInteger(pos << 1);
			return;
		}

		putInteger(len << 1 | 1);
		buf.put(encoded);
		stringReferences.put(str, stringReferences.size());
	}

	/** {@inheritDoc} */
	@Override
	public void putString(String string) {
		if ("".equals(string)) {
			// Empty string;
			putInteger(1);
			return;
		}

		final byte[] encoded = encodeString(string);
		putString(string, encoded);
	}

	/** {@inheritDoc} */
	@Override
	public void writeNumber(Number num) {
		writeAMF3();
		if (num.longValue() < AMF3.MIN_INTEGER_VALUE || num.longValue() > AMF3.MAX_INTEGER_VALUE) {
			// Out of range for integer encoding
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		} else if (num instanceof Long || num instanceof Integer || num instanceof Short || num instanceof Byte) {
			buf.put(AMF3.TYPE_INTEGER);
			putInteger(num.longValue());
		} else {
			buf.put(AMF3.TYPE_NUMBER);
			buf.putDouble(num.doubleValue());
		}
	}

	/** {@inheritDoc} */
	@Override
	public void writeString(String string) {
		writeAMF3();
		buf.put(AMF3.TYPE_STRING);
		if ("".equals(string)) {
			putInteger(1);
		} else {
			final byte[] encoded = encodeString(string);
			putString(string, encoded);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void writeDate(Date date) {
		writeAMF3();
		buf.put(AMF3.TYPE_DATE);
		if (hasReference(date)) {
			putInteger(getReferenceId(date) << 1);
			return;
		}

		storeReference(date);
		putInteger(1);
		buf.putDouble(date.getTime());
	}

	/** {@inheritDoc} */
	public void writeArray(Collection<?> array, Serializer serializer) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}

		storeReference(array);
		amf3_mode += 1;
		int count = array.size();
		putInteger(count << 1 | 1);
		putString("");
		for (Object item : array) {
			serializer.serialize(this, item);
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	public void writeArray(Object[] array, Serializer serializer) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}

		storeReference(array);
		amf3_mode += 1;
		int count = array.length;
		putInteger(count << 1 | 1);
		putString("");
		for (Object item : array) {
			serializer.serialize(this, item);
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	public void writeArray(Object array, Serializer serializer) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}

		storeReference(array);
		amf3_mode += 1;
		int count = Array.getLength(array);
		putInteger(count << 1 | 1);
		putString("");
		for (int i = 0; i < count; i++) {
			serializer.serialize(this, Array.get(array, i));
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	public void writeMap(Map<Object, Object> map, Serializer serializer) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(map)) {
			putInteger(getReferenceId(map) << 1);
			return;
		}

		storeReference(map);
		// Search number of starting integer keys
		int count = 0;
		for (int i = 0; i < map.size(); i++) {
			try {
				if (!map.containsKey(i))
					break;
			} catch (ClassCastException err) {
				// Map has non-number keys.
				break;
			}
			count++;
		}

		amf3_mode += 1;
		if (count == map.size()) {
			// All integer keys starting from zero: serialize as regular array
			putInteger(count << 1 | 1);
			putString("");
			for (int i = 0; i < count; i++) {
				serializer.serialize(this, map.get(i));
			}
			amf3_mode -= 1;
			return;
		}

		putInteger(count << 1 | 1);
		// Serialize key-value pairs first
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			Object key = entry.getKey();
			if ((key instanceof Number) && !(key instanceof Float) && !(key instanceof Double)
					&& ((Number) key).longValue() >= 0 && ((Number) key).longValue() < count) {
				// Entry will be serialized later
				continue;
			}
			putString(key.toString());
			serializer.serialize(this, entry.getValue());
		}
		putString("");
		// Now serialize integer keys starting from zero
		for (int i = 0; i < count; i++) {
			serializer.serialize(this, map.get(i));
		}
		amf3_mode -= 1;
	}

	/** {@inheritDoc} */
	public void writeMap(Collection<?> array, Serializer serializer) {
		writeAMF3();
		buf.put(AMF3.TYPE_ARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}

		storeReference(array);
		// TODO: we could optimize this by storing the first integer
		//       keys after the key-value pairs
		amf3_mode += 1;
		putInteger(1);
		int idx = 0;
		for (Object item : array) {
			if (item != null) {
				putString(String.valueOf(idx));
				serializer.serialize(this, item);
			}
			idx++;
		}
		amf3_mode -= 1;
		putString("");
	}

	/** {@inheritDoc} */
	@Override
	protected void writeArbitraryObject(Object object, Serializer serializer) {
		Class<?> objectClass = object.getClass();

		// If we need to serialize class information...
		if (!objectClass.isAnnotationPresent(Anonymous.class)) {
			putString(serializer.getClassName(objectClass));
		} else {
			putString("");
		}

		// Store key/value pairs
		amf3_mode += 1;
		// Iterate thru fields of an object to build "name-value" map from it
		for (Field field : objectClass.getFields()) {
			String fieldName = field.getName();

			log.debug("Field: {} class: {}", field, objectClass);
			// Check if the Field corresponding to the getter/setter pair is transient
			if (!serializeField(serializer, objectClass, fieldName, field, null)) {
				continue;
			}

			Object value;
			try {
				// Get field value
				value = field.get(object);
			} catch (IllegalAccessException err) {
				// Swallow on private and protected properties access exception
				continue;
			}

			// Write out prop name
			putString(fieldName);
			// Write out
			serializer.serialize(this, field, null, object, value);
		}
		amf3_mode -= 1;
		// Write out end of object marker
		putString("");
	}

	/** {@inheritDoc} */
//	@SuppressWarnings({ "rawtypes" })
	public void writeObject(Object object, Serializer serializer) {
/*
		writeAMF3();
		buf.put(AMF3.TYPE_OBJECT);
		if (hasReference(object)) {
			putInteger(getReferenceId(object) << 1);
			return;
		}

		storeReference(object);

		if (object instanceof IExternalizable) {
			// The object knows how to serialize itself.
			int type = 1 << 1 | 1;
			if (object instanceof ObjectProxy) {
				type |= AMF3.TYPE_OBJECT_PROXY << 2;
			} else {
				type |= AMF3.TYPE_OBJECT_EXTERNALIZABLE << 2;
			}
			putInteger(type);
			putString(serializer.getClassName(object.getClass()));
			amf3_mode += 1;
			((IExternalizable) object).writeExternal(new DataOutput(this, serializer));
			amf3_mode -= 1;
			return;
		}

		// We have an inline class that is not a reference.
		// We store the properties using key/value pairs
		int type = AMF3.TYPE_OBJECT_VALUE << 2 | 1 << 1 | 1;
		putInteger(type);
*/
		// Create new map out of bean properties
//		BeanMap beanMap = new BeanMap(object);
		// Set of bean attributes
//		Set set = beanMap.keySet();
//		if ((set.size() == 0) || (set.size() == 1 && beanMap.containsKey("class"))) {
//			// BeanMap is empty or can only access "class" attribute, skip it
//			writeArbitraryObject(object, serializer);
//			return;
//		}

		// Write out either start of object marker for class name or "empty" start of object marker
		Class<?> objectClass = object.getClass();
		if (!objectClass.isAnnotationPresent(Anonymous.class)) {
			// classname
			putString(serializer.getClassName(object.getClass()));
		} else {
			putString("");
		}

		// Store key/value pairs
//		amf3_mode += 1;
//		for (Object key : set) {
//			String fieldName = key.toString();
//			log.debug("Field name: {} class: {}", fieldName, objectClass);
//
//			Field field = getField(objectClass, fieldName);
//			Method getter = getGetter(objectClass, beanMap, fieldName);
//
//			// Check if the Field corresponding to the getter/setter pair is transient
//			if (!serializeField(serializer, objectClass, fieldName, field, getter)) {
//				continue;
//			}
//
//			putString(fieldName);
//			serializer.serialize(this, field, getter, object, beanMap.get(key));
//		}
		amf3_mode -= 1;

		// End of object marker
		putString("");
	}

	/** {@inheritDoc} */
	@Override
	public void writeObject(Map<Object, Object> map, Serializer serializer) {
		writeAMF3();
		buf.put(AMF3.TYPE_OBJECT);
		if (hasReference(map)) {
			putInteger(getReferenceId(map) << 1);
			return;
		}

		storeReference(map);
		// We have an inline class that is not a reference.
		// We store the properties using key/value pairs
		int type = AMF3.TYPE_OBJECT_VALUE << 2 | 1 << 1 | 1;
		putInteger(type);

		// No classname
		putString("");

		// Store key/value pairs
		amf3_mode += 1;
		for (Map.Entry<Object, Object> entry : map.entrySet()) {
			putString(entry.getKey().toString());
			serializer.serialize(this, entry.getValue());
		}
		amf3_mode -= 1;

		// End of object marker
		putString("");
	}

	/** {@inheritDoc} */
	@Override
	public void writeRecordSet(RecordSet recordset, Serializer serializer) {
		writeString("Not implemented.");
	}

	/** {@inheritDoc} */
	@Override
	public void writeXML(Document xml) {
		writeAMF3();
		buf.put(AMF3.TYPE_XML);
		if (hasReference(xml)) {
			putInteger(getReferenceId(xml) << 1);
			return;
		}
		final byte[] encoded = encodeString(XMLUtils.docToString(xml));
		putInteger(encoded.length << 1 | 1);
		buf.put(encoded);
		storeReference(xml);
	}

	/** {@inheritDoc} */
	public void writeByteArray(ByteArray array) {
		writeAMF3();
		buf.put(AMF3.TYPE_BYTEARRAY);
		if (hasReference(array)) {
			putInteger(getReferenceId(array) << 1);
			return;
		}

		storeReference(array);
		IoBuffer data = array.getData();
		putInteger(data.limit() << 1 | 1);
		byte[] tmp = new byte[data.limit()];
		int old = data.position();
		try {
			data.position(0);
			data.get(tmp);
			buf.put(tmp);
		} finally {
			data.position(old);
		}
	}

}