package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Fmt;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.console.Table;
import org.example.model.AccrualRequest;
import org.example.model.RequestStatus;
import org.example.model.SourceOperation;
import org.example.model.query.OperationFilter;
import org.example.model.query.OperationSort;
import org.example.service.ExportService;
import org.example.service.OperationService;
import org.example.service.PurchaseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class PurchaseSection implements MenuSection {

    private final ConsoleIO io;
    private final PurchaseService purchases;
    private final OperationService operations;
    private final ExportService exports;

    public PurchaseSection(ConsoleIO io, PurchaseService purchases, OperationService operations,
                           ExportService exports) {
        this.io = io;
        this.purchases = purchases;
        this.operations = operations;
        this.exports = exports;
    }

    @Override
    public String title() {
        return "Покупки и операции";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Оформить покупку", this::purchase)
                .add("Журнал операций (фильтр и сортировка)", this::journal);
    }

    private void journal() {
        LocalDate from = io.optionalDate("Период с");
        LocalDate to = io.optionalDate("Период по");
        String store = io.optionalText("Код магазина");
        BigDecimal minAmount = io.optionalDecimal("Сумма от");
        OperationSort sort = io.choose("Сортировка", List.of(OperationSort.values()), OperationSort::label);

        List<SourceOperation> list = operations.find(new OperationFilter(from, to, store, minAmount), sort);
        Table table = new Table("ID", "Дата", "Клиент", "Тип", "Сумма", "Чек", "Магазин");
        list.forEach(op -> table.row(op.id(), Fmt.dateTime(op.operationDate()), op.customerId(), op.type(),
                Fmt.money(op.amount()), op.externalId(), op.storeCode()));
        table.print(io);
        BigDecimal total = list.stream().map(SourceOperation::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        io.println("Найдено операций: " + list.size() + ", на сумму " + Fmt.money(total));
        if (!list.isEmpty() && io.confirm("Выгрузить результат в Excel?")) {
            io.success("Файл сохранён: " + exports.exportOperations(list));
        }
    }

    private void purchase() {
        String card = io.text("Номер карты");
        BigDecimal amount = io.decimal("Сумма покупки");
        BigDecimal redeem = null;
        BigDecimal available = purchases.redeemable(card, amount);
        if (available.signum() > 0) {
            redeem = io.optionalDecimal("Списать бонусов (доступно " + Fmt.money(available) + ")");
        }
        String externalId = io.optionalText("Номер чека");
        String storeCode = io.optionalText("Код магазина");

        PurchaseService.PurchaseResult result = purchases.purchase(card, amount, externalId, storeCode, redeem);

        io.success("Операция #" + result.operation().id() + " на сумму " + Fmt.money(result.operation().amount()));
        if (result.redemption() != null) {
            io.println("  Списано бонусов: " + Fmt.money(result.redemption().bonusAmount())
                    + ", к оплате деньгами: " + Fmt.money(result.paidByMoney()));
        }
        AccrualRequest accrual = result.accrual();
        if (accrual.status() == RequestStatus.APPROVED) {
            io.println("  Начислено бонусов: " + Fmt.money(accrual.bonusAmount())
                    + " (правило #" + accrual.ruleId() + ")");
        } else {
            io.println("  Бонусы не начислены: нет подходящего правила или счёт/клиент заблокирован");
        }
        io.println("  Баланс: " + Fmt.money(result.account().balance()));
        if (result.account().tier() != result.previousTier()) {
            io.println("  Новый уровень: " + result.previousTier() + " → " + result.account().tier());
        }
    }
}
