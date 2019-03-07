package coursier.api;

public final class Credentials {

    private final String user;
    private final String password;

    private Credentials(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj instanceof Credentials) {
            Credentials other = (Credentials) obj;
            return this.user.equals(other.user) && this.password.equals(other.password);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 37 * (17 + user.hashCode()) + password.hashCode();
    }

    @Override
    public String toString() {
        return "Credentials(" + user + ", ****)";
    }

    public static Credentials of(String user, String password) {
        return new Credentials(user, password);
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }
}
