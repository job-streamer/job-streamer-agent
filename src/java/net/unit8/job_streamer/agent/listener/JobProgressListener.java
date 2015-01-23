package net.unit8.job_streamer.agent.listener;

import clojure.java.api.Clojure;
import clojure.lang.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.batch.api.listener.AbstractJobListener;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import java.util.HashMap;

/**
 * @author kawasima
 */
public class JobProgressListener extends AbstractJobListener {
    private static final Logger logger = LoggerFactory.getLogger(JobProgressListener.class);

    @Inject
    private JobContext jobContext;

    @Override
    public void afterJob() {
        HashMap<Keyword, Object> command = new HashMap<Keyword, Object>();
        logger.debug("Send progress message... " + jobContext.getExecutionId());

        Var wsChannel = RT.var("job-streamer.agent.core", "ws-channel");
        IFn put = Clojure.var("clojure.core.async", "put!");
        PersistentHashMap commandMap = PersistentHashMap.create(
                Keyword.intern("command"), Keyword.intern("progress"),
                Keyword.intern("id"), Long.parseLong(jobContext.getProperties().getProperty("request-id")),
                Keyword.intern("execution-id"), jobContext.getExecutionId());
        put.invoke(wsChannel.get(), commandMap);
        logger.debug("Sent progress message:" + commandMap);
    }
}
