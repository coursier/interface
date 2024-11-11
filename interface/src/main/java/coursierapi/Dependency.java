package coursierapi;

import coursier.internal.api.ApiHelper;

import java.io.Serializable;
import java.util.*;

public final class Dependency implements Serializable {

    private Module module;
    private String version;
    private final Set<Map.Entry<String, String>> exclusions;
    private String configuration;
    private Publication publication;
    private boolean transitive;


    private Dependency(Module module, String version) {
        this.module = module;
        this.version = version;
        this.exclusions = new HashSet<>();
        this.configuration = "";
        this.publication = null;
        this.transitive = true;
    }

    public static Dependency of(Module module, String version) {
        return new Dependency(module, version);
    }
    public static Dependency of(String organization, String name, String version) {
        return new Dependency(Module.of(organization, name), version);
    }
    public static Dependency of(Dependency dependency) {
        return new Dependency(dependency.getModule(), dependency.getVersion())
                .withExclusion(dependency.getExclusions())
                .withConfiguration(dependency.getConfiguration())
                .withType(dependency.getType())
                .withClassifier(dependency.getClassifier())
                .withPublication(dependency.getPublication())
                .withTransitive(dependency.isTransitive());
    }

    public static Dependency parse(String dep, ScalaVersion scalaVersion) {
        return ApiHelper.parseDependency(dep, scalaVersion.getVersion());
    }


    public Dependency withModule(Module module) {
        this.module = module;
        return this;
    }

    public Dependency withVersion(String version) {
        this.version = version;
        return this;
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

    public Dependency withConfiguration(String configuration) {
        this.configuration = configuration;
        return this;
    }

    public Dependency withType(String type) {
        if (publication == null)
          publication = new Publication("", type, "", "");
        else
          publication = publication.withType(type);
        if (publication.isEmpty())
          publication = null;
        return this;
    }

    public Dependency withClassifier(String classifier) {
        if (publication == null)
          publication = new Publication("", "", "", classifier);
        else
          publication = publication.withClassifier(classifier);
        if (publication.isEmpty())
          publication = null;
        return this;
    }

    public Dependency withPublication(Publication publication) {
        this.publication = publication;
        return this;
    }

    public Dependency withTransitive(boolean transitive) {
        this.transitive = transitive;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dependency)) return false;
        Dependency that = (Dependency) o;
        return transitive == that.transitive &&
                Objects.equals(module, that.module) &&
                Objects.equals(version, that.version) &&
                Objects.equals(exclusions, that.exclusions) &&
                Objects.equals(configuration, that.configuration) &&
                Objects.equals(publication, that.publication);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                module,
                version,
                exclusions,
                configuration,
                publication,
                transitive);
    }

    @Override
    public String toString() {
        return "Dependency{" +
                "module=" + module +
                ", version='" + version + '\'' +
                ", exclusions=" + exclusions +
                ", configuration='" + configuration + '\'' +
                ", publication=" + publication +
                ", transitive=" + transitive +
                '}';
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

    public String getConfiguration() {
        return configuration;
    }

    public String getType() {
        if (publication == null) return "";
        return publication.getType();
    }

    public String getClassifier() {
        if (publication == null) return "";
        return publication.getClassifier();
    }

    public Publication getPublication() {
        return publication;
    }

    public boolean isTransitive() {
        return transitive;
    }
}
