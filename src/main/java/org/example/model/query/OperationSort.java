package org.example.model.query;

/** Варианты сортировки журнала операций. */
public enum OperationSort {
    DATE_DESC("сначала новые", "operation_date DESC, operation_id DESC"),
    DATE_ASC("сначала старые", "operation_date, operation_id"),
    AMOUNT_DESC("по сумме (убыв.)", "amount DESC, operation_id");

    private final String label;
    private final String orderBy;

    OperationSort(String label, String orderBy) {
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
