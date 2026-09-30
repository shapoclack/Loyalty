package org.example.service;

import org.example.db.Database;
import org.example.model.AccrualRequest;
import org.example.model.LoyaltyAccount;
import org.example.model.OperationType;
import org.example.model.RedemptionRequest;
import org.example.model.SourceOperation;
import org.example.model.Tier;
import org.example.repository.AccountRepository;
import org.example.repository.OperationRepository;
import org.example.service.tier.TierPolicy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Сценарий покупки: регистрирует исходную операцию, при необходимости списывает бонусы
 * в счёт оплаты, начисляет бонусы на оплаченную деньгами часть и пересчитывает уровень.
 */
public class PurchaseService {

    public record PurchaseResult(SourceOperation operation, RedemptionRequest redemption, AccrualRequest accrual,
                                 Tier previousTier, LoyaltyAccount account) {

        public BigDecimal paidByMoney() {
            return redemption == null ? operation.amount() : operation.amount().subtract(redemption.bonusAmount());
        }
    }

    private final Database db;
    private final AccountService accountService;
    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final AccrualService accrualService;
    private final RedemptionService redemptionService;
    private final TierPolicy tierPolicy;
    private final Clock clock;

    public PurchaseService(Database db, AccountService accountService, AccountRepository accounts,
                           OperationRepository operations, AccrualService accrualService,
                           RedemptionService redemptionService, TierPolicy tierPolicy, Clock clock) {
        this.db = db;
        this.accountService = accountService;
        this.accounts = accounts;
        this.operations = operations;
        this.accrualService = accrualService;
        this.redemptionService = redemptionService;
        this.tierPolicy = tierPolicy;
        this.clock = clock;
    }

    /** Сколько бонусов можно списать при покупке на указанную сумму. */
    public BigDecimal redeemable(String cardNumber, BigDecimal amount) {
        return db.inTransaction(c -> {
            LoyaltyAccount account = accountService.findByCard(c, cardNumber);
            if (!account.isActive()) {
                return BigDecimal.ZERO;
            }
            return redemptionService.findCurrentRule(c)
                    .filter(rule -> account.balance().compareTo(rule.minAmount()) >= 0)
                    .map(rule -> RedemptionService.limit(rule, account.balance(), amount, BigDecimal.ZERO))
                    .orElse(BigDecimal.ZERO);
        });
    }

    public PurchaseResult purchase(String cardNumber, BigDecimal amount, String externalId, String storeCode,
                                   BigDecimal bonusToRedeem) {
        if (amount == null || amount.signum() <= 0) {
            throw new LoyaltyException("Сумма покупки должна быть больше нуля");
        }
        return db.inTransaction(c -> {
            LoyaltyAccount account = accountService.findByCard(c, cardNumber);
            if (externalId != null && operations.externalIdExists(c, externalId)) {
                throw new LoyaltyException("Операция с внешним номером " + externalId + " уже зарегистрирована");
            }
            SourceOperation operation = operations.insert(c, account.customerId(), OperationType.PURCHASE,
                    externalId, amount, LocalDateTime.now(clock), storeCode);

            RedemptionRequest redemption = null;
            BigDecimal paidByMoney = amount;
            if (bonusToRedeem != null && bonusToRedeem.signum() > 0) {
                RedemptionRequest created = redemptionService.create(c, account.id(), operation.id(), bonusToRedeem);
                redemption = redemptionService.confirm(c, created.id());
                paidByMoney = amount.subtract(redemption.bonusAmount());
            }

            AccrualRequest accrual = accrualService.accrue(c, account.id(), operation, paidByMoney);
            refreshTier(c, account);
            LoyaltyAccount updated = accounts.findById(c, account.id()).orElseThrow();
            return new PurchaseResult(operation, redemption, accrual, account.tier(), updated);
        });
    }

    private void refreshTier(Connection c, LoyaltyAccount account) {
        BigDecimal total = operations.sumByCustomer(c, account.customerId(), OperationType.PURCHASE);
        Tier tier = tierPolicy.resolve(total);
        if (tier != account.tier()) {
            accounts.updateTier(c, account.id(), tier);
        }
    }
}
