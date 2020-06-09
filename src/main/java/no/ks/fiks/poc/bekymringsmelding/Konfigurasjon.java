/* Dette er Proof-of-Consept-kode og kan mangle logging,
 * prosessflyter og unntakshåndtering som man kan forvente
 * i produksjonsklar kode.
 */

package no.ks.fiks.poc.bekymringsmelding;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Properties;
import java.util.UUID;

public class Konfigurasjon {

    private final Properties properties;

    public Konfigurasjon(String stiTilkonfigurasjonsfil) {

        this.properties = new Properties();
        try (FileInputStream is = new FileInputStream(stiTilkonfigurasjonsfil)) {
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public KeyStore getVirksomhetssertifikatKeyStore() {
        try {
            KeyStore p12 = KeyStore.getInstance("pkcs12");
            p12.load(new FileInputStream(getVirksomhetssertifikatKeyStorePath()), getVirksomhetssertifikatKeyStorePassword().toCharArray());
            return p12;
        } catch (IOException | GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    public String getVirksomhetssertifikatKeyStorePath() {
        return properties.getProperty("virksomhetssertifikat.keyStore.path");
    }

    public String getVirksomhetssertifikatKeyStorePassword() {
        return properties.getProperty("virksomhetssertifikat.keyStorePassword");
    }

    public String getVirksomhetssertifikatKeyAlias() {
        return properties.getProperty("virksomhetssertifikat.keyAlias");
    }

    public String getVirksomhetssertifikatKeyPassword() {
        return properties.getProperty("virksomhetssertifikat.keyPassword");
    }

    public String getDifiIntegrasjonKlientId() {
        return properties.getProperty("difi.integrasjon.klientId");
    }

    public String getFiksIntegrationPassword() {
        return properties.getProperty("fiks.integrationPassword");
    }

    public UUID getFiksIntegrationId() {
        return UUID.fromString(properties.getProperty("fiks.integrationId"));
    }

    public UUID getFiksIoKontoId() {
        return UUID.fromString(properties.getProperty("fiks.io.kontoId"));
    }

    public PrivateKey getFiksIoPrivateKey() {
        try {
            byte[] keyArray = Files.readAllBytes(Paths.get(properties.getProperty("fiks.io.privatekey.path")));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDestinasjoskatalog() {
        return properties.getProperty("bekymringsmeldinger.destinasjonskatalog");
    }
}
