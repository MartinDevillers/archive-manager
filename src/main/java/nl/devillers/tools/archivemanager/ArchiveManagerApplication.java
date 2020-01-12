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
		filters.applyFilters(target);
		if(config.getExifFilter().getEnabled()) {
			filters.applyExifFilter(sources);
		}

		// Calculate missing files
		List<FileSummary> missing = mappers.rightWithoutLeft(target, sources);
		log.info("Missing {} of {} files", missing.size(), sourcesFilesCount);
		Path missingOutput =  Paths.get(String.format("missing-%s.txt", Instant.now().toEpochMilli()));
		persistPaths(missingOutput, missing);

		// Detect duplicates
		List<List<FileSummary>> duplicates = mappers.duplicates(target);
		log.info("Found {} duplicates", duplicates.size());
		Path duplicatesOutput =  Paths.get(String.format("duplicates-%s.txt", Instant.now().toEpochMilli()));
		persistNestedPaths(duplicatesOutput, duplicates);
	}

	private void persistPaths(Path outputFile, List<FileSummary> files) throws IOException {
		List<String> paths = files.stream().map(FileSummary::getPath).collect(Collectors.toList());
		Files.write(outputFile, paths, StandardOpenOption.CREATE_NEW);
	}

	private void persistNestedPaths(Path outputFile, List<List<FileSummary>> files) throws IOException {
		List<String> lines = files.stream()
				.map(x -> ">".concat(x.stream()
						.map(FileSummary::getPath)
						.collect(Collectors.joining(System.lineSeparator().concat(" ")))))
				.collect(Collectors.toList());
		Files.write(outputFile, lines, StandardOpenOption.CREATE_NEW);
	}

	public static void main(String[] args) {
		SpringApplication.run(ArchiveManagerApplication.class, args);
	}
}
