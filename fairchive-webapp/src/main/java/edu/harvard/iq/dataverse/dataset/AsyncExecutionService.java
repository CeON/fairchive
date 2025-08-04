package edu.harvard.iq.dataverse.dataset;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service for executing tasks asynchronously using the default managed executor service.
 *
 * In contrast to using the @{@link javax.ejb.Asynchronous} annotation, this service returns a CompletableFuture
 * which allows for better control over the execution and result handling.
 */
@Stateless
public class AsyncExecutionService {

    @Resource
    private ManagedExecutorService mes;

    public <T> CompletableFuture<T> executeAsync(Supplier<T> process) {
        return CompletableFuture.supplyAsync(process, mes);
    }
}
