package hudson.plugins.collabnet.tracker;

/**
 * Priority of the tracker artifact.
 *
 * @author Kohsuke Kawaguchi
 */
public enum Priority {
    P1(1,"Highest"),
    P2(2,"High"),
    P3(3,"Medium"),
    P4(4,"Low"),
    P5(5,"Lowest");

    public final int n;
    public final String text;

    Priority(int n, String text) {
        this.n = n;
        this.text = text;
    }

    public String getDisplayName() {
        return n+" - "+text;
    }

    public static Priority valueOf(int i) {
        for (Priority p : values()) {
            if (p.n==i)
                return p;
        }
        return DEFAULT; // failed to parse
    }

    public static final Priority DEFAULT = P3;
}
