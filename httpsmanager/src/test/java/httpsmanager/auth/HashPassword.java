package httpsmanager.auth;

import github.soltaufintel.amalia.auth.AuthService;

public class HashPassword {

    public static void main(String[] args) {
        int encryptionFrequency = 7000; // TO DO enter encryptionFrequency from AppConfig.properties
        String salt = ""; // TO DO enter salt
        String password = ""; // TO DO enter password
        
        String hashed = AuthService.hashPassword(salt + password, encryptionFrequency);
        
        System.out.println("user.salt=" + salt);
        System.out.println("user.password=" + hashed);
    }
}
