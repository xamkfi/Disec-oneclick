package fi.disec.csip;

import org.roda_project.commons_ip.model.ParseException;
import org.roda_project.commons_ip.utils.IPEnums;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip.utils.METSEnums;
import org.roda_project.commons_ip2.model.*;
import org.roda_project.commons_ip2.model.impl.BasicAIP;
import org.roda_project.commons_ip2.model.impl.eark.EARKAIP;
import org.roda_project.commons_ip2.model.impl.eark.EARKSIP;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static fi.disec.csip.Utils.*;


public class AIPToDIPConverter {
    private static final String DIP_PROFILE = "https://earkdip.dilcis.eu/profile/E-ARK-DIP.xml";

    private static final String PREMIS_PACKAGE = "gov.loc.premis.v3";
    private static final String PREMIS_FILENAME = "premis.xml";
    private static final String PREMIS_SCHEMA = "premis.xsd";
    private static final String PREMIS_CLASSPATH = "premis/v3/" + PREMIS_SCHEMA;
    private static final IPAgent CREATOR_AGENT = new IPAgent(
            "YksaAipToDip",
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
            final IPInterface earksip = EARKAIP.parse(sip, sipTemp);
            final var aip = convert(earksip, tempFolder);
            final Path built;
            try {
                built = aip.build(aipTemp);
            } catch (final IPException | InterruptedException e) {
                throw new RuntimeException(e);
            }
            // copyFolder(sipTemp.resolve(earksip.getId()).resolve(IPConstants.SUBMISSION), built.resolve(IPConstants.SUBMISSION), StandardCopyOption.REPLACE_EXISTING);
            aip.setType(IPEnums.IPType.DIP);

            Files.writeString(
                    aipTemp.resolve(aip.getId()).resolve("METS.xml"),
                    Files.readString(aipTemp.resolve(aip.getId()).resolve("METS.xml")).replace("csip:OAISPACKAGETYPE=\"AIP\"", "csip:OAISPACKAGETYPE=\"DIP\"")
            );

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
     * @param aip The SIP to convert into an AIP
     * @param tempFolder A temporary folder used for storing some files until the AIP has been built
     * @return An AIP with all necessary data that has not yet been built
     * @see #convert(Path, OutputStream)
     */
    public AIP convert(final IPInterface aip, final Path tempFolder) {
        try {
            final AIP dip = new EARKAIP(new BasicAIP(UUID.randomUUID().toString(), IPContentType.getMIXED()));
            dip.setType(IPEnums.IPType.AIP);
            dip.setProfile(DIP_PROFILE);

            // Not needed if there is no changes compared to the SIP
            /* for (IPRepresentation representation : earksip.getRepresentations()) {
	            dip.addRepresentation(representation);
	        } */

            for (final var descriptiveMetadatum : aip.getDescriptiveMetadata()) {
                dip.addDescriptiveMetadata(descriptiveMetadatum);
            }

            for (final var preservationMetadatum : aip.getPreservationMetadata()) {
                dip.addPreservationMetadata(preservationMetadatum);
            }

            final var schemas = aip.getBasePath().resolve(IPConstants.SCHEMAS);
            if (Files.exists(schemas)) {
                try (final var stream = Files.list(schemas)) {
                    stream.forEach(p -> dip.addSchema(new IPFile(p)));
                }
            }

            if (dip.getSchemas().isEmpty()) {
                addDefaultSchemas(dip.getSchemas(), tempFolder);
            }

            dip.addAgent(CREATOR_AGENT);

            final Path sipZip = tempFolder.resolve(aip.getId() + ".zip");
            try (var stream = Files.newOutputStream(sipZip)) {
                zip(aip.getBasePath(), stream);
            }
            dip.addSubmission(new IPFile(sipZip));

            return dip;
        } catch (final IPException | InterruptedException e) {
            throw new RuntimeException(e);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
