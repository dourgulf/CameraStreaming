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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.lang.reflect.Type;

import org.red5.io.amf3.ByteArray;
import org.w3c.dom.Document;

/**
 * Interface for Input which defines the contract methods which are
 * to be implemented. Input object provides
 * ways to read primitives, complex object and object references from byte buffer.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Luke Hubbard, Codegent Ltd (luke@codegent.com)
 */
public interface Input {
	/**
	 * Read type of data
	 * @return         Type of data as byte
	 */
	byte readDataType();

	/**
	 * Read a string without the string type header.
	 * 
	 * @return         String
	 */
	String getString();

	/**
	 * Read Null data type
	 * @param target target type
	 * @return         Null datatype (AS)
	 */
	Object readNull(Type target);

	/**
	 * Read Boolean value
	 * @param target target type
	 * @return         Boolean
	 */
	Boolean readBoolean(Type target);

	/**
	 * Read Number object
	 * @param target target type
	 * @return         Number
	 */
	Number readNumber(Type target);

	/**
	 * Read String object
	 * @param target target type 
	 * @return         String
	 */
	String readString(Type target);

	/**
	 * Read date object
	 * @param target target type 
	 * @return         Date
	 */
	Date readDate(Type target);

	/**
	 * Read an array. This can result in a List or Map being
	 * deserialized depending on the array type found.
	 * 
	 * @param deserializer deserializer
	 * @param target target type
	 * 
	 * @return		   array
	 */
	Object readArray(Deserializer deserializer, Type target);

	/**
	 * Read a map containing key - value pairs. This can result
	 * in a List or Map being deserialized depending on the
	 * map type found.
	 * @param deserializer deserializer
	 * @param target target type
	 * 
	 * @return		   Map
	 */
	Object readMap(Deserializer deserializer, Type target);

	/**
	 * Read an object.
	 * 
	 * @param deserializer deserializer
	 * @param target target type
	 * 
	 * @return		   object
	 */
	Object readObject(Deserializer deserializer, Type target);

	/**
	 * Read XML document
	 * @param target target type
	 * @return       XML DOM document
	 */
	Document readXML(Type target);

	/**
	 * Read custom object
	 * @param target target type
	 * @return          Custom object
	 */
	Object readCustom(Type target);

	/**
	 * Read ByteArray object.
	 * 
	 * @param target target type
	 * @return		ByteArray object
	 */
	ByteArray readByteArray(Type target);

	/**
	 * Read reference to Complex Data Type. Objects that are collaborators (properties) of other
	 * objects must be stored as references in map of id-reference pairs.
	 * @param target target type
	 * @return object
	 */
	Object readReference(Type target);

	/**
	 * Clears all references
	 */
	void clearReferences();

	/**
	 * Read key - value pairs. This is required for the RecordSet
	 * deserializer.
	 * @param deserializer deserializer
	 * @return key-value pairs
	 */
	Map<String, Object> readKeyValues(Deserializer deserializer);

	/**
	 * Read Vector<int> object.
	 * 
	 * @return List<Integer>
	 */
	List<Integer> readVectorInt();

	/**
	 * Read Vector<uint> object.
	 * 
	 * @return List<Long>
	 */
	List<Long> readVectorUInt();

	/**
	 * Read Vector<Number> object.
	 * 
	 * @return List<Double>
	 */
	List<Double> readVectorNumber();

	/**
	 * Read Vector<Object> object.
	 * 
	 * @return List<Object>
	 */
	List<Object> readVectorObject();

}
