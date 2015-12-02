package org.red5.io.utils;

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

import java.nio.Buffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;

/**
 * This was borrowed from the Soupdragon base64 library.
 *
 * <p>Codec to translate between hex coding and byte string.</p>
 * <p>Hex output is capital if the char set name is given in capitals.</p>
 * <p>hex:nn used as a charset name inserts \n after every nnth character.</p>
 * 
 * @author Malcolm McMahon
 */
public class HexCharset extends Charset {
	
	private final static String codeHEX = "0123456789ABCDEF";

	private final static String codehex = "0123456789abcdef";

	private String codes;

	private Integer measure;

	/** Creates a new instance of HexCharset 
	 * @param caps true for A-F, false for a-f
	 */
	public HexCharset(boolean caps) {
		super(caps ? "HEX" : "hex", new String[] { "HEX" });
		codes = caps ? codeHEX : codehex;
	}

	/**
	 * Construct the charset
	 * @param caps true for A-F, false for a-f
	 * @param measure Line width for decoding
	 */
	public HexCharset(boolean caps, int measure) {
		super((caps ? "HEX" : "hex") + ":" + measure, new String[] { "HEX" });
		codes = caps ? codeHEX : codehex;
		this.measure = measure;
	}

	/**
	 * Constructs a new encoder for this charset. </p>
	 * 
	 * @return  A new encoder for this charset
	 */
	public CharsetEncoder newEncoder() {
		return new Encoder();
	}

	/**
	 * Constructs a new decoder for this charset. </p>
	 * 
	 * @return  A new decoder for this charset
	 */
	public CharsetDecoder newDecoder() {
		return new Decoder();
	}

	/**
	 * Tells whether or not this charset contains the given charset.
	 * 
	 * <p> A charset <i>C</i> is said to <i>contain</i> a charset <i>D</i> if,
	 * and only if, every character representable in <i>D</i> is also
	 * representable in <i>C</i>.  If this relationship holds then it is
	 * guaranteed that every string that can be encoded in <i>D</i> can also be
	 * encoded in <i>C</i> without performing any replacements.
	 * 
	 * <p> That <i>C</i> contains <i>D</i> does not imply that each character
	 * representable in <i>C</i> by a particular byte sequence is represented
	 * in <i>D</i> by the same byte sequence, although sometimes this is the
	 * case.
	 * 
	 * <p> Every charset contains itself.
	 * 
	 * <p> This method computes an approximation of the containment relation:
	 * If it returns <tt>true</tt> then the given charset is known to be
	 * contained by this charset; if it returns <tt>false</tt>, however, then
	 * it is not necessarily the case that the given charset is not contained
	 * in this charset.
	 * 
	 * @return  <tt>true</tt> if, and only if, the given charset
	 *          is contained in this charset
	 */
	public boolean contains(Charset cs) {
		return cs instanceof HexCharset;
	}

	private class Encoder extends CharsetEncoder {
		private boolean unpaired;

		private int nyble;

		private Encoder() {
			super(HexCharset.this, 0.49f, 1f);

		}

		/**
		 * Flushes this encoder.
		 * 
		 * <p> The default implementation of this method does nothing, and always
		 * returns {@link CoderResult#UNDERFLOW}.  This method should be overridden
		 * by encoders that may need to write final bytes to the output buffer
		 * once the entire input sequence has been read. </p>
		 * 
		 * @param  out
		 *         The output byte buffer
		 * 
		 * @return  A coder-result object, either {@link CoderResult#UNDERFLOW} or
		 *          {@link CoderResult#OVERFLOW}
		 */
		protected java.nio.charset.CoderResult implFlush(java.nio.ByteBuffer out) {
			if (!unpaired) {
				implReset();
				return CoderResult.UNDERFLOW;
			} else
				throw new IllegalArgumentException("Hex string must be an even number of digits");
		}

