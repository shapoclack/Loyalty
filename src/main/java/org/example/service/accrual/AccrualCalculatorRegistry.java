package org.example.service.accrual;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Реестр алгоритмов начисления по типу правила. */
public class AccrualCalculatorRegistry {

    private final Map<String, AccrualCalculator> calculators = new LinkedHashMap<>();

    public static AccrualCalculatorRegistry defaults() {
        return new AccrualCalculatorRegistry()
                .register(new PercentCalculator())
                .register(new FixedCalculator())
                .register(new Per100Calculator());
    }

    public AccrualCalculatorRegistry register(AccrualCalculator calculator) {
        calculators.put(key(calculator.type()), calculator);
        return this;
    }

    public Optional<AccrualCalculator> find(String type) {
        return Optional.ofNullable(calculators.get(key(type)));
    }

    public Collection<AccrualCalculator> all() {
        return Collections.unmodifiableCollection(calculators.values());
    }

    private static String key(String type) {
        return type.trim().toUpperCase(Locale.ROOT);
    }
}
