package com.banking.health.service;

import com.banking.health.config.ServiceConfig;
import com.banking.health.config.ServiceConfig.ServiceInfo;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class HealthCheckService {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckService.class);

    private final ServiceConfig serviceConfig;
    private final WebClient webClient;
    private final MeterRegistry meterRegistry;
    private final Map<String, ServiceHealthStatus> healthStatuses = new ConcurrentHashMap<>();

    public HealthCheckService(ServiceConfig serviceConfig, MeterRegistry meterRegistry) {
        this.serviceConfig = serviceConfig;
        this.meterRegistry = meterRegistry;
        this.webClient = WebClient.builder().build();

        // Initialize gauges for each service
        for (ServiceInfo service : serviceConfig.getServices()) {
            Gauge.builder("service.health", () -> {
                ServiceHealthStatus status = healthStatuses.get(service.getName());
                return status != null && status.isHealthy() ? 1.0 : 0.0;
            })
            .tag("service", service.getName())
            .description("Health status of service (1=UP, 0=DOWN)")
            .register(meterRegistry);

            Gauge.builder("service.response.time", () -> {
                ServiceHealthStatus status = healthStatuses.get(service.getName());
                return status != null ? status.getResponseTimeMs() : -1;
            })
            .tag("service", service.getName())
            .description("Response time of service health check in ms")
            .register(meterRegistry);
        }
    }

    @Scheduled(fixedRateString = "${health.check-interval-seconds:30}000")
    public void checkAllServices() {
        log.debug("Running health checks for {} services", serviceConfig.getServices().size());
        for (ServiceInfo service : serviceConfig.getServices()) {
            checkService(service);
        }
    }

    private void checkService(ServiceInfo service) {
        Instant start = Instant.now();

        webClient.get()
                .uri(service.getUrl() + "/actuator/health")
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(serviceConfig.getTimeoutSeconds()))
                .doOnSuccess(response -> {
                    long responseTime = Duration.between(start, Instant.now()).toMillis();
                    String status = (String) response.getOrDefault("status", "UNKNOWN");
                    boolean healthy = "UP".equals(status);
                    
                    ServiceHealthStatus healthStatus = new ServiceHealthStatus(
                            service.getName(),
                            healthy,
                            status,
                            responseTime,
                            Instant.now(),
                            null
                    );
                    healthStatuses.put(service.getName(), healthStatus);
                    log.debug("Service {} is {} ({}ms)", service.getName(), status, responseTime);
                })
                .doOnError(error -> {
                    long responseTime = Duration.between(start, Instant.now()).toMillis();
                    ServiceHealthStatus healthStatus = new ServiceHealthStatus(
                            service.getName(),
                            false,
                            "DOWN",
                            responseTime,
                            Instant.now(),
                            error.getMessage()
                    );
                    healthStatuses.put(service.getName(), healthStatus);
                    log.warn("Service {} is DOWN: {}", service.getName(), error.getMessage());
                })
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    public Map<String, ServiceHealthStatus> getAllHealthStatuses() {
        return Map.copyOf(healthStatuses);
    }

    public ServiceHealthStatus getServiceHealth(String serviceName) {
        return healthStatuses.get(serviceName);
    }

    public AggregatedHealth getAggregatedHealth() {
        long totalServices = healthStatuses.size();
        long healthyServices = healthStatuses.values().stream()
                .filter(ServiceHealthStatus::isHealthy)
                .count();
        long unhealthyServices = totalServices - healthyServices;

        String overallStatus;
        if (healthyServices == totalServices) {
            overallStatus = "UP";
        } else if (healthyServices == 0) {
            overallStatus = "DOWN";
        } else {
            overallStatus = "DEGRADED";
        }

        double avgResponseTime = healthStatuses.values().stream()
                .filter(s -> s.getResponseTimeMs() >= 0)
                .mapToLong(ServiceHealthStatus::getResponseTimeMs)
                .average()
                .orElse(0);

        return new AggregatedHealth(overallStatus, totalServices, healthyServices, unhealthyServices, avgResponseTime);
    }

    public record ServiceHealthStatus(
            String serviceName,
            boolean healthy,
            String status,
            long responseTimeMs,
            Instant checkedAt,
            String error
    ) {
        public boolean isHealthy() { return healthy; }
        public long getResponseTimeMs() { return responseTimeMs; }
    }

    public record AggregatedHealth(
            String status,
            long totalServices,
            long healthyServices,
            long unhealthyServices,
            double avgResponseTimeMs
    ) {}
}
