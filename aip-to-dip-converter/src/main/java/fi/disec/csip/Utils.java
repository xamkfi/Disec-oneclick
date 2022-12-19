package fi.disec.csip;

import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.IPFile;
import org.roda_project.commons_ip2.model.IPFileInterface;
import org.roda_project.commons_ip2.model.impl.eark.EARKSIP;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

final class Utils {

    private Utils() {
    }

    static void zip(final Path sourceDirPath, final OutputStream out) {
        try (final var zs = new ZipOutputStream(out); final var stream = Files.walk(sourceDirPath)) {
            stream.filter(path -> !Files.isDirectory(path)).forEach(path -> {
                final var zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                try {
                    zs.putNextEntry(zipEntry);
                    Files.copy(path, zs);
                    zs.closeEntry();
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void copyFolder(final Path source, final Path target, final CopyOption... options) {
        try {
            Files.walkFileTree(source, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs) throws IOException {
                    Files.createDirectories(target.resolve(source.relativize(dir).toString()));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.copy(file, target.resolve(source.relativize(file).toString()), options);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static boolean deleteIfExists(final Path fileOrFolder) {
        if (fileOrFolder == null) {
            return false;
        }
        try {
            if (!Files.isDirectory(fileOrFolder)) {
                return Files.deleteIfExists(fileOrFolder);
            }
            try (final var stream = Files.walk(fileOrFolder)) {
                return stream.sorted(Comparator.reverseOrder()).map(Path::toFile).map(File::delete).reduce(true, (a, b) -> a && b);
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void addDefaultSchemas(final Collection<IPFileInterface> schemas, final Path buildDir) throws InterruptedException, IOException {
        final var earkCsipSchema = org.roda_project.commons_ip2.utils.Utils.copyResourceFromClasspathToDir(EARKSIP.class, buildDir, IPConstants.SCHEMA_EARK_CSIP_FILENAME, IPConstants.SCHEMA_EARK_CSIP_RELATIVE_PATH_FROM_RESOURCES);
        schemas.add(new IPFile(earkCsipSchema, IPConstants.SCHEMA_EARK_CSIP_FILENAME));
        final var earkSipSchema = org.roda_project.commons_ip2.utils.Utils.copyResourceFromClasspathToDir(EARKSIP.class, buildDir, IPConstants.SCHEMA_EARK_SIP_FILENAME, IPConstants.SCHEMA_EARK_SIP_RELATIVE_PATH_FROM_RESOURCES);
        schemas.add(new IPFile(earkSipSchema, IPConstants.SCHEMA_EARK_SIP_FILENAME));
        final var metsSchema = org.roda_project.commons_ip2.utils.Utils.copyResourceFromClasspathToDir(EARKSIP.class, buildDir, IPConstants.SCHEMA_METS_FILENAME_WITH_VERSION, IPConstants.SCHEMA_METS_RELATIVE_PATH_FROM_RESOURCES);
        schemas.add(new IPFile(metsSchema, IPConstants.SCHEMA_METS_FILENAME_WITH_VERSION));
        final var xlinkSchema = org.roda_project.commons_ip2.utils.Utils.copyResourceFromClasspathToDir(EARKSIP.class, buildDir, IPConstants.SCHEMA_XLINK_FILENAME, IPConstants.SCHEMA_XLINK_RELATIVE_PATH_FROM_RESOURCES);
        schemas.add(new IPFile(xlinkSchema, IPConstants.SCHEMA_XLINK_FILENAME));
    }

}
