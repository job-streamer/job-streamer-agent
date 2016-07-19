package net.unit8.job_streamer.agent.listener;

import clojure.java.api.Clojure;
import clojure.lang.*;
import org.slf4j.MDC;

import javax.batch.api.listener.AbstractStepListener;
import javax.batch.runtime.context.JobContext;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;
import java.util.Objects;

import net.unit8.job_streamer.agent.util.SystemUtil;


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
        Object system = SystemUtil.getSystem();

        Object connector = RT.get(system, Keyword.intern("connector"));
        Object runtime   = RT.get(system, Keyword.intern("runtime"));

        IFn sendMessage = Clojure.var("job-streamer.agent.component.connector", "send-message");

        Object instanceId = RT.get(runtime, Keyword.intern("instance-id"));
        MDC.put("stepExecutionId", Long.toString(stepContext.getStepExecutionId()));
        MDC.put("instanceId", Objects.toString(instanceId));

        PersistentHashMap commandMap = PersistentHashMap.create(
                Keyword.intern("command"), Keyword.intern("start-step"),
                Keyword.intern("id"), Long.parseLong(jobContext.getProperties().getProperty("request-id")),
                Keyword.intern("instance-id"), instanceId,
                Keyword.intern("execution-id"), jobContext.getExecutionId(),
                Keyword.intern("step-name"), stepContext.getStepName(),
                Keyword.intern("step-execution-id"), stepContext.getStepExecutionId());

        sendMessage.invoke(connector, commandMap);
    }

    @Override
    public void afterStep() {
        MDC.remove("stepExecutionId");

        Object system = SystemUtil.getSystem();

        Object connector = RT.get(system, Keyword.intern("connector"));
        Object runtime   = RT.get(system, Keyword.intern("runtime"));
        IFn sendMessage = Clojure.var("job-streamer.agent.component.connector", "send-message");
        Object instanceId = RT.get(runtime, Keyword.intern("instance-id"));

        PersistentHashMap commandMap = PersistentHashMap.create(
                Keyword.intern("command"), Keyword.intern("progress-step"),
                Keyword.intern("id"), Long.parseLong(jobContext.getProperties().getProperty("request-id")),
                Keyword.intern("instance-id"), instanceId,
                Keyword.intern("execution-id"), jobContext.getExecutionId(),
                Keyword.intern("step-execution-id"), stepContext.getStepExecutionId());
        sendMessage.invoke(connector, commandMap);
    }

}
