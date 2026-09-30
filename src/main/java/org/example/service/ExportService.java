package org.example.service;

import org.example.db.Database;
import org.example.exception.EntityNotFoundException;
import org.example.model.AccrualRequest;
import org.example.model.AccrualRule;
import org.example.model.BonusTransaction;
import org.example.model.Customer;
import org.example.model.LoyaltyAccount;
import org.example.model.RedemptionRequest;
import org.example.model.RedemptionRule;
import org.example.model.SourceOperation;
import org.example.model.query.AccountSort;
import org.example.model.query.CustomerSort;
import org.example.model.query.OperationFilter;
import org.example.model.query.OperationSort;
import org.example.repository.AccountRepository;
import org.example.repository.AccrualRequestRepository;
import org.example.repository.AccrualRuleRepository;
import org.example.repository.CustomerRepository;
import org.example.repository.OperationRepository;
import org.example.repository.RedemptionRequestRepository;
import org.example.repository.RedemptionRuleRepository;
import org.example.repository.TransactionRepository;
import org.example.util.ExcelExporter;
import org.example.util.ExportSheet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.example.util.ExportSheet.column;

/** Выгрузка данных программы лояльности в Excel. Файлы сохраняются в каталог экспорта. */
public class ExportService {

    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Database db;
    private final Path exportDir;
    private final ExcelExporter exporter;
    private final Clock clock;
    private final CustomerRepository customers;
    private final AccountRepository accounts;
    private final OperationRepository operations;
    private final AccrualRequestRepository accrualRequests;
    private final RedemptionRequestRepository redemptionRequests;
    private final TransactionRepository transactions;
    private final AccrualRuleRepository accrualRules;
    private final RedemptionRuleRepository redemptionRules;

    public ExportService(Database db, Path exportDir, ExcelExporter exporter, Clock clock,
                         CustomerRepository customers, AccountRepository accounts, OperationRepository operations,
                         AccrualRequestRepository accrualRequests, RedemptionRequestRepository redemptionRequests,
                         TransactionRepository transactions, AccrualRuleRepository accrualRules,
                         RedemptionRuleRepository redemptionRules) {
        this.db = db;
        this.exportDir = exportDir;
        this.exporter = exporter;
        this.clock = clock;
        this.customers = customers;
        this.accounts = accounts;
        this.operations = operations;
        this.accrualRequests = accrualRequests;
        this.redemptionRequests = redemptionRequests;
        this.transactions = transactions;
        this.accrualRules = accrualRules;
        this.redemptionRules = redemptionRules;
    }

    public Path exportDir() {
        return exportDir.toAbsolutePath();
    }

    /** Все таблицы базы — по листу на таблицу. */
    public Path exportAll() {
        List<ExportSheet> sheets = db.inTransaction(c -> List.of(
                customersSheet(customers.findAll(c, null, CustomerSort.ID)),
                accountsSheet(accounts.findAll(c, null, null, AccountSort.OPENED)),
                operationsSheet(operations.find(c, new OperationFilter(null, null, null, null), OperationSort.DATE_ASC)),
                accrualRequestsSheet(accrualRequests.findAll(c)),
                redemptionRequestsSheet(redemptionRequests.findAll(c)),
                transactionsSheet(transactions.findAll(c)),
                accrualRulesSheet(accrualRules.findAll(c)),
                redemptionRulesSheet(redemptionRules.findAll(c))));
        return write("loyalty_full", sheets);
    }

    /** Счёт, его транзакции и заявки. */
    public Path exportAccount(String cardNumber) {
        String card = cardNumber == null ? "" : cardNumber.replaceAll("\\s", "");
        List<ExportSheet> sheets = db.inTransaction(c -> {
            LoyaltyAccount account = accounts.findByCard(c, card)
                    .orElseThrow(() -> new EntityNotFoundException("Карта " + card + " не найдена"));
            return List.of(
                    accountsSheet(List.of(account)),
                    transactionsSheet(transactions.findByAccount(c, account.id())),
                    accrualRequestsSheet(accrualRequests.findByAccount(c, account.id())),
                    redemptionRequestsSheet(redemptionRequests.findByAccount(c, account.id())));
        });
        return write("account_" + card, sheets);
    }

    /** Уже отобранные операции (например, результат фильтра журнала). */
    public Path exportOperations(List<SourceOperation> list) {
        return write("operations", List.of(operationsSheet(list)));
    }

    private Path write(String prefix, List<ExportSheet> sheets) {
        Path file = uniqueFile(prefix + "_" + FILE_TIMESTAMP.format(LocalDateTime.now(clock)));
        exporter.export(file, sheets);
        return file.toAbsolutePath();
    }

