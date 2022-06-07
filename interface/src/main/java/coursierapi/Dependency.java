package coursierapi;

import coursier.internal.api.ApiHelper;

import java.io.Serializable;
import java.util.*;

public final class Dependency implements Serializable {

    private Module module;
    private String version;
    private final Set<Map.Entry<String, String>> exclusions;
    private String configuration;
    private String type;
    private String classifier;
    private Publication publication;
    private boolean transitive;


    private Dependency(Module module, String version) {
        this.module = module;
        this.version = version;
        this.exclusions = new HashSet<>();
        this.configuration = "";
        this.type = "";
        this.classifier = "";
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
        this.type = type;
        return this;
    }

    public Dependency withClassifier(String classifier) {
        this.classifier = classifier;
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
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Dependency) {
            Dependency other = (Dependency) obj;
            return this.module.equals(other.module) &&
                    this.version.equals(other.version) &&
                    this.exclusions.equals(other.exclusions) &&
                    this.configuration.equals(other.configuration) &&
                    this.type.equals(other.type) &&
                    this.classifier.equals(other.classifier) &&
                    Objects.equals(this.publication, other.publication) &&
                    this.transitive == other.transitive;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + module.hashCode()) + version.hashCode()) + exclusions.hashCode()) + configuration.hashCode()) + type.hashCode()) + classifier.hashCode()) + Objects.hashCode(publication)) + Boolean.hashCode(transitive);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Dependency(");
        b.append(module.toString());
        b.append(", ");
        b.append(version);
        if (!exclusions.isEmpty()) {
            for (Map.Entry<String, String> e : exclusions) {
                b.append(", exclude=");
                b.append(e.getKey());
                b.append(":");
                b.append(e.getValue());
            }
        }
        if (!configuration.isEmpty()) {
            b.append(", configuration=");
            b.append(configuration);
        }
        if (!type.isEmpty()) {
            b.append(", type=");
            b.append(type);
        }
        if (!classifier.isEmpty()) {
            b.append(", classifier=");
            b.append(classifier);
        }
        if (publication != null) {
            b.append(", publication=");
            b.append(publication);
        }
        if (!transitive) {
            b.append(", intransitive");
        }
        b.append(")");
        return b.toString();
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
        return type;
    }

    public String getClassifier() {
        return classifier;
    }

    public Publication getPublication() {
        return publication;
    }

    public boolean isTransitive() {
        return transitive;
    }
}
