package nl.devillers.tools.archivemanager.model;

import lombok.Data;

import java.util.List;

@Data
public class ExifFilter {
    private Boolean enabled;
    private List<String> extensions;
}
