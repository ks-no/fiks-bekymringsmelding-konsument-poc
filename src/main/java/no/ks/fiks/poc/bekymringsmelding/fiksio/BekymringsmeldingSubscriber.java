/* Dette er Proof-of-Consept-kode og kan mangle logging,
 * prosessflyter og unntakshåndtering som man kan forvente
 * i produksjonsklar kode.
 */

package no.ks.fiks.poc.bekymringsmelding.fiksio;

import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.MottattMelding;
import no.ks.fiks.io.client.model.StringPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.function.BiConsumer;

public interface BekymringsmeldingSubscriber extends BiConsumer<MottattMelding, SvarSender> {
    Logger logger = LoggerFactory.getLogger(BekymringsmeldingSubscriber.class);

    @Override
    default void accept(MottattMelding mottattMelding, SvarSender svarSender) {
        logger.info("Mottar melding (Fiks-IO-Meldingsid: {}), type {}", mottattMelding.getMeldingId(), mottattMelding.getMeldingType());
        if ("no.ks.fiks.bekymringsmelding.offentlig.v1".equalsIgnoreCase(mottattMelding.getMeldingType())) {
            mottakAvOffentligBekymringsmelding(mottattMelding, svarSender);
        } else if ("no.ks.fiks.bekymringsmelding.privat.v1".equalsIgnoreCase(mottattMelding.getMeldingType())) {
            mottakAvPrivatBekymringsmelding(mottattMelding, svarSender);
        } else if ("no.ks.fiks.kvittering.tidsavbrudd".equalsIgnoreCase(mottattMelding.getMeldingType())) {
            tidsavbrudd(mottattMelding, svarSender);
        } else {
            mottakAvUkjentMeldingstype(mottattMelding, svarSender);
        }
    }

    void mottakAvOffentligBekymringsmelding(MottattMelding mottattMelding, SvarSender svarSender);

    void mottakAvPrivatBekymringsmelding(MottattMelding mottattMelding, SvarSender svarSender);

    default void tidsavbrudd(MottattMelding mottattMelding, SvarSender svarSender) {
        logger.info("Mottok tidsavbrudd på melding {}", mottattMelding.getMeldingId());
        svarSender.ack();
    }

    default void mottakAvUkjentMeldingstype(MottattMelding mottattMelding, SvarSender svarSender) {
        sendFeil(svarSender, mottattMelding.getMeldingId().toString(), "Ukjent meldingstype");
    }

    default void sendMottatt(SvarSender svarSender, String id) {
        logger.info("Bekrefter mottak av melding (Fiks-IO-Meldingsid: {})", id);
        svarSender.svar("no.ks.fiks.bekymringsmelding.mottatt.v1");
        svarSender.ack();
    }

    default void sendFeil(SvarSender svarSender, String id, String melding) {
        logger.info("Sender feilmelding tilbake til avsender. (Fiks-IO-Meldingsid: {}), Feilmelding: {}", id, melding);
        svarSender.svar("no.ks.fiks.bekymringsmelding.feilet.v1", Collections.singletonList(new StringPayload("{\"melding\":\"" + melding + "\"}", "feilmelding.json")));
        svarSender.nack();
    }
}
