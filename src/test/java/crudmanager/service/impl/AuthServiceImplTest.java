package crudmanager.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.exception.ValidationException;
import crudmanager.model.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {

    private final FakeUserDAO users = new FakeUserDAO();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final AuthServiceImpl service = new AuthServiceImpl(users, hasher);

    AuthServiceImplTest() {
        users.add(1, "Emerson", "emerson@mail.com", hasher.hash("senha123"));
    }

    @Test
    @DisplayName("AUTH-03 e-mail e senha corretos devolvem o usuário")
    void loginOk() {
        Optional<User> u = service.login("emerson@mail.com", "senha123");
        assertTrue(u.isPresent());
        assertEquals(1, u.get().getId());
    }

    @Test
    @DisplayName("AUTH-04 senha errada devolve vazio")
    void wrongPassword() {
        assertTrue(service.login("emerson@mail.com", "errada").isEmpty());
    }

    @Test
    @DisplayName("AUTH-04 e-mail inexistente devolve vazio, igual à senha errada")
    void unknownEmail() {
        assertTrue(service.login("ninguem@mail.com", "senha123").isEmpty());
    }

    @Test
    @DisplayName("AUTH-05 e-mail ou senha vazios lançam ValidationException 'Informe e-mail e senha'")
    void emptyFields() {
        for (String[] c : new String[][] {{"", "x"}, {"a@b.c", ""}, {null, "x"}, {"a@b.c", null}, {"  ", "x"}}) {
            ValidationException e = assertThrows(ValidationException.class, () -> service.login(c[0], c[1]));
            assertEquals("Informe e-mail e senha", e.getErrors().get("login"));
        }
    }
}
