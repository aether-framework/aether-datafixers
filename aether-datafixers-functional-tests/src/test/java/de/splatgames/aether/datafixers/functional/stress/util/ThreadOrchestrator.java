/*
 * Copyright (c) 2025 Splatgames.de Software and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.splatgames.aether.datafixers.functional.stress.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Utility for coordinating concurrent test threads.
 *
 * <p>Provides a fluent API for:
 * <ul>
 *   <li>Submitting tasks to a thread pool</li>
 *   <li>Synchronizing all threads to start simultaneously</li>
 *   <li>Waiting for completion with timeout</li>
 *   <li>Collecting exceptions from all threads</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * ThreadOrchestrator orchestrator = ThreadOrchestrator.withThreads(100);
 * for (int i = 0; i < 100; i++) {
 *     orchestrator.submit(() -> {
 *         // Concurrent work
 *     });
 * }
 * orchestrator.startAll();
 * orchestrator.awaitCompletion(Duration.ofMinutes(5));
 * assertThat(orchestrator.errors()).isEmpty();
 * }</pre>
 */
public final class ThreadOrchestrator implements AutoCloseable {

    private final int threadCount;
    private final ExecutorService executor;
    private final CountDownLatch startLatch;
    private final CountDownLatch completionLatch;
    private final List<Throwable> errors;
    private final AtomicBoolean started;
    private int submittedTasks;

    private ThreadOrchestrator(int threadCount) {
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
        this.startLatch = new CountDownLatch(1);
        this.completionLatch = new CountDownLatch(threadCount);
        this.errors = Collections.synchronizedList(new ArrayList<>());
        this.started = new AtomicBoolean(false);
        this.submittedTasks = 0;
    }

    /**
     * Creates a new orchestrator with the specified thread count.
     *
     * @param threadCount the number of threads
     * @return a new orchestrator
     */
    public static ThreadOrchestrator withThreads(int threadCount) {
        if (threadCount <= 0) {
            throw new IllegalArgumentException("Thread count must be positive: " + threadCount);
        }
        return new ThreadOrchestrator(threadCount);
    }

    /**
     * Submits a task to be executed.
     *
     * <p>The task will wait at a barrier until {@link #startAll()} is called,
     * ensuring all threads begin simultaneously.
     *
     * @param task the task to execute
     * @return this orchestrator for chaining
     * @throws IllegalStateException if already started or all tasks submitted
     */
    public ThreadOrchestrator submit(Runnable task) {
        if (started.get()) {
            throw new IllegalStateException("Cannot submit tasks after startAll()");
        }
        if (submittedTasks >= threadCount) {
            throw new IllegalStateException(
                    "Cannot submit more than " + threadCount + " tasks"
            );
        }

        executor.submit(() -> {
            try {
                // Wait for all threads to be ready
                startLatch.await();
                // Execute the actual work
                task.run();
            } catch (Throwable t) {
                errors.add(t);
            } finally {
                completionLatch.countDown();
            }
        });

        submittedTasks++;
        return this;
    }

    /**
     * Submits multiple identical tasks.
     *
     * @param count the number of tasks to submit
     * @param task  the task to execute
     * @return this orchestrator for chaining
     */
    public ThreadOrchestrator submitAll(int count, Runnable task) {
        for (int i = 0; i < count; i++) {
            submit(task);
        }
        return this;
    }

    /**
     * Releases all threads to start executing simultaneously.
     *
     * @throws IllegalStateException if not all tasks have been submitted
     */
    public void startAll() {
        if (submittedTasks != threadCount) {
            throw new IllegalStateException(
                    "Expected " + threadCount + " tasks but only " + submittedTasks + " submitted"
            );
        }
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("Already started");
        }
        startLatch.countDown();
    }

    /**
     * Waits for all tasks to complete.
     *
     * @param timeout the maximum time to wait
     * @return true if all tasks completed within timeout, false if timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitCompletion(Duration timeout) throws InterruptedException {
        return completionLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Returns the list of exceptions thrown by tasks.
     *
     * @return unmodifiable list of exceptions
     */
    public List<Throwable> errors() {
        return Collections.unmodifiableList(new ArrayList<>(errors));
    }

    /**
     * Returns true if any tasks threw exceptions.
     *
     * @return true if errors occurred
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Shuts down the executor service.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void close() {
        shutdown();
    }
}
