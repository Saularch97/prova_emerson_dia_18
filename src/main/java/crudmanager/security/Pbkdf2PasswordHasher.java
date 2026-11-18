package crudmanager.security;

import crudmanager.exception.ModelException;
import crudmanager.security.PasswordHasher;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 210000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String hash(String raw) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] key = derive(raw, salt, ITERATIONS, KEY_BITS);
        Base64.Encoder enc = Base64.getEncoder();
        return ITERATIONS + ":" + enc.encodeToString(salt) + ":" + enc.encodeToString(key);
    }

    @Override
    public boolean matches(String raw, String stored) {
        if (raw == null || stored == null) {
            return false;
        }
        String[] parts = stored.split(":", -1);
        if (parts.length != 3) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            byte[] expected = Base64.getDecoder().decode(parts[2]);
            if (iterations < 1 || expected.length == 0) {
                return false;
            }
            byte[] actual = derive(raw, salt, iterations, expected.length * 8);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(String raw, byte[] salt, int iterations, int bits) {
        PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(), salt, iterations, bits);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new ModelException("Falha ao calcular o hash de senha", e);
        } finally {
            spec.clearPassword();
        }
    }
}
