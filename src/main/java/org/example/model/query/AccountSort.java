package org.example.model.query;

/** Варианты сортировки бонусных счетов. */
public enum AccountSort {
    BALANCE_DESC("по балансу (убыв.)", "balance DESC, account_id"),
    TIER("по уровню (GOLD → BASE)",
            "CASE tier WHEN 'GOLD' THEN 1 WHEN 'SILVER' THEN 2 ELSE 3 END, balance DESC, account_id"),
    OPENED("по дате открытия", "opened_at, account_id");

    private final String label;
    private final String orderBy;

    AccountSort(String label, String orderBy) {
        this.label = label;
        this.orderBy = orderBy;
    }

    public String label() {
        return label;
    }

    public String orderBy() {
        return orderBy;
    }
}
