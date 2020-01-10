package nl.devillers.tools.archivemanager;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
@Component
@ConfigurationProperties("config")
public class Config {
    // private Path source;
    private List<Archive> sources;
    private Archive target;
    private List<Pattern> regexFilters = new ArrayList<>();
    private Boolean ignoreEmptyFiles;

    @Data
    static class Archive {
        private Path root;
        private Path index;

        public Boolean isIndexed() {
            return index.toFile().exists() && index.toFile().length() > 0;
        }
    }
}
