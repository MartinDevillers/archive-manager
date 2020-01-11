package nl.devillers.tools.archivemanager;

import nl.devillers.tools.archivemanager.model.FileSummary;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class Mappers {

    public Map<Long, List<FileSummary>> accumulator(Map<Long, List<FileSummary>> accumulate, Map<Long, List<FileSummary>> next) {
        next.entrySet().forEach(x -> accumulate.merge(x.getKey(), x.getValue(), this::remapper));
        return accumulate;
    }

    private List<FileSummary> remapper(List<FileSummary> left, List<FileSummary> right) {
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
}
