package org.example.service.tier;

import org.example.model.Tier;

import java.math.BigDecimal;

/** Определяет уровень участника. Замените реализацию, чтобы изменить логику уровней. */
public interface TierPolicy {

    Tier resolve(BigDecimal totalPurchases);
}
