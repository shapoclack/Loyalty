package org.example.repository;

import org.example.db.Jdbc;
import org.example.model.RedemptionRequest;
import org.example.model.RequestStatus;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class RedemptionRequestRepository {

    public RedemptionRequest insert(Connection c, int accountId, int customerId, int ruleId, int operationId,
                                    BigDecimal bonusAmount) {
        return Jdbc.returning(c, """
                INSERT INTO redemption_request (account_id, customer_id, rule_id, operation_id, bonus_amount, status)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING *""", RedemptionRequestRepository::map,
                accountId, customerId, ruleId, operationId, bonusAmount, RequestStatus.PENDING);
    }

    public Optional<RedemptionRequest> lockById(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT * FROM redemption_request WHERE request_id = ? FOR UPDATE",
                RedemptionRequestRepository::map, id);
    }

    public RedemptionRequest markProcessed(Connection c, int id, RequestStatus status) {
        return Jdbc.returning(c, """
                UPDATE redemption_request SET status = ?, processed_at = now()
                WHERE request_id = ?
                RETURNING *""", RedemptionRequestRepository::map, status, id);
    }

    public List<RedemptionRequest> findPending(Connection c) {
        return Jdbc.query(c, "SELECT * FROM redemption_request WHERE status = ? ORDER BY request_id",
                RedemptionRequestRepository::map, RequestStatus.PENDING);
    }

    public List<RedemptionRequest> findByAccount(Connection c, int accountId) {
        return Jdbc.query(c, "SELECT * FROM redemption_request WHERE account_id = ? ORDER BY request_id",
                RedemptionRequestRepository::map, accountId);
    }

    /** Сумма бонусов, уже списанных или зарезервированных по операции. */
    public BigDecimal sumReservedForOperation(Connection c, int operationId) {
        return Jdbc.returning(c, """
                SELECT COALESCE(SUM(bonus_amount), 0) FROM redemption_request
                WHERE operation_id = ? AND status IN (?, ?)""",
                rs -> rs.getBigDecimal(1), operationId, RequestStatus.PENDING, RequestStatus.APPROVED);
    }

    static RedemptionRequest map(ResultSet rs) throws SQLException {
        return new RedemptionRequest(
                rs.getInt("request_id"),
                rs.getInt("account_id"),
                rs.getInt("customer_id"),
                rs.getInt("rule_id"),
                rs.getInt("operation_id"),
                rs.getBigDecimal("bonus_amount"),
                RequestStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", LocalDateTime.class),
                rs.getObject("processed_at", LocalDateTime.class));
    }
}
