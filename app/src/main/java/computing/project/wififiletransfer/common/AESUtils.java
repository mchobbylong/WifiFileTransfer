package computing.project.wififiletransfer.common;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {

    public static final String TAG = "AESUtils";

    public static final String KEY_ALGORITHM = "AES";

    public static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    public static final int KEY_LENGTH = 256;

    public static final int IV_BYTE_SIZE = 12;

    @RequiresApi(Build.VERSION_CODES.O)
    public static byte[] encrypt(byte[] input, int inputOffset, int inputLength, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException,BadPaddingException,
            IllegalBlockSizeException, ShortBufferException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        byte[] iv = new byte[IV_BYTE_SIZE];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        int outputLength = cipher.getOutputSize(inputLength);
        byte[] cipherText = new byte[IV_BYTE_SIZE + outputLength];
        System.arraycopy(iv, 0, cipherText, 0, IV_BYTE_SIZE);
        cipher.doFinal(input, inputOffset, inputLength, cipherText, IV_BYTE_SIZE);
        return cipherText;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    public static byte[] decrypt(byte[] cipherText, int inputLength, SecretKey key)
            throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException,
            IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        GCMParameterSpec ivSpec = new GCMParameterSpec(128, cipherText, 0, IV_BYTE_SIZE);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
        byte[] plainText = cipher.doFinal(cipherText, IV_BYTE_SIZE, inputLength - IV_BYTE_SIZE);
        return plainText;
    }

    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        SecretKey key = keyGenerator.generateKey();
        return key;
    }

    public static SecretKey generateAESKey(String passphrase) {
        return new SecretKeySpec(passphrase.getBytes(DEFAULT_CHARSET), KEY_ALGORITHM);
    }

    public static SecretKey generateAESKey(byte[] rawBytes) {
        return new SecretKeySpec(rawBytes, KEY_ALGORITHM);
    }

}
