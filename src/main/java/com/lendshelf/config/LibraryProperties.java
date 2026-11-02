package com.lendshelf.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Lending rules for the branch, bound from the {@code lendshelf.*} properties.
 */
@ConfigurationProperties(prefix = "lendshelf")
public record LibraryProperties(
        int loanDays,
        double lateFeePerDay,
        int maxRenewals,
        int maxOpenLoans,
        int pageSize,
        boolean seedDemoData) {
}
