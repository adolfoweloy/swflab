package com.adolfoeloy.swflab.swf.domain;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.swf.SwfClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The intention with this class is to create workers for both decider and activity worker which will both
 * run in a separate thread. Workers are managed by a cached thread pool and more can be added to scale deciders and activity workers.
 */
@Component
class WorkersConfig {
    private static final Logger logger = LoggerFactory.getLogger(WorkersConfig.class);

    private final SwfClient client;
    private final Workflow workflow;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    WorkersConfig(SwfClient client, Workflow workflow) {
        this.client = client;
        this.workflow = workflow;
    }

    @PostConstruct
    void init() {
        executor.submit(new Decider(client, workflow));
    }

}
