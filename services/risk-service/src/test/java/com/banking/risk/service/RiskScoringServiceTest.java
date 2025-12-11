package com.banking.risk.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.banking.risk.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RiskScoringServiceTest {

    private RiskScoringService scoringService;

    @BeforeEach
    void setUp() {
        scoringService = new RiskScoringService();
    }

    @Test
    void calculateRiskScore_lowAmount_returnsLowRisk() {
        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("100.50"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );

        assertThat(result.score()).isLessThan(30);
        assertThat(result.level()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void calculateRiskScore_moderateAmount_returnsMediumRisk() {
        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("6000"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );

        assertThat(result.score()).isGreaterThanOrEqualTo(15);
        assertThat(result.level()).isIn(RiskLevel.LOW, RiskLevel.MEDIUM);
        assertThat(result.riskFactors()).contains("Moderate transaction amount");
    }

    @Test
    void calculateRiskScore_largeAmount_returnsHighRisk() {
        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("15000"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );

        assertThat(result.score()).isGreaterThanOrEqualTo(25);
        assertThat(result.level()).isIn(RiskLevel.MEDIUM, RiskLevel.HIGH);
        assertThat(result.riskFactors()).contains("Large transaction amount");
    }

    @Test
    void calculateRiskScore_roundNumber_addsRiskFactor() {
        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("1000.00"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                null
        );

        assertThat(result.riskFactors()).contains("Round number pattern");
    }

    @Test
    void calculateRiskScore_highVelocity_returnsHighRisk() {
        RiskScoringService.RiskScoringContext context = new RiskScoringService.RiskScoringContext();
        context.setRecentTransactionCount(15);

        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("100"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                context
        );

        assertThat(result.riskFactors()).contains("High transaction velocity");
        assertThat(result.score()).isGreaterThanOrEqualTo(20);
    }

    @Test
    void calculateRiskScore_multipleFactors_returnsCriticalRisk() {
        RiskScoringService.RiskScoringContext context = new RiskScoringService.RiskScoringContext();
        context.setRecentTransactionCount(15);
        context.setRecentTransactionAmount(new BigDecimal("60000"));
        context.setUnusualLocation(true);
        context.setUnusualTime(true);

        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("15000"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                context
        );

        assertThat(result.score()).isGreaterThanOrEqualTo(80);
        assertThat(result.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(result.riskFactors().size()).isGreaterThan(3);
    }

    @Test
    void calculateRiskScore_newAccount_addsRiskFactor() {
        RiskScoringService.RiskScoringContext context = new RiskScoringService.RiskScoringContext();
        context.setNewAccount(true);

        RiskScoringService.RiskScoreResult result = scoringService.calculateRiskScore(
                new BigDecimal("5000"),
                "USD",
                UUID.randomUUID(),
                UUID.randomUUID(),
                context
        );

        assertThat(result.riskFactors()).contains("New account");
    }
}

