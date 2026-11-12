package crudmanager.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.model.Company;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcCompanyDAOIT extends PostgresTestBase {

    private final JdbcCompanyDAO dao = new JdbcCompanyDAO(connectionFactory());

    @AfterEach
    void cleanUp() {
        execute("DELETE FROM companies WHERE name LIKE 'IT:%'");
    }

    private Company save(String name, LocalDate end) {
        Company c = new Company();
        c.setName(name);
        c.setRole("Cargo");
        c.setStart(LocalDate.of(2020, 1, 15));
        c.setEnd(end);
        c.setUser(new User(2));
        assertTrue(dao.save(c));
        return dao.findById((int) scalar("SELECT id FROM companies WHERE name = '" + name + "'"));
    }

    @Test
    @DisplayName("ARQ-06 save com saída grava as duas datas e o usuário com nome")
    void saveWithEnd() {
        Company c = save("IT: com saída", LocalDate.of(2021, 12, 31));

        assertEquals(LocalDate.of(2020, 1, 15), c.getStart());
        assertEquals(LocalDate.of(2021, 12, 31), c.getEnd());
        assertEquals("Cargo", c.getRole());
        assertEquals(2, c.getUser().getId());
        assertEquals("Luiza Carvalho", c.getUser().getName());
    }

    @Test
    @DisplayName("ARQ-06 save sem saída grava a saída como nula")
    void saveWithoutEnd() {
        Company c = save("IT: sem saída", null);

        assertNull(c.getEnd());
    }

    @Test
    @DisplayName("ARQ-06 update altera campos, usuário e saída (inclusive limpando a saída)")
    void update() {
        Company c = save("IT: editar", LocalDate.of(2021, 1, 1));
        c.setName("IT: editado");
        c.setRole("Novo cargo");
        c.setEnd(null);
        c.setUser(new User(3));

        assertTrue(dao.update(c));

        Company after = dao.findById(c.getId());
        assertEquals("IT: editado", after.getName());
        assertEquals("Novo cargo", after.getRole());
        assertNull(after.getEnd());
        assertEquals(3, after.getUser().getId());
    }

    @Test
    @DisplayName("ARQ-06 delete remove a empresa")
    void delete() {
        Company c = save("IT: apagar", null);

        assertTrue(dao.delete(c));

        assertNull(dao.findById(c.getId()));
    }

    @Test
    @DisplayName("ARQ-06 findPage ordena por id crescente com LIMIT/OFFSET e nome do usuário; count devolve o total")
    void paging() {
        assertEquals(12, dao.count());

        List<Company> first = dao.findPage(0, 10);
        List<Company> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(2, second.size());
        assertEquals("Acme Tecnologia", first.get(0).getName());
        assertEquals("Emerson Carvalho", first.get(0).getUser().getName());
        assertEquals(LocalDate.of(2020, 6, 30), first.get(0).getEnd());
        assertNull(first.get(1).getEnd());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 listAll devolve todas as empresas")
    void listAll() {
        assertEquals(12, dao.listAll().size());
    }
}
