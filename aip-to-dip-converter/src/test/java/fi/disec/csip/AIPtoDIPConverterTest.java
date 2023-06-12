package fi.disec.csip;

import org.junit.jupiter.api.Test;
import org.roda_project.commons_ip2.validator.EARKSIPValidator;
import org.roda_project.commons_ip2.validator.reporter.ValidationReportOutputJson;
import org.roda_project.commons_ip2.validator.state.MetsValidatorState;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AIPtoDIPConverterTest {

    @Test
    void testConvert() throws URISyntaxException, IOException, ParserConfigurationException, SAXException, NoSuchFieldException, IllegalAccessException {
        final var aipToDIPConverter = new AIPToDIPConverter();
        final var tempFile = Files.createTempFile(UUID.randomUUID().toString(), "aip.zip");
        final var sampleSip = Path.of(Objects.requireNonNull(AIPToDIPConverter.class.getClassLoader().getResource("aip.zip")).toURI());
        try (var outputStream = Files.newOutputStream(tempFile)) {
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
        }
        assertTrue(Files.size(tempFile) > 0);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ValidationReportOutputJson jsonReporter = new ValidationReportOutputJson(tempFile, baos);
        jsonReporter.setIpType("DIP");
        final EARKSIPValidator earksipValidator = new EARKSIPValidator(jsonReporter);
        Field metsValidatorState = earksipValidator.getClass().getDeclaredField("metsValidatorState");
        metsValidatorState.setAccessible(true);
        ((MetsValidatorState) metsValidatorState.get(earksipValidator)).setIpType("DIP");
        final boolean isValid = earksipValidator.validate();
        assertTrue(isValid, baos.toString(StandardCharsets.UTF_8));
    }

}