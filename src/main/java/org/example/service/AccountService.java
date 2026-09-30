package org.example.service;

import org.example.db.Database;
import org.example.model.AccountStatus;
import org.example.model.AccrualRequest;
import org.example.model.BonusTransaction;
import org.example.model.Customer;
import org.example.model.LoyaltyAccount;
import org.example.model.RedemptionRequest;
import org.example.repository.AccountRepository;
import org.example.repository.AccrualRequestRepository;
import org.example.repository.CustomerRepository;
import org.example.repository.RedemptionRequestRepository;
import org.example.repository.TransactionRepository;

import java.sql.Connection;
import java.util.List;

public class AccountService {

    private static final int CARD_ATTEMPTS = 10;

    private final Database db;
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final AccrualRequestRepository accrualRequests;
    private final RedemptionRequestRepository redemptionRequests;
    private final CardNumberGenerator cardNumbers;

    public AccountService(Database db, AccountRepository accounts, CustomerRepository customers,
                          TransactionRepository transactions, AccrualRequestRepository accrualRequests,
                          RedemptionRequestRepository redemptionRequests, CardNumberGenerator cardNumbers) {
        this.db = db;
        this.accounts = accounts;
        this.customers = customers;
        this.transactions = transactions;
        this.accrualRequests = accrualRequests;
        this.redemptionRequests = redemptionRequests;
        this.cardNumbers = cardNumbers;
    }

    public LoyaltyAccount open(int customerId) {
        return db.inTransaction(c -> {
            Customer customer = customers.findById(c, customerId)
                    .orElseThrow(() -> new LoyaltyException("Клиент #" + customerId + " не найден"));
            return open(c, customer);
        });
    }

    LoyaltyAccount open(Connection c, Customer customer) {
        if (!customer.isActive()) {
            throw new LoyaltyException("Клиент заблокирован — открыть счёт нельзя");
        }
        for (int i = 0; i < CARD_ATTEMPTS; i++) {
            String card = cardNumbers.next();
            if (!accounts.cardExists(c, card)) {
                return accounts.insert(c, customer.id(), card);
            }
        }
        throw new LoyaltyException("Не удалось подобрать свободный номер карты");
    }

    public LoyaltyAccount getByCard(String cardNumber) {
        return db.inTransaction(c -> findByCard(c, cardNumber));
    }

    LoyaltyAccount findByCard(Connection c, String cardNumber) {
        String card = cardNumber == null ? "" : cardNumber.replaceAll("\\s", "");
        return accounts.findByCard(c, card)
                .orElseThrow(() -> new LoyaltyException("Карта " + card + " не найдена"));
    }

    public List<LoyaltyAccount> byCustomer(int customerId) {
        return db.inTransaction(c -> accounts.findByCustomer(c, customerId));
    }

    public void setStatus(int accountId, AccountStatus status) {
        db.inTransactionVoid(c -> {
            LoyaltyAccount account = accounts.lockById(c, accountId)
                    .orElseThrow(() -> new LoyaltyException("Счёт #" + accountId + " не найден"));
            if (account.status() == AccountStatus.CLOSED) {
                throw new LoyaltyException("Счёт закрыт — статус изменить нельзя");
            }
            accounts.updateStatus(c, accountId, status);
        });
    }

    public List<BonusTransaction> transactions(int accountId) {
        return db.inTransaction(c -> transactions.findByAccount(c, accountId));
    }

    public List<AccrualRequest> accrualRequests(int accountId) {
        return db.inTransaction(c -> accrualRequests.findByAccount(c, accountId));
    }

    public List<RedemptionRequest> redemptionRequests(int accountId) {
        return db.inTransaction(c -> redemptionRequests.findByAccount(c, accountId));
    }
}
