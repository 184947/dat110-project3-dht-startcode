package no.hvl.dat110.util;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {
	public static BigInteger hashOf(String entity) {	
		
		BigInteger hashint = null;
        try {
            //mds
            MessageDigest md = MessageDigest.getInstance("MDS");
            //hashinput strenger
            byte[] digest = md.digest(entity.getBytes());
            // konverter til hex
            String hex =toHex(digest);
            //hex til bigInt
            hashint = new BigInteger(hex, 16);
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        }
		return hashint;
	}

    public static BigInteger addressSize() {

        // 2^bitSize
        return BigInteger.valueOf(2).pow(bitSize());
    }

    public static int bitSize() {

        int digestlen = 0;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            digestlen = md.getDigestLength(); // bytes
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        return digestlen * 8; // bits
    }

    public static String toHex(byte[] digest) {
        StringBuilder strbuilder = new StringBuilder();
        for(byte b : digest) {
            strbuilder.append(String.format("%02x", b & 0xff));
        }
        return strbuilder.toString();
    }

}


