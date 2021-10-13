/* Dette er Proof-of-Consept-kode og kan mangle logging,
 * prosessflyter og unntakshåndtering som man kan forvente
 * i produksjonsklar kode.
 */

package no.ks.fiks.poc.bekymringsmelding;

import no.ks.fiks.bekymringsmelding.produsent.klient.BekymringsmeldingApi;
import no.ks.fiks.bekymringsmelding.produsent.klient.BekymringsmeldingApiImpl;
import no.ks.fiks.bekymringsmelding.produsent.klient.BekymringsmeldingKlient;
import no.ks.fiks.bekymringsmelding.produsent.klient.model.Bydel;
import no.ks.fiks.bekymringsmelding.produsent.klient.model.Historikk;
import no.ks.fiks.io.asice.AsicHandler;
import no.ks.fiks.io.asice.AsicHandlerBuilder;
import no.ks.fiks.io.asice.model.KeystoreHolder;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.maskinporten.Maskinportenklient;
import no.ks.fiks.maskinporten.MaskinportenklientProperties;
import no.ks.kryptering.CMSKrypteringImpl;
import org.apache.commons.cli.*;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/*

// Sender inn en bekymringsmelding
mvn compile exec:java -Dexec.mainClass="no.ks.fiks.poc.bekymringsmelding.SendBekymringsmelding" \
 -Dexec.args="-k sertifikater/konfigurasjon.properties -send 4699 bekymringsmeldinger/bekymringsmelding.pdf \
 bekymringsmeldinger/bekymringsmelding.json -b 99"

// Sjekker status
mvn compile exec:java -Dexec.mainClass="no.ks.fiks.poc.bekymringsmelding.SendBekymringsmelding" \
 -Dexec.args="-k sertifikater/konfigurasjon.properties --status dfc16e5c-946f-4195-b2e1-5e0460df12c1"

 */
public class SendBekymringsmelding {
    public static void main(String[] args) throws Exception {
        CommandLine cmd = getCommandLine(args);
        Konfigurasjon konfigurasjon = new Konfigurasjon(cmd.getOptionValue("konfigurasjon"));

        VirksomhetssertifikatKonfigurasjon virksomhetssertifikatKonfigurasjon = VirksomhetssertifikatKonfigurasjon.builder()
                .keyStore(konfigurasjon.getVirksomhetssertifikatKeyStore())
                .keyStorePassword(konfigurasjon.getVirksomhetssertifikatKeyStorePassword())
                .keyAlias(konfigurasjon.getVirksomhetssertifikatKeyAlias())
                .keyPassword(konfigurasjon.getVirksomhetssertifikatKeyPassword())
                .build();
        KeyStore keyStore = virksomhetssertifikatKonfigurasjon.getKeyStore();

        Maskinportenklient maskinportenklient = new Maskinportenklient(
                keyStore,
                virksomhetssertifikatKonfigurasjon.getKeyAlias(),
                virksomhetssertifikatKonfigurasjon.getKeyStorePassword().toCharArray(),
                MaskinportenklientProperties.builder()
                        .numberOfSecondsLeftBeforeExpire(10)
                        .issuer(konfigurasjon.getMaskinportenKlientId())
                        .audience(konfigurasjon.getMaskinportenAudience())
                        .tokenEndpoint(konfigurasjon.getMaskinportenTokenEndpoint())
                        .build());

        Client client = ClientBuilder.newBuilder().register(MultiPartFeature.class).build();
        BekymringsmeldingApi api = new BekymringsmeldingApiImpl(
                client,
                konfigurasjon.getAvsenderBekymringsmeldingBaseUrl(),
                maskinportenklient,
                konfigurasjon.getAvsenderFiksOrdId(),
                konfigurasjon.getAvsenderFiksIntegrationId(),
                konfigurasjon.getAvsenderFiksIntegrationPassword());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        AsicHandler asicHandler = AsicHandlerBuilder.create()
                .withExecutorService(executorService)
                .withKeyStoreHolder(KeystoreHolder.builder()
                        .withKeyAlias(virksomhetssertifikatKonfigurasjon.getKeyAlias())
                        .withKeyPassword(virksomhetssertifikatKonfigurasjon.getKeyPassword())
                        .withKeyStorePassword(virksomhetssertifikatKonfigurasjon.getKeyStorePassword())
                        .withKeyStore(keyStore)
                        .build())
                .build();

        CMSKrypteringImpl cmsKryptering = new CMSKrypteringImpl();
        BekymringsmeldingKlient klient = new BekymringsmeldingKlient(api, asicHandler, cmsKryptering);

        if (cmd.hasOption("status")) {
            visStatus(api, UUID.fromString(cmd.getOptionValue("status")));
        } else if (cmd.hasOption("send")) {
            String bydelsnummer = cmd.getOptionValue("bydel");
            String kommunenummer = cmd.getOptionValues("send")[0];
            String bekymringsmeldingPdfFilnavn = cmd.getOptionValues("send")[1];
            String bekymringsmeldingJsonFilnavn = cmd.getOptionValues("send")[2];

            List<Bydel> bydeler = api.getBydeler(kommunenummer);
            if (bydeler.isEmpty()) {
                throw new RuntimeException("Kommunen er ikke konfigurert til å motta bekymringsmelding via Fiks-plattformen");
            }

            if (bydelsnummer != null && !bydeler.stream().map(Bydel::getBydelsnummer).collect(Collectors.toList()).contains(bydelsnummer)) {
                throw new RuntimeException("Ukjent bydelsnummer");
            }

            FileInputStream bekymringsmeldingPdf = new FileInputStream(bekymringsmeldingPdfFilnavn);
            FileInputStream bekymringsmeldingJson = new FileInputStream(bekymringsmeldingJsonFilnavn);

            System.out.printf("Sender bekymringsmelding til kommunenr %s, bydel %s. Filer: %s, %s%n", kommunenummer, bydelsnummer, bekymringsmeldingPdfFilnavn, bekymringsmeldingJsonFilnavn);
            final UUID bekymringsmeldingId = Optional.ofNullable(bydelsnummer)
                    .map(e -> klient.krypterOgSendBekymringsmelding(kommunenummer, bydelsnummer, bekymringsmeldingPdf, bekymringsmeldingJson))
                    .orElseGet(()->klient.krypterOgSendBekymringsmelding(kommunenummer, bekymringsmeldingPdf, bekymringsmeldingJson));
            visStatus(api, bekymringsmeldingId);
        } else {
            visHjelpetekst(cmd);
        }

        shutdown(asicHandler, executorService, client);
    }

