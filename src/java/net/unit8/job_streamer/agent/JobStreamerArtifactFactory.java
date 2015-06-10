package net.unit8.job_streamer.agent;

import org.jberet.creation.AbstractArtifactFactory;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import java.util.Iterator;
import java.util.Set;

/**
 * Weld initialize by a given classloader.
 *
 * @author kawasima
 */
public class JobStreamerArtifactFactory extends AbstractArtifactFactory {
    private static final Logger LOG = LoggerFactory.getLogger(JobStreamerArtifactFactory.class);
    private final BeanManager beanManager;

    public JobStreamerArtifactFactory(ClassLoader classLoader) {
        LOG.info("classloader=" + classLoader);
        LOG.info("contextClassLoader=" + Thread.currentThread().getContextClassLoader());
        WeldContainer weldContainer = (new Weld()).initialize();
        this.beanManager = weldContainer.getBeanManager();
    }

        public Class<?> getArtifactClass(String ref, ClassLoader classLoader) {
        Bean bean = this.getBean(ref);
        return bean == null?null:bean.getBeanClass();
    }

    public Object create(String ref, Class<?> cls, ClassLoader classLoader) throws Exception {
        Bean bean = this.getBean(ref);
        return bean == null?null:this.beanManager.getReference(bean, bean.getBeanClass(), this.beanManager.createCreationalContext(bean));
    }

    private Bean<?> getBean(String ref) {
        Set beans = this.beanManager.getBeans(ref);
        Iterator it = beans.iterator();
        return it.hasNext()?(Bean)it.next():null;
    }
}
