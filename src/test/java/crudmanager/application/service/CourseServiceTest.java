package crudmanager.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CourseServiceTest {

    private final FakeCourseDAO courses = new FakeCourseDAO();
    private final CourseService service = new CourseService(courses);

    private Course valid(int id) {
        Course c = new Course(id);
        c.setTitle("Java");
        c.setDescription("Curso de Java");
        c.setPrice(new BigDecimal("99.90"));
        c.setStartDate(LocalDate.of(2026, 2, 1));
        c.setLevel(Course.Level.INICIANTE);
        c.setActive(true);
        c.setInstructor(new User(1));
        return c;
    }

    private static ValidationException invalid(org.junit.jupiter.api.function.Executable e) {
        return assertThrows(ValidationException.class, e);
    }

    @Test
    @DisplayName("CURSO-03 curso válido é gravado")
    void createValid() {
        service.create(valid(0));
        assertEquals(1, courses.count());
        assertEquals("Java", courses.store.values().iterator().next().getTitle());
    }

    @Test
    @DisplayName("CURSO-05 update válido atualiza o curso")
    void updateValid() {
        courses.store.put(4, valid(4));
        Course edited = valid(4);
        edited.setPrice(new BigDecimal("150.00"));
        service.update(edited);
        assertEquals(new BigDecimal("150.00"), courses.findById(4).getPrice());
    }

    @Test
    @DisplayName("CURSO-06 delete remove o curso")
    void deleteRemoves() {
        courses.store.put(4, valid(4));
        service.delete(4);
        assertEquals(0, courses.count());
    }

    @Test
    @DisplayName("CURSO-07 curso com matrículas mantém-se e repassa RelatedDataException")
    void deleteRelated() {
        courses.store.put(4, valid(4));
        courses.deleteBlocked = true;
        assertThrows(RelatedDataException.class, () -> service.delete(4));
        assertNotNull(courses.findById(4));
    }

    @Test
    @DisplayName("CURSO-08 título, descrição, início e instrutor vazios geram uma mensagem por campo")
    void requiredFields() {
        Course c = new Course();
        ValidationException e = invalid(() -> service.create(c));
        assertTrue(e.getErrors().keySet().containsAll(java.util.List.of(
                "title", "description", "startDate", "instructor", "price", "level")));
        assertEquals("Informe o título.", e.getErrors().get("title"));
        assertEquals("Informe a descrição.", e.getErrors().get("description"));
        assertEquals("Informe a data de início.", e.getErrors().get("startDate"));
        assertEquals("Informe o instrutor.", e.getErrors().get("instructor"));
        assertEquals("Informe o nível.", e.getErrors().get("level"));
        assertEquals("Informe um preço válido (número maior ou igual a zero, com até 2 casas decimais).",
                e.getErrors().get("price"));
        assertEquals(6, e.getErrors().size());
        assertEquals(0, courses.count());
    }

    @Test
    @DisplayName("CURSO-08 preço negativo ou ausente (não numérico) é rejeitado; zero é aceito")
    void price() {
        Course neg = valid(0);
        neg.setPrice(new BigDecimal("-0.01"));
        assertTrue(invalid(() -> service.create(neg)).getErrors().containsKey("price"));
        Course missing = valid(0);
        missing.setPrice(null);
        assertTrue(invalid(() -> service.create(missing)).getErrors().containsKey("price"));
        Course zero = valid(0);
        zero.setPrice(BigDecimal.ZERO);
        service.create(zero);
        assertEquals(1, courses.count());
    }

    @Test
    @DisplayName("CURSO-08 preço com mais de 2 casas decimais é rejeitado")
    void priceScale() {
        Course c = valid(0);
        c.setPrice(new BigDecimal("1.234"));
        assertTrue(invalid(() -> service.create(c)).getErrors().containsKey("price"));
    }

    @Test
    @DisplayName("CURSO-08 preço 99999999.99 é aceito; 100000000 é rejeitado com a mensagem do limite máximo")
    void priceUpperBound() {
        Course top = valid(0);
        top.setPrice(new BigDecimal("99999999.99"));
        service.create(top);
        assertEquals(1, courses.count());
        Course over = valid(0);
        over.setPrice(new BigDecimal("100000000"));
        assertEquals("O preço deve ser no máximo 99999999,99.",
                invalid(() -> service.create(over)).getErrors().get("price"));
        assertEquals(1, courses.count());
    }

    @Test
    @DisplayName("CURSO-09 título com 151 caracteres é rejeitado com mensagem de tamanho máximo; 150 é aceito")
    void titleTooLong() {
        Course c = valid(0);
        c.setTitle("x".repeat(151));
        ValidationException e = invalid(() -> service.create(c));
        assertEquals("O título deve ter no máximo 150 caracteres.", e.getErrors().get("title"));
        Course ok = valid(0);
        ok.setTitle("x".repeat(150));
        service.create(ok);
        assertEquals(1, courses.count());
    }

    @Test
    @DisplayName("CURSO-09 o limite conta caracteres, não unidades UTF-16: 100 e 150 emojis são aceitos, 151 é rejeitado")
    void titleCountsCodePoints() {
        String emoji = new String(Character.toChars(0x1F600));
        for (int n : new int[] {100, 150}) {
            Course ok = valid(0);
            ok.setTitle(emoji.repeat(n));
            service.create(ok);
        }
        assertEquals(2, courses.count());
        Course tooLong = valid(0);
        tooLong.setTitle(emoji.repeat(151));
        assertEquals("O título deve ter no máximo 150 caracteres.",
                invalid(() -> service.create(tooLong)).getErrors().get("title"));
        assertEquals(2, courses.count());
    }

    @Test
    @DisplayName("CURSO-12 find, update e delete de id inexistente lançam NotFoundException 'Curso não encontrado.'")
    void notFound() {
        assertEquals("Curso não encontrado.", assertThrows(NotFoundException.class, () -> service.find(9)).getMessage());
        assertThrows(NotFoundException.class, () -> service.delete(9));
        assertThrows(NotFoundException.class, () -> service.update(valid(9)));
    }

    @Test
    @DisplayName("ARQ-03 o valor de active é gravado como recebido (false permanece false)")
    void inactiveKept() {
        Course c = valid(0);
        c.setActive(false);
        service.create(c);
        assertEquals(false, courses.store.values().iterator().next().isActive());
    }

    @Test
    @DisplayName("ARQ-03 list(page) pagina em 10 e clampa a página")
    void list() {
        for (int i = 1; i <= 25; i++) {
            courses.store.put(i, valid(i));
        }
        assertEquals(10, service.list(1).getItems().size());
        assertEquals(3, service.list(99).getPage());
        assertEquals(5, service.list(99).getItems().size());
    }

    @Test
    @DisplayName("MATR-02 listAll devolve todos os cursos, sem paginar, para o select do formulário de matrícula")
    void listAll() {
        for (int i = 1; i <= 25; i++) {
            courses.store.put(i, valid(i));
        }
        assertEquals(25, service.listAll().size());
    }
}
