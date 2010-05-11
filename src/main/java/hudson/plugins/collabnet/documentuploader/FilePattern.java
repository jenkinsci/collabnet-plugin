package hudson.plugins.collabnet.documentuploader;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import hudson.Extension;
import hudson.model.*;
import hudson.plugins.collabnet.util.CommonUtil;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

/**
 * File pattern to upload
 *
 * @author Kohsuke Kawaguchi
 */
public class FilePattern extends AbstractDescribableImpl<FilePattern> {
    public final String value;

    @DataBoundConstructor
    public FilePattern(String value) {
        this.value = value;
    }

    public String interpret(AbstractBuild<?,?> build, TaskListener listener) throws IOException, InterruptedException {
        return CommonUtil.getInterpreted(build.getEnvironment(listener), value);
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<FilePattern> {
        public String getDisplayName() { return ""; }
    }

    /**
     * To remain backward compatible with the earlier string-only serialization format.
     */
    public static final class ConverterImpl implements Converter {
        public ConverterImpl() {
        }

        public boolean canConvert(Class type) {
            return type==FilePattern.class;
        }

        public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
            FilePattern src = (FilePattern) source;
            writer.setValue(src.value);
        }

        public Object unmarshal(HierarchicalStreamReader reader, final UnmarshallingContext context) {
            return new FilePattern(reader.getValue());
        }
    }
}
