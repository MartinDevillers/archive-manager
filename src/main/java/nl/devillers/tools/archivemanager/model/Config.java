package nl.devillers.tools.archivemanager.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
@Component
@ConfigurationProperties("config")
public class Config {
    private List<Archive> sources;
    private Archive target;
    private List<Pattern> regexFilters = new ArrayList<>();
    private Boolean ignoreEmptyFiles;
    private ExifFilter exifFilter;
}
