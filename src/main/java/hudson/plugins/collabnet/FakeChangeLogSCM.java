package hudson.plugins.collabnet;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.User;
import hudson.scm.ChangeLogParser;
import hudson.scm.RepositoryBrowser;
import hudson.scm.ChangeLogSet;
import hudson.scm.SCMDescriptor;
import hudson.scm.ChangeLogSet.Entry;
import hudson.scm.NullSCM;
import hudson.scm.SCM;
import hudson.util.AdaptedIterator;
import hudson.util.IOException2;

import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import jenkins.model.Jenkins;
import static java.util.Arrays.asList;

/**
 * {@link SCM} for test that enables the caller to fake changelog entries programatically.
 * This {@link SCM} doesn't really touch any files.
 *
 * TODO: to be moved to Jenkins' test harness.
 * 
 * @author Kohsuke Kawaguchi
 */
public class FakeChangeLogSCM extends NullSCM {
    private List<Commit> commits = new ArrayList<Commit>();

    public static final class Commit implements Serializable {
        private String message = "a commit";
        private String user="someone";
        private List<String> paths = new ArrayList<String>();

        public Commit by(String user) {
            this.user = user;
            return this;
        }

        public Commit says(String message) {
            this.message = message;
            return this;
        }

        private static final long serialVersionUID = 1L;
    }

    /**
     * Fakes a new commit, to be made available to Jenkins in the next build.
     * The contents of the commit should be filled with the fluent API pattern.
     *
     * @param paths
     *      The paths that are touched by a commit.
     */
    public Commit commit(String... paths) {
        Commit c = new Commit();
        c.paths.addAll(asList(paths));
        commits.add(c);
        return c;
    }

    @Override
    public boolean checkout(AbstractBuild<?, ?> build, Launcher launcher, FilePath remoteDir, BuildListener listener, File changeLogFile) throws IOException, InterruptedException {
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(changeLogFile));
        oos.writeObject(commits);
        oos.close();
        commits.clear();
        return true;
    }

    @Override
    public RepositoryBrowser<?> getBrowser() {
        return new RepositoryBrowser<ChangeLogSet.Entry>() {
            @Override public URL getChangeSetLink(ChangeLogSet.Entry changeSet) throws IOException {
                return null;
            }
        };
    }

    @Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeLogParser() {
            @Override
            public ChangeLogSet<? extends Entry> parse(AbstractBuild build, File changelogFile) throws IOException, SAXException {
                try {
                    ObjectInputStream ois = new ObjectInputStream(new FileInputStream(changelogFile));
                    final List<Commit> commits = (List<Commit>)ois.readObject();
                    ois.close();

                    return new ChangeLogSet<Entry>(build) {
                        private final ChangeLogSet THIS = this;
                        public boolean isEmptySet() {
                            return commits.isEmpty();
                        }

                        public Iterator<Entry> iterator() {
                            return new AdaptedIterator<Commit,Entry>(commits.iterator()) {
                                @Override
                                protected Entry adapt(final Commit item) {
                                    return new Entry() {
                                        {
                                            setParent(THIS);
                                        }
                                        @Override
                                        public String getMsg() {
                                            return item.message;
                                        }

                                        @Override
                                        public User getAuthor() {
                                            return User.get(item.user);
                                        }

                                        @Override
                                        public Collection<String> getAffectedPaths() {
                                            return item.paths;
                                        }
                                    };
                                }
                            };
                        }
                    };
                } catch (ClassNotFoundException e) {
                    throw new IOException2(e);
                }
            }
        };
    }
}
