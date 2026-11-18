package crudmanager.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.exception.ModelException;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcConnectionFactoryTest {

    @Test
    @DisplayName("DOCK-03 sem DB_PASSWORD a configuração falha com mensagem clara")
    void missingPasswordFails() {
        Map<String, String> env = new HashMap<>();
        env.put("DB_URL", "jdbc:postgresql://h:5432/d");
        env.put("DB_USER", "u");

        ModelException e = assertThrows(ModelException.class, () -> new JdbcConnectionFactory(env));

        assertTrue(e.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    @DisplayName("DOCK-03 com DB_URL, DB_USER e DB_PASSWORD lê os três valores do ambiente")
    void readsThreeValues() {
        Map<String, String> env = Map.of("DB_URL", "jdbc:postgresql://h:5432/d", "DB_USER", "u", "DB_PASSWORD", "p");

        JdbcConnectionFactory factory = new JdbcConnectionFactory(env);

        assertEquals("jdbc:postgresql://h:5432/d", factory.getUrl());
        assertEquals("u", factory.getUser());
        assertEquals("p", factory.getPassword());
    }

    @Test
    @DisplayName("DOCK-03 variável em branco também é tratada como ausente")
    void blankFails() {
        Map<String, String> env = Map.of("DB_URL", "x", "DB_USER", " ", "DB_PASSWORD", "p");

        ModelException e = assertThrows(ModelException.class, () -> new JdbcConnectionFactory(env));

        assertTrue(e.getMessage().contains("DB_USER"));
    }
}
