package org.example.console.sections;

import org.example.console.ConsoleIO;
import org.example.console.Fmt;
import org.example.console.Menu;
import org.example.console.MenuSection;
import org.example.model.AccrualRequest;
import org.example.model.RequestStatus;
import org.example.service.PurchaseService;

import java.math.BigDecimal;

public class PurchaseSection implements MenuSection {

    private final ConsoleIO io;
    private final PurchaseService purchases;

    public PurchaseSection(ConsoleIO io, PurchaseService purchases) {
        this.io = io;
        this.purchases = purchases;
    }

    @Override
    public String title() {
        return "Покупки";
    }

    @Override
    public void fill(Menu menu) {
        menu.add("Оформить покупку", this::purchase);
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
