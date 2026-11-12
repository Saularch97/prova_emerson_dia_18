package crudmanager.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserServiceTest {

    private final FakeUserDAO users = new FakeUserDAO();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final UserService service = new UserService(users, hasher);

    private User valid() {
        User u = new User();
        u.setName("Ana");
        u.setGender("F");
        u.setEmail("ana@mail.com");
        return u;
    }

    @Test
    @DisplayName("AUTH-12 create grava só o hash da senha, nunca a senha")
    void createStoresHashOnly() {
        service.create(valid(), "segredo");
        User saved = users.findByEmail("ana@mail.com");
        assertNotNull(saved);
        assertEquals("hash(segredo)", saved.getPasswordHash());
        assertFalse(saved.getPasswordHash().equals("segredo"));
    }

    @Test
    @DisplayName("AUTH-12 create sem senha lança ValidationException no campo password e não grava")
    void createRequiresPassword() {
        for (String blank : new String[] {null, ""}) {
            ValidationException e = assertThrows(ValidationException.class, () -> service.create(valid(), blank));
            assertTrue(e.getErrors().containsKey("password"));
        }
        assertEquals(0, users.count());
    }

    @Test
    @DisplayName("AUTH-13 update com senha em branco mantém o hash atual")
    void updateBlankKeepsHash() {
        users.add(1, "Ana", "ana@mail.com", "hash(antiga)");
        User edited = valid();
        edited = new UserWithId(1).fill(edited);
        edited.setName("Ana Maria");
        service.update(edited, "");
        assertEquals("hash(antiga)", users.findById(1).getPasswordHash());
        assertEquals("Ana Maria", users.findById(1).getName());
    }

    @Test
    @DisplayName("AUTH-13 update com senha nova grava o novo hash")
    void updateNewPassword() {
        users.add(1, "Ana", "ana@mail.com", "hash(antiga)");
        service.update(new UserWithId(1).fill(valid()), "nova");
        assertEquals("hash(nova)", users.findById(1).getPasswordHash());
    }

    @Test
    @DisplayName("EDGE-1 excluir usuário com dados relacionados repassa RelatedDataException e mantém o usuário")
    void deleteRelated() {
        users.add(1, "Ana", "ana@mail.com", "h");
        users.deleteBlocked = true;
        assertThrows(RelatedDataException.class, () -> service.delete(1));
        assertNotNull(users.findById(1));
    }

    @Test
    @DisplayName("EDGE-1 excluir id inexistente lança NotFoundException")
    void deleteMissing() {
        assertThrows(NotFoundException.class, () -> service.delete(99));
    }

    @Test
    @DisplayName("EDGE-1 find de id inexistente lança NotFoundException")
    void findMissing() {
        assertThrows(NotFoundException.class, () -> service.find(99));
    }

    @Test
    @DisplayName("ARQ-03 validação de usuário roda no serviço, sem Servlet, com mensagem por campo")
    void validationWithoutServlet() {
        User bad = new User();
        bad.setGender("X");
        ValidationException e = assertThrows(ValidationException.class, () -> service.create(bad, "s"));
        assertTrue(e.getErrors().containsKey("name"));
        assertTrue(e.getErrors().containsKey("email"));
        assertTrue(e.getErrors().containsKey("gender"));
        assertEquals(0, users.count());
    }

    @Test
    @DisplayName("ARQ-03 list(page) pagina de 10 em 10 e clampa a página")
    void listPaginates() {
        for (int i = 1; i <= 25; i++) {
            users.add(i, "U" + i, "u" + i + "@mail.com", "h");
        }
        Page<User> p3 = service.list(3);
        assertEquals(5, p3.getItems().size());
        assertEquals(3, p3.getPage());
        assertEquals(21, p3.getItems().get(0).getId());
        Page<User> p99 = service.list(99);
        assertEquals(3, p99.getPage());
        assertEquals(10, service.list(1).getItems().size());
    }

    /** Cria User com id e copia os campos de outro (modelo não tem setId). */
    private static final class UserWithId {
        private final int id;

        UserWithId(int id) {
            this.id = id;
        }

        User fill(User from) {
            User u = new User(id);
            u.setName(from.getName());
            u.setGender(from.getGender());
            u.setEmail(from.getEmail());
            return u;
        }
    }

    @Test
    @DisplayName("ARQ-06 listAll devolve todos os usuários, sem paginar, para os selects")
    void listAllReturnsEveryUser() {
        for (int i = 0; i < 12; i++) {
            User u = valid();
            u.setEmail("u" + i + "@mail.com");
            service.create(u, "segredo");
        }
        assertEquals(12, service.listAll().size());
    }

    @Test
    @DisplayName("PAG-07 excluir o único item da última página e listar a página antiga devolve a nova última página")
    void pag07DeletingLastPageItem() {
        for (int i = 1; i <= 11; i++) {
            users.add(i, "U" + i, "u" + i + "@x.com", "h");
        }
        assertEquals(1, service.list(2).getItems().size());

        service.delete(11);
        crudmanager.domain.Page<User> page = service.list(2);

        assertEquals(1, page.getPage());
        assertEquals(1, page.getTotalPages());
        assertEquals(10, page.getItems().size());
        assertEquals(1, page.getItems().get(0).getId());
        assertEquals(10, page.getItems().get(9).getId());
    }
}
