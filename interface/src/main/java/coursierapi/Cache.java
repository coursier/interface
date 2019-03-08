package coursierapi;

import coursier.internal.api.ApiHelper;

import java.io.File;
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
