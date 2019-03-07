package coursier.api;

public class MavenRepository implements Repository {

    private final String base;
    private Credentials credentials;


    private MavenRepository(String base) {
        this.base = base;
        this.credentials = null;
    }


    public static MavenRepository of(String base) {
        return new MavenRepository(base);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof MavenRepository) {
            MavenRepository other = (MavenRepository) obj;
            return this.base.equals(other.base) && this.credentials.equals(other.credentials);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (17 + base.hashCode()) + credentials.hashCode();
    }

    @Override
    public String toString() {
        String credentialsString;
        if (credentials == null)
            credentialsString = "";
        else
            credentialsString = ", " + credentials;
        return "MavenRepository(" + base + credentialsString + ")";
    }


    public String getBase() {
        return base;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public MavenRepository withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }
}
