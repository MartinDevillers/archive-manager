package nl.devillers.tools.archivemanager;

import com.drew.imaging.FileType;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import nl.devillers.tools.archivemanager.model.Config;
import nl.devillers.tools.archivemanager.model.FileSummary;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class Filters {

    @NonNull Config config;

    public void applyFilters(Map<Long, List<FileSummary>> index) {
        if(config.getIgnoreEmptyFiles()) {
            index.values().forEach(x -> x.removeIf(y -> y.getSize() == 0));
        }
        index.values().forEach(x -> x.removeIf(y -> !regexFilter(y)));
    }

    private Boolean regexFilter(FileSummary file) {
        return config.getRegexFilters().stream().anyMatch(x -> x.matcher(file.getPath()).find());
    }

    public void applyExifFilter(Map<Long, List<FileSummary>> index) {

        index.values().forEach(x -> x.removeIf(y -> !exifFilter(y)));
    }

    private Boolean exifFilter(FileSummary file) {
        if(isExifSupportedFileType(file)) {
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(new File(file.getPath()));
                ExifIFD0Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
                return directory != null && (directory.containsTag(ExifIFD0Directory.TAG_MAKE) || directory.containsTag(ExifIFD0Directory.TAG_MODEL));
                //return metadata.containsDirectoryOfType(ExifIFD0Directory.class) || metadata.containsDirectoryOfType(ExifSubIFDDirectory.class);
            } catch (Exception e) {
                //log.warn("Error reading EXIF data", e);
            }
        }
        return false;
    }

    private Boolean isExifSupportedFileType(FileSummary file) {
        return getConfiguredExifExtensions().stream().anyMatch(x -> StringUtils.endsWithIgnoreCase(file.getPath(), ".".concat(x)));
    }

    private List<String> getConfiguredExifExtensions() {
        Boolean allExtensions = config.getExifFilter().getExtensions().stream().anyMatch(x -> "*".equals(x));
        if(allExtensions) {
            return EnumSet.allOf(FileType.class)
                    .stream()
                    .map(FileType::getAllExtensions)
                    .flatMap(Arrays::stream)
                    .collect(Collectors.toList());
        }
        return config.getExifFilter().getExtensions();
    }
}
