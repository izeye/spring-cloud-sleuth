/*
 * Copyright 2013-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.sleuth.instrument.async;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.cloud.sleuth.SpanNamer;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.internal.ContextUtil;

/**
 * A decorator class for {@link ExecutorService} to support tracing in Executors.
 *
 * @author Gaurav Rai Mazra
 * @since 1.0.0
 */
// public as most types in this package were documented for use
public class TraceableExecutorService implements ExecutorService {

	private static final Map<ExecutorService, TraceableExecutorService> CACHE = new ConcurrentHashMap<>();

	final ExecutorService delegate;

	final String spanName;

	Tracer tracer;

	SpanNamer spanNamer;

	BeanFactory beanFactory;

	public TraceableExecutorService(BeanFactory beanFactory, final ExecutorService delegate) {
		this(beanFactory, delegate, null);
	}

	public TraceableExecutorService(BeanFactory beanFactory, final ExecutorService delegate, String spanName) {
		this.delegate = delegate;
		this.beanFactory = beanFactory;
		this.spanName = spanName;
	}

	/**
	 * Wraps the Executor in a trace instance.
	 * @param beanFactory bean factory
	 * @param delegate delegate to wrap
	 * @param beanName bean name
	 * @return traced instance
	 */
	public static TraceableExecutorService wrap(BeanFactory beanFactory, ExecutorService delegate, String beanName) {
		return CACHE.computeIfAbsent(delegate, e -> new TraceableExecutorService(beanFactory, delegate, beanName));
	}

	/**
	 * Wraps the Executor in a trace instance.
	 * @param beanFactory bean factory
	 * @param delegate delegate to wrap
	 * @return traced instance
	 */
	public static TraceableExecutorService wrap(BeanFactory beanFactory, ExecutorService delegate) {
		return CACHE.computeIfAbsent(delegate, e -> new TraceableExecutorService(beanFactory, delegate, null));
	}

	@Override
	public void execute(Runnable command) {
		this.delegate.execute(ContextUtil.isContextUnusable(this.beanFactory) ? command
				: new TraceRunnable(tracer(), spanNamer(), command, this.spanName));
	}

	@Override
	public void shutdown() {
		this.delegate.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return this.delegate.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return this.delegate.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.delegate.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.delegate.awaitTermination(timeout, unit);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.delegate.submit(ContextUtil.isContextUnusable(this.beanFactory) ? task
				: new TraceCallable<>(tracer(), spanNamer(), task, this.spanName));
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return this.delegate.submit(ContextUtil.isContextUnusable(this.beanFactory) ? task
				: new TraceRunnable(tracer(), spanNamer(), task, this.spanName), result);
	}

	@Override
	public Future<?> submit(Runnable task) {
		return this.delegate.submit(ContextUtil.isContextUnusable(this.beanFactory) ? task
				: new TraceRunnable(tracer(), spanNamer(), task, this.spanName));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.delegate
				.invokeAll(ContextUtil.isContextUnusable(this.beanFactory) ? tasks : wrapCallableCollection(tasks));
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return this.delegate.invokeAll(
				ContextUtil.isContextUnusable(this.beanFactory) ? tasks : wrapCallableCollection(tasks), timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return this.delegate
				.invokeAny(ContextUtil.isContextUnusable(this.beanFactory) ? tasks : wrapCallableCollection(tasks));
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.delegate.invokeAny(
				ContextUtil.isContextUnusable(this.beanFactory) ? tasks : wrapCallableCollection(tasks), timeout, unit);
	}

	private <T> Collection<? extends Callable<T>> wrapCallableCollection(Collection<? extends Callable<T>> tasks) {
		List<Callable<T>> ts = new ArrayList<>();
		for (Callable<T> task : tasks) {
			if (!(task instanceof TraceCallable)) {
				ts.add(new TraceCallable<>(tracer(), spanNamer(), task, this.spanName));
			}
		}
		return ts;
	}

	Tracer tracer() {
		if (this.tracer == null && this.beanFactory != null) {
			this.tracer = this.beanFactory.getBean(Tracer.class);
		}
		return this.tracer;
	}

	SpanNamer spanNamer() {
		if (this.spanNamer == null && this.beanFactory != null) {
			this.spanNamer = this.beanFactory.getBean(SpanNamer.class);
		}
		return this.spanNamer;
	}

}
