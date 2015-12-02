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
import java.nio.ByteOrder;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

import org.apache.mina.core.buffer.IoBuffer;
import org.red5.io.object.Deserializer;
import org.red5.io.object.Serializer;

/**
 * Red5 version of the Flex ByteArray class.
 * 
 * @author The Red5 Project (red5@osflash.org)
 * @author Joachim Bauch (jojo@struktur.de)
 */
public class ByteArray implements IDataInput, IDataOutput {

	/** Internal storage for array contents. */
	protected IoBuffer data;
	
	/** Object used to read from array. */
	protected IDataInput dataInput;
	
	/** Object used to write to array. */
	protected IDataOutput dataOutput;
	
	/**
	 * Internal constructor used to create ByteArray during deserialization.
	 * 
	 * @param buffer
	 * @param length
	 */
	protected ByteArray(IoBuffer buffer, int length) {
		data = IoBuffer.allocate(length);
		data.setAutoExpand(true);
		byte[] tmp = new byte[length];
		buffer.get(tmp);
		data.put(tmp);
		data.flip();
		prepareIO();
	}
	
	/**
	 * Public constructor. Creates new empty ByteArray.
	 */
	public ByteArray() {
		data = IoBuffer.allocate(0);
		data.setAutoExpand(true);
		prepareIO();
	}

	/**
	 * Create internal objects used for reading and writing.
	 */
	protected void prepareIO() {
		// we assume that everything in ByteArray is in AMF3
		Input input = new Input(data);
		input.enforceAMF3();
		dataInput = new DataInput(input, new Deserializer());

		Output output = new Output(data);
		output.enforceAMF3();
		dataOutput = new DataOutput(output, new Serializer());
	}
	
	/**
	 * Get internal data.
	 * 
	 * @return byte buffer
	 */
	protected IoBuffer getData() {
		return data;
	}
	
	/**
	 * Get the current position in the data.
	 * 
	 * @return current position
	 */
	public int position() {
		return data.position();
	}
	
	/**
	 * Set the current position in the data.
	 * 
	 * @param position position to set
	 */
	public void position(int position) {
		data.position(position);
	}
	
	/**
	 * Return number of bytes available for reading.
	 * 
	 * @return bytes available
	 */
	public int bytesAvailable() {
		return length() - position();
	}
	
	/**
	 * Return total number of bytes in array.
	 * 
	 * @return number of bytes in array
	 */
	public int length() {
		return data.limit();
	}
	
	/**
	 * Return string representation of the array's contents.
	 * 
	 * @return string representaiton of array's contents.
	 */
	public String toString() {
		int old = data.position();
		try {
			data.position(0);
			return data.asCharBuffer().toString();
		} finally {
			data.position(old);
		}
	}
	
	/**
	 * Compress contents using zlib.
	 */
	public void compress() {
		IoBuffer tmp = IoBuffer.allocate(0);
		tmp.setAutoExpand(true);
		DeflaterOutputStream deflater = new DeflaterOutputStream(tmp.asOutputStream(), new Deflater(Deflater.BEST_COMPRESSION));
		byte[] tmpData = new byte[data.limit()];
		data.position(0);
		data.get(tmpData);
		try {
			deflater.write(tmpData);
			deflater.finish();
		} catch (IOException e) {
			//docs state that free is optional
			tmp.free();
			throw new RuntimeException("could not compress data", e);
		} finally {
			if (deflater != null) {
				try {
					deflater.close();
				} catch (IOException e1) {
				}
			}
		}
		data.free();
		data = tmp;
		data.flip();
		prepareIO();
	}
	
	/**
	 * Decompress contents using zlib.
	 */
	public void uncompress() {
		data.position(0);
		InflaterInputStream inflater = new InflaterInputStream(data.asInputStream());
		byte[] buffer = new byte[8192];
		IoBuffer tmp = IoBuffer.allocate(0);
		tmp.setAutoExpand(true);
		try {
			while (inflater.available() > 0) {
				int decompressed = inflater.read(buffer);
				if (decompressed <= 0) {
					// Finished decompression
					break;
				}
				tmp.put(buffer, 0, decompressed);
			}
		} catch (IOException e) {
			tmp.free();
			throw new RuntimeException("could not uncompress data", e);
		} finally {
			if (inflater != null) {
				try {
					inflater.close();
				} catch (IOException e1) {
				}
			}
		}
		data.free();
		data = tmp;
		data.flip();
		prepareIO();
	}
	
