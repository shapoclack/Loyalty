package org.example.service;

import org.example.db.Database;
import org.example.exception.BusinessException;
import org.example.model.SourceOperation;
import org.example.model.query.OperationFilter;
import org.example.model.query.OperationSort;
import org.example.repository.OperationRepository;

import java.util.List;

/** Журнал исходных операций: поиск с фильтрами и сортировкой. */
public class OperationService {

    private final Database db;
    private final OperationRepository operations;

    public OperationService(Database db, OperationRepository operations) {
        this.db = db;
        this.operations = operations;
    }

    public List<SourceOperation> find(OperationFilter filter, OperationSort sort) {
        if (filter.from() != null && filter.to() != null && filter.to().isBefore(filter.from())) {
            throw new BusinessException("Дата окончания периода раньше даты начала");
        }
        return db.inTransaction(c -> operations.find(c, filter, sort));
    }
}
