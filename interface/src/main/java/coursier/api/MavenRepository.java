package coursier.api;

public class MavenRepository implements Repository {

    private final String base;


    private MavenRepository(String base) {
        this.base = base;
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
            return this.base.equals(other.base);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17 + base.hashCode();
    }

    @Override
    public String toString() {
        return "MavenRepository(" + base + ")";
    }


    public String getBase() {
        return base;
    }

}
