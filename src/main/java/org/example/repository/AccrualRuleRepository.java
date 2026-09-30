package org.example.repository;

import org.example.db.Jdbc;
import org.example.model.AccrualRule;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AccrualRuleRepository {

    public AccrualRule insert(Connection c, String name, String ruleType, BigDecimal value, String category,
                              LocalDateTime validFrom, LocalDateTime validTo) {
        return Jdbc.returning(c, """
                INSERT INTO accrual_rule (name, rule_type, value, category, valid_from, valid_to, is_active)
                VALUES (?, ?, ?, ?, ?, ?, TRUE)
                RETURNING *""", AccrualRuleRepository::map, name, ruleType, value, category, validFrom, validTo);
    }

    public List<AccrualRule> findAll(Connection c) {
        return Jdbc.query(c, "SELECT * FROM accrual_rule ORDER BY rule_id", AccrualRuleRepository::map);
    }

    public List<AccrualRule> findActive(Connection c) {
        return Jdbc.query(c, "SELECT * FROM accrual_rule WHERE is_active ORDER BY rule_id", AccrualRuleRepository::map);
    }

    /** Использовалось ли правило хотя бы в одной заявке. */
    public boolean isUsed(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT 1 FROM accrual_request WHERE rule_id = ? LIMIT 1", rs -> true, id).isPresent();
    }

    public int delete(Connection c, int id) {
        return Jdbc.update(c, "DELETE FROM accrual_rule WHERE rule_id = ?", id);
    }

    public int setActive(Connection c, int id, boolean active) {
        return Jdbc.update(c, "UPDATE accrual_rule SET is_active = ? WHERE rule_id = ?", active, id);
    }

    static AccrualRule map(ResultSet rs) throws SQLException {
        return new AccrualRule(
                rs.getInt("rule_id"),
                rs.getString("name"),
                rs.getString("rule_type"),
                rs.getBigDecimal("value"),
                rs.getString("category"),
                rs.getObject("valid_from", LocalDateTime.class),
                rs.getObject("valid_to", LocalDateTime.class),
                rs.getBoolean("is_active"));
    }
}
