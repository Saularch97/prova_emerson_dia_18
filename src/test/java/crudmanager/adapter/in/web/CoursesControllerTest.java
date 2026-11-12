package crudmanager.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import crudmanager.application.port.in.CourseUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.port.out.CourseDAO;
import crudmanager.application.service.CourseService;
import crudmanager.domain.Page;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CoursesControllerTest extends ControllerTestBase {

    private CourseUseCase courses;
    private UserUseCase users;
    private CoursesController controller;
    private CourseDAO dao;
    /** Controller ligado ao serviço real (DAO mockado): prova a mescla de erros de parse e de regra. */
    private CoursesController realController;
    private final List<User> allUsers = List.of(new User(1), new User(2));

    @BeforeEach
    void setUp() {
        courses = mock(CourseUseCase.class);
        users = mock(UserUseCase.class);
        controller = new CoursesController(courses, users);
        dao = mock(CourseDAO.class);
        realController = new CoursesController(new CourseService(dao), users);
        when(users.listAll()).thenReturn(allUsers);
        jsp("/courses.jsp");
        jsp("/form-course.jsp");
    }

    private Course course(int id) {
        Course c = new Course(id);
        c.setTitle("Java");
        c.setDescription("Curso de Java");
        c.setPrice(new BigDecimal("99.90"));
        c.setStartDate(LocalDate.of(2026, 2, 1));
        c.setLevel(Course.Level.INICIANTE);
        c.setActive(true);
        c.setInstructor(new User(2));
        return c;
    }

    /** Parâmetros de um envio válido; chaves de {@code overrides} substituem, valor null remove o parâmetro. */
    private Map<String, String> params(Map<String, String> overrides) {
        Map<String, String> p = new HashMap<>(Map.of("title", "Java", "description", "Curso de Java",
                "price", "99,90", "startDate", "2026-02-01", "level", "INTERMEDIARIO", "active", "on",
                "instructor", "2"));
        overrides.forEach((k, v) -> {
            if (v == null) {
                p.remove(k);
            } else {
                p.put(k, v);
            }
        });
        return p;
    }

    private Map<String, String> withNo(String key) {
        Map<String, String> m = new HashMap<>();
        m.put(key, null);
        return m;
    }

    @Test
    @DisplayName("CURSO-01 GET /courses repassa page, expõe pageData e pageBaseUrl /courses e exibe courses.jsp")
    void list() throws Exception {
        Page<Course> page = Page.of(List.of(course(1)), 2, 10, 11);
        when(courses.list(2)).thenReturn(page);
        route("/courses", Map.of("page", "2"));

        controller.doGet(req, resp);

        verify(req).setAttribute("pageData", page);
        verify(req).setAttribute("pageBaseUrl", "/courses");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao listar exibe mensagem genérica na própria lista, sem stack trace")
    void listModelException() throws Exception {
        when(courses.list(1)).thenThrow(new ModelException("detalhe interno"));
        route("/courses", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("message", WebUtil.DB_ERROR_MESSAGE);
        verify(req).setAttribute("alertType", 0);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("CURSO-02 GET /course/form exibe o formulário com action insert, usuários e níveis")
    void formInsert() throws Exception {
        route("/course/form", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("levels", Course.Level.values());
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("CURSO-04 GET /course/update?courseId=4 exibe o formulário preenchido com action update")
    void formUpdate() throws Exception {
        Course c = course(4);
        when(courses.find(4)).thenReturn(c);
        route("/course/update", Map.of("courseId", "4"));

        controller.doGet(req, resp);

        verify(req).setAttribute("course", c);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("CURSO-12 GET /course/update de curso inexistente redireciona com 'Curso não encontrado.'")
    void formUpdateUnknown() throws Exception {
        when(courses.find(9)).thenThrow(new NotFoundException("x"));
        route("/course/update", Map.of("courseId", "9"));

        controller.doGet(req, resp);

        verifyFlash("Curso não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/courses");
        verify(dispatcher, never()).forward(req, resp);
    }

    @Test
    @DisplayName("EDGE-2 GET /course/update com id não numérico redireciona com 'Curso não encontrado.', sem erro 500")
    void formUpdateNonNumericId() throws Exception {
        route("/course/update", Map.of("courseId", "abc"));

        controller.doGet(req, resp);

        verifyFlash("Curso não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/courses");
    }

    @Test
    @DisplayName("CURSO-03 POST /course/insert chama create com todos os campos, avisa 'salvo com sucesso' e redireciona")
    void insert() throws Exception {
        route("/course/insert", params(Map.of()));

        controller.doPost(req, resp);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).create(captor.capture());
        Course c = captor.getValue();
        assertEquals("Java", c.getTitle());
        assertEquals("Curso de Java", c.getDescription());
        assertEquals(new BigDecimal("99.90"), c.getPrice());
        assertEquals(LocalDate.of(2026, 2, 1), c.getStartDate());
        assertEquals(Course.Level.INTERMEDIARIO, c.getLevel());
        assertTrue(c.isActive());
        assertEquals(2, c.getInstructor().getId());
        verifyFlash("Curso 'Java' salvo com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/courses");
    }

    @Test
    @DisplayName("CURSO-05 POST /course/update altera o curso existente, avisa 'atualizado com sucesso' e redireciona")
    void update() throws Exception {
        when(courses.find(4)).thenReturn(course(4));
        Map<String, String> p = params(Map.of("courseId", "4", "title", "Novo", "price", "150.5"));
        route("/course/update", p);

        controller.doPost(req, resp);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).update(captor.capture());
        assertEquals(4, captor.getValue().getId());
        assertEquals("Novo", captor.getValue().getTitle());
        assertEquals(new BigDecimal("150.5"), captor.getValue().getPrice());
        verifyFlash("Curso 'Novo' atualizado com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/courses");
    }

    @Test
    @DisplayName("CURSO-06 POST /course/delete remove o curso, avisa 'deletado com sucesso' e volta para a mesma página")
    void delete() throws Exception {
        when(courses.find(7)).thenReturn(course(7));
        route("/course/delete", Map.of("id", "7", "page", "3"));

        controller.doPost(req, resp);

        verify(courses).delete(7);
        verifyFlash("Curso 'Java' deletado com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/courses?page=3");
    }

    @Test
    @DisplayName("CURSO-07 POST /course/delete de curso com matrículas mantém o curso e avisa dados relacionados")
    void deleteRelated() throws Exception {
        when(courses.find(7)).thenReturn(course(7));
        doThrow(new RelatedDataException("fk")).when(courses).delete(7);
        route("/course/delete", Map.of("id", "7", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Curso 'Java' não pode ser deletado. Há dados relacionados ao curso.", 0);
        verify(resp).sendRedirect("/crud-manager/courses?page=2");
    }

    @Test
    @DisplayName("CURSO-08 envio inválido reexibe o formulário com valores digitados, errors, users, levels e action")
    void insertInvalid() throws Exception {
        Map<String, String> errors = Map.of("title", "Informe o título.");
        doThrow(new ValidationException(errors)).when(courses).create(any(Course.class));
        route("/course/insert", params(Map.of("title", "", "price", "10")));

        controller.doPost(req, resp);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(req).setAttribute(eq("course"), captor.capture());
        assertEquals("Curso de Java", captor.getValue().getDescription());
        assertEquals(new BigDecimal("10"), captor.getValue().getPrice());
        assertEquals(2, captor.getValue().getInstructor().getId());
        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("CURSO-08 preço não numérico vira erro no campo price, mantém o texto digitado e nada é gravado")
    void nonNumericPrice() throws Exception {
        route("/course/insert", params(Map.of("price", "abc")));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        verify(req).setAttribute("errors", Map.of("price", "Informe um preço numérico válido (ex.: 99,90)."));
        verify(req).setAttribute("priceInput", "abc");
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("CURSO-08 data de início inválida vira erro no campo startDate e nada é gravado")
    void invalidStartDate() throws Exception {
        when(dao.findById(4)).thenReturn(course(4));
        route("/course/update", params(Map.of("courseId", "4", "startDate", "31/02/2026")));

        realController.doPost(req, resp);

        verify(dao, never()).update(any());
        verify(req).setAttribute("errors", Map.of("startDate", "Informe uma data de início válida."));
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("CURSO-08 título vazio e preço abc mostram as duas mensagens e nada é gravado")
    void emptyTitleAndNonNumericPrice() throws Exception {
        route("/course/insert", params(Map.of("title", "", "price", "abc")));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        Map<String, String> expected = new java.util.LinkedHashMap<>();
        expected.put("price", "Informe um preço numérico válido (ex.: 99,90).");
        expected.put("title", "Informe o título.");
        verify(req).setAttribute("errors", expected);
        verify(req).setAttribute("priceInput", "abc");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("CURSO-08 data inválida e descrição vazia mostram as duas mensagens e nada é gravado")
    void invalidDateAndEmptyDescription() throws Exception {
        route("/course/insert", params(Map.of("startDate", "garbage", "description", "")));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        Map<String, String> expected = new java.util.LinkedHashMap<>();
        expected.put("startDate", "Informe uma data de início válida.");
        expected.put("description", "Informe a descrição.");
        verify(req).setAttribute("errors", expected);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("CURSO-08 preço abc, data inválida e título vazio mostram as três mensagens, a de parse prevalece")
    void allThreeInvalid() throws Exception {
        route("/course/insert", params(Map.of("title", "", "price", "abc", "startDate", "x")));

        realController.doPost(req, resp);

        verify(dao, never()).save(any());
        Map<String, String> expected = new java.util.LinkedHashMap<>();
        expected.put("price", "Informe um preço numérico válido (ex.: 99,90).");
        expected.put("startDate", "Informe uma data de início válida.");
        expected.put("title", "Informe o título.");
        verify(req).setAttribute("errors", expected);
    }

    @Test
    @DisplayName("CURSO-08 preço com vírgula ou ponto é aceito; preço negativo segue ao serviço, que o rejeita")
    void priceFormats() {
        assertEquals(new BigDecimal("99.90"), CoursesController.parsePrice("99,90"));
        assertEquals(new BigDecimal("99.9"), CoursesController.parsePrice(" 99.9 "));
        assertEquals(new BigDecimal("-5"), CoursesController.parsePrice("-5"));
        assertNull(CoursesController.parsePrice(""));
        assertNull(CoursesController.parsePrice(null));
    }

    @Test
    @DisplayName("CURSO-08 campos ausentes chegam nulos ao serviço, que gera uma mensagem por campo")
    void missingFieldsGoToService() throws Exception {
        Map<String, String> errors = Map.of("level", "Informe o nível.", "instructor", "Informe o instrutor.");
        doThrow(new ValidationException(errors)).when(courses).create(any(Course.class));
        Map<String, String> p = params(withNo("level"));
        p.remove("instructor");
        route("/course/insert", p);

        controller.doPost(req, resp);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).create(captor.capture());
        assertNull(captor.getValue().getLevel());
        assertNull(captor.getValue().getInstructor());
        verify(req).setAttribute("errors", errors);
    }

    @Test
    @DisplayName("CURSO-10 checkbox ativo ausente grava o curso como inativo")
    void activeUnchecked() throws Exception {
        route("/course/insert", params(withNo("active")));

        controller.doPost(req, resp);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).create(captor.capture());
        assertFalse(captor.getValue().isActive());
    }

    @Test
    @DisplayName("CURSO-10 checkbox ativo marcado na edição grava o curso como ativo")
    void activeChecked() throws Exception {
        Course existing = course(4);
        existing.setActive(false);
        when(courses.find(4)).thenReturn(existing);
        route("/course/update", params(Map.of("courseId", "4")));

        controller.doPost(req, resp);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courses).update(captor.capture());
        assertTrue(captor.getValue().isActive());
    }

    @Test
    @DisplayName("CURSO-12 POST /course/update de curso inexistente redireciona com 'Curso não encontrado.'")
    void updateUnknown() throws Exception {
        when(courses.find(9)).thenThrow(new NotFoundException("x"));
        route("/course/update", params(Map.of("courseId", "9")));

        controller.doPost(req, resp);

        verifyFlash("Curso não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/courses");
        verify(courses, never()).update(any());
    }

    @Test
    @DisplayName("CURSO-12 POST /course/delete de curso inexistente redireciona com 'Curso não encontrado.'")
    void deleteUnknown() throws Exception {
        when(courses.find(9)).thenThrow(new NotFoundException("x"));
        route("/course/delete", Map.of("id", "9"));

        controller.doPost(req, resp);

        verifyFlash("Curso não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/courses?page=1");
    }

    @Test
    @DisplayName("EDGE-2 POST /course/delete com id não numérico redireciona com 'Curso não encontrado.' sem excluir")
    void deleteNonNumericId() throws Exception {
        route("/course/delete", Map.of("id", "x", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Curso não encontrado.", 0);
        verify(resp).sendRedirect("/crud-manager/courses?page=2");
        verify(courses, never()).delete(anyInt());
    }

    @Test
    @DisplayName("PAG-07 POST /course/delete sem page redireciona para a página 1 e com page inválida também")
    void deletePageFallback() throws Exception {
        when(courses.find(7)).thenReturn(course(7));
        route("/course/delete", Map.of("id", "7", "page", "abc"));

        controller.doPost(req, resp);

        verify(resp).sendRedirect("/crud-manager/courses?page=1");
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao salvar mostra mensagem genérica sem stack trace e redireciona")
    void insertModelException() throws Exception {
        doThrow(new ModelException("PSQLException")).when(courses).create(any(Course.class));
        route("/course/insert", params(Map.of()));

        controller.doPost(req, resp);

        verifyFlash(WebUtil.DB_ERROR_MESSAGE, 0);
        verify(resp).sendRedirect("/crud-manager/courses");
    }

    @Test
    @DisplayName("ARQ-05 POST em rota desconhecida responde 405")
    void unknownPost() throws Exception {
        route("/courses", Map.of());

        controller.doPost(req, resp);

        verify(resp).sendError(405);
    }
}
