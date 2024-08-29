package com.truethic.soninx.SoniNxAPI.common;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import java.security.spec.KeySpec;
import java.util.Base64;

@Component
public class PasswordEncoders {
    private Cipher cipher;
    private String myEncryptionKey;
    private String myEncryptionScheme;
    private SecretKey key;
    byte[] arrayBytes;
    private KeySpec ks;
    private SecretKeyFactory skf;
    private static final String UNICODE_FORMAT = "UTF8";
    public static final String DESEDE_ENCRYPTION_SCHEME = "DESede";
    public String encryptedText;

    public BCryptPasswordEncoder passwordEncoderNew() {
        return new BCryptPasswordEncoder();
    }

    public String encrypt(String unencryptedString) {

        try {
            encryptedText = Base64.getEncoder().encodeToString(unencryptedString.getBytes());
           /* Base64.Encoder encoder = Base64.getEncoder();
            encryptedText = encoder.encodeToString(encryptedByte);*/

        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedText;
    }

    public String decrypt(String encodedString) {
        String decodedString = "";
        try {
            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            decodedString = new String(decodedBytes);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return encryptedText;
    }
}
