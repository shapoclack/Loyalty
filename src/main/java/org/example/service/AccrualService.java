package org.example.service;

import org.example.model.AccrualRequest;
import org.example.model.AccrualRule;
import org.example.model.Customer;
import org.example.model.LoyaltyAccount;
import org.example.model.RequestStatus;
import org.example.model.SourceOperation;
import org.example.model.TransactionType;
import org.example.repository.AccountRepository;
import org.example.repository.AccrualRequestRepository;
import org.example.repository.AccrualRuleRepository;
import org.example.repository.CustomerRepository;
import org.example.repository.TransactionRepository;
import org.example.service.accrual.AccrualCalculatorRegistry;

import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Comparator;
import java.util.Optional;

/** Начисление бонусов: выбирает самое выгодное из применимых правил. */
public class AccrualService {

    private record Candidate(AccrualRule rule, BigDecimal bonus) {
    }

    private final AccrualRuleRepository rules;
    private final AccrualRequestRepository requests;
    private final AccountRepository accounts;
    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final AccrualCalculatorRegistry calculators;

    public AccrualService(AccrualRuleRepository rules, AccrualRequestRepository requests, AccountRepository accounts,
                          CustomerRepository customers, TransactionRepository transactions,
                          AccrualCalculatorRegistry calculators) {
        this.rules = rules;
        this.requests = requests;
        this.accounts = accounts;
        this.customers = customers;
        this.transactions = transactions;
        this.calculators = calculators;
    }

    /** Создаёт заявку на начисление по операции и сразу обрабатывает её. */
    AccrualRequest accrue(Connection c, int accountId, SourceOperation operation, BigDecimal baseAmount) {
        LoyaltyAccount account = accounts.lockById(c, accountId)
                .orElseThrow(() -> new LoyaltyException("Счёт #" + accountId + " не найден"));
        Customer customer = customers.findById(c, account.customerId()).orElseThrow();

        Optional<Candidate> best = account.isActive() && customer.isActive()
                ? selectRule(c, account, operation, baseAmount)
                : Optional.empty();

        AccrualRequest request = requests.insert(c, account.id(), account.customerId(),
                best.map(b -> b.rule().id()).orElse(null), operation.id(),
                best.map(Candidate::bonus).orElse(BigDecimal.ZERO), RequestStatus.PENDING);
        return process(c, account, request);
    }

    /**
     * Обработка заявки. Вынесена отдельно, чтобы в будущем заявки можно было
     * подтверждать асинхронно или вручную (например, после окончания периода возврата).
     */
    private AccrualRequest process(Connection c, LoyaltyAccount account, AccrualRequest request) {
        if (request.ruleId() == null || request.bonusAmount().signum() <= 0) {
            return requests.markProcessed(c, request.id(), RequestStatus.REJECTED);
        }
        BigDecimal newBalance = account.balance().add(request.bonusAmount());
        accounts.updateBalance(c, account.id(), newBalance);
        transactions.insert(c, account.id(), request.id(), null, TransactionType.ACCRUAL,
                request.bonusAmount(), newBalance);
        return requests.markProcessed(c, request.id(), RequestStatus.APPROVED);
    }

    private Optional<Candidate> selectRule(Connection c, LoyaltyAccount account, SourceOperation operation,
                                           BigDecimal baseAmount) {
        return rules.findActive(c).stream()
                .filter(rule -> rule.isApplicable(operation.operationDate(), account.tier()))
                .flatMap(rule -> calculators.find(rule.ruleType())
                        .map(calc -> new Candidate(rule, Money.bonus(calc.calculate(rule, baseAmount))))
                        .stream())
                .max(Comparator.comparing(Candidate::bonus));
    }
}
