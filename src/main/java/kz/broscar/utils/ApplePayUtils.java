package kz.broscar.utils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.UUID;

/*
* ORIGINAL CODE AUTHOR @hoaknoppix
* ALL CREDS AND RIGHTS RESERVED TO HIM
* YOU MAY FIND ORIGINAL SOURCE CODE AT
* https://github.com/hoaknoppix/ApplePayUtils
* */

public final class ApplePayUtils {

    private static final String SIGNATURE_ALGORITHM_NAME = "SHA256withECDSA";
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    private static final byte[] COUNTER = {0x00, 0x00, 0x00, 0x01};
    private static final byte[] APPLE_OEM = "Apple".getBytes(ApplePayUtils.UTF_8);
    private static final byte[] ALG_IDENTIFIER_BYTES = "id-aes256-GCM".getBytes(ApplePayUtils.UTF_8);

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    private ApplePayUtils() {
        //do nothing
    }

    private static boolean verify(PrivateKey privateKey, PublicKey publicKey) throws Exception {
        byte[] data = UUID.randomUUID().toString().getBytes();
        byte[] digitalSignature = ApplePayUtils.signData(privateKey, data);
        return ApplePayUtils.verifySig(publicKey, data, digitalSignature);
    }

    private static byte[] signData(PrivateKey privateKey, byte[] data) throws Exception {
        Signature signer = Signature.getInstance(ApplePayUtils.SIGNATURE_ALGORITHM_NAME);
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();
    }

    private static boolean verifySig(PublicKey publicKey, byte[] data, byte[] sig) throws Exception {
        Signature signer = Signature.getInstance(ApplePayUtils.SIGNATURE_ALGORITHM_NAME);
        signer.initVerify(publicKey);
        signer.update(data);
        return signer.verify(sig);
    }

    private static Certificate inflateCertificate(String certificateString) throws Exception {
        byte[] certificateBytes = Base64.getDecoder().decode(certificateString);
        InputStream stream = new ByteArrayInputStream(certificateBytes);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        Certificate certificate = certificateFactory.generateCertificate(stream);
        stream.close();
        return certificate;
    }

