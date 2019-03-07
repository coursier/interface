package coursier.api;

public class IvyRepository implements Repository {

    private final String pattern;
    private String metadataPattern;
    private Credentials credentials;


    private IvyRepository(String pattern, String metadataPattern) {
        this.pattern = pattern;
        this.metadataPattern = metadataPattern;
        this.credentials = null;
    }


    public static IvyRepository of(String pattern, String metadataPattern) {
        return new IvyRepository(pattern, metadataPattern);
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof IvyRepository) {
            IvyRepository other = (IvyRepository) obj;
            return this.pattern.equals(other.pattern) &&
                    this.metadataPattern.equals(other.metadataPattern) &&
                    this.credentials.equals(other.credentials);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (37 * (17 + pattern.hashCode()) + metadataPattern.hashCode()) + credentials.hashCode();
    }

    @Override
    public String toString() {
        String credentialsString = "";
        if (credentials != null)
            credentialsString = ", " + credentials;
        String mdPatternString = "";
        if (metadataPattern != null)
            mdPatternString = ", " + metadataPattern;
        return "IvyRepository(" + pattern + mdPatternString + credentialsString + ")";
    }


    public String getPattern() {
        return pattern;
    }

    public String getMetadataPattern() {
        return metadataPattern;
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public IvyRepository withMetadataPattern(String metadataPattern) {
        this.metadataPattern = metadataPattern;
        return this;
    }

    public IvyRepository withCredentials(Credentials credentials) {
        this.credentials = credentials;
        return this;
    }
}
