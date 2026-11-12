package crudmanager.adapter.out.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class Pbkdf2PasswordHasherTest {

    private static final String SEED_HASH =
            "210000:bwqZ/8+HdsEayk5IdH3kqA==:Ue0ne4GdGZ4Yrx0rjSPLELEgOAtyAA+J49DL12ZPysw=";

    private final Pbkdf2PasswordHasher hasher = new Pbkdf2PasswordHasher();

    @Test
    @DisplayName("AUTH-10 dois hashes da mesma senha diferem e não contêm a senha")
    void saltedAndOpaque() {
        String a = hasher.hash("senha123");
        String b = hasher.hash("senha123");
        assertNotEquals(a, b);
        assertFalse(a.contains("senha123"));
        assertTrue(a.matches("210000:[A-Za-z0-9+/=]+:[A-Za-z0-9+/=]+"));
    }

    @Test
    @DisplayName("AUTH-10 matches aceita a senha certa e recusa a errada")
    void matchesRightAndWrong() {
        String h = hasher.hash("senha123");
        assertTrue(hasher.matches("senha123", h));
        assertFalse(hasher.matches("outra", h));
    }

    @Test
    @DisplayName("AUTH-10 matches confere hash do seed de script-sql.sql")
    void matchesSeedHash() {
        assertTrue(hasher.matches("senha123", SEED_HASH));
        assertFalse(hasher.matches("errada", SEED_HASH));
    }

    @Test
    @DisplayName("AUTH-10 formato inválido recusa sem exceção")
    void malformedStored() {
        assertFalse(hasher.matches("x", null));
        assertFalse(hasher.matches(null, SEED_HASH));
        assertFalse(hasher.matches("x", ""));
        assertFalse(hasher.matches("x", "abc"));
        assertFalse(hasher.matches("x", "a:b:c"));
        assertFalse(hasher.matches("x", "210000:!!!:!!!"));
        assertFalse(hasher.matches("x", "0:AAAA:AAAA"));
    }
}
