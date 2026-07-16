package com.enterprise.rag.admin.kb.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public final class FileSystemIndexInputStore implements IndexInputStore {

    private static final int BUFFER_SIZE = 8192;
    private static final long DEFAULT_MAX_INPUT_SIZE_BYTES = 50L * 1024L * 1024L;

    private final Path root;
    private final Path realRoot;
    private final Path stagingRoot;
    private final Path objectRoot;
    private final long maxInputSizeBytes;
    private final long minimumUsableSpaceBytes;

    public FileSystemIndexInputStore(Path root) {
        this(root, DEFAULT_MAX_INPUT_SIZE_BYTES, 0L);
    }

    public FileSystemIndexInputStore(Path root, long maxInputSizeBytes, long minimumUsableSpaceBytes) {
        if (maxInputSizeBytes <= 0L || minimumUsableSpaceBytes < 0L) {
            throw new IllegalArgumentException("Invalid index input storage limits");
        }
        this.root = root.toAbsolutePath().normalize();
        this.stagingRoot = this.root.resolve("staging");
        this.objectRoot = this.root.resolve("objects");
        this.maxInputSizeBytes = maxInputSizeBytes;
        this.minimumUsableSpaceBytes = minimumUsableSpaceBytes;
        try {
            Files.createDirectories(stagingRoot);
            Files.createDirectories(objectRoot);
            this.realRoot = this.root.toRealPath();
            verifyWritableAtomicRoot();
        } catch (IOException e) {
            throw IndexInputStorageException.writeFailed(e);
        }
    }

    private void verifyWritableAtomicRoot() throws IOException {
        ensureUsableSpace();
        String probeName = ".probe-" + UUID.randomUUID();
        Path stagingProbe = stagingRoot.resolve(probeName + ".part");
        Path objectProbe = objectRoot.resolve(probeName);
        try {
            Files.write(stagingProbe, new byte[] { 1 }, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            Files.move(stagingProbe, objectProbe, StandardCopyOption.ATOMIC_MOVE);
        } finally {
            Files.deleteIfExists(stagingProbe);
            Files.deleteIfExists(objectProbe);
        }
    }

    @Override
    public StoredIndexInput put(InputStream input) {
        String objectName = UUID.randomUUID() + ".bin";
        Path staging = stagingRoot.resolve(objectName + ".part");
        Path target = objectRoot.resolve(objectName);
        MessageDigest digest = sha256Digest();
        long size = 0L;

        try {
            ensureUsableSpace();
        } catch (IOException e) {
            throw IndexInputStorageException.writeFailed(e);
        }

        try (InputStream source = input;
                OutputStream output = Files.newOutputStream(
                        staging, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = source.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                if (size > maxInputSizeBytes - read) {
                    throw IndexInputStorageException.tooLarge();
                }
                output.write(buffer, 0, read);
                digest.update(buffer, 0, read);
                size += read;
            }
        } catch (IndexInputStorageException e) {
            deleteQuietly(staging);
            throw e;
        } catch (IOException e) {
            deleteQuietly(staging);
            throw IndexInputStorageException.writeFailed(e);
        }

        try {
            Files.move(staging, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            deleteQuietly(staging);
            throw IndexInputStorageException.writeFailed(e);
        } catch (IOException e) {
            deleteQuietly(staging);
            throw IndexInputStorageException.writeFailed(e);
        }

        return new StoredIndexInput(
                root.relativize(target).toString().replace('\\', '/'),
                size,
                HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public InputStream openVerified(String storageKey, long expectedSizeBytes, String expectedSha256) {
        Path input = resolveStorageKey(storageKey);
        try {
            if (!Files.isRegularFile(input) || Files.isSymbolicLink(input)) {
                throw IndexInputStorageException.unavailable(null);
            }
            long actualSize = Files.size(input);
            String actualSha256 = sha256(input);
            if (actualSize != expectedSizeBytes || !actualSha256.equalsIgnoreCase(expectedSha256)) {
                throw IndexInputStorageException.corrupt();
            }
            return Files.newInputStream(input, StandardOpenOption.READ);
        } catch (IndexInputStorageException e) {
            throw e;
        } catch (IOException e) {
            throw IndexInputStorageException.unavailable(e);
        }
    }

    @Override
    public DeleteResult delete(String storageKey) {
        Path input = resolveStorageKey(storageKey);
        try {
            return Files.deleteIfExists(input) ? DeleteResult.DELETED : DeleteResult.ALREADY_MISSING;
        } catch (IOException e) {
            return DeleteResult.FAILED;
        }
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw IndexInputStorageException.unavailable(null);
        }
        Path key = Path.of(storageKey);
        if (key.isAbsolute()) {
            throw IndexInputStorageException.unavailable(null);
        }
        for (Path segment : key) {
            if ("..".equals(segment.toString()) || ".".equals(segment.toString())) {
                throw IndexInputStorageException.unavailable(null);
            }
        }
        Path resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(objectRoot)) {
            throw IndexInputStorageException.unavailable(null);
        }
        rejectLinkedOrEscapedSegments(resolved);
        return resolved;
    }

    private void rejectLinkedOrEscapedSegments(Path resolved) {
        Path current = root;
        try {
            for (Path segment : root.relativize(resolved)) {
                current = current.resolve(segment);
                if (!Files.exists(current)) {
                    continue;
                }
                if (Files.isSymbolicLink(current) || !current.toRealPath().startsWith(realRoot)) {
                    throw IndexInputStorageException.unavailable(null);
                }
            }
        } catch (IndexInputStorageException e) {
            throw e;
        } catch (IOException e) {
            throw IndexInputStorageException.unavailable(e);
        }
    }

    private static String sha256(Path path) throws IOException {
        MessageDigest digest = sha256Digest();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read);
                }
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private void ensureUsableSpace() throws IOException {
        if (Files.getFileStore(root).getUsableSpace() < minimumUsableSpaceBytes) {
            throw new IOException("Durable index input root is below its usable-space floor");
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable", e);
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // A later reconciliation pass owns cleanup of an unremovable staging file.
        }
    }
}
