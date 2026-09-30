package org.example.repository;

import org.example.db.Jdbc;
import org.example.db.QueryBuilder;
import org.example.model.Customer;
import org.example.model.CustomerStatus;
import org.example.model.query.CustomerSort;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class CustomerRepository {

    public Customer insert(Connection c, String fullName, String phone, String email, LocalDate birthDate) {
        return Jdbc.returning(c, """
                INSERT INTO customer (full_name, phone, email, birth_date, status)
                VALUES (?, ?, ?, ?, ?)
                RETURNING *""", CustomerRepository::map, fullName, phone, email, birthDate, CustomerStatus.ACTIVE);
    }

    public Optional<Customer> findById(Connection c, int id) {
        return Jdbc.queryOne(c, "SELECT * FROM customer WHERE customer_id = ?", CustomerRepository::map, id);
    }

    public Optional<Customer> findByPhone(Connection c, String phone) {
        return Jdbc.queryOne(c, "SELECT * FROM customer WHERE phone = ?", CustomerRepository::map, phone);
    }

    public List<Customer> search(Connection c, String text) {
        String pattern = "%" + text + "%";
        return Jdbc.query(c, """
                SELECT * FROM customer
                WHERE full_name ILIKE ? OR phone LIKE ? OR email ILIKE ?
                ORDER BY customer_id""", CustomerRepository::map, pattern, pattern, pattern);
    }

    public List<Customer> findAll(Connection c, CustomerStatus status, CustomerSort sort) {
        QueryBuilder query = new QueryBuilder("SELECT * FROM customer")
                .where("status = ?", status)
                .orderBy(sort.orderBy());
        return Jdbc.query(c, query.sql(), CustomerRepository::map, query.params());
    }

    public void delete(Connection c, int id) {
        Jdbc.update(c, "DELETE FROM customer WHERE customer_id = ?", id);
    }

    public void updateStatus(Connection c, int id, CustomerStatus status) {
        Jdbc.update(c, "UPDATE customer SET status = ? WHERE customer_id = ?", status, id);
    }

    static Customer map(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customer_id"),
                rs.getString("full_name"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getObject("birth_date", LocalDate.class),
                rs.getObject("created_at", LocalDateTime.class),
                CustomerStatus.valueOf(rs.getString("status")));
    }
}