		/**
		 * Encodes one or more characters into one or more bytes.
		 * 
		 * <p> This method encapsulates the basic encoding loop, encoding as many
		 * characters as possible until it either runs out of input, runs out of room
		 * in the output buffer, or encounters an encoding error.  This method is
		 * invoked by the {@link #encode encode} method, which handles result
		 * interpretation and error recovery.
		 * 
		 * <p> The buffers are read from, and written to, starting at their current
		 * positions.  At most {@link Buffer#remaining in.remaining()} characters
		 * will be read, and at most {@link Buffer#remaining out.remaining()}
		 * bytes will be written.  The buffers' positions will be advanced to
		 * reflect the characters read and the bytes written, but their marks and
		 * limits will not be modified.
		 * 
		 * <p> This method returns a {@link CoderResult} object to describe its
		 * reason for termination, in the same manner as the {@link #encode encode}
		 * method.  Most implementations of this method will handle encoding errors
		 * by returning an appropriate result object for interpretation by the
		 * {@link #encode encode} method.  An optimized implementation may instead
		 * examine the relevant error action and implement that action itself.
		 * 
		 * <p> An implementation of this method may perform arbitrary lookahead by
		 * returning {@link CoderResult#UNDERFLOW} until it receives sufficient
		 * input.  </p>
		 * 
		 * @param  in
		 *         The input character buffer
		 * 
		 * @param  out
		 *         The output byte buffer
		 * 
		 * @return  A coder-result object describing the reason for termination
		 */
		public java.nio.charset.CoderResult encodeLoop(java.nio.CharBuffer in, java.nio.ByteBuffer out) {
			while (in.remaining() > 0) {
				if (out.remaining() <= 0)
					return CoderResult.OVERFLOW;
				char inch = in.get();
				if (!Character.isWhitespace(inch)) {
					int d = Character.digit(inch, 16);
					if (d < 0)
						throw new IllegalArgumentException("Bad hex character " + inch);
					if (unpaired)
						out.put((byte) (nyble | d));
					else
						nyble = d << 4;
					unpaired = !unpaired;
				}
			}
			return CoderResult.UNDERFLOW;
		}

		/**
		 * Clear state
		 */

		protected void implReset() {
			unpaired = false;
			nyble = 0;
		}

	}

	private class Decoder extends CharsetDecoder {
		private int charCount;

		private Decoder() {
			super(HexCharset.this, 2f, measure == null ? 2f : 2f + (2f / (float) measure));
		}

		/**
		 * Decodes one or more bytes into one or more characters.
		 * 
		 * <p> This method encapsulates the basic decoding loop, decoding as many
		 * bytes as possible until it either runs out of input, runs out of room
		 * in the output buffer, or encounters a decoding error.  This method is
		 * invoked by the {@link #decode decode} method, which handles result
		 * interpretation and error recovery.
		 * 
		 * <p> The buffers are read from, and written to, starting at their current
		 * positions.  At most {@link Buffer#remaining in.remaining()} bytes
		 * will be read, and at most {@link Buffer#remaining out.remaining()}
		 * characters will be written.  The buffers' positions will be advanced to
		 * reflect the bytes read and the characters written, but their marks and
		 * limits will not be modified.
		 * 
		 * <p> This method returns a {@link CoderResult} object to describe its
		 * reason for termination, in the same manner as the {@link #decode decode}
		 * method.  Most implementations of this method will handle decoding errors
		 * by returning an appropriate result object for interpretation by the
		 * {@link #decode decode} method.  An optimized implementation may instead
		 * examine the relevant error action and implement that action itself.
		 * 
		 * <p> An implementation of this method may perform arbitrary lookahead by
		 * returning {@link CoderResult#UNDERFLOW} until it receives sufficient
		 * input.  </p>
		 * 
		 * @param  in
		 *         The input byte buffer
		 * 
		 * @param  out
		 *         The output character buffer
		 * 
		 * @return  A coder-result object describing the reason for termination
		 */
		public java.nio.charset.CoderResult decodeLoop(java.nio.ByteBuffer in, java.nio.CharBuffer out) {
			while (in.remaining() > 0) {
				if (measure != null && charCount >= measure) {
					if (out.remaining() == 0)
						return CoderResult.OVERFLOW;
					out.put('\n');
					charCount = 0;
				}
				if (out.remaining() < 2)
					return CoderResult.OVERFLOW;
				int b = in.get() & 0xff;
				out.put(codes.charAt(b >>> 4));
				out.put(codes.charAt(b & 0x0f));
				charCount += 2;
			}
			return CoderResult.UNDERFLOW;
		}

		/**
		 * Resets this decoder, clearing any charset-specific internal state.
		 * 
		 * <p> The default implementation of this method does nothing.  This method
		 * should be overridden by decoders that maintain internal state.  </p>
		 */
		protected void implReset() {
			charCount = 0;
		}

	}
}
