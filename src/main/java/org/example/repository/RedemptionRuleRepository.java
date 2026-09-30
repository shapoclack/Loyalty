package org.example.repository;

import org.example.db.Jdbc;
import org.example.model.RedemptionRule;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class RedemptionRuleRepository {

    public RedemptionRule insert(Connection c, String name, BigDecimal maxPercent, BigDecimal minAmount,
                                 BigDecimal maxAmount, int expireDays) {
        return Jdbc.returning(c, """
                INSERT INTO redemption_rule (name, max_percent, min_amount, max_amount, expire_days, is_active)
                VALUES (?, ?, ?, ?, ?, TRUE)
                RETURNING *""", RedemptionRuleRepository::map, name, maxPercent, minAmount, maxAmount, expireDays);
    }

    public List<RedemptionRule> findAll(Connection c) {
        return Jdbc.query(c, "SELECT * FROM redemption_rule ORDER BY rule_id", RedemptionRuleRepository::map);
    }

    public Optional<RedemptionRule> findById(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT * FROM redemption_rule WHERE rule_id = ?", RedemptionRuleRepository::map, id);
    }

    /** Действующее правило списания — самое новое из активных. */
    public Optional<RedemptionRule> findCurrent(Connection c) {
        return Jdbc.queryOne(c, "SELECT * FROM redemption_rule WHERE is_active ORDER BY rule_id DESC LIMIT 1",
                RedemptionRuleRepository::map);
    }

    /** Использовалось ли правило хотя бы в одной заявке. */
    public boolean isUsed(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT 1 FROM redemption_request WHERE rule_id = ? LIMIT 1", rs -> true, id).isPresent();
    }

    public int delete(Connection c, int id) {
        return Jdbc.update(c, "DELETE FROM redemption_rule WHERE rule_id = ?", id);
    }

    public int setActive(Connection c, int id, boolean active) {
        return Jdbc.update(c, "UPDATE redemption_rule SET is_active = ? WHERE rule_id = ?", active, id);
    }

    static RedemptionRule map(ResultSet rs) throws SQLException {
        return new RedemptionRule(
                rs.getInt("rule_id"),
                rs.getString("name"),
                rs.getBigDecimal("max_percent"),
                rs.getBigDecimal("min_amount"),
                rs.getBigDecimal("max_amount"),
                rs.getInt("expire_days"),
                rs.getBoolean("is_active"));
    }
}
