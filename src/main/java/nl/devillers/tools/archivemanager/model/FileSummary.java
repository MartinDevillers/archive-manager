package nl.devillers.tools.archivemanager.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class FileSummary implements Serializable {
    private String path;
    private Long size;
    private String fingerprint;
}
