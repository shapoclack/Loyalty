package org.example.repository;

import org.example.db.Jdbc;
import org.example.db.QueryBuilder;
import org.example.model.OperationType;
import org.example.model.SourceOperation;
import org.example.model.query.OperationFilter;
import org.example.model.query.OperationSort;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class OperationRepository {

    public SourceOperation insert(Connection c, int customerId, OperationType type, String externalId,
                                  BigDecimal amount, LocalDateTime date, String storeCode) {
        return Jdbc.returning(c, """
                INSERT INTO source_operation (customer_id, operation_type, external_id, amount, operation_date, store_code)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING *""", OperationRepository::map, customerId, type, externalId, amount, date, storeCode);
    }

    public Optional<SourceOperation> findById(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT * FROM source_operation WHERE operation_id = ?",
                OperationRepository::map, id);
    }

    public boolean externalIdExists(Connection c, String externalId) {
        return Jdbc.queryOne(c, "SELECT 1 FROM source_operation WHERE external_id = ?",
                rs -> true, externalId).isPresent();
    }

    public List<SourceOperation> findByCustomer(Connection c, int customerId) {
        return Jdbc.query(c, "SELECT * FROM source_operation WHERE customer_id = ? ORDER BY operation_date, operation_id",
                OperationRepository::map, customerId);
    }

    public List<SourceOperation> find(Connection c, OperationFilter filter, OperationSort sort) {
        QueryBuilder query = new QueryBuilder("SELECT * FROM source_operation")
                .where("operation_date >= ?", filter.from() == null ? null : filter.from().atStartOfDay())
                .where("operation_date < ?", filter.to() == null ? null : filter.to().plusDays(1).atStartOfDay())
                .where("store_code = ?", filter.storeCode())
                .where("amount >= ?", filter.minAmount())
                .orderBy(sort.orderBy());
        return Jdbc.query(c, query.sql(), OperationRepository::map, query.params());
    }

    public BigDecimal sumByCustomer(Connection c, int customerId, OperationType type) {
        return Jdbc.returning(c, """
                SELECT COALESCE(SUM(amount), 0) FROM source_operation
                WHERE customer_id = ? AND operation_type = ?""", rs -> rs.getBigDecimal(1), customerId, type);
    }

    static SourceOperation map(ResultSet rs) throws SQLException {
        return new SourceOperation(
                rs.getInt("operation_id"),
                rs.getInt("customer_id"),
                OperationType.valueOf(rs.getString("operation_type")),
                rs.getString("external_id"),
                rs.getBigDecimal("amount"),
                rs.getObject("operation_date", LocalDateTime.class),
                rs.getString("store_code"));
    }
}
