package org.example.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Собирает SELECT с необязательными условиями. Значения фильтров передаются
 * только как параметры (?); в текст запроса попадают лишь фрагменты из кода.
 */
public class QueryBuilder {

    private final StringBuilder sql;
    private final List<Object> params = new ArrayList<>();
    private boolean hasWhere;

    public QueryBuilder(String select) {
        this.sql = new StringBuilder(select);
    }

    /** Добавляет условие вида "column = ?", если значение задано. */
    public QueryBuilder where(String condition, Object value) {
        if (value != null) {
            sql.append(hasWhere ? " AND " : " WHERE ").append(condition);
            params.add(value);
            hasWhere = true;
        }
        return this;
    }

    public QueryBuilder orderBy(String orderBy) {
        sql.append(" ORDER BY ").append(orderBy);
        return this;
    }

    public String sql() {
        return sql.toString();
    }

    public Object[] params() {
        return params.toArray();
    }
}
