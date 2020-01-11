package nl.devillers.tools.archivemanager;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@Slf4j
public class Fingerprinter {
    private static final int CHUNK_SIZE = 1024;

    public static Optional<String> fingerprint(Path path) {
        File file = path.toFile();
        byte[] content;
        try {
            if(file.length() > CHUNK_SIZE * 2) {
                content = new byte[2 * CHUNK_SIZE];
                RandomAccessFile raf = new RandomAccessFile(file, "r");
                raf.read(content, 0, CHUNK_SIZE);
                raf.seek(file.length() - CHUNK_SIZE);
                raf.read(content, CHUNK_SIZE, CHUNK_SIZE);
                raf.close();
            } else {
                content = Files.readAllBytes(path);
            }
        } catch (IOException e) {
            log.warn(String.format("Failed to fingerprint file: %s", path), e);
            return Optional.empty();
        }
        return Optional.of(DigestUtils.sha1Hex(content));
    }
}
