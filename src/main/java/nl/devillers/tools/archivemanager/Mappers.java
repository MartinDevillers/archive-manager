package nl.devillers.tools.archivemanager;

import nl.devillers.tools.archivemanager.model.FileSummary;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Mappers {

    public Map<Long, List<FileSummary>> accumulator(Map<Long, List<FileSummary>> accumulate, Map<Long, List<FileSummary>> next) {
        next.entrySet().forEach(x -> accumulate.merge(x.getKey(), x.getValue(), this::concatenate));
        return accumulate;
    }

    private List<FileSummary> concatenate(List<FileSummary> left, List<FileSummary> right) {
        return Stream.of(left, right)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<FileSummary> rightWithoutLeft(Map<Long, List<FileSummary>> left, Map<Long, List<FileSummary>> right) {
        return right.values()
                .stream()
                .flatMap(Collection::stream)
                .filter(x -> !left.containsKey(x.getSize()) || left.get(x.getSize()).stream().noneMatch(y -> x.getFingerprint().equals(y.getFingerprint())))
                .collect(Collectors.toList());
    }

    public List<List<FileSummary>> duplicates(Map<Long, List<FileSummary>> index) {
        return index.entrySet()
                .stream()
                .filter(x -> x.getValue().size() > 1)
                .flatMap(x -> duplicates(x.getValue()).entrySet().stream())
                .filter(x -> x.getValue().size() > 1)
                .map(x -> x.getValue())
                .collect(Collectors.toList());
    }

    private Map<String, List<FileSummary>> duplicates(List<FileSummary> summaries) {
        Map<String, List<FileSummary>> fingerprintedIndex = new HashMap<>();
        for (FileSummary summary : summaries) {
            List<FileSummary> bucket = fingerprintedIndex.computeIfAbsent(summary.getFingerprint(), x -> new ArrayList<>());
            bucket.add(summary);
        }
        return fingerprintedIndex;
    }
}
