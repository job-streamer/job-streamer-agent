package net.unit8.job_streamer.agent.listener;

import clojure.java.api.Clojure;
import clojure.lang.*;
import org.slf4j.MDC;

import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 * @author kawasima
 */
public class StepProgressListener extends AbstractStepListener {
    @Inject
    private JobContext jobContext;

    @Inject
    private StepContext stepContext;

    @Override
    public void beforeStep() {
        Var wsChannel = RT.var("job-streamer.agent.core", "ws-channel");
        IFn put = Clojure.var("clojure.core.async", "put!");

        Var instanceId = RT.var("job-streamer.agent.core", "instance-id");
        MDC.put("stepExecutionId", Long.toString(stepContext.getStepExecutionId()));
        MDC.put("instanceId", instanceId.get().toString());

        PersistentHashMap commandMap = PersistentHashMap.create(
                Keyword.intern("command"), Keyword.intern("start-step"),
                Keyword.intern("id"), Long.parseLong(jobContext.getProperties().getProperty("request-id")),
                Keyword.intern("instance-id"), instanceId.get(),
                Keyword.intern("execution-id"), jobContext.getExecutionId(),
                Keyword.intern("step-name"), stepContext.getStepName(),
                Keyword.intern("step-execution-id"), stepContext.getStepExecutionId());

        put.invoke(wsChannel.get(), commandMap);

    }

    @Override
    public void afterStep() {
        MDC.remove("stepExecutionId");
        Var wsChannel = RT.var("job-streamer.agent.core", "ws-channel");
        IFn put = Clojure.var("clojure.core.async", "put!");

        Var instanceId = RT.var("job-streamer.agent.core", "instance-id");
        PersistentHashMap commandMap = PersistentHashMap.create(
                Keyword.intern("command"), Keyword.intern("progress-step"),
                Keyword.intern("id"), Long.parseLong(jobContext.getProperties().getProperty("request-id")),
                Keyword.intern("instance-id"), instanceId.get(),
                Keyword.intern("execution-id"), jobContext.getExecutionId(),
                Keyword.intern("step-execution-id"), stepContext.getStepExecutionId());
        put.invoke(wsChannel.get(), commandMap);
    }

}
