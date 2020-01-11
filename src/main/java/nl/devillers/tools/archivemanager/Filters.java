package nl.devillers.tools.archivemanager;

import com.drew.imaging.FileType;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import nl.devillers.tools.archivemanager.model.Config;
import nl.devillers.tools.archivemanager.model.FileSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class Filters {

    @Autowired
    Config config;

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
        //List<String> exifFileTypes = Arrays.asList("jpg");
        //List<String> exifFileTypes = Arrays.asList("jpg", "png", "gif", "jpeg", "mp4", "m4a", "m4p", "m4b", "m4r", "m4v", "mov", "qt", "pcx", "ico", "bmp", "avi", "webp", "psd", "wav", "xmp", "tiff", "tif");
        List<String> exifFileTypes = EnumSet.allOf(FileType.class)
                .stream()
                .map(FileType::getAllExtensions)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());

        if(extensionFilter(file, exifFileTypes)) {
            try {
                Metadata metadata = ImageMetadataReader.readMetadata(new File(file.getPath()));
                return metadata.containsDirectoryOfType(ExifIFD0Directory.class) && metadata.getFirstDirectoryOfType(ExifIFD0Directory.class).containsTag(ExifIFD0Directory.TAG_MAKE);
                //return metadata.containsDirectoryOfType(ExifIFD0Directory.class) || metadata.containsDirectoryOfType(ExifSubIFDDirectory.class);
            } catch (Exception e) {
                //log.warn("Error reading EXIF data", e);
            }
        }
        return false;
    }

    private Boolean extensionFilter(FileSummary file, List<String> extensions) {
        return extensions.stream().anyMatch(x -> StringUtils.endsWithIgnoreCase(file.getPath(), ".".concat(x)));
    }
}
