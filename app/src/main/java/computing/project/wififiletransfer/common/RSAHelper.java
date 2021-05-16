package computing.project.wififiletransfer.common;

import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import androidx.annotation.RequiresApi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class RSAHelper {

    public static final int KEY_BIT_SIZE = 2048;

    public static final String CERT_ALIAS = "ECDHSigningCert";

    public static final String SIGN_ALGORITHM = "SHA256withRSA";

    private KeyStore keyStore;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    @RequiresApi(Build.VERSION_CODES.M)
    public RSAHelper() throws KeyStoreException, CertificateException, NoSuchAlgorithmException,
            IOException, InvalidAlgorithmParameterException, UnrecoverableEntryException,
            NoSuchProviderException {
        keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(CERT_ALIAS)) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder(
                    CERT_ALIAS, KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY)
                    .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                    .setKeySize(KEY_BIT_SIZE)
                    .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                    .build());
            KeyPair keyPair = generator.generateKeyPair();
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } else {
           privateKey = (PrivateKey) keyStore.getKey(CERT_ALIAS, null);
           publicKey = keyStore.getCertificate(CERT_ALIAS).getPublicKey();
        }
    }

    public byte[] sign(byte[] rawContent) throws NoSuchAlgorithmException, SignatureException,
            InvalidKeyException, IOException {
        Signature s = Signature.getInstance(SIGN_ALGORITHM);
        s.initSign(privateKey);
        s.update(rawContent);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        byte[] publicKeyRaw = publicKey.getEncoded();
        stream.write(ByteBuffer.allocate(4).putInt(publicKeyRaw.length).array());
        stream.write(publicKeyRaw);
        stream.write(s.sign());
        return stream.toByteArray();
    }

    public static boolean verify(byte[] rawContent, byte[] composedSignature)
            throws NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException,
            SignatureException {
        ByteBuffer buffer = ByteBuffer.wrap(composedSignature);
        int publicKeySize = buffer.getInt();
        byte[] publicKeyRaw = new byte[publicKeySize];
        buffer.get(publicKeyRaw);
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKeyRaw));
        byte[] signature = new byte[buffer.remaining()];
        buffer.get(signature);
        Signature s = Signature.getInstance(SIGN_ALGORITHM);
        s.initVerify(publicKey);
        s.update(rawContent);
        return s.verify(signature);
    }

}