    private static PrivateKey inflatePrivateKey(String privateKey) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
    }


    private static byte[] performKeyDerivationFunction(X509Certificate certificate,
                                                       byte[] sharedSecret) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Add counter
        byteArrayOutputStream.write(ApplePayUtils.COUNTER);

        // Add shared secret
        byteArrayOutputStream.write(sharedSecret);

        // Add algorithm identifier len
        byteArrayOutputStream.write(ApplePayUtils.ALG_IDENTIFIER_BYTES.length);

        // Add algorithm identifier
        byteArrayOutputStream.write(ApplePayUtils.ALG_IDENTIFIER_BYTES);

        // Add Wallet Provider
        byteArrayOutputStream.write(ApplePayUtils.APPLE_OEM);

        // Add Merchant Id
        byteArrayOutputStream.write(HexBin.decode(new String(certificate.getExtensionValue("1.2.840.113635.100.6.32"),
                                                               ApplePayUtils.UTF_8)
                                                            .substring(4)));

        // Perform KDF
        MessageDigest messageDigest = MessageDigest.getInstance("SHA256", "BC");

        return messageDigest.digest(byteArrayOutputStream.toByteArray());
    }


    private static byte[] decrypt(String ephemeralKeyString, String privateKeyString,
                                  String certificateString, String data) throws Exception {
        X509Certificate certificate = (X509Certificate) ApplePayUtils
                .inflateCertificate(certificateString);
        PrivateKey privateKey = ApplePayUtils.inflatePrivateKey(privateKeyString);

        if (!ApplePayUtils.verify(privateKey, certificate.getPublicKey())) {
            throw new Exception("Asymmetric keys do not match!");
        }

        byte[] ephemeralPublicKeyBytes = Base64.getDecoder().decode(ephemeralKeyString);

        // Reconstitute Ephemeral Public Key
        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(ephemeralPublicKeyBytes);
        ECPublicKey ephemeralPublicKey = (ECPublicKey) keyFactory.generatePublic(encodedKeySpec);
        // Perform KeyAgreement
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH", "BC");
        agreement.init(privateKey);
        agreement.doPhase(ephemeralPublicKey, true);
        byte[] sharedSecret = agreement.generateSecret();

        // Perform KDF
        byte[] derivedSecret = ApplePayUtils
                .performKeyDerivationFunction(certificate, sharedSecret);

        // Use the derived secret to decrypt the data
        SecretKeySpec key = new SecretKeySpec(derivedSecret, "AES");
        byte[] ivBytes = new byte[16];
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        aesCipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        return aesCipher.doFinal(Base64.getDecoder().decode(data));
    }

    private static byte[] decrypt(String ephemeralKeyString, PrivateKey privateKey,
                                  X509Certificate certificate, String data) throws Exception {

        if (!ApplePayUtils.verify(privateKey, certificate.getPublicKey())) {
            throw new Exception("Asymmetric keys do not match!");
        }

        byte[] ephemeralPublicKeyBytes = Base64.getDecoder().decode(ephemeralKeyString);

        // Reconstitute Ephemeral Public Key
        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");
        X509EncodedKeySpec encodedKeySpec = new X509EncodedKeySpec(ephemeralPublicKeyBytes);
        ECPublicKey ephemeralPublicKey = (ECPublicKey) keyFactory.generatePublic(encodedKeySpec);
        // Perform KeyAgreement
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH", "BC");
        agreement.init(privateKey);
        agreement.doPhase(ephemeralPublicKey, true);
        byte[] sharedSecret = agreement.generateSecret();

        // Perform KDF
        byte[] derivedSecret = ApplePayUtils
                .performKeyDerivationFunction(certificate, sharedSecret);

        // Use the derived secret to decrypt the data
        SecretKeySpec key = new SecretKeySpec(derivedSecret, "AES");
        byte[] ivBytes = new byte[16];
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        aesCipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        return aesCipher.doFinal(Base64.getDecoder().decode(data));
    }

    /**
     * Return decrypted data as an array of bytes from encrypted ApplePay payment data
     *
     * @param ephemeralKeyString the ephemeral public key received in payload from ios app after a
     * successful Apple payment process
     * @param privateKeyString the private key which its certificate uploaded to Apple developer
     * console
     * @param certificateString the certificate download from Apple developer console
     * @param data the encrypted data received in payload from ios app after a successful Apple
     * payment process
     * @return Decrypted Base64 bytes
     * @throws Exception if the decryption has any issues i.e. private key and public key is a pair
     */
    public static byte[] decryptAsBytes(String ephemeralKeyString, String privateKeyString,
                                        String certificateString, String data)
            throws Exception {
        return ApplePayUtils.decrypt(ephemeralKeyString, privateKeyString, certificateString, data);
    }

    /**
     * Return decrypted data as a json String from encrypted ApplePay payment data
     *
     * @param ephemeralKeyString the ephemeral public key received in payload from ios app after a
     * successful Apple payment process
     * @param privateKeyString the private key which its certificate uploaded to Apple developer
     * console
     * @param certificateString the certificate download from Apple developer console
     * @param data the encrypted data received in payload from ios app after a successful Apple
     * payment process
     * @return Decrypted JSON String
     * @throws Exception if the decryption has any issues i.e. private key and public key is a pair
     */
    public static String decryptAsString(String ephemeralKeyString,
                                         String privateKeyString,
                                         String certificateString, String data)
            throws Exception {
        byte[] bytes = ApplePayUtils
                .decrypt(ephemeralKeyString, privateKeyString, certificateString, data);
        return new String(bytes, ApplePayUtils.UTF_8);
    }

    /**
     * Return decrypted data as a json String from encrypted ApplePay payment data
     *
     * @param ephemeralKeyString the ephemeral public key received in payload from ios app after a
     * successful Apple payment process
     * @param data the encrypted data received in payload from ios app after a successful Apple
     * payment process
     * @return Decrypted JSON String
     * @throws Exception if the decryption has any issues i.e. private key and public key is a pair
     */
    public static String decryptAsString(String ephemeralKeyString,
                                         PrivateKey privateKey,
                                         X509Certificate certificate, String data)
            throws Exception {
        byte[] bytes = ApplePayUtils
                .decrypt(ephemeralKeyString, privateKey, certificate, data);
        return new String(bytes, ApplePayUtils.UTF_8);
    }

}