    private Path uniqueFile(String baseName) {
        Path file = exportDir.resolve(baseName + ".xlsx");
        for (int i = 2; Files.exists(file); i++) {
            file = exportDir.resolve(baseName + "_" + i + ".xlsx");
        }
        return file;
    }

    private static ExportSheet customersSheet(List<Customer> list) {
        return ExportSheet.of("Клиенты", list,
                column("ID", Customer::id),
                column("ФИО", Customer::fullName),
                column("Телефон", Customer::phone),
                column("E-mail", Customer::email),
                column("Дата рождения", Customer::birthDate),
                column("Зарегистрирован", Customer::createdAt),
                column("Статус", Customer::status));
    }

    private static ExportSheet accountsSheet(List<LoyaltyAccount> list) {
        return ExportSheet.of("Счета", list,
                column("ID", LoyaltyAccount::id),
                column("ID клиента", LoyaltyAccount::customerId),
                column("Карта", LoyaltyAccount::cardNumber),
                column("Баланс", LoyaltyAccount::balance),
                column("Уровень", LoyaltyAccount::tier),
                column("Статус", LoyaltyAccount::status),
                column("Открыт", LoyaltyAccount::openedAt),
                column("Изменён", LoyaltyAccount::updatedAt));
    }

    private static ExportSheet operationsSheet(List<SourceOperation> list) {
        return ExportSheet.of("Операции", list,
                column("ID", SourceOperation::id),
                column("ID клиента", SourceOperation::customerId),
                column("Тип", SourceOperation::type),
                column("Чек", SourceOperation::externalId),
                column("Сумма", SourceOperation::amount),
                column("Дата", SourceOperation::operationDate),
                column("Магазин", SourceOperation::storeCode));
    }

    private static ExportSheet accrualRequestsSheet(List<AccrualRequest> list) {
        return ExportSheet.of("Заявки на начисление", list,
                column("ID", AccrualRequest::id),
                column("ID счёта", AccrualRequest::accountId),
                column("ID клиента", AccrualRequest::customerId),
                column("ID правила", AccrualRequest::ruleId),
                column("ID операции", AccrualRequest::operationId),
                column("Бонусы", AccrualRequest::bonusAmount),
                column("Статус", AccrualRequest::status),
                column("Создана", AccrualRequest::createdAt),
                column("Обработана", AccrualRequest::processedAt));
    }

    private static ExportSheet redemptionRequestsSheet(List<RedemptionRequest> list) {
        return ExportSheet.of("Заявки на списание", list,
                column("ID", RedemptionRequest::id),
                column("ID счёта", RedemptionRequest::accountId),
                column("ID клиента", RedemptionRequest::customerId),
                column("ID правила", RedemptionRequest::ruleId),
                column("ID операции", RedemptionRequest::operationId),
                column("Бонусы", RedemptionRequest::bonusAmount),
                column("Статус", RedemptionRequest::status),
                column("Создана", RedemptionRequest::createdAt),
                column("Обработана", RedemptionRequest::processedAt));
    }

    private static ExportSheet transactionsSheet(List<BonusTransaction> list) {
        return ExportSheet.of("Транзакции", list,
                column("ID", BonusTransaction::id),
                column("ID счёта", BonusTransaction::accountId),
                column("Заявка на начисление", BonusTransaction::accrualRequestId),
                column("Заявка на списание", BonusTransaction::redemptionRequestId),
                column("Тип", BonusTransaction::type),
                column("Сумма", BonusTransaction::amount),
                column("Баланс после", BonusTransaction::balanceAfter),
                column("Дата", BonusTransaction::createdAt));
    }

    private static ExportSheet accrualRulesSheet(List<AccrualRule> list) {
        return ExportSheet.of("Правила начисления", list,
                column("ID", AccrualRule::id),
                column("Название", AccrualRule::name),
                column("Тип", AccrualRule::ruleType),
                column("Значение", AccrualRule::value),
                column("Уровень", AccrualRule::category),
                column("Действует с", AccrualRule::validFrom),
                column("Действует до", AccrualRule::validTo),
                column("Активно", AccrualRule::active));
    }

    private static ExportSheet redemptionRulesSheet(List<RedemptionRule> list) {
        return ExportSheet.of("Правила списания", list,
                column("ID", RedemptionRule::id),
                column("Название", RedemptionRule::name),
                column("Макс. % чека", RedemptionRule::maxPercent),
                column("Мин. бонусов", RedemptionRule::minAmount),
                column("Макс. бонусов", RedemptionRule::maxAmount),
                column("Срок заявки, дн.", RedemptionRule::expireDays),
                column("Активно", RedemptionRule::active));
    }
}
