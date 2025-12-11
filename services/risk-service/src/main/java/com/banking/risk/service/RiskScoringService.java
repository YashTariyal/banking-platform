package com.banking.risk.service;

import com.banking.risk.domain.RiskLevel;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RiskScoringService {

    private static final int LOW_THRESHOLD = 30;
    private static final int MEDIUM_THRESHOLD = 60;
    private static final int HIGH_THRESHOLD = 80;

    public RiskScoreResult calculateRiskScore(
            BigDecimal amount,
            String currency,
            UUID customerId,
            UUID accountId,
            RiskScoringContext context
    ) {
        List<String> riskFactors = new ArrayList<>();
        int riskScore = 0;

        // Amount-based risk factors
        if (amount != null) {
            if (amount.compareTo(new BigDecimal("10000")) >= 0) {
                riskFactors.add("Large transaction amount");
                riskScore += 25;
            } else if (amount.compareTo(new BigDecimal("5000")) >= 0) {
                riskFactors.add("Moderate transaction amount");
                riskScore += 15;
            }

            // Check for round numbers (potential structuring)
            if (isRoundNumber(amount)) {
                riskFactors.add("Round number pattern");
                riskScore += 10;
            }
        }

        // Velocity checks (if context provided)
        if (context != null) {
            if (context.getRecentTransactionCount() > 10) {
                riskFactors.add("High transaction velocity");
                riskScore += 20;
            }

            if (context.getRecentTransactionAmount() != null &&
                    context.getRecentTransactionAmount().compareTo(new BigDecimal("50000")) > 0) {
                riskFactors.add("High cumulative transaction amount");
                riskScore += 15;
            }

            if (context.isNewAccount()) {
                riskFactors.add("New account");
                riskScore += 10;
            }

            if (context.isUnusualLocation()) {
                riskFactors.add("Unusual transaction location");
                riskScore += 20;
            }

            if (context.isUnusualTime()) {
                riskFactors.add("Unusual transaction time");
                riskScore += 15;
            }
        }

        RiskLevel riskLevel = determineRiskLevel(riskScore);

        return new RiskScoreResult(riskScore, riskLevel, riskFactors);
    }

    private boolean isRoundNumber(BigDecimal amount) {
        // Check if amount is a round number (ends in .00 or .0)
        BigDecimal remainder = amount.remainder(BigDecimal.ONE);
        return remainder.compareTo(BigDecimal.ZERO) == 0 ||
                remainder.compareTo(new BigDecimal("0.0")) == 0 ||
                remainder.compareTo(new BigDecimal("0.00")) == 0;
    }

    private RiskLevel determineRiskLevel(int riskScore) {
        if (riskScore >= HIGH_THRESHOLD) {
            return RiskLevel.CRITICAL;
        } else if (riskScore >= MEDIUM_THRESHOLD) {
            return RiskLevel.HIGH;
        } else if (riskScore >= LOW_THRESHOLD) {
            return RiskLevel.MEDIUM;
        } else {
            return RiskLevel.LOW;
        }
    }

    public record RiskScoreResult(int score, RiskLevel level, List<String> riskFactors) {
    }

    public static class RiskScoringContext {
        private int recentTransactionCount;
        private BigDecimal recentTransactionAmount;
        private boolean newAccount;
        private boolean unusualLocation;
        private boolean unusualTime;

        public int getRecentTransactionCount() {
            return recentTransactionCount;
        }

        public void setRecentTransactionCount(int recentTransactionCount) {
            this.recentTransactionCount = recentTransactionCount;
        }

        public BigDecimal getRecentTransactionAmount() {
            return recentTransactionAmount;
        }

        public void setRecentTransactionAmount(BigDecimal recentTransactionAmount) {
            this.recentTransactionAmount = recentTransactionAmount;
        }

        public boolean isNewAccount() {
            return newAccount;
        }

        public void setNewAccount(boolean newAccount) {
            this.newAccount = newAccount;
        }

        public boolean isUnusualLocation() {
            return unusualLocation;
        }

        public void setUnusualLocation(boolean unusualLocation) {
            this.unusualLocation = unusualLocation;
        }

        public boolean isUnusualTime() {
            return unusualTime;
        }

        public void setUnusualTime(boolean unusualTime) {
            this.unusualTime = unusualTime;
        }
    }
}

