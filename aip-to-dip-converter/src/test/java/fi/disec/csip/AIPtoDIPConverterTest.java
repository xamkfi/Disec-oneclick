package fi.disec.csip;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AIPtoDIPConverterTest {

    @Test
    void testConvert() throws URISyntaxException {
        final var aipToDIPConverter = new AIPToDIPConverter();
        final var outputStream = new ByteArrayOutputStream();
        final var sampleSip = Path.of(Objects.requireNonNull(AIPToDIPConverter.class.getClassLoader().getResource("aip.zip")).toURI());
        final var dip = aipToDIPConverter.convert(sampleSip, outputStream);
        assertEquals("DIP", dip.getType());
        assertEquals("https://earkdip.dilcis.eu/profile/E-ARK-DIP.xml", dip.getProfile());
        assertEquals(Set.of("metadata/descriptive/DC.xml",
                "metadata/preservation/premis.xml",
                "schemas/DILCISExtensionSIPMETS.xsd",
                "schemas/xlink.xsd",
                "schemas/DILCISExtensionMETS.xsd",
                "schemas/mets1_12.xsd",
                "schemas/premis.xsd",
                "submission/6b3bd4ac-7b83-4605-888a-a8a4989223d0.zip",
                "METS.xml"), dip.getZipEntries().keySet());
        assertNotEquals(dip.getId(), "6b3bd4ac-7b83-4605-888a-a8a4989223d0");
        assertTrue(outputStream.toByteArray().length > 0);
    }

}