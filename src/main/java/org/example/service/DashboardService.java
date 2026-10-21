package org.example.service;

import org.example.db.Database;
import org.example.model.*;
import org.example.model.query.*;
import org.example.repository.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** Согласованный снимок для списка и статистики; SQL остаётся в репозиториях. */
public final class DashboardService {
    public record CustomerRow(Customer customer, List<LoyaltyAccount> accounts) {
        public BigDecimal balance() {
            return accounts.stream().map(LoyaltyAccount::balance).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        public String tiers() {
            return accounts.stream().map(a -> a.tier().name()).distinct().sorted().collect(Collectors.joining(", "));
        }
        public boolean hasTier(Tier tier) { return accounts.stream().anyMatch(a -> a.tier() == tier); }
    }
    public record Snapshot(List<CustomerRow> customers, List<SourceOperation> operations) { }
    private final Database db;
    private boolean initialized;
    public DashboardService(Database db) { this.db = db; }

    public Snapshot load() {
        if (!initialized) {
            db.runScript("/db/schema.sql");
            db.runScript("/db/seed.sql");
            initialized = true;
        }
        return db.inTransaction(c -> {
            var customers = new CustomerRepository().findAll(c, null, CustomerSort.ID);
            var accounts = new AccountRepository().findAll(c, null, null, AccountSort.OPENED);
            var grouped = accounts.stream().collect(Collectors.groupingBy(LoyaltyAccount::customerId));
            var rows = customers.stream().map(customer -> new CustomerRow(customer,
                    List.copyOf(grouped.getOrDefault(customer.id(), List.of())))).toList();
            var operations = new OperationRepository().find(c,
                    new OperationFilter(null, null, null, null), OperationSort.DATE_DESC);
            return new Snapshot(rows, List.copyOf(operations));
        });
    }
}
