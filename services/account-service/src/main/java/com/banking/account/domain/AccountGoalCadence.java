package com.banking.account.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.time.ZonedDateTime;
import java.util.Locale;

public enum AccountGoalCadence {
    DAILY {
        @Override
        public String periodKey(Instant instant) {
            return LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
        }

        @Override
        public Instant nextExecutionFrom(Instant reference) {
            return increment(reference, 0, 0, 1);
        }
    },
    WEEKLY {
        @Override
        public String periodKey(Instant instant) {
            LocalDate date = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            int year = date.get(WeekFields.of(Locale.US).weekBasedYear());
            int week = date.get(WeekFields.ISO.weekOfWeekBasedYear());
            return year + "-W" + week;
        }

        @Override
        public Instant nextExecutionFrom(Instant reference) {
            return increment(reference, 0, 0, 7);
        }
    },
    MONTHLY {
        @Override
        public String periodKey(Instant instant) {
            LocalDate date = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        }

        @Override
        public Instant nextExecutionFrom(Instant reference) {
            ZonedDateTime zoned = ZonedDateTime.ofInstant(reference, ZoneOffset.UTC).plusMonths(1).withHour(0).withMinute(0).withSecond(0).withNano(0);
            return zoned.toInstant();
        }
    };

    public abstract String periodKey(Instant instant);

    public abstract Instant nextExecutionFrom(Instant reference);

    protected Instant increment(Instant reference, int years, int months, int days) {
        ZonedDateTime zoned = ZonedDateTime.ofInstant(reference, ZoneOffset.UTC)
                .plusYears(years)
                .plusMonths(months)
                .plusDays(days)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
        return zoned.toInstant();
    }
}

