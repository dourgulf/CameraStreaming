package org.red5.io.object;
/*
 * RED5 Open Source Flash Server - http://code.google.com/p/red5/
 *
 * Copyright (c) 2006-2008 by respective authors (see below). All rights reserved.
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

/**
 * The UnsignedInt class wraps a value of an unsigned 32 bits number.
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public final class UnsignedInt extends UnsignedNumber {
	static final long serialVersionUID = 1L;

	private long value;

	public UnsignedInt(byte c) {
		value = c;
	}

	public UnsignedInt(short c) {
		value = c;
	}

	public UnsignedInt(int c) {
		value = c;
	}

	public UnsignedInt(long c) {
		value = c & 0xFFFFFFFFL;
	}

	private UnsignedInt() {
		value = 0;
	}

	public static UnsignedInt fromBytes(byte[] c) {
		return fromBytes(c, 0);
	}

	public static UnsignedInt fromBytes(byte[] c, int idx) {
		UnsignedInt number = new UnsignedInt();
		if ((c.length - idx) < 4)
			throw new IllegalArgumentException(
					"An UnsignedInt number is composed of 4 bytes.");

		number.value = (c[0] << 24 | c[1] << 16 | c[2] << 8 | c[3]);
		return number;
	}

	public static UnsignedInt fromString(String c) {
		return fromString(c, 10);
	}

	public static UnsignedInt fromString(String c, int radix) {
		UnsignedInt number = new UnsignedInt();
		long v = Long.parseLong(c, radix);
		number.value = v & 0xFFFFFFFFL;
		return number;
	}

	@Override
	public double doubleValue() {
		return value;
	}

	@Override
	public float floatValue() {
		return value;
	}

	@Override
	public int intValue() {
		return (int) (value & 0xFFFFFFFFL);
	}

	@Override
	public long longValue() {
		return value & 0xFFFFFFFFL;
	}

	@Override
	public byte[] getBytes() {
		byte[] c = new byte[4];
		c[0] = (byte) ((value >> 24) & 0xFF);
		c[1] = (byte) ((value >> 16) & 0xFF);
		c[2] = (byte) ((value >> 8) & 0xFF);
		c[3] = (byte) ((value >> 0) & 0xFF);
		return c;
	}

	@Override
	public int compareTo(UnsignedNumber other) {
		long otherValue = other.longValue();
		if (value > otherValue)
			return +1;
		else if (value < otherValue)
			return -1;
		return 0;
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof Number))
			return false;
		return value == ((Number) other).longValue();
	}

	@Override
	public String toString() {
		return Long.toString(value & 0xFFFFFFFFL);
	}
	
	@Override
	public int hashCode() {
		return (int)(value ^ (value >>> 32));
	}
	
	@Override
	public void shiftRight(int nBits) {
		if (Math.abs(nBits) > 32)
			throw new IllegalArgumentException("Cannot right shift " + nBits
					+ " an UnsignedInt.");

		value >>>= nBits;
	}
	
	@Override
	public void shiftLeft(int nBits) {
		if (Math.abs(nBits) > 32)
			throw new IllegalArgumentException("Cannot left shift " + nBits
					+ " an UnsignedInt.");

		value <<= nBits;
	}

}
