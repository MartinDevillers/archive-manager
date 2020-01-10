package nl.devillers.tools.archivemanager;

import com.drew.imaging.FileType;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
@Slf4j
public class ArchiveManagerApplication implements CommandLineRunner {

	private static final int CHUNK_SIZE = 1024;
	private static final FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();

	@Autowired
	private Config config;

	@Autowired
	private Indexer indexer;

	private Integer fileCount = 0;
	private long byteCount = 0;

	@Override
	public void run(String... args) throws Exception {
		// Create/read all indexes
		Map<Long, List<FileSummary>> sources = config.getSources()
				.stream()
				.map(indexer::readIndex)
				.reduce(new HashMap<>(), this::accumulator);
		long sourcesFilesCount = sources.values().stream().flatMap(Collection::stream).count();
		log.info("Built master index containing {} entries and {} files", sources.size(), sourcesFilesCount);

		Map<Long, List<FileSummary>> target = indexer.readIndex(config.getTarget());

		// Apply filters
		applyFilters(sources);
		//applyFilter2(sources);
		applyFilters(target);

		// Calculate missing files
		List<FileSummary> missing = rightWithoutLeft(target, sources);
		log.info("Missing {} of {} files", missing.size(), sourcesFilesCount);
		Path output =  Paths.get(String.format("missing-%s.txt", Instant.now().toEpochMilli()));
		persistPaths(output, missing);

		// Detect duplicates

		/*
		Set<Map.Entry<String, List<FileSummary>>> duplicates = index.entrySet()
				.stream()
				.filter(x -> x.getKey() > 1024 * 1024 && x.getValue().size() > 1)
				.flatMap(x -> duplicates(x.getValue()).entrySet().stream())
				.collect(Collectors.toSet());
				//.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		log.info("Found {} possible duplicates", duplicates.size());
		 */
	}

	private Map<Long, List<FileSummary>> accumulator(Map<Long, List<FileSummary>> accumulate, Map<Long, List<FileSummary>> next) {
		next.entrySet().forEach(x -> accumulate.merge(x.getKey(), x.getValue(), this::remapper));
		return accumulate;
	}

	private List<FileSummary> remapper(List<FileSummary> left, List<FileSummary> right) {
		return Stream.of(left, right)
				.flatMap(List::stream)
				.collect(Collectors.toList());
	}

	private void applyFilters(Map<Long, List<FileSummary>> index) {
		if(config.getIgnoreEmptyFiles()) {
			index.values().forEach(x -> x.removeIf(y -> y.getSize() == 0));
		}
		index.values().forEach(x -> x.removeIf(y -> !regexFilter(y)));
	}

	private void applyFilter2(Map<Long, List<FileSummary>> index) {
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

	private Boolean regexFilter(FileSummary file) {
		return config.getRegexFilters().stream().anyMatch(x -> x.matcher(file.getPath()).find());
	}

	private Boolean extensionFilter(FileSummary file, List<String> extensions) {
		return extensions.stream().anyMatch(x -> StringUtils.endsWithIgnoreCase(file.getPath(), ".".concat(x)));
	}

	private List<FileSummary> rightWithoutLeft(Map<Long, List<FileSummary>> left, Map<Long, List<FileSummary>> right) {
		return right.values()
				.stream()
				.flatMap(Collection::stream)
				.filter(x -> !left.containsKey(x.getSize()) || left.get(x.getSize()).stream().noneMatch(y -> x.getFingerprint().equals(y.getFingerprint())))
				.collect(Collectors.toList());
	}

	private void persistPaths(Path outputFile, List<FileSummary> files) throws IOException {
		List<String> paths = files.stream().map(FileSummary::getPath).collect(Collectors.toList());
		Files.write(outputFile, paths, StandardOpenOption.CREATE_NEW);
	}

	private void detectDuplicates(Map<Long, List<FileSummary>> index) {
		List<Map.Entry<String, List<FileSummary>>> duplicates = index.entrySet()
				.stream()
				.filter(x -> x.getValue().size() > 1)
				.flatMap(x -> duplicates(x.getValue()).entrySet().stream())
				.filter(x -> x.getValue().size() > 1)
				.collect(Collectors.toList());
		log.info("Found {} duplicates", duplicates.size());
	}

	private Map<String, List<FileSummary>> duplicates(List<FileSummary> summaries) {
		Map<String, List<FileSummary>> fingerprintedIndex = new HashMap<>();
		for (FileSummary summary : summaries) {
			List<FileSummary> bucket = fingerprintedIndex.computeIfAbsent(summary.getFingerprint(), x -> new ArrayList<>());
			bucket.add(summary);
		}
		return fingerprintedIndex;
	}

	public static void main(String[] args) {
		SpringApplication.run(ArchiveManagerApplication.class, args);
	}
}
