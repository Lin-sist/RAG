package com.enterprise.rag.admin.kb.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileSystemIndexInputStoreTest {

    @TempDir
    Path root;

    @Test
    void inputPublishedByOneInstanceCanBeVerifiedAndReadByAnotherInstance() throws Exception {
        byte[] content = "durable-index-input".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        FileSystemIndexInputStore firstInstance = new FileSystemIndexInputStore(root);

        StoredIndexInput stored = firstInstance.put(new ByteArrayInputStream(content));

        assertFalse(Path.of(stored.storageKey()).isAbsolute());

        FileSystemIndexInputStore restartedInstance = new FileSystemIndexInputStore(root);
        try (var input = restartedInstance.openVerified(
                stored.storageKey(), stored.sizeBytes(), stored.sha256())) {
            assertArrayEquals(content, input.readAllBytes());
        }
    }

    @Test
    void traversalKeyIsRejectedEvenWhenItNormalizesBackInsideTheRoot() {
        byte[] content = "safe-content".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root);
        StoredIndexInput stored = store.put(new ByteArrayInputStream(content));
        String traversalKey = "objects/../" + stored.storageKey();

        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> store.openVerified(traversalKey, stored.sizeBytes(), stored.sha256()));

        assertEquals("INDEX_INPUT_UNAVAILABLE", error.getErrorCode());
    }

    @Test
    void replacedInputIsRejectedBeforeItCanBeConsumed() throws Exception {
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root);
        StoredIndexInput stored = store.put(new ByteArrayInputStream("original".getBytes()));
        java.nio.file.Files.writeString(root.resolve(stored.storageKey()), "tampered");

        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> store.openVerified(stored.storageKey(), stored.sizeBytes(), stored.sha256()));

        assertEquals("INDEX_INPUT_CORRUPT", error.getErrorCode());
    }

    @Test
    void deleteIsIdempotent() {
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root);
        StoredIndexInput stored = store.put(new ByteArrayInputStream("delete-me".getBytes()));

        assertEquals(IndexInputStore.DeleteResult.DELETED, store.delete(stored.storageKey()));
        assertEquals(IndexInputStore.DeleteResult.ALREADY_MISSING, store.delete(stored.storageKey()));
    }

    @Test
    void failedPublishDoesNotLeavePartialStagingOrFinalObject() throws Exception {
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root);
        Files.delete(root.resolve("objects"));
        Files.writeString(root.resolve("objects"), "blocked");

        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> store.put(new ByteArrayInputStream("partial".getBytes())));

        assertEquals("INDEX_INPUT_WRITE_FAILED", error.getErrorCode());
        try (var stagingFiles = Files.list(root.resolve("staging"))) {
            assertEquals(0L, stagingFiles.count());
        }
    }

    @Test
    void startupFailsWhenConfiguredRootCannotBeUsedAsDirectory() throws Exception {
        Path invalidRoot = root.resolve("not-a-directory");
        Files.writeString(invalidRoot, "blocked");

        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> new FileSystemIndexInputStore(invalidRoot));

        assertEquals("INDEX_INPUT_WRITE_FAILED", error.getErrorCode());
    }

    @Test
    void storeRejectsInputAboveItsOwnSizeLimitAndRemovesStagingFile() throws Exception {
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root, 4L, 0L);

        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> store.put(new ByteArrayInputStream("12345".getBytes())));

        assertEquals("INDEX_INPUT_TOO_LARGE", error.getErrorCode());
        try (var stagingFiles = Files.list(root.resolve("staging"))) {
            assertEquals(0L, stagingFiles.count());
        }
    }

    @Test
    void startupRejectsRootBelowConfiguredUsableSpaceFloor() {
        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> new FileSystemIndexInputStore(root, 1024L, Long.MAX_VALUE));

        assertEquals("INDEX_INPUT_WRITE_FAILED", error.getErrorCode());
    }

    @Test
    void absoluteKeyIsRejectedWithoutLeakingTheRawMarker() {
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root);
        String marker = "raw-secret-marker.bin";
        Path absoluteKey = root.resolve(marker).toAbsolutePath();

        IndexInputStorageException error = assertThrows(
                IndexInputStorageException.class,
                () -> store.openVerified(absoluteKey.toString(), 1L, "unused"));

        assertEquals("INDEX_INPUT_UNAVAILABLE", error.getErrorCode());
        assertFalse(error.getMessage().contains(marker));
    }

    @Test
    void symlinkedObjectDirectoryIsRejected() throws Exception {
        FileSystemIndexInputStore store = new FileSystemIndexInputStore(root);
        Path outside = root.resolveSibling(root.getFileName() + "-outside");
        java.nio.file.Files.createDirectories(outside);
        Path outsideFile = outside.resolve("escaped.bin");
        java.nio.file.Files.writeString(outsideFile, "escaped");
        java.nio.file.Files.delete(root.resolve("objects"));
        createDirectoryLink(root.resolve("objects"), outside);

        try {
            IndexInputStorageException error = assertThrows(
                    IndexInputStorageException.class,
                    () -> store.openVerified("objects/escaped.bin", 7L,
                            "92c4f8ac8a99a9e47e017240a4fbe4f6d832f0ddf6170a57e025bc04f026cd0c"));

            assertEquals("INDEX_INPUT_UNAVAILABLE", error.getErrorCode());
        } finally {
            java.nio.file.Files.deleteIfExists(outsideFile);
            java.nio.file.Files.deleteIfExists(outside);
        }
    }

    private static void createDirectoryLink(Path link, Path target) throws Exception {
        try {
            java.nio.file.Files.createSymbolicLink(link, target);
        } catch (java.nio.file.FileSystemException e) {
            if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
                throw e;
            }
            Process process = new ProcessBuilder(
                    "cmd", "/c", "mklink", "/J", link.toString(), target.toString())
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("Unable to create test directory link");
            }
        }
    }
}
