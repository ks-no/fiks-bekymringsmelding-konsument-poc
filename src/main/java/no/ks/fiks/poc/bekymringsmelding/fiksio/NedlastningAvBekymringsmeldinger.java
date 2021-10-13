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
            extractFilesFromZip(mottattMelding.getDekryptertZipStream(), mottattMelding.getMeldingId().toString());
            sendMottatt(svarSender, mottattMelding.getMeldingId().toString());
        } catch (Exception e) {
            logger.error("Feil under mottak", e);
            sendFeil(svarSender, mottattMelding.getMeldingId().toString(), "Klarer ikke å hente ut filer fra asice-filen");
        }
    }

    private void extractFilesFromZip(ZipInputStream zip, String meldingsid) throws IOException {
        File dir = new File(outputKatalog, meldingsid);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        ZipEntry entry;
        while ((entry = zip.getNextEntry()) != null) {
            File file = new File(dir, entry.getName());
            logger.info("{} lagres som {}", entry.getName(), file.getPath());
            try (OutputStream outputStream = new FileOutputStream(file)) {
                getFileAsByteArrayOutputStream(zip).writeTo(outputStream);
            }
        }
    }

    private ByteArrayOutputStream getFileAsByteArrayOutputStream(ZipInputStream zip) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = zip.read(buf, 0, 1024)) > -1) {
            outputStream.write(buf, 0, n);
        }
        return outputStream;
    }
}
