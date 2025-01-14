/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.sleuth.autoconfig.instrument.prometheus;

import io.prometheus.client.exemplars.tracer.common.SpanContextSupplier;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.cloud.sleuth.instrument.prometheus.prometheus.LazySleuthSpanContextSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} enables Prometheus Exemplars support.
 *
 * @author Jonatan Ivanov
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(SpanContextSupplier.class)
@ConditionalOnProperty(value = "spring.sleuth.enabled", matchIfMissing = true)
@AutoConfigureBefore(
		name = "org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration")
public class PrometheusExemplarsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(value = "spring.sleuth.prometheus.exemplars.enabled", matchIfMissing = true)
	SpanContextSupplier sleuthSpanContextSupplier(ObjectProvider<Tracer> tracerProvider) {
		return new LazySleuthSpanContextSupplier(tracerProvider);
	}

}
