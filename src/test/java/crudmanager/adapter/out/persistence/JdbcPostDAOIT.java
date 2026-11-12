package crudmanager.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.model.Post;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcPostDAOIT extends PostgresTestBase {

    private final JdbcPostDAO dao = new JdbcPostDAO(connectionFactory());

    @AfterEach
    void cleanUp() {
        execute("DELETE FROM posts WHERE content LIKE 'IT:%'");
    }

    private Post save(String content, int userId) {
        Post p = new Post();
        p.setContent(content);
        p.setUser(new User(userId));
        assertTrue(dao.save(p));
        return dao.findById((int) scalar("SELECT id FROM posts WHERE content = '" + content + "'"));
    }

    @Test
    @DisplayName("ARQ-06 save grava o post com a data de hoje e o usuário com nome")
    void saveStoresTodayAndUser() {
        Post p = save("IT: novo post çãé", 2);

        assertEquals("IT: novo post çãé", p.getContent());
        assertEquals(LocalDate.now(), p.getPostDate());
        assertEquals(2, p.getUser().getId());
        assertEquals("Luiza Carvalho", p.getUser().getName());
    }

    @Test
    @DisplayName("ARQ-06 update troca conteúdo e usuário sem alterar a data")
    void updateChangesContentAndUser() {
        Post p = save("IT: antes", 1);
        execute("UPDATE posts SET post_date = '2020-01-02' WHERE id = " + p.getId());
        Post stored = dao.findById(p.getId());
        stored.setContent("IT: depois");
        stored.setUser(new User(3));

        assertTrue(dao.update(stored));

        Post after = dao.findById(p.getId());
        assertEquals("IT: depois", after.getContent());
        assertEquals(3, after.getUser().getId());
        assertEquals(LocalDate.of(2020, 1, 2), after.getPostDate());
    }

    @Test
    @DisplayName("ARQ-06 delete remove o post")
    void deleteRemoves() {
        Post p = save("IT: apagar", 1);

        assertTrue(dao.delete(p));

        assertNull(dao.findById(p.getId()));
    }

    @Test
    @DisplayName("PAG-01 findPage devolve 10 linhas por página, em ordem crescente de id, e a 11ª fica na página 2")
    void pag01PageOfTen() {
        assertTrue(dao.count() > 10);

        List<Post> first = dao.findPage(0, 10);
        List<Post> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(scalar("SELECT MIN(id) FROM posts"), first.get(0).getId());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertEquals(dao.count() - 10, second.size());
        assertEquals(scalar("SELECT id FROM posts ORDER BY id OFFSET 10 LIMIT 1"), second.get(0).getId());
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 findPage ordena por id crescente com LIMIT/OFFSET e traz o nome do usuário; count devolve o total")
    void paging() {
        assertEquals(13, dao.count());

        List<Post> first = dao.findPage(0, 10);
        List<Post> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(3, second.size());
        assertEquals("Olá do Emerson", first.get(0).getContent());
        assertEquals("Emerson Carvalho", first.get(0).getUser().getName());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 listAll devolve todos os posts com o nome do usuário")
    void listAll() {
        List<Post> all = dao.listAll();

        assertEquals(13, all.size());
        assertEquals("Emerson Carvalho", all.get(0).getUser().getName());
    }
}
