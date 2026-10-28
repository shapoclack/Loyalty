package org.example.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class DbConfigTest {
    @TempDir Path directory;

    private Properties legacy() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:postgresql://old-host:5433/old_db");
        properties.setProperty("db.user", "legacy_user");
        properties.setProperty("db.password", "legacy_password");
        return properties;
    }

    @Test void readsQuotedValuesCommentsAndSpecialCharactersLiterally() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "\ufeff# comment\n\nPOSTGRES_DB=loyalty\nPOSTGRES_PASSWORD='Test!a#b=c${x}\\tail'\nNAME=\"Имя пользователя\"\nEMPTY=\n");
        var values = DotEnv.read(file);
        assertEquals("loyalty", values.get("POSTGRES_DB"));
        assertEquals("Test!a#b=c${x}\\tail", values.get("POSTGRES_PASSWORD"));
        assertEquals("Имя пользователя", values.get("NAME"));
        assertEquals("", values.get("EMPTY"));
        assertTrue(DotEnv.read(directory.resolve("missing.env")).isEmpty());
    }

    @Test void rejectsMalformedFileWithoutExposingItsContents() throws Exception {
        Path file = directory.resolve(".env");
        Files.writeString(file, "POSTGRES_PASSWORD='synthetic-secret\n");
        var error = assertThrows(IllegalStateException.class, () -> DotEnv.read(file));
        assertTrue(error.getMessage().contains("строка 1"));
        assertFalse(error.getMessage().contains("synthetic-secret"));
        Files.writeString(file, "INVALID KEY=value\n");
        assertThrows(IllegalStateException.class, () -> DotEnv.read(file));
    }

    @Test void buildsAddressFromPostgresVariablesAndSupportsEmptyPassword() {
        var config = DbConfig.resolve(Map.of(), Map.of("POSTGRES_DB", "loyalty", "POSTGRES_PORT", "55432",
                "POSTGRES_USER", "loyalty_app", "POSTGRES_PASSWORD", ""), legacy());
        assertEquals("jdbc:postgresql://localhost:55432/loyalty", config.url());
        assertEquals("loyalty_app", config.user());
        assertEquals("", config.password());
    }

    @Test void processSettingsOverrideDotenvAndMergeAddressComponents() {
        var config = DbConfig.resolve(Map.of("POSTGRES_PORT", "55433", "POSTGRES_USER", "process_user",
                "POSTGRES_PASSWORD", "process_password"),
                Map.of("POSTGRES_HOST", "db.local", "POSTGRES_DB", "loyalty", "POSTGRES_PORT", "55432",
                        "POSTGRES_USER", "file_user", "POSTGRES_PASSWORD", "file_password"), legacy());
        assertEquals("jdbc:postgresql://db.local:55433/loyalty", config.url());
        assertEquals("process_user", config.user());
        assertEquals("process_password", config.password());
    }

    @Test void supportsLegacyLoyaltyNamesAndGivesThemPriorityWithinSource() {
        var values = Map.of("LOYALTY_DB_URL", "jdbc:postgresql://direct:5432/custom",
                "POSTGRES_DB", "ignored", "LOYALTY_DB_USER", "direct_user", "POSTGRES_USER", "ignored",
                "LOYALTY_DB_PASSWORD", "direct_password", "POSTGRES_PASSWORD", "ignored");
        var config = DbConfig.resolve(Map.of(), values, legacy());
        assertEquals(values.get("LOYALTY_DB_URL"), config.url());
        assertEquals("direct_user", config.user());
        assertEquals("direct_password", config.password());
        assertEquals("jdbc:postgresql://process:5432/ignored",
                DbConfig.resolve(Map.of("POSTGRES_HOST", "process"), values, legacy()).url());
    }

    @Test void preservesPropertiesAndDefaultFallback() {
        var config = DbConfig.resolve(Map.of(), Map.of(), legacy());
        assertEquals("jdbc:postgresql://old-host:5433/old_db", config.url());
        assertEquals("legacy_user", config.user());
        assertEquals("legacy_password", config.password());
        var defaults = DbConfig.resolve(Map.of(), Map.of(), new Properties());
        assertEquals("jdbc:postgresql://localhost:5432/loyalty", defaults.url());
        assertEquals(System.getProperty("user.name"), defaults.user());
        assertEquals("", defaults.password());
    }

    @Test void validatesPortAndAddressComponents() {
        for (String port : new String[]{"0", "65536", "abc"}) {
            assertThrows(IllegalStateException.class, () -> DbConfig.resolve(Map.of(), Map.of("POSTGRES_PORT", port), legacy()));
        }
        assertThrows(IllegalStateException.class, () -> DbConfig.resolve(Map.of(), Map.of("POSTGRES_DB", "loyalty?other=1"), legacy()));
    }
}
