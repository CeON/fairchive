package edu.harvard.iq.dataverse.dataset;

import io.vavr.control.Option;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ensures that a given task can only be executed once at a time.
 */
public class OneAtATimeExecutionGuard<T> {

    private final AtomicBoolean running = new AtomicBoolean();
    private final Callable<T> perform;

    public OneAtATimeExecutionGuard(Callable<T> perform) {
        this.perform = perform;
    }

    public Option<T> execute() {
        if (running.compareAndSet(false, true)) {
            try {
                return Option.of(perform.call());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                running.set(false);
            }
        }
        return Option.none();
    }

    public boolean isRunning() {
        return running.get();
    }
}
