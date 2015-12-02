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

/*
 * RandomGUID
 * @version 1.2.1 11/05/02
 * @author Marc A. Mnich
 *
 * From www.JavaExchange.com, Open Software licensing
 *
 * 11/05/02 -- Performance enhancement from Mike Dubman.  
 *             Moved InetAddr.getLocal to static block.  Mike has measured
 *             a 10 fold improvement in run time.
 * 01/29/02 -- Bug fix: Improper seeding of nonsecure Random object
 *             caused duplicate GUIDs to be produced.  Random object
 *             is now only created once per JVM.
 * 01/19/02 -- Modified random seeding and added new constructor
 *             to allow secure random feature.
 * 01/14/02 -- Added random function seeding with JVM run time
 *
 */

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * In the multitude of java GUID generators, I found none that
 * guaranteed randomness.  GUIDs are guaranteed to be globally unique
 * by using ethernet MACs, IP addresses, time elements, and sequential
 * numbers.  GUIDs are not expected to be random and most often are
 * easy/possible to guess given a sample from a given generator.
 * SQL Server, for example generates GUID that are unique but
 * sequential within a given instance.
 *
 * GUIDs can be used as security devices to hide things such as
 * files within a filesystem where listings are unavailable (e.g. files
 * that are served up from a Web server with indexing turned off).
 * This may be desirable in cases where standard authentication is not
 * appropriate. In this scenario, the RandomGUIDs are used as directories.
 * Another example is the use of GUIDs for primary keys in a database
 * where you want to ensure that the keys are secret.  Random GUIDs can
 * then be used in a URL to prevent hackers (or users) from accessing
 * records by guessing or simply by incrementing sequential numbers.
 *
 * There are many other possibilities of using GUIDs in the realm of
 * security and encryption where the element of randomness is important.
 * This class was written for these purposes but can also be used as a
 * general purpose GUID generator as well.
 *
 * RandomGUID generates truly random GUIDs by using the system's
 * IP address (name/IP), system time in milliseconds (as an integer),
 * and a very large random number joined together in a single String
 * that is passed through an MD5 hash.  The IP address and system time
 * make the MD5 seed globally unique and the random number guarantees
 * that the generated GUIDs will have no discernible pattern and
 * cannot be guessed given any number of previously generated GUIDs.
 * It is generally not possible to access the seed information (IP, time,
 * random number) from the resulting GUIDs as the MD5 hash algorithm
 * provides one way encryption.
 *
 * ----> Security of RandomGUID: <-----
 * RandomGUID can be called one of two ways -- with the basic java Random
 * number generator or a cryptographically strong random generator
 * (SecureRandom).  The choice is offered because the secure random
 * generator takes about 3.5 times longer to generate its random numbers
 * and this performance hit may not be worth the added security
 * especially considering the basic generator is seeded with a
 * cryptographically strong random seed.
 *
 * Seeding the basic generator in this way effectively decouples
 * the random numbers from the time component making it virtually impossible
 * to predict the random number component even if one had absolute knowledge
 * of the System time.  Thanks to Ashutosh Narhari for the suggestion
 * of using the static method to prime the basic random generator.
 *
 * Using the secure random option, this class complies with the statistical
 * random number generator tests specified in FIPS 140-2, Security
 * Requirements for Cryptographic Modules, section 4.9.1.
 *
 * I converted all the pieces of the seed to a String before handing
 * it over to the MD5 hash so that you could print it out to make
 * sure it contains the data you expect to see and to give a nice
 * warm fuzzy.  If you need better performance, you may want to stick
 * to byte[] arrays.
 *
 * I believe that it is important that the algorithm for
 * generating random GUIDs be open for inspection and modification.
 * This class is free for all uses.
 *
 *
 * - Marc
 */
public class RandomGUID extends Object {

	private static Logger log = LoggerFactory.getLogger(RandomGUID.class);
	
	private static final String hexChars = "0123456789ABCDEF";

	private static Random myRand;

	private static SecureRandom mySecureRand;

	private static String s_id;

	public String valueBeforeMD5 = "";

	public String valueAfterMD5 = "";

	/*
	 * Static block to take care of one time secureRandom seed.
	 * It takes a few seconds to initialize SecureRandom.  You might
	 * want to consider removing this static block or replacing
	 * it with a "time since first loaded" seed to reduce this time.
	 * This block will run only once per JVM instance.
	 */

	static {
		mySecureRand = new SecureRandom();
		long secureInitializer = mySecureRand.nextLong();
		myRand = new Random(secureInitializer);
		try {
			s_id = InetAddress.getLocalHost().toString();
		} catch (UnknownHostException e) {
			log.warn("Exception {}", e);
		}

	}