	/** {@inheritDoc} */
	public ByteOrder getEndian() {
		return dataInput.getEndian();
	}

	/** {@inheritDoc} */
	public boolean readBoolean() {
		return dataInput.readBoolean();
	}

	/** {@inheritDoc} */
	public byte readByte() {
		return dataInput.readByte();
	}

	/** {@inheritDoc} */
	public void readBytes(byte[] bytes) {
		dataInput.readBytes(bytes);
	}

	/** {@inheritDoc} */
	public void readBytes(byte[] bytes, int offset) {
		dataInput.readBytes(bytes, offset);
	}

	/** {@inheritDoc} */
	public void readBytes(byte[] bytes, int offset, int length) {
		dataInput.readBytes(bytes, offset, length);
	}

	/** {@inheritDoc} */
	public double readDouble() {
		return dataInput.readDouble();
	}

	/** {@inheritDoc} */
	public float readFloat() {
		return dataInput.readFloat();
	}

	/** {@inheritDoc} */
	public int readInt() {
		return dataInput.readInt();
	}

	/** {@inheritDoc} */
	public String readMultiByte(int length, String charSet) {
		return dataInput.readMultiByte(length, charSet);
	}

	/** {@inheritDoc} */
	public Object readObject() {
		// according to AMF3 spec, each object should have its own "reference" tables,
		// so we must recreate Input object before reading each object 
		prepareIO();
		return dataInput.readObject();
	}
	
	/** {@inheritDoc} */
	public short readShort() {
		return dataInput.readShort();
	}

	/** {@inheritDoc} */
	public String readUTF() {
		return dataInput.readUTF();
	}

	/** {@inheritDoc} */
	public String readUTFBytes(int length) {
		return dataInput.readUTFBytes(length);
	}

	/** {@inheritDoc} */
	public int readUnsignedByte() {
		return dataInput.readUnsignedByte();
	}

	/** {@inheritDoc} */
	public long readUnsignedInt() {
		return dataInput.readUnsignedInt();
	}

	/** {@inheritDoc} */
	public int readUnsignedShort() {
		return dataInput.readUnsignedShort();
	}

	/** {@inheritDoc} */
	public void setEndian(ByteOrder endian) {
		dataInput.setEndian(endian);
		dataOutput.setEndian(endian);
	}

	/** {@inheritDoc} */
	public void writeBoolean(boolean value) {
		dataOutput.writeBoolean(value);
	}

	/** {@inheritDoc} */
	public void writeByte(byte value) {
		dataOutput.writeByte(value);
	}

	/** {@inheritDoc} */
	public void writeBytes(byte[] bytes) {
		dataOutput.writeBytes(bytes);
	}

	/** {@inheritDoc} */
	public void writeBytes(byte[] bytes, int offset) {
		dataOutput.writeBytes(bytes, offset);
	}

	/** {@inheritDoc} */
	public void writeBytes(byte[] bytes, int offset, int length) {
		dataOutput.writeBytes(bytes, offset, length);
	}

	/** {@inheritDoc} */
	public void writeDouble(double value) {
		dataOutput.writeDouble(value);
	}

	/** {@inheritDoc} */
	public void writeFloat(float value) {
		dataOutput.writeFloat(value);
	}

	/** {@inheritDoc} */
	public void writeInt(int value) {
		dataOutput.writeInt(value);
	}

	/** {@inheritDoc} */
	public void writeMultiByte(String value, String encoding) {
		dataOutput.writeMultiByte(value, encoding);
	}

	/** {@inheritDoc} */
	public void writeObject(Object value) {
		// according to AMF3 spec, each object should have its own "reference" tables,
		// so we must recreate Input object before writing each object 
		prepareIO();
		dataOutput.writeObject(value);
	}

	/** {@inheritDoc} */
	public void writeShort(short value) {
		dataOutput.writeShort(value);
	}

	/** {@inheritDoc} */
	public void writeUTF(String value) {
		dataOutput.writeUTF(value);
	}

	/** {@inheritDoc} */
	public void writeUTFBytes(String value) {
		dataOutput.writeUTFBytes(value);
	}

	/** {@inheritDoc} */
	public void writeUnsignedInt(long value) {
		dataOutput.writeUnsignedInt(value);
	}
	
}
