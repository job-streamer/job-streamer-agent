package net.unit8.job_streamer.agent;

import org.jberet.tools.AbstractJobXmlResolver;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author kawasima
 */
public class FileSystemJobXmlResolver extends AbstractJobXmlResolver {
    @Override
    public InputStream resolveJobXml(String jobXml, ClassLoader classLoader) throws IOException {
        return Files.newInputStream(Paths.get(jobXml));
    }
}
