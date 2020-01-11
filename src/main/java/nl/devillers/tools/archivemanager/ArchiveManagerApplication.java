package nl.devillers.tools.archivemanager;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.devillers.tools.archivemanager.model.Config;
import nl.devillers.tools.archivemanager.model.FileSummary;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@SpringBootApplication
@Slf4j
@AllArgsConstructor
public class ArchiveManagerApplication implements CommandLineRunner {

	private Config config;
	private Indexer indexer;
	private Mappers mappers;
	private Filters filters;

	@Override
	public void run(String... args) throws Exception {
		// Create/read all indexes
		Map<Long, List<FileSummary>> sources = config.getSources()
				.stream()
				.map(indexer::readIndex)
				.reduce(new HashMap<>(), mappers::accumulator);
		long sourcesFilesCount = sources.values()
				.stream()
				.flatMap(Collection::stream)
				.count();
		log.info("Built master index containing {} entries and {} files", sources.size(), sourcesFilesCount);

		Map<Long, List<FileSummary>> target = indexer.readIndex(config.getTarget());

		// Apply filters
		filters.applyFilters(sources);
		//applyFilter2(sources);
		filters.applyFilters(target);

		// Calculate missing files
		List<FileSummary> missing = mappers.rightWithoutLeft(target, sources);
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