    private static void visHjelpetekst(CommandLine cmd) {
        HelpFormatter formatter = new HelpFormatter();
        Options options = new Options();
        Arrays.asList(cmd.getOptions()).forEach(options::addOption);
        formatter.printHelp("SendBekymringsmelding", options);
    }

    private static void visStatus(BekymringsmeldingApi api, UUID bekymringsmeldingid) {
        List<Historikk> status = api.status(bekymringsmeldingid);
        System.out.println("Historikk på bekymringsmelding " + bekymringsmeldingid);
        status.forEach(System.out::println);
    }

    private static void shutdown(AsicHandler asicHandler, ExecutorService executorService, Client client) {
        try {
            asicHandler.close();
        } catch (Exception e) {
            //log
        }
        try {
            executorService.shutdown();
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            //log
        }
        try {
            client.close();
        } catch (Exception e) {
            //log
        }
    }

    public static CommandLine getCommandLine(String[] args) {
        Options options = new Options()
                .addOption(Option.builder("send")
                        .longOpt("send")
                        .desc("sender bekymringsmelding")
                        .hasArg()
                        .numberOfArgs(3)
                        .argName("kommunenummer, pdfFil, jsonFil")
                        .build())
                .addOption(Option.builder("b")
                        .longOpt("bydel")
                        .desc("setter bydel")
                        .hasArg()
                        .numberOfArgs(1)
                        .argName("bydel")
                        .build())
                .addOption(Option.builder("s")
                        .longOpt("status")
                        .desc("sjekke status på en bekymringsmelding.")
                        .hasArg()
                        .numberOfArgs(1)
                        .argName("bekymringsmeldingsid")
                        .type(UUID.class)
                        .build())
                .addOption(Option.builder("k")
                        .longOpt("konfigurasjon")
                        .desc("Skriv inn sti og navn på konfigurasjonsfil.")
                        .hasArg()
                        .numberOfArgs(1)
                        .argName("fil")
                        .required()
                        .build());
        try {
            return new DefaultParser().parse(options, args);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(SendBekymringsmelding.class.getSimpleName(), options);
            throw new RuntimeException(e);
        }
    }
}
