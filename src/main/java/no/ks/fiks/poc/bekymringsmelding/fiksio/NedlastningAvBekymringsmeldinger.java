/* Dette er Proof-of-Consept-kode og kan mangle logging,
 * prosessflyter og unntakshåndtering som man kan forvente
 * i produksjonsklar kode.
 */

package no.ks.fiks.poc.bekymringsmelding.fiksio;

import no.ks.fiks.io.client.SvarSender;
import no.ks.fiks.io.client.model.MottattMelding;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NedlastningAvBekymringsmeldinger implements BekymringsmeldingSubscriber {
    private static final String INPUT_FILNAVN = "bekymringsmelding.pdf";
    private static final String OUTPUT_FILNAVN = "bekymringsmelding-%s.pdf";
    private final String outputKatalog;

    public NedlastningAvBekymringsmeldinger(String outputKatalog) {
        this.outputKatalog = outputKatalog;
    }

    @Override
    public void mottakAvOffentligBekymringsmelding(MottattMelding mottattMelding, SvarSender svarSender) {
        lastnedOgLagreBekymringsmelding(mottattMelding, svarSender);
    }

    @Override
    public void mottakAvPrivatBekymringsmelding(MottattMelding mottattMelding, SvarSender svarSender) {
        lastnedOgLagreBekymringsmelding(mottattMelding, svarSender);
    }

    private void lastnedOgLagreBekymringsmelding(MottattMelding mottattMelding, SvarSender svarSender) {
        try {
            ByteArrayOutputStream baos = getFilFraZip(mottattMelding.getDekryptertZipStream());
            File dir = new File(outputKatalog);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            try (OutputStream outputStream = new FileOutputStream(new File(dir, String.format(OUTPUT_FILNAVN, mottattMelding.getMeldingId())))) {
                baos.writeTo(outputStream);
                sendMottatt(svarSender, mottattMelding.getMeldingId().toString());
            }
        } catch (IOException e) {
            sendFeil(svarSender, mottattMelding.getMeldingId().toString(), "Klarer ikke å hente ut " + INPUT_FILNAVN);
        }
    }

    private ByteArrayOutputStream getFilFraZip(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream outputStream = null;
        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            if (entry.getName().equalsIgnoreCase(INPUT_FILNAVN)) {
                outputStream = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = zip.read(buf, 0, 1024)) > -1) {
                    outputStream.write(buf, 0, n);
                }
                return outputStream;
            }
        }
        return outputStream;
    }
}
