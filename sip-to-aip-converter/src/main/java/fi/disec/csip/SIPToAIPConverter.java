package fi.disec.csip;

import gov.loc.premis.v3.AgentComplexType;
import gov.loc.premis.v3.ObjectFactory;
import gov.loc.premis.v3.PremisComplexType;
import gov.loc.premis.v3.StringPlusAuthority;
import org.roda_project.commons_ip.model.ParseException;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip.utils.METSEnums;
import org.roda_project.commons_ip2.model.*;
import org.roda_project.commons_ip2.model.impl.BasicAIP;
import org.roda_project.commons_ip2.model.impl.eark.EARKAIP;
import org.roda_project.commons_ip2.model.impl.eark.EARKSIP;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

import static fi.disec.csip.Utils.*;


public class SIPToAIPConverter {
    private static final String PREMIS_PACKAGE = "gov.loc.premis.v3";
    private static final String PREMIS_FILENAME = "premis.xml";
    private static final String PREMIS_SCHEMA = "premis.xsd";
    private static final String PREMIS_CLASSPATH = "premis/v3/" + PREMIS_SCHEMA;
    private static final IPAgent CREATOR_AGENT = new IPAgent(
            "YksaSipToAip",
            "CREATOR",
            null,
            METSEnums.CreatorType.OTHER,
            "SOFTWARE",
            "1.0.0",
            IPAgentNoteTypeEnum.SOFTWARE_VERSION
    );

    /**
     * Convert a SIP into a zipped AIP and write it to disk
     *
     * @param sip Path to the SIP, this is directly passed to {@link EARKSIP#parse(Path, Path)}
     * @param aip Path to write the zip file to
     * @return The resulting AIP that has already been written to the given path
     */
    public AIP convert(final Path sip, final Path aip) {
        try (final var out = Files.newOutputStream(aip)) {
            return convert(sip, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Convert a SIP into a zipped AIP and write it into an outputStream
     *
     * @param sip Path to the SIP, this is directly passed to {@link EARKSIP#parse(Path, Path)}
     * @param out The stream to write the zip file to
     * @return The resulting AIP that has already been written to the outputStream
     */
    public AIP convert(final Path sip, final OutputStream out) {
        Path tempFolder = null;
        try {
            tempFolder = Files.createTempDirectory("sip-to-aip");
            final var sipTemp = Files.createTempDirectory(tempFolder, "sip-extracted");
            final var aipTemp = Files.createTempDirectory(tempFolder, "aip-extracted");
            final IPInterface earksip = EARKSIP.parse(sip, sipTemp);
            final var aip = convert(earksip, tempFolder);
            final Path built;
            try {
                built = aip.build(aipTemp);
            } catch (final IPException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            // copyFolder(sipTemp.resolve(earksip.getId()), built.resolve(IPConstants.SUBMISSION), StandardCopyOption.REPLACE_EXISTING);
            zip(aipTemp, out);
            return aip;
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        } catch (final ParseException ex) {
            throw new RuntimeException(ex);
        } finally {
            deleteIfExists(tempFolder);
        }
    }

    /**
     * Convert a SIP into an AIP without writing anywhere. When using this method
     * you will need to call {@link AIP#build(Path)} yourself to build the AIP
     * and then remove the tempFolder after the AIP has been built.
     *
     * @param sip The SIP to convert into an AIP
     * @param tempFolder A temporary folder used for storing some files until the AIP has been built
     * @return An AIP with all necessary data that has not yet been built
     * @see #convert(Path, OutputStream)
     */
    public AIP convert(final IPInterface sip, final Path tempFolder) {
        try {
            final AIP aip = new EARKAIP(new BasicAIP(UUID.randomUUID().toString(), IPContentType.getMIXED()));
            aip.setProfile(sip.getProfile());

            // Not needed if there is no changes compared to the SIP
            /* for (IPRepresentation representation : earksip.getRepresentations()) {
	            aip.addRepresentation(representation);
	        } */

            for (final var descriptiveMetadatum : sip.getDescriptiveMetadata()) {
                aip.addDescriptiveMetadata(descriptiveMetadatum);
            }

            final var schemas = sip.getBasePath().resolve(IPConstants.SCHEMAS);
            if (Files.exists(schemas)) {
                try (final var stream = Files.list(schemas)) {
                    stream.forEach(p -> aip.addSchema(new IPFile(p)));
                }
            }

            if (aip.getSchemas().isEmpty()) {
                addDefaultSchemas(aip.getSchemas(), tempFolder);
            }

            final Path sipZip = tempFolder.resolve(sip.getId() + ".zip");
            try (var stream = Files.newOutputStream(sipZip)) {
                zip(sip.getBasePath(), stream);
            }
            aip.addSubmission(new IPFile(sipZip));

            addPreservationMetadata(aip, tempFolder);
            aip.addAgent(CREATOR_AGENT);

            return aip;
        } catch (final IPException | JAXBException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void addPreservationMetadata(final AIP aip, final Path tempFolder) throws JAXBException, IPException, IOException {
        final var premis = new PremisComplexType();
        final var agentComplexType = new AgentComplexType();
        final var agentType = new StringPlusAuthority();
        agentType.setValue("Software");
        agentComplexType.setAgentType(agentType);
        final var agentName = new StringPlusAuthority();
        agentName.setValue("Yksa");
        agentComplexType.getAgentName().add(agentName);
        premis.getAgent().add(agentComplexType);
        final var premisTemp = Files.createTempFile(tempFolder, "premis", aip.getId());
        JAXBContext.newInstance(PREMIS_PACKAGE).createMarshaller().marshal(new ObjectFactory().createPremis(premis), premisTemp.toFile());
        aip.addPreservationMetadata(new IPMetadata(new IPFile(premisTemp, PREMIS_FILENAME), new MetadataType(MetadataType.MetadataTypeEnum.PREMIS)));
        final var premisSchema = Files.createTempFile(tempFolder, "", PREMIS_SCHEMA);
        try (final var out = Files.newOutputStream(premisSchema)) {
            Objects.requireNonNull(SIPToAIPConverter.class.getClassLoader().getResourceAsStream(PREMIS_CLASSPATH), "Could not find resource " + PREMIS_CLASSPATH).transferTo(out);
        }
        aip.addSchema(new IPFile(premisSchema, PREMIS_SCHEMA));
    }

}
