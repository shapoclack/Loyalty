package org.example.model.query;

/** Варианты сортировки клиентов; SQL-фрагмент берётся только из этого перечисления. */
public enum CustomerSort {
    ID("по номеру", "customer_id"),
    NAME("по ФИО", "full_name, customer_id"),
    NEWEST("сначала новые", "created_at DESC, customer_id DESC");

    private final String label;
    private final String orderBy;

    CustomerSort(String label, String orderBy) {
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
