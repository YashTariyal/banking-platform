package com.banking.card.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "card_analytics")
public class CardAnalytics {

    @Id
    @Column(name = "card_id")
    private UUID cardId;

    @OneToOne
    @JoinColumn(name = "card_id")
    private Card card;

    @Column(name = "total_transactions")
    private Integer totalTransactions;

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "average_transaction_amount")
    private BigDecimal averageTransactionAmount;

    @Column(name = "declined_transactions")
    private Integer declinedTransactions;

    @Column(name = "last_transaction_date")
    private Instant lastTransactionDate;

    @Column(name = "top_merchant_category", length = 10)
    private String topMerchantCategory;

    @Column(name = "most_used_country", length = 2)
    private String mostUsedCountry;

    @Column(name = "last_updated_at", nullable = false)
    private Instant lastUpdatedAt;

    public UUID getCardId() {
        return cardId;
    }

    public void setCardId(UUID cardId) {
        this.cardId = cardId;
    }

    public Card getCard() {
        return card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public Integer getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(Integer totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getAverageTransactionAmount() {
        return averageTransactionAmount;
    }

    public void setAverageTransactionAmount(BigDecimal averageTransactionAmount) {
        this.averageTransactionAmount = averageTransactionAmount;
    }

    public Integer getDeclinedTransactions() {
        return declinedTransactions;
    }

    public void setDeclinedTransactions(Integer declinedTransactions) {
        this.declinedTransactions = declinedTransactions;
    }

    public Instant getLastTransactionDate() {
        return lastTransactionDate;
    }

    public void setLastTransactionDate(Instant lastTransactionDate) {
        this.lastTransactionDate = lastTransactionDate;
    }

    public String getTopMerchantCategory() {
        return topMerchantCategory;
    }

    public void setTopMerchantCategory(String topMerchantCategory) {
        this.topMerchantCategory = topMerchantCategory;
    }

    public String getMostUsedCountry() {
        return mostUsedCountry;
    }

    public void setMostUsedCountry(String mostUsedCountry) {
        this.mostUsedCountry = mostUsedCountry;
    }

    public Instant getLastUpdatedAt() {
        return lastUpdatedAt;
    }

    public void setLastUpdatedAt(Instant lastUpdatedAt) {
        this.lastUpdatedAt = lastUpdatedAt;
    }
}

