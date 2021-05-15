package computing.project.wififiletransfer.common;

import org.whispersystems.curve25519.Curve25519;
import org.whispersystems.curve25519.Curve25519KeyPair;

public class Curve25519Helper {

    public static final int KEY_BYTE_SIZE = 32;

    private Curve25519 cipher;
    public Curve25519KeyPair keyPair;

    public Curve25519Helper() {
        cipher = Curve25519.getInstance(Curve25519.BEST);
        keyPair = cipher.generateKeyPair();
    }

    public byte[] getPublicKey() {
        return keyPair.getPublicKey();
    }

    public byte[] getSharedSecret(byte[] anotherPublicKey) {
        return cipher.calculateAgreement(anotherPublicKey, keyPair.getPrivateKey());
    }

}
