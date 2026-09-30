package org.example;

import org.example.console.ConsoleIO;
import org.example.console.MenuSection;
import org.example.console.sections.AccountSection;
import org.example.console.sections.CustomerSection;
import org.example.console.sections.PurchaseSection;
import org.example.console.sections.RedemptionSection;
import org.example.console.sections.RuleSection;
import org.example.db.Database;
import org.example.repository.AccountRepository;
import org.example.repository.AccrualRequestRepository;
import org.example.repository.AccrualRuleRepository;
import org.example.repository.CustomerRepository;
import org.example.repository.OperationRepository;
import org.example.repository.RedemptionRequestRepository;
import org.example.repository.RedemptionRuleRepository;
import org.example.repository.TransactionRepository;
import org.example.service.AccountService;
import org.example.service.AccrualService;
import org.example.service.CardNumberGenerator;
import org.example.service.CustomerService;
import org.example.service.PurchaseService;
import org.example.service.RedemptionService;
import org.example.service.RuleService;
import org.example.service.accrual.AccrualCalculatorRegistry;
import org.example.service.tier.ThresholdTierPolicy;

import java.time.Clock;
import java.util.List;

/** Ручная сборка зависимостей приложения. Здесь подключаются новые сервисы и разделы меню. */
public class AppContext {

    private final List<MenuSection> sections;

    public AppContext(Database db, ConsoleIO io) {
        Clock clock = Clock.systemDefaultZone();

        CustomerRepository customerRepo = new CustomerRepository();
        AccountRepository accountRepo = new AccountRepository();
        OperationRepository operationRepo = new OperationRepository();
        AccrualRuleRepository accrualRuleRepo = new AccrualRuleRepository();
        RedemptionRuleRepository redemptionRuleRepo = new RedemptionRuleRepository();
        AccrualRequestRepository accrualRequestRepo = new AccrualRequestRepository();
        RedemptionRequestRepository redemptionRequestRepo = new RedemptionRequestRepository();
        TransactionRepository transactionRepo = new TransactionRepository();

        AccrualCalculatorRegistry calculators = AccrualCalculatorRegistry.defaults();

        AccountService accountService = new AccountService(db, accountRepo, customerRepo, transactionRepo,
                accrualRequestRepo, redemptionRequestRepo, new CardNumberGenerator());
        CustomerService customerService = new CustomerService(db, customerRepo, operationRepo, accountService);
        AccrualService accrualService = new AccrualService(accrualRuleRepo, accrualRequestRepo, accountRepo,
                customerRepo, transactionRepo, calculators);
        RedemptionService redemptionService = new RedemptionService(db, redemptionRuleRepo, redemptionRequestRepo,
                accountRepo, customerRepo, operationRepo, transactionRepo, clock);
        PurchaseService purchaseService = new PurchaseService(db, accountService, accountRepo, operationRepo,
                accrualService, redemptionService, ThresholdTierPolicy.defaults(), clock);
        RuleService ruleService = new RuleService(db, accrualRuleRepo, redemptionRuleRepo, calculators);

        this.sections = List.of(
                new CustomerSection(io, customerService, accountService),
                new AccountSection(io, accountService),
                new PurchaseSection(io, purchaseService),
                new RedemptionSection(io, redemptionService, accountService, customerService),
                new RuleSection(io, ruleService));
    }

    public List<MenuSection> sections() {
        return sections;
    }
}
