package org.example.service;

import org.example.db.Database;
import org.example.exception.BusinessException;
import org.example.exception.EntityNotFoundException;
import org.example.model.Customer;
import org.example.model.CustomerStatus;
import org.example.model.LoyaltyAccount;
import org.example.model.SourceOperation;
import org.example.model.query.CustomerSort;
import org.example.repository.CustomerRepository;
import org.example.repository.OperationRepository;

import java.time.LocalDate;
import java.util.List;

public class CustomerService {

    public record Registration(Customer customer, LoyaltyAccount account) {
    }

    private final Database db;
    private final CustomerRepository customers;
    private final OperationRepository operations;
    private final AccountService accountService;

    public CustomerService(Database db, CustomerRepository customers, OperationRepository operations,
                           AccountService accountService) {
        this.db = db;
        this.customers = customers;
        this.operations = operations;
        this.accountService = accountService;
    }

    /** Регистрирует клиента и сразу открывает ему бонусный счёт. */
    public Registration register(String fullName, String phone, String email, LocalDate birthDate) {
        CustomerValidation.validate(fullName, phone, email, birthDate);
        String normalizedPhone = normalizePhone(phone);
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException("Дата рождения не может быть в будущем");
        }
        return db.inTransaction(c -> {
            if (customers.findByPhone(c, normalizedPhone).isPresent()) {
                throw new BusinessException("Клиент с телефоном " + normalizedPhone + " уже зарегистрирован");
            }
            Customer customer = customers.insert(c, fullName.trim(), normalizedPhone, blankToNull(email), birthDate);
            LoyaltyAccount account = accountService.open(c, customer);
            return new Registration(customer, account);
        });
    }

    public Customer get(int customerId) {
        return db.inTransaction(c -> customers.findById(c, customerId))
                .orElseThrow(() -> new EntityNotFoundException("Клиент #" + customerId + " не найден"));
    }

    public Customer update(int id, String name, String phone, String email, LocalDate birthDate,
                           CustomerStatus status) {
        CustomerValidation.validate(name, phone, email, birthDate);
        if (status == null) throw new BusinessException("Выберите статус клиента");
        String normalized = normalizePhone(phone);
        return db.inTransaction(c -> {
            customers.findById(c, id)
                    .orElseThrow(() -> new EntityNotFoundException("Клиент не найден. Обновите список"));
            customers.findByPhone(c, normalized).filter(other -> other.id() != id).ifPresent(other -> {
                throw new BusinessException("Телефон уже используется другим клиентом");
            });
            return customers.update(c, id, name.trim(), normalized, blankToNull(email), birthDate, status);
        });
    }

    public Customer getByPhone(String phone) {
        String normalized = normalizePhone(phone);
        return db.inTransaction(c -> customers.findByPhone(c, normalized))
                .orElseThrow(() -> new EntityNotFoundException("Клиент с телефоном " + normalized + " не найден"));
    }

    public List<Customer> search(String text) {
        return db.inTransaction(c -> text == null || text.isBlank()
                ? customers.findAll(c, null, CustomerSort.ID)
                : customers.search(c, text.trim()));
    }

    /** Список клиентов с фильтром по статусу (null — все). */
    public List<Customer> list(CustomerStatus status, CustomerSort sort) {
        return db.inTransaction(c -> customers.findAll(c, status, sort));
    }

    public void setStatus(int customerId, CustomerStatus status) {
        db.inTransactionVoid(c -> {
            customers.findById(c, customerId)
                    .orElseThrow(() -> new EntityNotFoundException("Клиент #" + customerId + " не найден"));
            customers.updateStatus(c, customerId, status);
        });
    }

    /**
     * Удаляет ошибочно созданного клиента вместе с его счетами. Клиента с операциями
     * удалить нельзя — история начислений должна сохраняться, такого клиента блокируют.
     */
    public void delete(int customerId) {
        db.inTransactionVoid(c -> {
            customers.findById(c, customerId)
                    .orElseThrow(() -> new EntityNotFoundException("Клиент #" + customerId + " не найден"));
            if (!operations.findByCustomer(c, customerId).isEmpty()) {
                throw new BusinessException("У клиента есть операции — удалить нельзя, его можно заблокировать");
            }
            accountService.deleteAllOf(c, customerId);
            customers.delete(c, customerId);
        });
    }

    public List<SourceOperation> operations(int customerId) {
        return db.inTransaction(c -> operations.findByCustomer(c, customerId));
    }

    static String normalizePhone(String phone) {
        return CustomerValidation.phone(phone);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
