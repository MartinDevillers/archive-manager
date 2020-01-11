package nl.devillers.tools.archivemanager;

import nl.devillers.tools.archivemanager.model.Archive;
import nl.devillers.tools.archivemanager.model.FileSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MappersTest {

    @TempDir
    Path index;

    Path archives = Paths.get("src","test","resources", "archives");

    @Test
    void rightWithoutLeft_different_archives_with_same_files_produces_empty_result() {
        // Arrange
        Mappers mappers = new Mappers();
        Indexer indexer = new Indexer();

        Archive multipleArchive = new Archive();
        multipleArchive.setRoot(archives.resolve("multiple"));
        multipleArchive.setIndex(index.resolve("multiple.fst"));
        Map<Long, List<FileSummary>> multipleIndex = indexer.readIndex(multipleArchive);

        Archive treeArchive = new Archive();
        treeArchive.setRoot(archives.resolve("tree"));
        treeArchive.setIndex(index.resolve("tree.fst"));
        Map<Long, List<FileSummary>> treeIndex = indexer.readIndex(treeArchive);

        // Act
        List<FileSummary> lefty = mappers.rightWithoutLeft(multipleIndex, treeIndex);
        List<FileSummary> righty = mappers.rightWithoutLeft(treeIndex, multipleIndex);

        // Assert
        assertTrue(lefty.isEmpty());
        assertTrue(righty.isEmpty());
    }

    @Test
    void rightWithoutLeft_archives_with_different_files_produces_difference() {
        // Arrange
        Mappers mappers = new Mappers();
        Indexer indexer = new Indexer();

        Archive multipleArchive = new Archive();
        multipleArchive.setRoot(archives.resolve("multiple"));
        multipleArchive.setIndex(index.resolve("multiple.fst"));
        Map<Long, List<FileSummary>> multipleIndex = indexer.readIndex(multipleArchive);

        Archive duplicateArchive = new Archive();
        duplicateArchive.setRoot(archives.resolve("duplicate"));
        duplicateArchive.setIndex(index.resolve("duplicate.fst"));
        Map<Long, List<FileSummary>> duplicateIndex = indexer.readIndex(duplicateArchive);

        // Act
        List<FileSummary> actual = mappers.rightWithoutLeft(duplicateIndex, multipleIndex);

        // Assert
        assertEquals(3, actual.size());
    }
}