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
 * @author Matteo Merli (matteo.merli@gmail.com)
 */
public abstract class UnsignedNumber extends Number {

	private static final long serialVersionUID = -6404256963187584919L;

	/**
	 * Get a byte array representation of the number. The order will be MSB
	 * first (Big Endian).
	 * 
	 * @return the serialized number
	 */
	public abstract byte[] getBytes();

	/**
	 * Perform a bit right shift of the value.
	 * 
	 * @param nBits
	 *            the number of positions to shift
	 */
	public abstract void shiftRight(int nBits);

	/**
	 * Perform a bit left shift of the value.
	 * 
	 * @param nBits
	 *            the number of positions to shift
	 */
	public abstract void shiftLeft(int nBits);

	public abstract String toString();

	public abstract int compareTo(UnsignedNumber other);

	public abstract boolean equals(Object other);

	public abstract int hashCode();

	public String toHexString() {
		return toHexString(false);
	}

	public String toHexString(boolean pad) {
		StringBuilder sb = new StringBuilder();
		boolean started = false;
		for (byte b : getBytes()) {
			if (!started && b == 0) {
				if (pad) {
					sb.append("00");
				}
			} else {
				sb.append(hexLetters[(byte) ((b >> 4) & 0x0F)]).append(hexLetters[b & 0x0F]);
				started = true;
			}
		}
		if (sb.length() == 0) {
			return "0";
		}
		return sb.toString();
	}

	protected static final char[] hexLetters = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
			'E', 'F' };
}
