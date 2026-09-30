package org.example.repository;

import org.example.db.Jdbc;
import org.example.model.BonusTransaction;
import org.example.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class TransactionRepository {

    public BonusTransaction insert(Connection c, int accountId, Integer accrualRequestId, Integer redemptionRequestId,
                                   TransactionType type, BigDecimal amount, BigDecimal balanceAfter) {
        return Jdbc.returning(c, """
                INSERT INTO bonus_transaction
                    (account_id, accrual_request_id, redemption_request_id, transaction_type, amount, balance_after)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING *""", TransactionRepository::map,
                accountId, accrualRequestId, redemptionRequestId, type, amount, balanceAfter);
    }

    public List<BonusTransaction> findAll(Connection c) {
        return Jdbc.query(c, "SELECT * FROM bonus_transaction ORDER BY transaction_id", TransactionRepository::map);
    }

    public List<BonusTransaction> findByAccount(Connection c, int accountId) {
        return Jdbc.query(c, "SELECT * FROM bonus_transaction WHERE account_id = ? ORDER BY transaction_id",
                TransactionRepository::map, accountId);
    }

    static BonusTransaction map(ResultSet rs) throws SQLException {
        return new BonusTransaction(
                rs.getInt("transaction_id"),
                rs.getInt("account_id"),
                rs.getObject("accrual_request_id", Integer.class),
                rs.getObject("redemption_request_id", Integer.class),
                TransactionType.valueOf(rs.getString("transaction_type")),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("balance_after"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
