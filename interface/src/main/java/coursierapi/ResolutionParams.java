package coursierapi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ResolutionParams implements Serializable {

    private Integer maxIterations;
    private final HashMap<Module, String> forceVersions;
    private final HashMap<String, String> forcedProperties;

    private ResolutionParams() {
        maxIterations = null;
        forceVersions = new HashMap<>();
        forcedProperties = new HashMap<>();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResolutionParams) {
            ResolutionParams other = (ResolutionParams) obj;
            return Objects.equals(this.maxIterations, other.maxIterations) &&
                    this.forcedProperties.equals(other.forcedProperties);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * (17 + Objects.hashCode(maxIterations)) + forceVersions.hashCode()) + forcedProperties.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("ResolutionParams(");
        boolean needSep = false;
        if (maxIterations != null) {
            b.append("maxIterations=");
            b.append(maxIterations.toString());
            needSep = true;
        }
        if (!forceVersions.isEmpty()) {
            if (needSep)
                b.append(", ");
            else
                needSep = true;

            b.append("forceVersions=[");
            boolean first = true;
            for (Map.Entry<Module, String> e : forceVersions.entrySet()) {
                if (first)
                    first = false;
                else
                    b.append(", ");
                b.append(e.getKey().toString());
                b.append(":");
                b.append(e.getValue());
            }
            b.append("]");
        }
        if (!forcedProperties.isEmpty()) {
            if (needSep)
                b.append(", ");
            else
                needSep = true;

            b.append("forcedProperties=[");
            boolean first = true;
            for (Map.Entry<String, String> e : forcedProperties.entrySet()) {
                if (first)
                    first = false;
                else
                    b.append(", ");
                b.append(e.getKey());
                b.append("=");
                b.append(e.getValue());
            }
            b.append("]");
        }
        b.append(")");
        return b.toString();
    }

    public static ResolutionParams create() {
        return new ResolutionParams();
    }

    public static ResolutionParams of(ResolutionParams params) {
        return new ResolutionParams()
                .withMaxIterations(params.maxIterations)
                .withForceProperties(params.forcedProperties)
                .withForceVersions(params.forceVersions);
    }

    public ResolutionParams withMaxIterations(Integer maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public ResolutionParams forceProperty(String name, String value) {
        this.forcedProperties.put(name, value);
        return this;
    }

    public ResolutionParams forceProperties(Map<String, String> properties) {
        this.forcedProperties.putAll(properties);
        return this;
    }

    public ResolutionParams withForceProperties(Map<String, String> properties) {
        this.forcedProperties.clear();
        this.forcedProperties.putAll(properties);
        return this;
    }

    public ResolutionParams forceVersion(Module module, String version) {
        this.forceVersions.put(module, version);
        return this;
    }

    public ResolutionParams forceVersions(Map<Module, String> versions) {
        this.forceVersions.putAll(versions);
        return this;
    }

    public ResolutionParams withForceVersions(Map<Module, String> versions) {
        this.forceVersions.clear();
        this.forceVersions.putAll(versions);
        return this;
    }

    public Integer getMaxIterations() {
        return maxIterations;
    }

    public Map<String, String> getForcedProperties() {
        return Collections.unmodifiableMap(forcedProperties);
    }

    public Map<Module, String> getForceVersions() {
        return Collections.unmodifiableMap(forceVersions);
    }
}
