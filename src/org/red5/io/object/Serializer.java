package org.red5.io.object;

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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

//import org.apache.commons.beanutils.BeanMap;
import org.red5.annotations.DontSerialize;
import org.red5.annotations.RemoteClass;
import org.red5.io.amf3.ByteArray;
import org.red5.io.amf3.IExternalizable;
import org.red5.io.utils.ObjectMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 * The Serializer class writes data output and handles the data according to the
 * core data types
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 * @author Harald Radi (harald.radi@nme.at)
 */
public class Serializer {

	/**
	 * Logger
	 */
	protected static Logger log = LoggerFactory.getLogger(Serializer.class);

	/**
	 * Serializes output to a core data type object
	 * 
	 * @param out Output writer
	 * @param any Object to serialize
	 */
	public void serialize(Output out, Object any) {
		serialize(out, null, null, null, any);
	}

	/**
	 * Serializes output to a core data type object
	 * 
	 * @param out Output writer
	 * @param field The field to serialize
	 * @param getter The getter method if not a field
	 * @param object Parent object
	 * @param value Object to serialize
	 */
	public void serialize(Output out, Field field, Method getter, Object object, Object value) {
//		log.trace("serialize");
		if (value instanceof IExternalizable) {
			// Make sure all IExternalizable objects are serialized as objects
			out.writeObject(value, this);
		} else if (value instanceof ByteArray) {
			// Write ByteArray objects directly
			out.writeByteArray((ByteArray) value);
		} else {
			if (writeBasic(out, value)) {
//				log.trace("Wrote as basic");
			} else if (!writeComplex(out, value)) {
				log.trace("Unable to serialize: {}", value);
			}
		}
	}

