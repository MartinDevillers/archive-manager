package nl.devillers.tools.archivemanager;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import nl.devillers.tools.archivemanager.model.Archive;
import nl.devillers.tools.archivemanager.model.FileSummary;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class Indexer {

    private static final FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();

    @SneakyThrows
    public Map<Long, List<FileSummary>> readIndex(Archive archive) {
        if(!archive.isIndexed()) createIndex(archive);
        Map<Long, List<FileSummary>> index = (HashMap<Long, List<FileSummary>>)fst.asObject(Files.readAllBytes(archive.getIndex()));
        log.info("Read {} entries from {}", index.size(), archive.getIndex());
        return index;
    }

    @SneakyThrows
    public void createIndex(Archive archive)  {
        log.info("Indexing {}", archive.getRoot());
        IndexingFileVisitor visitor = new IndexingFileVisitor();
        Files.walkFileTree(archive.getRoot(), visitor);
        log.info("Indexed {} files / {} bytes", visitor.getFileCount(), visitor.getByteCount());
        Files.write(archive.getIndex(), fst.asByteArray(visitor.getIndex()), StandardOpenOption.CREATE_NEW);
        log.info("Saved {} entries to {}", visitor.getIndex().size(), archive.getRoot());
    }

    @Data
    static class IndexingFileVisitor extends SimpleFileVisitor<Path> {

        private final HashMap<Long, List<FileSummary>> index = new HashMap<>();
        private final AtomicInteger fileCount = new AtomicInteger();
        private final AtomicLong byteCount = new AtomicLong();

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Optional<String> fingerprint = Fingerprinter.fingerprint(file);
            if(!fingerprint.isPresent()) return FileVisitResult.CONTINUE;
            FileSummary summary = new FileSummary(file.toString(), attrs.size(), fingerprint.get());
            List<FileSummary> bucket = index.computeIfAbsent(attrs.size(), x -> new ArrayList<>());
            bucket.add(summary);
            byteCount.addAndGet(attrs.size());
            if(fileCount.incrementAndGet() % 1000 == 0) log.info("Indexed {} files", fileCount);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            log.warn(String.format("Failed to index file: %s", file), exc);
            return FileVisitResult.CONTINUE;
        }
    }
}
