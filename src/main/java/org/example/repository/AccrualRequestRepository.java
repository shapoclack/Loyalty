package org.example.repository;

import org.example.db.Jdbc;
import org.example.model.AccrualRequest;
import org.example.model.RequestStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AccrualRequestRepository {

    public AccrualRequest insert(Connection c, int accountId, int customerId, Integer ruleId, int operationId,
                                 BigDecimal bonusAmount, RequestStatus status) {
        return Jdbc.returning(c, """
                INSERT INTO accrual_request (account_id, customer_id, rule_id, operation_id, bonus_amount, status)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING *""", AccrualRequestRepository::map,
                accountId, customerId, ruleId, operationId, bonusAmount, status);
    }

    public AccrualRequest markProcessed(Connection c, int id, RequestStatus status) {
        return Jdbc.returning(c, """
                UPDATE accrual_request SET status = ?, processed_at = now()
                WHERE request_id = ?
                RETURNING *""", AccrualRequestRepository::map, status, id);
    }

    public List<AccrualRequest> findByAccount(Connection c, int accountId) {
        return Jdbc.query(c, "SELECT * FROM accrual_request WHERE account_id = ? ORDER BY request_id",
                AccrualRequestRepository::map, accountId);
    }

    static AccrualRequest map(ResultSet rs) throws SQLException {
        return new AccrualRequest(
                rs.getInt("request_id"),
                rs.getInt("account_id"),
                rs.getInt("customer_id"),
                rs.getObject("rule_id", Integer.class),
                rs.getInt("operation_id"),
                rs.getBigDecimal("bonus_amount"),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("processed_at", LocalDateTime.class));
    }
}
