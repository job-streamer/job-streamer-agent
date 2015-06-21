package net.unit8.job_streamer.agent;

import net.unit8.weld.PrescannedWeld;
import net.unit8.wscl.WebSocketClassLoader;
import org.jberet.creation.AbstractArtifactFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

/**
 * Weld initialize by a given classloader.
 *
 * @author kawasima
 */
public class JobStreamerArtifactFactory extends AbstractArtifactFactory {
    private static final Logger LOG = LoggerFactory.getLogger(JobStreamerArtifactFactory.class);
    private BeanManager beanManager;
    private ClassLoader classLoader;

    public JobStreamerArtifactFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public Class<?> getArtifactClass(String ref, ClassLoader classLoader) {
        synchronized (this) {
            if (beanManager == null) initContainer(classLoader);
        }

        Bean bean = this.getBean(ref);
        return bean == null?null:bean.getBeanClass();
    }

    @Override
    public Object create(String ref, Class<?> cls, ClassLoader classLoader) throws Exception {
        synchronized (this) {
            if (beanManager == null) initContainer(classLoader);
        }
        Bean bean = this.getBean(ref);
        return bean == null?null:this.beanManager.getReference(bean, bean.getBeanClass(), this.beanManager.createCreationalContext(bean));
    }

    private Bean<?> getBean(String ref) {
        return beanManager.resolve(beanManager.getBeans(ref));
    }

    private void initContainer(ClassLoader classLoader) {
        ClassLoader currentLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            WeldContainer weldContainer;
            InputStream is = classLoader.getResourceAsStream("weld-deployment.xml");
            if (is == null) {
                weldContainer = (new Weld()).initialize();
            } else {
                LOG.info("Use Prescanned Weld.");
                weldContainer = new PrescannedWeld().setDeploymentStream(is).initialize();
            }
            LOG.info("Init container at classload " + classLoader);
            this.beanManager = weldContainer.getBeanManager();
        } finally {
            Thread.currentThread().setContextClassLoader(currentLoader);
        }
    }
}
