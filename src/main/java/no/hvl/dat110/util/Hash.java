package no.hvl.dat110.util;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

    public static BigInteger hashOf(String entity) {

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(entity.getBytes(StandardCharsets.UTF_8));

            // Viktig: 1 betyr positiv BigInteger
            return new BigInteger(1, digest);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static BigInteger addressSize() {
        return BigInteger.valueOf(2).pow(bitSize());
    }

    public static int bitSize() {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            int digestlen = md.getDigestLength();   // 16 bytes for MD5
            return digestlen * 8;                   // 128 bits
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toHex(byte[] digest) {
        StringBuilder strbuilder = new StringBuilder();
        for(byte b : digest) {
            strbuilder.append(String.format("%02x", b & 0xff));
        }
        return strbuilder.toString();
    }
}


