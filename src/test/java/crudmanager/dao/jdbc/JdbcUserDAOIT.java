package crudmanager.dao.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.exception.DuplicateException;
import crudmanager.exception.RelatedDataException;
import crudmanager.model.User;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcUserDAOIT extends PostgresTestBase {

    private final JdbcUserDAO dao = new JdbcUserDAO(connectionFactory());
    private final List<String> createdEmails = new ArrayList<>();

    private final List<String> cleanupSql = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        for (String sql : cleanupSql) {
            execute(sql);
        }
        for (String email : createdEmails) {
            User u = dao.findByEmail(email);
            if (u != null) {
                dao.delete(u);
            }
        }
    }

    private User saved(String email) {
        User u = new User();
        u.setName("Teste Ação");
        u.setGender("F");
        u.setEmail(email);
        u.setPasswordHash("210000:abc:def");
        createdEmails.add(email);
        assertTrue(dao.save(u));
        return dao.findByEmail(email);
    }

    @Test
    @DisplayName("AUTH-12 salvar grava o hash e findByEmail devolve o usuário com o mesmo hash")
    void savesHashAndFindsByEmail() {
        User found = saved("it-hash@mail.com");

        assertNotNull(found);
        assertEquals("Teste Ação", found.getName());
        assertEquals("F", found.getGender());
        assertEquals("210000:abc:def", found.getPasswordHash());
        assertEquals(found.getEmail(), dao.findById(found.getId()).getEmail());
    }

    @Test
    @DisplayName("AUTH-12 updatePasswordHash troca somente o hash persistido")
    void updatesPasswordHash() {
        User u = saved("it-newhash@mail.com");

        assertTrue(dao.updatePasswordHash(u.getId(), "210000:novo:hash"));

        User after = dao.findById(u.getId());
        assertEquals("210000:novo:hash", after.getPasswordHash());
        assertEquals("Teste Ação", after.getName());
    }

    @Test
    @DisplayName("AUTH-12 update persiste nome, sexo, e-mail e hash")
    void updatePersists() {
        User u = saved("it-upd@mail.com");
        u.setName("Outro Nome");
        u.setGender("M");
        u.setPasswordHash("210000:x:y");

        assertTrue(dao.update(u));

        User after = dao.findById(u.getId());
        assertEquals("Outro Nome", after.getName());
        assertEquals("M", after.getGender());
        assertEquals("210000:x:y", after.getPasswordHash());
    }

    @Test
    @DisplayName("AUTH-12 e-mail duplicado lança DuplicateException")
    void duplicateEmail() {
        saved("it-dup@mail.com");
        User other = new User();
        other.setName("Outro");
        other.setGender("M");
        other.setEmail("it-dup@mail.com");
        other.setPasswordHash("h");

        assertThrows(DuplicateException.class, () -> dao.save(other));
    }

    @Test
    @DisplayName("EDGE-1 excluir usuário referenciado lança RelatedDataException e o usuário permanece")
    void deleteReferencedUser() {
        User emerson = dao.findByEmail("emerson@mail.com");

        assertThrows(RelatedDataException.class, () -> dao.delete(emerson));

        assertNotNull(dao.findById(emerson.getId()));
    }

    private void assertDeleteRefused(String email, String relationSql, String relationCleanupSql) {
        User u = saved(email);
        execute(relationSql.replace(":id", String.valueOf(u.getId())));
        cleanupSql.add(0, relationCleanupSql.replace(":id", String.valueOf(u.getId())));

        assertThrows(RelatedDataException.class, () -> dao.delete(u));

        assertNotNull(dao.findById(u.getId()));
    }

    @Test
    @DisplayName("EDGE-1 excluir usuário que é instrutor de curso lança RelatedDataException e o usuário permanece")
    void deleteInstructorRefused() {
        assertDeleteRefused("it-edge1-instructor@mail.com",
                "INSERT INTO courses (title, description, price, start_date, level, active, instructor_id) "
                        + "VALUES ('IT edge1', 'd', 1, '2026-01-01', 'INICIANTE', TRUE, :id)",
                "DELETE FROM courses WHERE instructor_id = :id");
    }

    @Test
    @DisplayName("EDGE-1 excluir usuário que é aluno de matrícula lança RelatedDataException e o usuário permanece")
    void deleteStudentRefused() {
        assertDeleteRefused("it-edge1-student@mail.com",
                "INSERT INTO enrollments (student_id, course_id, enrolled_on, status, progress, paid) "
                        + "VALUES (:id, (SELECT MIN(id) FROM courses), '2026-01-01', 'ATIVA', 0, FALSE)",
                "DELETE FROM enrollments WHERE student_id = :id");
    }

    @Test
    @DisplayName("EDGE-1 excluir usuário que é autor de post lança RelatedDataException e o usuário permanece")
    void deleteAuthorRefused() {
        assertDeleteRefused("it-edge1-author@mail.com",
                "INSERT INTO posts (content, post_date, user_id) VALUES ('IT edge1', '2026-01-01', :id)",
                "DELETE FROM posts WHERE user_id = :id");
    }

    @Test
    @DisplayName("EDGE-1 excluir usuário que é dono de empresa lança RelatedDataException e o usuário permanece")
    void deleteCompanyOwnerRefused() {
        assertDeleteRefused("it-edge1-owner@mail.com",
                "INSERT INTO companies (name, role, start, user_id) VALUES ('IT edge1', 'Dev', '2026-01-01', :id)",
                "DELETE FROM companies WHERE user_id = :id");
    }

    @Test
    @DisplayName("ARQ-06 excluir usuário sem vínculos o remove; findById e findByEmail devolvem null para inexistente")
    void deleteAndMissing() {
        User u = saved("it-del@mail.com");

        assertTrue(dao.delete(u));

        assertNull(dao.findById(u.getId()));
        assertNull(dao.findByEmail("it-del@mail.com"));
    }

    @Test
    @DisplayName("PAG-01 findPage devolve 10 linhas por página, em ordem crescente de id, e a 11ª fica na página 2")
    void pag01PageOfTen() {
        assertTrue(dao.count() > 10);

        List<User> first = dao.findPage(0, 10);
        List<User> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(scalar("SELECT MIN(id) FROM users"), first.get(0).getId());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertEquals(dao.count() - 10, second.size());
        assertEquals(scalar("SELECT id FROM users ORDER BY id OFFSET 10 LIMIT 1"), second.get(0).getId());
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 findPage ordena por id crescente com LIMIT/OFFSET e count devolve o total")
    void paging() {
        assertEquals(15, dao.count());

        List<User> first = dao.findPage(0, 10);
        List<User> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(5, second.size());
        assertEquals("Emerson Carvalho", first.get(0).getName());
        assertEquals(1, first.get(0).getId());
        assertEquals(11, second.get(0).getId());
        assertEquals(15, second.get(4).getId());
    }

    @Test
    @DisplayName("ARQ-06 listAll devolve todos os usuários")
    void listAll() {
        assertEquals(15, dao.listAll().size());
    }
}
