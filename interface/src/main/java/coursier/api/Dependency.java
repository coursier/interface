package coursier.api;

import java.io.Serializable;

public final class Dependency implements Serializable {

    private final Module module;
    private final String version;


    private Dependency(Module module, String version) {
        this.module = module;
        this.version = version;
    }

    public static Dependency of(Module module, String version) {
        return new Dependency(module, version);
    }
    public static Dependency of(String organization, String name, String version) {
        return new Dependency(Module.of(organization, name), version);
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
}
