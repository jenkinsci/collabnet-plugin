package hudson.plugins.collabnet.util;

import org.jvnet.hudson.test.HudsonTestCase;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.net.URL;
import java.lang.annotation.Documented;
import static java.lang.annotation.ElementType.METHOD;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

import org.jvnet.hudson.test.recipes.Recipe;

/**
 * Installs a local plugin from the user's directory before launching Jenkins.
 */
@Documented
@Recipe(WithLocalPlugin.RunnerImpl.class)
@Target(METHOD)
@Retention(RUNTIME)
public @interface WithLocalPlugin {
    /**
     * Name of the plugin.
     *
     * Should be at target/${plugin_name}.hpi.
     */
    String value();

    public class RunnerImpl extends Recipe.Runner<WithLocalPlugin> {
        private WithLocalPlugin wlp;

        @Override
        public void setup(HudsonTestCase testCase, WithLocalPlugin recipe) 
            throws Exception {
            wlp = recipe;
        }

        @Override
        public void decorateHome(HudsonTestCase testCase, File home) 
            throws Exception {
            String mvnHome = System.getProperty("project.build.directory");
            File dir = new File(mvnHome + "/WEB-INF/plugins/");
            File plugin = null;
            for (String file: dir.list()) {
                if (file.startsWith(wlp.value())) {
                    plugin = new File(dir, file);
                    break;
                }
            }
            if (plugin == null) {
                throw new RuntimeException("Cannot find plugin: " + 
                                            wlp.value() + " in dir " + dir);
            }
            else if (!plugin.exists()) {
                throw new RuntimeException ("Cannot find plugin: " + plugin);
            }
            FileUtils.copyFile(plugin, new File(home, "plugins/" + wlp.value() 
                                                + ".hpi"));
        }
    }
}
