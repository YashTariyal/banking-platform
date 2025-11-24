package com.banking.account.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.Locale;

public enum AccountGoalCadence {
    DAILY {
        @Override
        public String periodKey(Instant instant) {
            return LocalDate.ofInstant(instant, ZoneOffset.UTC).toString();
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
    },
    MONTHLY {
        @Override
        public String periodKey(Instant instant) {
            LocalDate date = LocalDate.ofInstant(instant, ZoneOffset.UTC);
            return date.getYear() + "-" + String.format("%02d", date.getMonthValue());
        }
    };

    public abstract String periodKey(Instant instant);
}

