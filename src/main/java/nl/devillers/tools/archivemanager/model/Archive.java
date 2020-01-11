package nl.devillers.tools.archivemanager.model;

import lombok.Data;

import java.nio.file.Path;

@Data
public class Archive {
    private Path root;
    private Path index;

    public Boolean isIndexed() {
        return index.toFile().exists() && index.toFile().length() > 0;
    }
}