	/*
	 * Default constructor.  With no specification of security option,
	 * this constructor defaults to lower security, high performance.
	 */
	public RandomGUID() {
		getRandomGUID(false);
	}

	/*
	 * Constructor with security option.  Setting secure true
	 * enables each random number generated to be cryptographically
	 * strong.  Secure false defaults to the standard Random function seeded
	 * with a single cryptographically strong random number.
	 */
	public RandomGUID(boolean secure) {
		getRandomGUID(secure);
	}

	/*
	 * Method to generate the random GUID
	 */
	private void getRandomGUID(boolean secure) {
		MessageDigest md5 = null;
		StringBuilder sbValueBeforeMD5 = new StringBuilder();

		try {
			md5 = MessageDigest.getInstance("MD5");
			long time = System.currentTimeMillis();
			long rand = 0;

			if (secure) {
				rand = mySecureRand.nextLong();
			} else {
				rand = myRand.nextLong();
			}

			// This StringBuffer can be a long as you need; the MD5
			// hash will always return 128 bits.  You can change
			// the seed to include anything you want here.
			// You could even stream a file through the MD5 making
			// the odds of guessing it at least as great as that
			// of guessing the contents of the file!
			sbValueBeforeMD5.append(s_id);
			sbValueBeforeMD5.append(':');
			sbValueBeforeMD5.append(Long.toString(time));
			sbValueBeforeMD5.append(':');
			sbValueBeforeMD5.append(Long.toString(rand));

			valueBeforeMD5 = sbValueBeforeMD5.toString();
			md5.update(valueBeforeMD5.getBytes());

			byte[] array = md5.digest();
			StringBuilder sb = new StringBuilder();
			for (int j = 0; j < array.length; ++j) {
				int b = array[j] & 0xFF;
				if (b < 0x10)
					sb.append('0');
				sb.append(Integer.toHexString(b));
			}

			valueAfterMD5 = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			System.out.println("Error: " + e);
		} catch (Exception e) {
			System.out.println("Error:" + e);
		}
	}

	/**
	 * Returns a byte array for the given uuid or guid.
	 * 
	 * @param uid
	 * @return array of bytes containing the id
	 */
	public final static byte[] toByteArray(String uid) {
		byte[] result = new byte[16];
		char[] chars = uid.toCharArray();
		int r = 0;
		for (int i = 0; i < chars.length; ++i) {
			if (chars[i] == '-') {
				continue;
			}
			int h1 = Character.digit(chars[i], 16);
			++i;
			int h2 = Character.digit(chars[i], 16);
			result[(r++)] = (byte) ((h1 << 4 | h2) & 0xFF);
		}
		return result;
	}

	/**
	 * Returns a uuid / guid for a given byte array.
	 * 
	 * @param ba array of bytes containing the id
	 * @return id
	 */
	public static String fromByteArray(byte[] ba) {
		if ((ba != null) && (ba.length == 16)) {
			StringBuilder result = new StringBuilder(36);
			for (int i = 0; i < 16; ++i) {
				if ((i == 4) || (i == 6) || (i == 8) || (i == 10)) {
					result.append('-');
				}
				result.append(hexChars.charAt(((ba[i] & 0xF0) >>> 4)));
				result.append(hexChars.charAt((ba[i] & 0xF)));
			}
			return result.toString();
		}
		return null;
	}

	/**
	 * Returns a nice neat formatted string.
	 * 
	 * @param str unformatted string
	 * @return formatted string
	 */
	public static String getPrettyFormatted(String str) {
		return String.format("%s-%s-%s-%s-%s", new Object[] { str.substring(0, 8), str.substring(8, 12),
				str.substring(12, 16), str.substring(16, 20), str.substring(20) });
	}
	
	/*
	 * Convert to the standard format for GUID
	 * (Useful for SQL Server UniqueIdentifiers, etc.)
	 * Example: C2FEEEAC-CFCD-11D1-8B05-00600806D9B6
	 */
	public String toString() {
		return RandomGUID.getPrettyFormatted(valueAfterMD5.toUpperCase());
	}

	/*
	 * Demonstraton and self test of class
	 */
	public static void main(String args[]) {
		for (int i = 0; i < 100; i++) {
			RandomGUID myGUID = new RandomGUID();
			System.out.println("Seeding String=" + myGUID.valueBeforeMD5);
			System.out.println("rawGUID=" + myGUID.valueAfterMD5);
			System.out.println("RandomGUID=" + myGUID.toString());
		}
	}
}
