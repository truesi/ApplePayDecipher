package kz.broscar.component;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

// Should be a component but no spring is in the project
public class KeyStoreComponent {

    private static final String KEYSTORE_TYPE = "JKS";
    //    @Value("keystore.password")
    private String KEYSTORE_PASSWORD = "keystorepass";
    //    @Value("keystore.alias")
    private String CERTIFICATE_ALIAS = "cert_alias";
    //    @Value("keystore.upload.path")
    private String UPLOAD_DIR = "C:/path/to/keystore.jks";

    public KeyStore getKeyStore() throws Exception {
        FileInputStream inputStream = new FileInputStream(UPLOAD_DIR);
        KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE);
        keystore.load(inputStream, KEYSTORE_PASSWORD.toCharArray());
        inputStream.close();
        return keystore;
    }

    public Certificate getCertificate() {
        try {
            KeyStore keystore = getKeyStore();
            return keystore.getCertificate(CERTIFICATE_ALIAS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public PrivateKey getPrivateKey() {
        try {
            KeyStore keystore = getKeyStore();
            return (PrivateKey) keystore.getKey(CERTIFICATE_ALIAS, "pass_for_private".toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
