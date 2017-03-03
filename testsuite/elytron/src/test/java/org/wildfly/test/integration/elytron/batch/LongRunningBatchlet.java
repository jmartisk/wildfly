package org.wildfly.test.integration.elytron.batch;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javax.batch.api.Batchlet;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.jboss.as.test.shared.TimeoutUtil;

/**
 * @author Jan Martiska
 */
@Named
public class LongRunningBatchlet implements Batchlet {

    private final CompletableFuture<Void> SHOULD_STOP = new CompletableFuture<>();

    @Inject
    JobContext ctx;

    @Override
    public String process() throws Exception {
        SHOULD_STOP.get(TimeoutUtil.adjust(10), TimeUnit.SECONDS);
        return null;
    }

    @Override
    public void stop() throws Exception {
        SHOULD_STOP.complete(null);
    }
}
