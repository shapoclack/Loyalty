package org.example.service;

import org.example.model.CustomerStatus;
import org.example.model.Tier;
import org.example.service.DashboardService.CustomerRow;
import java.util.Locale;
import java.util.function.Predicate;

public final class CustomerViewFilter {
    private CustomerViewFilter() { }
    public static Predicate<CustomerRow> of(String search, CustomerStatus status, Tier tier) {
        String text = search == null ? "" : search.strip().toLowerCase(Locale.ROOT);
        String phoneText = text.replaceAll("[()\\s-]", "");
        return row -> {
            var c = row.customer();
            boolean found = text.isEmpty() || c.fullName().toLowerCase(Locale.ROOT).contains(text)
                    || c.phone().contains(text)
                    || (!phoneText.isEmpty() && phoneText.matches("\\+?[0-9]+") && c.phone().contains(phoneText))
                    || (c.email() != null && c.email().toLowerCase(Locale.ROOT).contains(text));
            return found && (status == null || c.status() == status) && (tier == null || row.hasTier(tier));
        };
    }
}
