package org.example.repository;

import org.example.db.Jdbc;
import org.example.model.AccountStatus;
import org.example.model.LoyaltyAccount;
import org.example.model.Tier;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class AccountRepository {

    public LoyaltyAccount insert(Connection c, int customerId, String cardNumber) {
        return Jdbc.returning(c, """
                INSERT INTO loyalty_account (customer_id, card_number, balance, tier, status)
                VALUES (?, ?, 0, ?, ?)
                RETURNING *""", AccountRepository::map, customerId, cardNumber, Tier.BASE, AccountStatus.ACTIVE);
    }

    public Optional<LoyaltyAccount> findById(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT * FROM loyalty_account WHERE account_id = ?", AccountRepository::map, id);
    }

    /** Блокирует строку счёта до конца транзакции — для изменения баланса. */
    public Optional<LoyaltyAccount> lockById(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT * FROM loyalty_account WHERE account_id = ? FOR UPDATE",
                AccountRepository::map, id);
    }

    public Optional<LoyaltyAccount> findByCard(Connection c, String cardNumber) {
        return Jdbc.queryOne(c, "SELECT * FROM loyalty_account WHERE card_number = ?",
                AccountRepository::map, cardNumber);
    }

    public boolean cardExists(Connection c, String cardNumber) {
        return findByCard(c, cardNumber).isPresent();
    }

    public List<LoyaltyAccount> findByCustomer(Connection c, int customerId) {
        return Jdbc.query(c, "SELECT * FROM loyalty_account WHERE customer_id = ? ORDER BY account_id",
                AccountRepository::map, customerId);
    }

    public void updateBalance(Connection c, int id, BigDecimal balance) {
        Jdbc.update(c, "UPDATE loyalty_account SET balance = ?, updated_at = now() WHERE account_id = ?", balance, id);
    }

    public void updateTier(Connection c, int id, Tier tier) {
        Jdbc.update(c, "UPDATE loyalty_account SET tier = ?, updated_at = now() WHERE account_id = ?", tier, id);
    }

    public void updateStatus(Connection c, int id, AccountStatus status) {
        Jdbc.update(c, "UPDATE loyalty_account SET status = ?, updated_at = now() WHERE account_id = ?", status, id);
    }

    static LoyaltyAccount map(ResultSet rs) throws SQLException {
        return new LoyaltyAccount(
                rs.getInt("account_id"),
                rs.getInt("customer_id"),
                rs.getString("card_number"),
                rs.getBigDecimal("balance"),
                Tier.valueOf(rs.getString("tier")),
                AccountStatus.valueOf(rs.getString("status")),
                rs.getObject("opened_at", LocalDateTime.class),
                rs.getObject("updated_at", LocalDateTime.class));
    }
}
