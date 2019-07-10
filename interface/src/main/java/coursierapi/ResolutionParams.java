package coursierapi;

import java.io.Serializable;
import java.util.*;

public class ResolutionParams implements Serializable {

    private Integer maxIterations;
    private final HashMap<Module, String> forceVersions;
    private final HashMap<String, String> forcedProperties;
    private final HashSet<String> profiles;
    private final HashSet<Map.Entry<String, String>> exclusions;
    private boolean useSystemOsInfo;
    private boolean useSystemJdkVersion;

    private ResolutionParams() {
        maxIterations = null;
        forceVersions = new HashMap<>();
        forcedProperties = new HashMap<>();
        profiles = new HashSet<>();
        exclusions = new HashSet<>();
        useSystemOsInfo = true;
        useSystemJdkVersion = true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ResolutionParams) {
            ResolutionParams other = (ResolutionParams) obj;
            return Objects.equals(this.maxIterations, other.maxIterations) &&
                    this.forcedProperties.equals(other.forcedProperties) &&
                    this.profiles.equals(other.profiles) && this.exclusions.equals(other.exclusions) &&
                    Objects.equals(this.useSystemOsInfo, other.useSystemOsInfo) &&
                    Objects.equals(this.useSystemJdkVersion, other.useSystemJdkVersion);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * (37 * (37 * (37 * (37 * (17 + Objects.hashCode(maxIterations)) + forceVersions.hashCode()) + forcedProperties.hashCode()) + profiles.hashCode()) + exclusions.hashCode()) + Boolean.hashCode(useSystemOsInfo)) + Boolean.hashCode(useSystemJdkVersion);
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
        if (!profiles.isEmpty()) {
            if (needSep)
                b.append(", ");
            else
                needSep = true;

            b.append("profiles=[");
            boolean first = true;
            for (String profile : profiles) {
                if (first)
                    first = false;
                else
                    b.append(", ");
                b.append(profile);
            }
            b.append("]");
        }
        if (!exclusions.isEmpty()) {
            if (needSep)
                b.append(", ");
            else
                needSep = true;

            b.append("exclusions=[");
            boolean first = true;
            for (Map.Entry<String, String> exclusion : exclusions) {
                if (first)
                    first = false;
                else
                    b.append(", ");
                b.append(exclusion.getKey());
                b.append(":");
                b.append(exclusion.getValue());
            }
            b.append("]");
        }

        if (needSep)
            b.append(", ");
        else
            needSep = true;

        b.append("useSystemOsInfo=");
        b.append(useSystemOsInfo);

        if (needSep)
            b.append(", ");
        else
            needSep = true;

        b.append("useSystemJdkVersion=");
        b.append(useSystemJdkVersion);

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
                .withForceVersions(params.forceVersions)
                .withProfiles(params.profiles)
                .withExclusions(params.exclusions)
                .withUseSystemOsInfo(params.useSystemOsInfo)
                .withUseSystemJdkVersion(params.useSystemJdkVersion);
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

    public ResolutionParams addProfile(String profile) {
        this.profiles.add(profile);
        return this;
    }
    public ResolutionParams removeProfile(String profile) {
        this.profiles.remove(profile);
        return this;
    }
    public ResolutionParams withProfiles(Set<String> profiles) {
        this.profiles.clear();
        this.profiles.addAll(profiles);
        return this;
    }

    public ResolutionParams addExclusion(String organization, String moduleName) {
        // FIXME Make the Map.Entry read-only?
        this.exclusions.add(new AbstractMap.SimpleEntry<>(organization, moduleName));
        return this;
    }
    public ResolutionParams removeExclusion(String organization, String moduleName) {
        this.exclusions.remove(new AbstractMap.SimpleEntry<>(organization, moduleName));
        return this;
    }
    public ResolutionParams withExclusions(Set<Map.Entry<String, String>> exclusions) {
        this.exclusions.clear();
        this.exclusions.addAll(exclusions);
        return this;
    }

    public ResolutionParams withUseSystemOsInfo(boolean useSystemOsInfo) {
        this.useSystemOsInfo = useSystemOsInfo;
        return this;
    }

    public ResolutionParams withUseSystemJdkVersion(boolean useSystemJdkVersion) {
        this.useSystemJdkVersion = useSystemJdkVersion;
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

    public Set<String> getProfiles() {
        return Collections.unmodifiableSet(profiles);
    }

    public Set<Map.Entry<String, String>> getExclusions() {
        return Collections.unmodifiableSet(exclusions);
    }

    public boolean getUseSystemOsInfo() {
        return useSystemOsInfo;
    }

    public boolean getUseSystemJdkVersion() {
        return useSystemJdkVersion;
    }
}
