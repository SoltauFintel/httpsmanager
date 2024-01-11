package httpsmanager.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashPassword {

    public static void main(String[] args) {
        int encryptionFrequency = 7000; // TO DO enter encryptionFrequency from AppConfig.properties
        String salt = ""; // TO DO enter salt
        String password = ""; // TO DO enter password
        
        String hashed = hashPassword(salt + password, encryptionFrequency);
        
        System.out.println("user.salt=" + salt);
        System.out.println("user.password=" + hashed);
    }
    
    private static String hashPassword(String password, int encryptionFrequency) {
        try {
            byte[] b = password.getBytes();
            MessageDigest algo = MessageDigest.getInstance("MD5");
            for (int i = 0; i < encryptionFrequency; i++) {
                b = algo.digest(b);
            }
            StringBuilder sb = new StringBuilder();
            for (byte i : b) {
                sb.append(String.format("%02X", i));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
