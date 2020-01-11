package nl.devillers.tools.archivemanager;

import nl.devillers.tools.archivemanager.model.Archive;
import nl.devillers.tools.archivemanager.model.FileSummary;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class IndexerTest {

    @TempDir
    Path index;

    Path archives = Paths.get("src","test","resources", "archives");

    @Test
    void readIndex_empty() {
        // Arrange
        Indexer indexer = new Indexer();
        Archive archive = new Archive();
        archive.setRoot(archives.resolve("empty"));
        archive.setIndex(index.resolve("index.fst"));
        Map<Long, List<FileSummary>> expected = Collections.emptyMap();

        // Act
        Map<Long, List<FileSummary>> actual = indexer.readIndex(archive);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void readIndex_single() {
        // Arrange
        Indexer indexer = new Indexer();
        Archive archive = new Archive();
        Path single = archives.resolve("single");
        archive.setRoot(single);
        archive.setIndex(index.resolve("index.fst"));
        FileSummary fileSummary = new FileSummary(single.resolve("gibberish.txt").toString(), 3599L, "ba5e28b1e872e9162da643e5845504cece08f005");
        Map<Long, List<FileSummary>> expected = Collections.singletonMap(3599L, Lists.list(fileSummary));

        // Act
        Map<Long, List<FileSummary>> actual = indexer.readIndex(archive);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void readIndex_multiple() {
        // Arrange
        Indexer indexer = new Indexer();
        Archive archive = new Archive();
        Path multiple = archives.resolve("multiple");
        archive.setRoot(multiple);
        archive.setIndex(index.resolve("index.fst"));
        Map<Long, List<FileSummary>> expected = new HashMap<>();
        expected.put(3409L, Lists.list(new FileSummary(multiple.resolve("gibberish1.txt").toString(), 3409L, "91199b9d6528fd4d60f573bd27d80d5bd9284f75")));
        expected.put(3780L, Lists.list(new FileSummary(multiple.resolve("gibberish2.txt").toString(), 3780L, "76cee4e451bdf3945a57412d747bc06f3e4b2270")));
        expected.put(3105L, Lists.list(new FileSummary(multiple.resolve("gibberish3.txt").toString(), 3105L, "bd79e8f6cf2a2b1c10343c1c734b54c17678879c")));

        // Act
        Map<Long, List<FileSummary>> actual = indexer.readIndex(archive);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void readIndex_tree() {
        // Arrange
        Indexer indexer = new Indexer();
        Archive archive = new Archive();
        Path tree = archives.resolve("tree");
        archive.setRoot(tree);
        archive.setIndex(index.resolve("index.fst"));
        Map<Long, List<FileSummary>> expected = new HashMap<>();
        expected.put(3409L, Lists.list(new FileSummary(tree.resolve("gibberish1.txt").toString(), 3409L, "91199b9d6528fd4d60f573bd27d80d5bd9284f75")));
        expected.put(3780L, Lists.list(new FileSummary(tree.resolve(Paths.get("sub", "gibberish2.txt")).toString(), 3780L, "76cee4e451bdf3945a57412d747bc06f3e4b2270")));
        expected.put(3105L, Lists.list(new FileSummary(tree.resolve(Paths.get("sub", "sub", "gibberish3.txt")).toString(), 3105L, "bd79e8f6cf2a2b1c10343c1c734b54c17678879c")));

        // Act
        Map<Long, List<FileSummary>> actual = indexer.readIndex(archive);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    void readIndex_duplicate() {
        // Arrange
        Indexer indexer = new Indexer();
        Archive archive = new Archive();
        Path duplicate = archives.resolve("duplicate");
        archive.setRoot(duplicate);
        archive.setIndex(index.resolve("index.fst"));
        Map<Long, List<FileSummary>> expected = new HashMap<>();
        expected.put(3555L, Lists.list(
                new FileSummary(duplicate.resolve("gibberish_d1.txt").toString(), 3555L, "91ebf629160f7e7c1dd7e3e8751c0aa77141beb2"),
                new FileSummary(duplicate.resolve("gibberish_d2.txt").toString(), 3555L, "91ebf629160f7e7c1dd7e3e8751c0aa77141beb2"))
        );

        // Act
        Map<Long, List<FileSummary>> actual = indexer.readIndex(archive);

        // Assert
        assertEquals(expected, actual);
    }
}