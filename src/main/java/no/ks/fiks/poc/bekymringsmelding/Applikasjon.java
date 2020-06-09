/* Dette er Proof-of-Consept-kode og kan mangle logging,
 * prosessflyter og unntakshåndtering som man kan forvente
 * i produksjonsklar kode.
 */

package no.ks.fiks.poc.bekymringsmelding;

import no.ks.fiks.io.client.FiksIOKlient;
import no.ks.fiks.io.client.FiksIOKlientFactory;
import no.ks.fiks.io.client.konfigurasjon.FiksIOKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.KontoKonfigurasjon;
import no.ks.fiks.io.client.konfigurasjon.VirksomhetssertifikatKonfigurasjon;
import no.ks.fiks.io.client.model.KontoId;
import no.ks.fiks.poc.bekymringsmelding.fiksio.NedlastningAvBekymringsmeldinger;
import org.apache.commons.cli.*;

public class Applikasjon {
    public static void main(String[] args) {
        CommandLine cmd = getCommandLine(args);
        Konfigurasjon konfigurasjon = new Konfigurasjon(cmd.getOptionValue("konfigurasjon"));

        final FiksIOKonfigurasjon fiksIOKonfigurasjon = FiksIOKonfigurasjon.defaultTestConfiguration(
                konfigurasjon.getDifiIntegrasjonKlientId(),
                konfigurasjon.getFiksIntegrationId(),
                konfigurasjon.getFiksIntegrationPassword(),
                KontoKonfigurasjon.builder()
                        .kontoId(new KontoId(konfigurasjon.getFiksIoKontoId()))
                        .privatNokkel(konfigurasjon.getFiksIoPrivateKey())
                        .build(),
                VirksomhetssertifikatKonfigurasjon.builder()
                        .keyStore(konfigurasjon.getVirksomhetssertifikatKeyStore())
                        .keyStorePassword(konfigurasjon.getVirksomhetssertifikatKeyStorePassword())
                        .keyAlias(konfigurasjon.getVirksomhetssertifikatKeyAlias())
                        .keyPassword(konfigurasjon.getVirksomhetssertifikatKeyPassword())
                        .build());

        final FiksIOKlientFactory fiksIOKlientFactory = new FiksIOKlientFactory(fiksIOKonfigurasjon);
        final FiksIOKlient fiksIOKlient = fiksIOKlientFactory.build();

        fiksIOKlient.newSubscription(new NedlastningAvBekymringsmeldinger(konfigurasjon.getDestinasjoskatalog()));
    }

    public static CommandLine getCommandLine(String[] args) {
        Options options = new Options().addOption(Option.builder("k")
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
            formatter.printHelp(Applikasjon.class.getSimpleName(), options);
            throw new RuntimeException(e);
        }
    }
}
