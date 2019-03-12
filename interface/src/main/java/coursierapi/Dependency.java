package coursierapi;

import coursier.internal.api.ApiHelper;

import java.io.Serializable;
import java.util.*;

public final class Dependency implements Serializable {

    private final Module module;
    private final String version;
    private final Set<Map.Entry<String, String>> exclusions;


    private Dependency(Module module, String version) {
        this.module = module;
        this.version = version;
        this.exclusions = new HashSet<>();
    }

    public static Dependency of(Module module, String version) {
        return new Dependency(module, version);
    }
    public static Dependency of(String organization, String name, String version) {
        return new Dependency(Module.of(organization, name), version);
    }

    public static Dependency parse(String dep, ScalaVersion scalaVersion) {
        return ApiHelper.parseDependency(dep, scalaVersion.getVersion());
    }


    public Dependency addExclusion(String organization, String name) {
        this.exclusions.add(new AbstractMap.SimpleImmutableEntry<>(organization, name));
        return this;
    }

    public Dependency withExclusion(Set<Map.Entry<String, String>> exclusions) {
        this.exclusions.clear();
        this.exclusions.addAll(exclusions);
        return this;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Dependency) {
            Dependency other = (Dependency) obj;
            return this.module.equals(other.module) && this.version.equals(other.version);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (17 + module.hashCode()) + version.hashCode();
    }

    @Override
    public String toString() {
        return "Dependency(" + module + ", " + version + ")";
    }


    public Module getModule() {
        return module;
    }

    public String getVersion() {
        return version;
    }

    public Set<Map.Entry<String, String>> getExclusions() {
        return Collections.unmodifiableSet(exclusions);
    }
}
