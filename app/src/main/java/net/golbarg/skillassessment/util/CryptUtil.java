package net.golbarg.skillassessment.util;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Obfuscates the stored coin balance. The key is shipped in the app, so this only deters casual
 * edits of the database. Algorithm, key and IV must stay unchanged: they read balances written by
 * every earlier version.
 */
public final class CryptUtil {
    private static final String ALGORITHM = "Blowfish";
    private static final String MODE = "Blowfish/CBC/PKCS5Padding";
    private static final String IV = "glbrgnet";
    private static final String KEY = "g0lB@rg#2021";

    private CryptUtil() {
    }

    public static String encrypt(String value) throws GeneralSecurityException {
        byte[] encrypted = cipher(Cipher.ENCRYPT_MODE).doFinal(value.getBytes(StandardCharsets.UTF_8));
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    public static String decrypt(String value) throws GeneralSecurityException {
        byte[] decrypted = cipher(Cipher.DECRYPT_MODE).doFinal(Base64.decode(value, Base64.DEFAULT));
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static Cipher cipher(int mode) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(MODE);
        cipher.init(mode, new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), ALGORITHM),
                new IvParameterSpec(IV.getBytes(StandardCharsets.UTF_8)));
        return cipher;
    }
}
