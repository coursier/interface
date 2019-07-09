package coursierapi;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class FetchResult implements Serializable {

    private List<Map.Entry<Artifact, File>> artifacts;

    private FetchResult(List<Map.Entry<Artifact, File>> artifacts) {
        this.artifacts = Collections.unmodifiableList(new ArrayList<>(artifacts));
    }

    public static FetchResult of(List<Map.Entry<Artifact, File>> artifacts) {
        return new FetchResult(artifacts);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof FetchResult) {
            FetchResult other = (FetchResult) obj;
            return this.artifacts.equals(other.artifacts);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (17 + artifacts.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("FetchResult(");
        boolean isFirst = true;
        for (Map.Entry<Artifact, File> entry : artifacts) {
            if (isFirst) {
                isFirst = false;
            } else {
                b.append(", ");
            }

            b.append(entry.getKey().toString());
            b.append(": ");
            b.append(entry.getValue().toString());
        }
        b.append(")");
        return b.toString();
    }

    public List<Map.Entry<Artifact, File>> getArtifacts() {
        return artifacts;
    }

    public List<File> getFiles() {
        ArrayList<File> l = new ArrayList<>();
        for (Map.Entry<Artifact, File> entry : artifacts) {
            l.add(entry.getValue());
        }
        return l;
    }
}
