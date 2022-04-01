package coursierapi;

import coursier.internal.api.ApiHelper;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

public final class Cache {

    private ExecutorService pool;
    private File location;
    private Logger logger;

    private Cache() {
        pool = ApiHelper.defaultPool();
        location = ApiHelper.defaultLocation();
        logger = null;
    }

    public static Cache create() {
        return new Cache();
    }

    public File get(Artifact artifact) {
        return ApiHelper.cacheGet(this, artifact);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Cache) {
            Cache other = (Cache) obj;
            return this.pool.equals(other.pool) &&
                    this.location.equals(other.location) &&
                    Objects.equals(this.logger, other.logger);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * (17 + pool.hashCode()) + location.hashCode()) + Objects.hashCode(logger);
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder("Cache(pool=");
        b.append(pool.toString());
        b.append(", location=");
        b.append(location.toString());
        if (logger != null) {
            b.append(", logger=");
            b.append(logger.toString());
        }
        b.append(")");
        return b.toString();
    }

    public Cache withPool(ExecutorService pool) {
        this.pool = pool;
        return this;
    }

    public Cache withLocation(File location) {
        this.location = location;
        return this;
    }

    public Cache withLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    public ExecutorService getPool() {
        return pool;
    }

    public File getLocation() {
        return location;
    }

    public Logger getLogger() {
        return logger;
    }
}
