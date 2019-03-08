package coursier.api;

import coursier.internal.api.ApiHelper;

import java.io.File;
import java.util.*;

public final class Fetch {

    private final List<Dependency> dependencies;
    private final List<Repository> repositories;
    private Cache cache;
    private Boolean mainArtifacts;
    private final Set<String> classifiers;
    private File fetchCache;

    private Fetch() {
        dependencies = new ArrayList<>();
        repositories = new ArrayList<>(Arrays.asList(ApiHelper.defaultRepositories()));
        cache = Cache.create();
        mainArtifacts = null;
        classifiers = new HashSet<>();
        fetchCache = null;
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

    public Fetch withFetchCache(File fetchCache) {
        this.fetchCache = fetchCache;
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

    public File getFetchCache() {
        return fetchCache;
    }

    public List<File> fetch() {
        File[] files = ApiHelper.doFetch(this);
        return Arrays.asList(files);
    }

}
