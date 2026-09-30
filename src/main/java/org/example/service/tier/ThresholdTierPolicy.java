package org.example.service.tier;

import org.example.model.Tier;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** Уровень по сумме всех покупок клиента: наибольший порог, который он преодолел. */
public class ThresholdTierPolicy implements TierPolicy {

    private final NavigableMap<BigDecimal, Tier> thresholds = new TreeMap<>();

    public ThresholdTierPolicy(Map<Tier, BigDecimal> thresholds) {
        thresholds.forEach((tier, from) -> this.thresholds.put(from, tier));
        this.thresholds.putIfAbsent(BigDecimal.ZERO, Tier.BASE);
    }

    public static ThresholdTierPolicy defaults() {
        return new ThresholdTierPolicy(Map.of(
                Tier.BASE, BigDecimal.ZERO,
                Tier.SILVER, new BigDecimal("10000"),
                Tier.GOLD, new BigDecimal("50000")));
    }

    @Override
    public Tier resolve(BigDecimal totalPurchases) {
        Map.Entry<BigDecimal, Tier> entry = thresholds.floorEntry(totalPurchases);
        return entry == null ? Tier.BASE : entry.getValue();
    }
}
