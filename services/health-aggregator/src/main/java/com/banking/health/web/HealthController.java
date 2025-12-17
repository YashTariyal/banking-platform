package com.banking.health.web;

import com.banking.health.service.BusinessMetricsService;
import com.banking.health.service.BusinessMetricsService.MetricsSummary;
import com.banking.health.service.HealthCheckService;
import com.banking.health.service.HealthCheckService.AggregatedHealth;
import com.banking.health.service.HealthCheckService.ServiceHealthStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Aggregator", description = "Aggregated health status and metrics APIs")
public class HealthController {

    private final HealthCheckService healthCheckService;
    private final BusinessMetricsService metricsService;

    public HealthController(HealthCheckService healthCheckService, BusinessMetricsService metricsService) {
        this.healthCheckService = healthCheckService;
        this.metricsService = metricsService;
    }

    @GetMapping
    @Operation(summary = "Get aggregated health status")
    public ResponseEntity<AggregatedHealth> getAggregatedHealth() {
        return ResponseEntity.ok(healthCheckService.getAggregatedHealth());
    }

    @GetMapping("/services")
    @Operation(summary = "Get health status of all services")
    public ResponseEntity<Map<String, ServiceHealthStatus>> getAllServiceHealth() {
        return ResponseEntity.ok(healthCheckService.getAllHealthStatuses());
    }

    @GetMapping("/services/{serviceName}")
    @Operation(summary = "Get health status of a specific service")
    public ResponseEntity<ServiceHealthStatus> getServiceHealth(@PathVariable String serviceName) {
        ServiceHealthStatus status = healthCheckService.getServiceHealth(serviceName);
        if (status == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(status);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get business metrics summary")
    public ResponseEntity<MetricsSummary> getMetricsSummary() {
        return ResponseEntity.ok(metricsService.getMetricsSummary());
    }

    // Endpoints to record metrics (called by other services or via Kafka)
    @PostMapping("/metrics/transaction")
    @Operation(summary = "Record a transaction metric")
    public ResponseEntity<Void> recordTransaction(@RequestBody TransactionMetric metric) {
        metricsService.recordTransaction(metric.approved(), metric.processingTimeMs());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/metrics/login")
    @Operation(summary = "Record a login attempt metric")
    public ResponseEntity<Void> recordLogin(@RequestBody LoginMetric metric) {
        metricsService.recordLoginAttempt(metric.success());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/metrics/loan")
    @Operation(summary = "Record a loan application metric")
    public ResponseEntity<Void> recordLoan(@RequestBody LoanMetric metric) {
        metricsService.recordLoanApplication(metric.outcome());
        return ResponseEntity.ok().build();
    }

    public record TransactionMetric(boolean approved, long processingTimeMs) {}
    public record LoginMetric(boolean success) {}
    public record LoanMetric(String outcome) {}
}
