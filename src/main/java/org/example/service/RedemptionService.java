package org.example.service;

import org.example.db.Database;
import org.example.model.Customer;
import org.example.model.LoyaltyAccount;
import org.example.model.RedemptionRequest;
import org.example.model.RedemptionRule;
import org.example.model.RequestStatus;
import org.example.model.SourceOperation;
import org.example.model.TransactionType;
import org.example.repository.AccountRepository;
import org.example.repository.CustomerRepository;
import org.example.repository.OperationRepository;
import org.example.repository.RedemptionRequestRepository;
import org.example.repository.RedemptionRuleRepository;
import org.example.repository.TransactionRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Списание бонусов в два шага: заявка (резерв лимита) -> подтверждение или отмена. */
public class RedemptionService {

    private final Database db;
    private final RedemptionRuleRepository rules;
    private final RedemptionRequestRepository requests;
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final OperationRepository operations;
    private final TransactionRepository transactions;
    private final Clock clock;

    public RedemptionService(Database db, RedemptionRuleRepository rules, RedemptionRequestRepository requests,
                             AccountRepository accounts, CustomerRepository customers,
                             OperationRepository operations, TransactionRepository transactions, Clock clock) {
        this.db = db;
        this.rules = rules;
        this.requests = requests;
        this.accounts = accounts;
        this.customers = customers;
        this.operations = operations;
        this.transactions = transactions;
        this.clock = clock;
    }

    /** Сколько бонусов можно списать по операции с учётом правила, баланса и уже зарезервированного. */
    public static BigDecimal limit(RedemptionRule rule, BigDecimal balance, BigDecimal operationAmount,
                                   BigDecimal alreadyReserved) {
        BigDecimal limit = operationAmount.multiply(rule.maxPercent()).movePointLeft(2).subtract(alreadyReserved);
        if (rule.maxAmount() != null) {
            limit = limit.min(rule.maxAmount().subtract(alreadyReserved));
        }
        return Money.nonNegative(Money.bonus(limit.min(balance)));
    }

    public RedemptionRequest create(int accountId, int operationId, BigDecimal amount) {
        return db.inTransaction(c -> create(c, accountId, operationId, amount));
    }

    public RedemptionRequest confirm(int requestId) {
        return db.inTransaction(c -> confirm(c, requestId));
    }

    public RedemptionRequest cancel(int requestId) {
        return db.inTransaction(c -> {
            RedemptionRequest request = lockPending(c, requestId);
            return requests.markProcessed(c, request.id(), RequestStatus.CANCELLED);
        });
    }

    public List<RedemptionRequest> pending() {
        return db.inTransaction(requests::findPending);
    }

    Optional<RedemptionRule> findCurrentRule(Connection c) {
        return rules.findCurrent(c);
    }

    RedemptionRule currentRule(Connection c) {
        return findCurrentRule(c).orElseThrow(() -> new LoyaltyException("Нет активного правила списания"));
    }

    RedemptionRequest create(Connection c, int accountId, int operationId, BigDecimal amount) {
        LoyaltyAccount account = accounts.findById(c, accountId)
                .orElseThrow(() -> new LoyaltyException("Счёт #" + accountId + " не найден"));
        Customer customer = customers.findById(c, account.customerId()).orElseThrow();
        if (!account.isActive() || !customer.isActive()) {
            throw new LoyaltyException("Счёт или клиент заблокирован — списание невозможно");
        }
        SourceOperation operation = operations.findById(c, operationId)
                .orElseThrow(() -> new LoyaltyException("Операция #" + operationId + " не найдена"));
        if (operation.customerId() != account.customerId()) {
            throw new LoyaltyException("Операция принадлежит другому клиенту");
        }
        RedemptionRule rule = currentRule(c);
        BigDecimal bonus = Money.bonus(amount);
        if (bonus.signum() <= 0) {
            throw new LoyaltyException("Сумма списания должна быть больше нуля");
        }
        if (bonus.compareTo(rule.minAmount()) < 0) {
            throw new LoyaltyException("Минимальная сумма списания: " + rule.minAmount());
        }
        BigDecimal reserved = requests.sumReservedForOperation(c, operation.id());
        BigDecimal limit = limit(rule, account.balance(), operation.amount(), reserved);
        if (bonus.compareTo(limit) > 0) {
            throw new LoyaltyException("Можно списать не более " + limit + " бонусов");
        }
        return requests.insert(c, account.id(), account.customerId(), rule.id(), operation.id(), bonus);
    }

    /** Подтверждает заявку. Просроченная заявка получает статус EXPIRED, бонусы не списываются. */
    RedemptionRequest confirm(Connection c, int requestId) {
        RedemptionRequest request = lockPending(c, requestId);
        RedemptionRule rule = rules.findById(c, request.ruleId()).orElseThrow();
        if (request.createdAt().plusDays(rule.expireDays()).isBefore(LocalDateTime.now(clock))) {
            return requests.markProcessed(c, request.id(), RequestStatus.EXPIRED);
        }
        LoyaltyAccount account = accounts.lockById(c, request.accountId()).orElseThrow();
        if (!account.isActive()) {
            throw new LoyaltyException("Счёт заблокирован — списание невозможно");
        }
        if (account.balance().compareTo(request.bonusAmount()) < 0) {
            throw new LoyaltyException("Недостаточно бонусов: на счёте " + account.balance());
        }
        BigDecimal newBalance = account.balance().subtract(request.bonusAmount());
        accounts.updateBalance(c, account.id(), newBalance);
        transactions.insert(c, account.id(), null, request.id(), TransactionType.REDEMPTION,
                request.bonusAmount().negate(), newBalance);
        return requests.markProcessed(c, request.id(), RequestStatus.APPROVED);
    }

    private RedemptionRequest lockPending(Connection c, int requestId) {
        RedemptionRequest request = requests.lockById(c, requestId)
                .orElseThrow(() -> new LoyaltyException("Заявка #" + requestId + " не найдена"));
        if (request.status() != RequestStatus.PENDING) {
            throw new LoyaltyException("Заявка уже обработана, статус: " + request.status());
        }
        return request;
    }
}
