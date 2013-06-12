package de.yovi.chat.system;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import org.apache.log4j.Logger;

public class PasswordUtil {

	private final static Logger logger = Logger.getLogger(PasswordUtil.class);
	
	private final static String ALGORITHM = "SHA-256";
	private static MessageDigest md;
	private final static Random random = new Random();
	
	private final static int KEY_LENGTH = 10;
	
	private static char[] CONSONANTS = new char[] {
		'B','C','D','F','G','H','J','K','L','M','N','P','Q','R','S','T','V','W','X','Y','Z'
	};
	private static char[] VOWELS = new char[] {
		'a','e','i','o','u'
	};
	private static char[] SPECIALS = new char[] {
		'+','-','.'
	};
	
	public synchronized static String getSugar() {
		String output = "";
		while (output.length() < 8) {
			output += Integer.toHexString(random.nextInt(16));
		}
		return output;
	}
	
	public static String createKey() {
		char[] keyChars = new char[KEY_LENGTH];
		boolean upper = true;
		boolean lower = false;
		boolean doubleLower = false;
		boolean special = false;
		for (int i = 0; i < KEY_LENGTH; i++) {
			if (upper) {
				keyChars[i] = CONSONANTS[random.nextInt(CONSONANTS.length)];
				upper = false;
				lower = true;
			} else if (lower) {
				keyChars[i] = VOWELS[random.nextInt(VOWELS.length)];
				lower = false;
				if (random.nextBoolean()) {
					doubleLower = true;
				} else {
					special = true;
				}
			} else if (doubleLower) {
				keyChars[i] = VOWELS[random.nextInt(VOWELS.length)];
				doubleLower = false;
				special = true;
			} else if (special) {
				keyChars[i] = SPECIALS[random.nextInt(SPECIALS.length)];
				special = false;
				upper = true;
			}
		}
		return new String(keyChars);
	}
	
	public synchronized static String encrypt(String pwhash, String sugar) {
		String result = null;
		if (pwhash != null) {
			try {
				if (md == null) {
					md = MessageDigest.getInstance(ALGORITHM);
				} else {
					md.reset();
				}
				if (sugar != null) {
					md.update(sugar.getBytes());
				}
				md.update(pwhash.getBytes());
				byte[] digest = md.digest();
				result = bytesToHex(digest);
			} catch (NoSuchAlgorithmException e) {
				logger.error("Error while creating a MessageDigester for algorithm: " + ALGORITHM);
			} finally {
				if (md != null) {
					md.reset();
				}
			}
		}
		return result;
	}
	
	private static String bytesToHex(byte[] bytes) {
	    final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static void main(String[] args) {
		System.out.println(encrypt("test", "sugar"));
	}
	
}
