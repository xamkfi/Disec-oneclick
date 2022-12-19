package fi.disec.csip;

import org.junit.jupiter.api.Test;
import org.roda_project.commons_ip2.validator.EARKSIPValidator;
import org.roda_project.commons_ip2.validator.reporter.ValidationReportOutputJson;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SIPToAIPConverterTest {

    @Test
    void testConvert() throws URISyntaxException, IOException, ParserConfigurationException, SAXException {
        final var sipToAIPConverter = new SIPToAIPConverter();
        final var tempFile = Files.createTempFile(UUID.randomUUID().toString(), "aip.zip");
        final var sampleSip = Path.of(Objects.requireNonNull(SIPToAIPConverter.class.getClassLoader().getResource("uuid-B3E228EE-B429-45D8-B814-5F567B1A8754.zip")).toURI());
        try (final var outputStream = Files.newOutputStream(tempFile)) {
            final var aip = sipToAIPConverter.convert(sampleSip, outputStream);
            assertEquals("AIP", aip.getType());
            assertEquals("https://earkcsip.dilcis.eu/profile/E-ARK-CSIP.xml", aip.getProfile());
            assertEquals(Set.of("metadata/descriptive/DC.xml", "metadata/preservation/premis.xml", "schemas/DILCISExtensionSIPMETS.xsd", "schemas/xlink.xsd", "schemas/DILCISExtensionMETS.xsd", "schemas/mets1_12.xsd", "schemas/premis.xsd", "submission/uuid-B3E228EE-B429-45D8-B814-5F567B1A8754.zip", "METS.xml"), aip.getZipEntries().keySet());
            assertTrue(Files.size(tempFile) > 0);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final ValidationReportOutputJson jsonReporter = new ValidationReportOutputJson(tempFile, baos);
            final EARKSIPValidator earksipValidator = new EARKSIPValidator(jsonReporter);
            final boolean isValid = earksipValidator.validate();
            assertTrue(isValid, baos.toString(StandardCharsets.UTF_8));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}