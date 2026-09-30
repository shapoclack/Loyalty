package org.example.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Минимальный помощник для JDBC-запросов с позиционными параметрами. */
public final class Jdbc {

    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    private Jdbc() {
    }

    public static <T> List<T> query(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        try (PreparedStatement ps = prepare(c, sql, params); ResultSet rs = ps.executeQuery()) {
            List<T> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapper.map(rs));
            }
            return result;
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    public static <T> Optional<T> queryOne(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        List<T> rows = query(c, sql, mapper, params);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }

    /** Для INSERT/UPDATE ... RETURNING *: возвращает единственную строку. */
    public static <T> T returning(Connection c, String sql, RowMapper<T> mapper, Object... params) {
        return queryOne(c, sql, mapper, params)
                .orElseThrow(() -> new DataAccessException("Запрос не вернул строку: " + sql, null));
    }

    public static int update(Connection c, String sql, Object... params) {
        try (PreparedStatement ps = prepare(c, sql, params)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException(e);
        }
    }

    private static PreparedStatement prepare(Connection c, String sql, Object... params) throws SQLException {
        PreparedStatement ps = c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            Object value = params[i] instanceof Enum<?> e ? e.name() : params[i];
            ps.setObject(i + 1, value);
        }
        return ps;
    }
}
