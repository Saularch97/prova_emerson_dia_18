package crudmanager.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JdbcCourseDAOIT extends PostgresTestBase {

    private final JdbcCourseDAO dao = new JdbcCourseDAO(connectionFactory());

    @AfterEach
    void cleanUp() {
        execute("DELETE FROM courses WHERE title LIKE 'IT:%'");
    }

    private Course newCourse(String title, String description, boolean active) {
        Course c = new Course();
        c.setTitle(title);
        c.setDescription(description);
        c.setPrice(new BigDecimal("123.45"));
        c.setStartDate(LocalDate.of(2026, 8, 3));
        c.setLevel(Course.Level.INTERMEDIARIO);
        c.setActive(active);
        c.setInstructor(new User(2));
        return c;
    }

    private Course saveAndReload(Course c) {
        assertTrue(dao.save(c));
        return dao.findById((int) scalar("SELECT id FROM courses WHERE title = '" + c.getTitle() + "'"));
    }

    @Test
    @DisplayName("ARQ-06 save grava todos os campos e findById traz o nome do instrutor")
    void saveStoresAllFields() {
        Course c = saveAndReload(newCourse("IT: completo", "Descrição", true));

        assertEquals("Descrição", c.getDescription());
        assertEquals(0, new BigDecimal("123.45").compareTo(c.getPrice()));
        assertEquals(LocalDate.of(2026, 8, 3), c.getStartDate());
        assertEquals(Course.Level.INTERMEDIARIO, c.getLevel());
        assertEquals(2, c.getInstructor().getId());
        assertEquals("Luiza Carvalho", c.getInstructor().getName());
    }

    @Test
    @DisplayName("CURSO-10 curso gravado com active=false volta inativo")
    void inactivePersists() {
        Course c = saveAndReload(newCourse("IT: inativo", "d", false));

        assertFalse(c.isActive());
    }

    @Test
    @DisplayName("CURSO-10 update de ativo para inativo e de volta persiste o valor")
    void activeToggles() {
        Course c = saveAndReload(newCourse("IT: alterna", "d", true));
        assertTrue(c.isActive());
        c.setActive(false);
        assertTrue(dao.update(c));
        assertFalse(dao.findById(c.getId()).isActive());
    }

    @Test
    @DisplayName("EDGE-3 descrição com acentos e emoji grava e volta idêntica")
    void unicodeDescription() {
        String description = "Programação avançada: ação, coração, 日本語 e emoji 🚀🎓 ✓";

        Course c = saveAndReload(newCourse("IT: unicode", description, true));

        assertEquals(description, c.getDescription());
    }

    @Test
    @DisplayName("CURSO-07 excluir curso com matrícula lança RelatedDataException e o curso continua lá")
    void deleteCourseWithEnrollment() {
        Course java = dao.findById(1);
        assertNotNull(java);

        assertThrows(RelatedDataException.class, () -> dao.delete(java));

        assertEquals("Java Básico", dao.findById(1).getTitle());
    }

    @Test
    @DisplayName("ARQ-06 update altera campos e instrutor; delete de curso sem matrícula o remove")
    void updateAndDelete() {
        Course c = saveAndReload(newCourse("IT: editar", "d", true));
        c.setTitle("IT: editado");
        c.setPrice(new BigDecimal("0.00"));
        c.setLevel(Course.Level.AVANCADO);
        c.setInstructor(new User(3));

        assertTrue(dao.update(c));

        Course after = dao.findById(c.getId());
        assertEquals("IT: editado", after.getTitle());
        assertEquals(0, BigDecimal.ZERO.compareTo(after.getPrice()));
        assertEquals(Course.Level.AVANCADO, after.getLevel());
        assertEquals("Elenice Carvalho", after.getInstructor().getName());

        assertTrue(dao.delete(after));
        assertNull(dao.findById(c.getId()));
    }

    @Test
    @DisplayName("PAG-01 findPage devolve 10 linhas por página, em ordem crescente de id, e a 11ª fica na página 2")
    void pag01PageOfTen() {
        assertTrue(dao.count() > 10);

        List<Course> first = dao.findPage(0, 10);
        List<Course> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(scalar("SELECT MIN(id) FROM courses"), first.get(0).getId());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertEquals(dao.count() - 10, second.size());
        assertEquals(scalar("SELECT id FROM courses ORDER BY id OFFSET 10 LIMIT 1"), second.get(0).getId());
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 findPage ordena por id crescente com LIMIT/OFFSET e instrutor com nome; count devolve o total")
    void paging() {
        assertEquals(12, dao.count());

        List<Course> first = dao.findPage(0, 10);
        List<Course> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(2, second.size());
        assertEquals("Java Básico", first.get(0).getTitle());
        assertEquals("Emerson Carvalho", first.get(0).getInstructor().getName());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 listAll devolve todos os cursos para o select")
    void listAll() {
        assertEquals(12, dao.listAll().size());
    }
}
