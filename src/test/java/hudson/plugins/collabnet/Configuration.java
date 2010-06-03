package hudson.plugins.collabnet;

import org.apache.commons.beanutils.ConvertUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

/**
 * Configuration parameters for a test.
 *
 * <p>
 * These parameters are first loaded from {@code ~/.teamforge} property file, then
 * whatever values specified in the system properties will override them.
 * 
 * @author Kohsuke Kawaguchi
 */
public class Configuration extends Properties {
    public static final Configuration INSTANCE = new Configuration();

    private Configuration() {
        try {
            File f = new File(new File(System.getProperty("user.home")), ".teamforge");
            if (f.exists()) {
                FileInputStream in = new FileInputStream(f);
                try {
                    this.load(in);
                } finally {
                    in.close();
                }
            }

            // system properties take precedence
            putAll(System.getProperties());
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    /**
     * Injects all the {@link TestParam}s on the given instance.
     */
    public void injectTo(Object o) throws Exception {
        for (Class c=o.getClass(); c!=null; c=c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getAnnotation(TestParam.class)!=null) {
                    String value = getProperty(f.getName());
                    if (value!=null) {
                        f.setAccessible(true);
                        f.set(o,ConvertUtils.convert(value,f.getType()));
                    }
                }
            }
        }
    }
}
