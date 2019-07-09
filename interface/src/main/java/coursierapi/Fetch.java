package coursierapi;

import coursier.internal.api.ApiHelper;
import coursierapi.error.CoursierError;

import java.io.File;
import java.util.*;

public final class Fetch {

    private final List<Dependency> dependencies;
    private final List<Repository> repositories;
    private Cache cache;
    private Boolean mainArtifacts;
    private final Set<String> classifiers;
    private Set<String> artifactTypes;
    private File fetchCache;
    private ResolutionParams resolutionParams;

    private Fetch() {
        dependencies = new ArrayList<>();
        repositories = new ArrayList<>(Arrays.asList(ApiHelper.defaultRepositories()));
        cache = Cache.create();
        mainArtifacts = null;
        classifiers = new HashSet<>();
        artifactTypes = null;
        fetchCache = null;
        resolutionParams = ResolutionParams.create();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Fetch) {
            Fetch other = (Fetch) obj;
            return this.dependencies.equals(other.dependencies) &&
                    this.repositories.equals(other.repositories) &&
                    this.cache.equals(other.cache) &&
                    Objects.equals(this.mainArtifacts, other.mainArtifacts) &&
                    this.classifiers.equals(other.classifiers) &&
                    Objects.equals(this.artifactTypes, other.artifactTypes) &&
                    Objects.equals(this.fetchCache, other.fetchCache) &&
                    this.resolutionParams.equals(other.resolutionParams);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * (37 * (37 * (37 * (37 * (37 * (17 + dependencies.hashCode()) + repositories.hashCode()) + cache.hashCode()) + Objects.hashCode(mainArtifacts)) + classifiers.hashCode()) + Objects.hashCode(fetchCache)) + resolutionParams.hashCode()) + Objects.hashCode(artifactTypes);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Fetch(dependencies=[");
        for (Dependency dep : dependencies) {
            b.append(dep.toString());
            b.append(", ");
        }
        b.append("], repositories=[");
        for (Repository repo : repositories) {
            b.append(repo.toString());
            b.append(", ");
        }
        b.append("], cache=");
        b.append(cache.toString());
        if (mainArtifacts != null) {
            b.append(", mainArtifacts=");
            b.append(mainArtifacts.toString());
        }
        b.append(", classifiers=[");
        for (String cl : classifiers) {
            b.append(cl);
            b.append(", ");
        }
        b.append("]");
        if (artifactTypes != null) {
            b.append(", artifactTypes=[");
            boolean first = true;
            for (String t : artifactTypes) {
                if (first)
                    first = false;
                else
                    b.append(", ");
                b.append(t);
            }
            b.append("]");
        }
        if (fetchCache != null) {
            b.append(", fetchCache=");
            b.append(fetchCache.toString());
        }
        b.append(", resolutionParams=");
        b.append(resolutionParams.toString());
        b.append(")");
        return b.toString();
    }

    public static Fetch create() {
        return new Fetch();
    }


    public Fetch addDependencies(Dependency... dependencies) {
        this.dependencies.addAll(Arrays.asList(dependencies));
        return this;
    }

    public Fetch addRepositories(Repository... repositories) {
        this.repositories.addAll(Arrays.asList(repositories));
        return this;
    }

    public Fetch withRepositories(Repository... repositories) {
        this.repositories.clear();
        this.repositories.addAll(Arrays.asList(repositories));
        return this;
    }

    public Fetch withCache(Cache cache) {
        this.cache = cache;
        return this;
    }

    public Fetch withMainArtifacts(Boolean mainArtifacts) {
        this.mainArtifacts = mainArtifacts;
        return this;
    }
    public Fetch withMainArtifacts(boolean mainArtifacts) {
        this.mainArtifacts = mainArtifacts;
        return this;
    }
    public Fetch withMainArtifacts() {
        this.mainArtifacts = true;
        return this;
    }

    public Fetch withClassifiers(Set<String> classifiers) {
        this.classifiers.clear();
        this.classifiers.addAll(classifiers);
        return this;
    }
    public Fetch addClassifiers(String... classifiers) {
        this.classifiers.addAll(Arrays.asList(classifiers));
        return this;
    }

    public Fetch withArtifactTypes(Set<String> types) {
        if (types == null) {
            this.artifactTypes = null;
        } else {
            this.artifactTypes = new HashSet<>(types);
        }
        return this;
    }
    public Fetch addArtifactTypes(String... types) {
        if (this.artifactTypes == null)
            this.artifactTypes = new HashSet<>();
        this.artifactTypes.addAll(Arrays.asList(types));
        return this;
    }

    public Fetch withFetchCache(File fetchCache) {
        this.fetchCache = fetchCache;
        return this;
    }

    public Fetch withResolutionParams(ResolutionParams resolutionParams) {
        this.resolutionParams = ResolutionParams.of(resolutionParams);
        return this;
    }

    public List<Dependency> getDependencies() {
        return Collections.unmodifiableList(dependencies);
    }

    public List<Repository> getRepositories() {
        return Collections.unmodifiableList(repositories);
    }

    public Cache getCache() {
        return cache;
    }

    public Boolean getMainArtifacts() {
        return mainArtifacts;
    }

    public Set<String> getClassifiers() {
        return Collections.unmodifiableSet(classifiers);
    }

    public Set<String> getArtifactTypes() {
        if (artifactTypes == null)
            return null;
        else
          return Collections.unmodifiableSet(artifactTypes);
    }

    public File getFetchCache() {
        return fetchCache;
    }

    public ResolutionParams getResolutionParams() {
        return resolutionParams;
    }

    public List<File> fetch() throws CoursierError {
        FetchResult result = fetchResult();
        return result.getFiles();
    }

    public FetchResult fetchResult() throws CoursierError {
        return ApiHelper.doFetch(this);
    }

}
