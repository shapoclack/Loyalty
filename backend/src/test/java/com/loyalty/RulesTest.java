package com.loyalty;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
class RulesTest {
 @Test void tierBoundaries(){assertThat(Models.tier(new BigDecimal("9999.99"))).isEqualTo(Models.Tier.BASE);assertThat(Models.tier(new BigDecimal("10000"))).isEqualTo(Models.Tier.SILVER);assertThat(Models.tier(new BigDecimal("49999.99"))).isEqualTo(Models.Tier.SILVER);assertThat(Models.tier(new BigDecimal("50000"))).isEqualTo(Models.Tier.GOLD);}
 @Test void passwordPolicy(){assertThatThrownBy(()->AuthService.policy("short")).isInstanceOf(Problem.class);assertThatThrownBy(()->AuthService.policy("lowercase123456")).isInstanceOf(Problem.class);assertThatThrownBy(()->AuthService.policy("Aa1"+"я".repeat(40))).isInstanceOf(Problem.class);AuthService.policy("ValidPassword123");}
 @Test void csvFormulaInjection(){assertThat(CsvService.safe("=SUM(A1:A2)")).startsWith("'");assertThat(CsvService.unprotect(CsvService.safe("+7 900 0000000"))).isEqualTo("+7 900 0000000");assertThat(CsvService.safe("Обычный текст")).isEqualTo("Обычный текст");}
}
