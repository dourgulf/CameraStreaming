package org.red5.io.object;

import java.util.Arrays;

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
 * The UnsignedByte class wraps a value of an unsigned 16 bits number.
 * 
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public final class UnsignedShort extends UnsignedNumber {
	static final long serialVersionUID = 1L;

	private int value;

	public UnsignedShort(byte c) {
		value = c;
	}

	public UnsignedShort(short c) {
		value = c;
	}

	public UnsignedShort(int c) {
		value = c & 0xFFFF;
	}

	public UnsignedShort(long c) {
		value = (int) (c & 0xFFFFL);
	}

	private UnsignedShort() {
		value = 0;
	}

	public static UnsignedShort fromBytes(byte[] c) {
		return fromBytes(c, 0);
	}

	public static UnsignedShort fromBytes(byte[] c, int idx) {
		UnsignedShort number = new UnsignedShort();
		if ((c.length - idx) < 2) {
			throw new IllegalArgumentException(
					"An UnsignedShort number is composed of 2 bytes.");
		}
		number.value = ((c[0] << 8) | (c[1] & 0xFFFF));
		return number;
	}

	public static UnsignedShort fromString(String c) {
		return fromString(c, 10);
	}

	public static UnsignedShort fromString(String c, int radix) {
		UnsignedShort number = new UnsignedShort();
		long v = Integer.parseInt(c, radix);
		number.value = (int) (v & 0xFFFF);
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
	public short shortValue() {
		return (short) (value & 0xFFFF);
	}

	@Override
	public int intValue() {
		return value & 0xFFFF;
	}

	@Override
	public long longValue() {
		return value & 0xFFFFL;
	}

	@Override
	public byte[] getBytes() {
		return new byte[]{(byte) ((value >> 8) & 0xFF), (byte) (value & 0xFF)};
	}

	@Override
	public int compareTo(UnsignedNumber other) {
		int otherValue = other.intValue();
		if (value > otherValue) {
			return 1;
		} else if (value < otherValue) {
			return -1;
		}
		return 0;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Number) {
			return Arrays.equals(getBytes(), ((UnsignedNumber) other).getBytes());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public void shiftRight(int nBits) {
		if (Math.abs(nBits) > 16) {
			throw new IllegalArgumentException("Cannot right shift " + nBits
					+ " an UnsignedShort.");
		}
		value >>>= nBits;
	}

	@Override
	public void shiftLeft(int nBits) {
		if (Math.abs(nBits) > 16) {
			throw new IllegalArgumentException("Cannot left shift " + nBits
					+ " an UnsignedShort.");
		}
		value <<= nBits;
	}

}
