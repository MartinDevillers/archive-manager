package nl.devillers.tools.archivemanager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FingerprinterTest {

    Path files = Paths.get("src","test","resources", "files");

    /**
     * An empty file should produce the (famous) empty SHA-1 hash
     *
     * @see <a href="https://stackoverflow.com/a/12913925/546967">How to check if PHP field is empty when using SHA1</a>
     */
    @Test
    void empty_file_produces_famous_empty_sha1_hash() {
        // Act
        Optional<String> fingerprint = Fingerprinter.fingerprint(files.resolve("0.txt"));

        // Assert
        assertTrue(fingerprint.isPresent());
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", fingerprint.get());
    }

    /**
     * Different input should files should produce different hashes. Also, up until the {@link Fingerprinter#CHUNK_SIZE},
     * the hash of a file should be the default SHA-1 hash value.
     *
     * @see <a href="https://emn178.github.io/online-tools/sha1_checksum.html">SHA1 File Checksum</a>
     * @see <a href="https://randomtextgenerator.com/">Random Text Generator</a>
     * @param path Location of the test file to be fingerprinted
     * @param expected Expected hash
     */
    @ParameterizedTest
    @CsvSource({
        "506.txt, deb075a04ad68f87c78578cb6bf43df56ddab3d7",
        "931.txt, 8d17fa36fc309c782458e1691a686d0a41925d36",
        "1083.txt, 57d558c53217bbf1f84f34d8d4a49928da9c091e",
        "1440.txt, d7b8ed2171a6d24a2276b50c7ed10d4ff6bc760c",
        "2727.txt, 1277916327f0a354ee9f5db76f664228907343b3",
        "3855.txt, d2f7c41a4aa199e6471492590370f1fd72c85047"
    })
    void different_files_should_produce_different_hashes(String path, String expected) {
        // Act
        Optional<String> actual = Fingerprinter.fingerprint(files.resolve(path));

        // Assert
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    /**
     * Files that are bigger than the chunksize will produce identical hashes if they contain repeating content.
     *
     * @param path Location of the test file to be fingerprinted
     * @param expected Expected hash
     */
    @ParameterizedTest
    @CsvSource({
        "A2047.txt, 56fd594cb093f596310dc5c2b8a0633c5d75f9d3",
        "A2048.txt, dd3d1453b5235ab92ba8eb55388caba6da4d16d1",
        "A2049.txt, dd3d1453b5235ab92ba8eb55388caba6da4d16d1", // Note: this hash is the same for A2048.txt
    })
    void repeating_file_content_should_produce_same_hash_beyond_chunksize(String path, String expected) {
        // Act
        Optional<String> actual = Fingerprinter.fingerprint(files.resolve(path));

        // Assert
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }

    /**
     * Files that are identical to each other, except for the start or the beginning, will produce different hashes.
     * However, if the only difference is exactly in between the start of the beginning, the hash will be identical.
     *
     * @param path Location of the test file to be fingerprinted
     * @param expected Expected hash
     */
    @ParameterizedTest
    @CsvSource({
        "A2049.txt, dd3d1453b5235ab92ba8eb55388caba6da4d16d1",
        "A2049-B-start.txt, fa656fe95ab62d4ebcd7bdf7c313fda3bbb9113c",
        "A2049-B-mid.txt, dd3d1453b5235ab92ba8eb55388caba6da4d16d1", // Note: this hash is the same for A2049.txt
        "A2049-B-end.txt, 5ab9c5c48aaf44d6a67ea6d23887dc7aabbed275",
    })
    void fingerprint_chunksize_start_mid_end(String path, String expected) {
        // Act
        Optional<String> actual = Fingerprinter.fingerprint(files.resolve(path));

        // Assert
        assertTrue(actual.isPresent());
        assertEquals(expected, actual.get());
    }
}