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

    public String getMaskinportenKlientId() {
        return properties.getProperty("digdir.maskinporten.klientId");
    }

    public String getMaskinportenAudience() {
        return properties.getProperty("digdir.maskinporten.audience");
    }

    public String getMaskinportenTokenEndpoint() {
        return properties.getProperty("digdir.maskinporten.tokenEndpoint");
    }

    public String getMottakerFiksIntegrationPassword() {
        return properties.getProperty("mottaker.integrasjonPassord");
    }

    public UUID getMottakerFiksIntegrationId() {
        return UUID.fromString(properties.getProperty("mottaker.integrasjonId"));
    }

    public UUID getMottakerFiksIoKontoId() {
        return UUID.fromString(properties.getProperty("mottaker.fiks.io.kontoId"));
    }

    public PrivateKey getMottakerFiksIoPrivateKey() {
        try {
            byte[] keyArray = Files.readAllBytes(Paths.get(properties.getProperty("mottaker.fiks.io.privatekey.path")));
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyArray);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public String getMottakerDestinasjoskatalog() {
        return properties.getProperty("mottaker.destinasjonskatalog");
    }

    public String getAvsenderBekymringsmeldingBaseUrl() {
        return properties.getProperty("avsender.baseUrl");
    }

    public UUID getAvsenderFiksOrdId() {
        return UUID.fromString(properties.getProperty("avsender.fiksOrdId"));
    }

    public String getAvsenderFiksIntegrationPassword() {
        return properties.getProperty("avsender.integrasjonPassord");
    }

    public String getAvsenderFiksIntegrationId() {
        return properties.getProperty("avsender.integrasjonId");
    }
}
