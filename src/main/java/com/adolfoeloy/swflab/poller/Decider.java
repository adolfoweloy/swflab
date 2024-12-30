package com.adolfoeloy.swflab.poller;

import com.adolfoeloy.swflab.swf.domain.Workflow;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * According to AWS docs, the decider directs the workflow by receiving decision tasks from Amazon SWF and responding
 * back to Amazon SWF with decisions. This can be simply translated to: the decider is a program that developers write
 * which polls decision tasks from SWF. This class name is consistent at the extent that this is the entrypoint of deciders
 * which I'm actually implementing as {@code Runnable} submitted to an {@code ExecutorService}.
 * Since the logic happens within a class that runs in a separate thread I'm calling the Runnable a {@code WorkflowWorker}
 * which is consistent with the Java example using SDK 1.0.
 */
@Component
class Decider {
    private static final Logger logger = LoggerFactory.getLogger(Decider.class);

    private final SwfClient client;
    private final Workflow workflow;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    Decider(SwfClient client, Workflow workflow) {
        this.client = client;
        this.workflow = workflow;
    }

    @PostConstruct
    void init() {
        executor.submit(new WorkflowWorker(client, workflow));
    }

}
