package computing.project.wififiletransfer.common;
import android.os.Build;
import android.util.Base64;

import androidx.annotation.RequiresApi;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class RSAUtils {
    private static PublicKey publickey;
    private static PrivateKey privatekey;
    private static KeyPair keyPair;

    public RSAUtils() {
        keyPair = RSAUtils.generateRSAKeyPair(2048);
    }

    public static PublicKey getPublicKey(){
        publickey = keyPair.getPublic();
        return publickey;
    }

    public static PrivateKey getPrivateKey(){
        privatekey = keyPair.getPrivate();
        return privatekey;
    }

    public static void storeKeys(String filebase){
        String fileBase;
        fileBase = filebase;
        try (FileOutputStream out = new FileOutputStream(fileBase + ".key")) {
            out.write(keyPair.getPrivate().getEncoded());
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (FileOutputStream out = new FileOutputStream(fileBase + ".pub")) {
            out.write(keyPair.getPublic().getEncoded());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void restorePublicKey(URI pubkeyfile) throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        URI pubKeyFile;
        pubKeyFile = pubkeyfile;
        byte[] bytes = Files.readAllBytes(Paths.get(pubKeyFile));
        X509EncodedKeySpec ks = new X509EncodedKeySpec(bytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey pub = kf.generatePublic(ks);
    }

    /***TO BE FINISHED since I have no idea wether you can use it or not***/
    public static void restorePrivateKey(){

    }
    /***
     *   Following are examples of encrypting by public key
     *   decrypting by private key(return dataset)
     *
     * ***/
    public static String encryptDataByPublicKey(byte[] srcDate, PublicKey publickey){
        byte[] resultBytes = processData(srcDate, publickey, Cipher.ENCRYPT_MODE);
        return Base64.encodeToString(resultBytes, Base64.DEFAULT);
    }

    public static byte[] decryptDataByPrivate(String encryptedData, PrivateKey privatekey){
        byte[] bytes = Base64.decode(encryptedData, Base64.DEFAULT);
        return processData(bytes, privatekey, Cipher.DECRYPT_MODE);
    }

    /***
     *   Following are examples of encrypting by private key
     *   decrypting by public key(return data set)
     *
     * ***/
    public static String encryptDataByPrivateKey(byte[] srcData,PrivateKey privatekey){
        byte[] resultBytes = processData(srcData, privatekey, Cipher.ENCRYPT_MODE);
        return Base64.encodeToString(resultBytes, Base64.DEFAULT);
    }
    public static byte[] decryptDataByPublicKey(String encryptedData, PublicKey publickey){
        byte[] bytes = Base64.decode(encryptedData, Base64.DEFAULT);
        return processData(bytes, publickey, Cipher.DECRYPT_MODE);

    }




    private static byte[] processData(byte[] srcData, Key key, int mode) {
        byte[] resultBytes = null;
        try {
            Cipher cipher = Cipher.getInstance("RSA/NONE/PKCS1Padding");
            cipher.init(mode, key);
            resultBytes = cipher.doFinal(srcData);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        return resultBytes;
    }

    public static KeyPair generateRSAKeyPair(int keyLength){
        KeyPair keyPair1 = null;
        try{
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair1 = keyPairGenerator.generateKeyPair();
        }catch(NoSuchAlgorithmException e){
            e.printStackTrace();
        }
        return keyPair1;

    }

}