	/**
	 * Writes a primitive out as an object
	 * 
	 * @param out
	 *            Output writer
	 * @param basic
	 *            Primitive
	 * @return boolean true if object was successfully serialized, false
	 *         otherwise
	 */
	@SuppressWarnings("rawtypes")
	protected boolean writeBasic(Output out, Object basic) {
		if (basic == null) {
			out.writeNull();
		} else if (basic instanceof Boolean) {
			out.writeBoolean((Boolean) basic);
		} else if (basic instanceof Number) {
			out.writeNumber((Number) basic);
		} else if (basic instanceof String) {
			out.writeString((String) basic);
		} else if (basic instanceof Enum) {
			out.writeString(((Enum) basic).name());
		} else if (basic instanceof Date) {
			out.writeDate((Date) basic);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a complex type object out
	 * 
	 * @param out Output writer
	 * @param complex Complex datatype object
	 * @return boolean true if object was successfully serialized, false
	 *         otherwise
	 */
	public boolean writeComplex(Output out, Object complex) {
//		log.trace("writeComplex");
		if (writeListType(out, complex)) {
			return true;
		} else if (writeArrayType(out, complex)) {
			return true;
		} else if (writeXMLType(out, complex)) {
			return true;
		} else if (writeCustomType(out, complex)) {
			return true;
		} else if (writeObjectType(out, complex)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Writes Lists out as a data type
	 * 
	 * @param out
	 *            Output write
	 * @param listType
	 *            List type
	 * @return boolean true if object was successfully serialized, false
	 *         otherwise
	 */
	protected boolean writeListType(Output out, Object listType) {
//		log.trace("writeListType");
		if (listType instanceof List<?>) {
			writeList(out, (List<?>) listType);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a List out as an Object
	 * 
	 * @param out
	 *            Output writer
	 * @param list
	 *            List to write as Object
	 */
	protected void writeList(Output out, List<?> list) {
		if (!list.isEmpty()) {
    		int size = list.size();
    		// if its a small list, write it as an array
    		if (size < 100) {
    			out.writeArray(list, this);
    			return;
    		}
    		// else we should check for lots of null values,
    		// if there are over 80% then its probably best to do it as a map
    		int nullCount = 0;
    		for (int i = 0; i < size; i++) {
    			if (list.get(i) == null) {
    				nullCount++;
    			}
    		}
    		if (nullCount > (size * 0.8)) {
    			out.writeMap(list, this);
    		} else {
    			out.writeArray(list, this);
    		}
		} else {
			out.writeArray(new Object[]{}, this);
		}
	}

	/**
	 * Writes array (or collection) out as output Arrays, Collections, etc
	 * 
	 * @param out
	 *            Output object
	 * @param arrType
	 *            Array or collection type
	 * @return <code>true</code> if the object has been written, otherwise
	 *         <code>false</code>
	 */
	@SuppressWarnings("all")
	protected boolean writeArrayType(Output out, Object arrType) {
//		log.trace("writeArrayType");
		if (arrType instanceof Collection) {
			out.writeArray((Collection<Object>) arrType, this);
		} else if (arrType instanceof Iterator) {
			writeIterator(out, (Iterator<Object>) arrType);
		} else if (arrType.getClass().isArray() && arrType.getClass().getComponentType().isPrimitive()) {
			out.writeArray(arrType, this);
		} else if (arrType instanceof Object[]) {
			out.writeArray((Object[]) arrType, this);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes an iterator out to the output
	 * 
	 * @param out
	 *            Output writer
	 * @param it
	 *            Iterator to write
	 */
	protected void writeIterator(Output out, Iterator<Object> it) {
//		log.trace("writeIterator");
		// Create LinkedList of collection we iterate thru and write it out
		// later
		LinkedList<Object> list = new LinkedList<Object>();
		while (it.hasNext()) {
			list.addLast(it.next());
		}
		// Write out collection
		out.writeArray(list, this);
	}

	/**
	 * Writes an xml type out to the output
	 * 
	 * @param out
	 *            Output writer
	 * @param xml
	 *            XML
	 * @return boolean <code>true</code> if object was successfully written,
	 *         <code>false</code> otherwise
	 */
	protected boolean writeXMLType(Output out, Object xml) {
//		log.trace("writeXMLType");
		// If it's a Document write it as Document
		if (xml instanceof Document) {
			writeDocument(out, (Document) xml);
		} else {
			return false;
		}
		return true;
	}

	/**
	 * Writes a document to the output
	 * 
	 * @param out
	 *            Output writer
	 * @param doc
	 *            Document to write
	 */
	protected void writeDocument(Output out, Document doc) {
		out.writeXML(doc);
	}

	/**
	 * Write typed object to the output
	 * 
	 * @param out
	 *            Output writer
	 * @param obj
	 *            Object type to write
	 * @return <code>true</code> if the object has been written, otherwise
	 *         <code>false</code>
	 */
	@SuppressWarnings("all")
	protected boolean writeObjectType(Output out, Object obj) {
//		if (obj instanceof ObjectMap || obj instanceof BeanMap) {
		if (obj instanceof ObjectMap ) {
			out.writeObject((Map) obj, this);
		} else if (obj instanceof Map) {
			out.writeMap((Map) obj, this);
		} else if (obj instanceof RecordSet) {
			out.writeRecordSet((RecordSet) obj, this);
		} else {
			out.writeObject(obj, this);
		}
		return true;
	}

	// Extension points
	/**
	 * Pre processes an object TODO // must be implemented
	 * 
	 * @return Prerocessed object
	 * @param any Object to preprocess
	 */
	public Object preProcessExtension(Object any) {
		// Does nothing right now but will later
		return any;
	}

	/**
	 * Writes a custom data to the output
	 * 
	 * @param out Output writer
	 * @param obj Custom data
	 * @return <code>true</code> if the object has been written, otherwise
	 *         <code>false</code>
	 */
	protected boolean writeCustomType(Output out, Object obj) {
		if (out.isCustom(obj)) {
			// Write custom data
			out.writeCustom(obj);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks whether the field should be serialized or not
	 * 
	 * @param keyName key name
	 * @param field The field to be serialized
	 * @param getter Getter method for field
	 * @return <code>true</code> if the field should be serialized, otherwise
	 *         <code>false</code>
	 */
	public boolean serializeField(String keyName, Field field, Method getter) {
//		log.trace("serializeField - keyName: {} field: {} method: {}", new Object[] { keyName, field, getter });
		if ("class".equals(keyName)) {
			return false;
		}
		if ((field != null && field.isAnnotationPresent(DontSerialize.class)) || (getter != null && getter.isAnnotationPresent(DontSerialize.class))) {
//			log.trace("Skipping {} because its marked with @DontSerialize", keyName);
			return false;
		}
//		log.trace("Serialize field: {}", field);
		return true;
	}

	/**
	 * Handles classes by name, also provides "shortened" class aliases where appropriate.
	 * 
	 * @param objectClass
	 * @return class name for given object
	 */
	public String getClassName(Class<?> objectClass) {
		RemoteClass annotation = objectClass.getAnnotation(RemoteClass.class);
		if (annotation != null) {
			return annotation.alias();
		}
		String className = objectClass.getName();
		if (className.startsWith("org.red5.compatibility.")) {
			// Strip compatibility prefix from classname
			className = className.substring(23);
			if ("flex.messaging.messages.AsyncMessageExt".equals(className)) {
				className = "DSA";
			} else if ("flex.messaging.messages.CommandMessageExt".equals(className)) {
				className = "DSC";
			} else if ("flex.messaging.messages.AcknowledgeMessageExt".equals(className)) {
				className = "DSK";
			}
		}

		return className;
	}
}