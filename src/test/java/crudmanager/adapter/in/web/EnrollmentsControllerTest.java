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
import crudmanager.application.port.in.EnrollmentUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.domain.Page;
import crudmanager.domain.error.DuplicateException;
import crudmanager.domain.error.ModelException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.Enrollment;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EnrollmentsControllerTest extends ControllerTestBase {

    private EnrollmentUseCase enrollments;
    private UserUseCase users;
    private CourseUseCase courses;
    private EnrollmentsController controller;
    private final List<User> allUsers = List.of(new User(1), new User(2));
    private final List<Course> allCourses = List.of(new Course(5), new Course(6));

    @BeforeEach
    void setUp() {
        enrollments = mock(EnrollmentUseCase.class);
        users = mock(UserUseCase.class);
        courses = mock(CourseUseCase.class);
        controller = new EnrollmentsController(enrollments, users, courses);
        when(users.listAll()).thenReturn(allUsers);
        when(courses.listAll()).thenReturn(allCourses);
        jsp("/enrollments.jsp");
        jsp("/form-enrollment.jsp");
    }

    private Enrollment enrollment(int id) {
        Enrollment e = new Enrollment(id);
        e.setStudent(new User(2));
        e.setCourse(new Course(5));
        e.setEnrolledOn(LocalDate.of(2026, 3, 1));
        e.setStatus(Enrollment.Status.ATIVA);
        e.setProgress(10);
        e.setPaid(true);
        return e;
    }

    /** Parâmetros de um envio válido; valor null em {@code overrides} remove o parâmetro. */
    private Map<String, String> params(Map<String, String> overrides) {
        Map<String, String> p = new HashMap<>(Map.of("student", "2", "course", "5", "enrolledOn", "2026-03-01",
                "status", "ATIVA", "progress", "40", "paid", "on"));
        overrides.forEach((k, v) -> {
            if (v == null) {
                p.remove(k);
            } else {
                p.put(k, v);
            }
        });
        return p;
    }

    private Map<String, String> without(String key) {
        Map<String, String> m = new HashMap<>();
        m.put(key, null);
        return m;
    }

    @Test
    @DisplayName("MATR-01 GET /enrollments repassa page, expõe pageData e pageBaseUrl /enrollments e exibe enrollments.jsp")
    void list() throws Exception {
        Page<Enrollment> page = Page.of(List.of(enrollment(1)), 2, 10, 11);
        when(enrollments.list(2)).thenReturn(page);
        route("/enrollments", Map.of("page", "2"));

        controller.doGet(req, resp);

        verify(req).setAttribute("pageData", page);
        verify(req).setAttribute("pageBaseUrl", "/enrollments");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao listar exibe mensagem genérica na própria lista, sem stack trace")
    void listModelException() throws Exception {
        when(enrollments.list(1)).thenThrow(new ModelException("detalhe interno"));
        route("/enrollments", Map.of());

        controller.doGet(req, resp);

        verify(req).setAttribute("message", WebUtil.DB_ERROR_MESSAGE);
        verify(req).setAttribute("alertType", 0);
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("MATR-02 GET /enrollment/form traz alunos, cursos, situações, data de hoje e progresso 0 como padrão")
    void formInsert() throws Exception {
        route("/enrollment/form", Map.of());

        controller.doGet(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(req).setAttribute(eq("enrollment"), captor.capture());
        assertEquals(LocalDate.now(), captor.getValue().getEnrolledOn());
        assertEquals(0, captor.getValue().getProgress());
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("courses", allCourses);
        verify(req).setAttribute("statuses", Enrollment.Status.values());
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("MATR-04 GET /enrollment/update?enrollmentId=4 exibe o formulário preenchido com action update")
    void formUpdate() throws Exception {
        Enrollment e = enrollment(4);
        when(enrollments.find(4)).thenReturn(e);
        route("/enrollment/update", Map.of("enrollmentId", "4"));

        controller.doGet(req, resp);

        verify(req).setAttribute("enrollment", e);
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("MATR-12 GET /enrollment/update de matrícula inexistente redireciona com 'Matrícula não encontrada.'")
    void formUpdateUnknown() throws Exception {
        when(enrollments.find(9)).thenThrow(new NotFoundException("x"));
        route("/enrollment/update", Map.of("enrollmentId", "9"));

        controller.doGet(req, resp);

        verifyFlash("Matrícula não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/enrollments");
        verify(dispatcher, never()).forward(req, resp);
    }

    @Test
    @DisplayName("EDGE-2 GET /enrollment/update com id não numérico redireciona com 'Matrícula não encontrada.'")
    void formUpdateNonNumericId() throws Exception {
        route("/enrollment/update", Map.of("enrollmentId", "abc"));

        controller.doGet(req, resp);

        verifyFlash("Matrícula não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/enrollments");
    }

    @Test
    @DisplayName("MATR-03 POST /enrollment/insert chama create com todos os campos, avisa 'Matrícula salva com sucesso.'")
    void insert() throws Exception {
        route("/enrollment/insert", params(Map.of()));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).create(captor.capture());
        Enrollment e = captor.getValue();
        assertEquals(2, e.getStudent().getId());
        assertEquals(5, e.getCourse().getId());
        assertEquals(LocalDate.of(2026, 3, 1), e.getEnrolledOn());
        assertEquals(Enrollment.Status.ATIVA, e.getStatus());
        assertEquals(40, e.getProgress());
        assertTrue(e.isPaid());
        verifyFlash("Matrícula salva com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/enrollments");
    }

    @Test
    @DisplayName("MATR-05 POST /enrollment/update altera a matrícula existente e avisa 'Matrícula atualizada com sucesso.'")
    void update() throws Exception {
        when(enrollments.find(4)).thenReturn(enrollment(4));
        route("/enrollment/update", params(Map.of("enrollmentId", "4", "status", "CONCLUIDA", "progress", "100")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).update(captor.capture());
        assertEquals(4, captor.getValue().getId());
        assertEquals(Enrollment.Status.CONCLUIDA, captor.getValue().getStatus());
        assertEquals(100, captor.getValue().getProgress());
        verifyFlash("Matrícula atualizada com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/enrollments");
    }

    @Test
    @DisplayName("MATR-06 POST /enrollment/delete remove a matrícula, avisa 'Matrícula deletada com sucesso.' e volta à página")
    void delete() throws Exception {
        when(enrollments.find(7)).thenReturn(enrollment(7));
        route("/enrollment/delete", Map.of("id", "7", "page", "3"));

        controller.doPost(req, resp);

        verify(enrollments).delete(7);
        verifyFlash("Matrícula deletada com sucesso.", 1);
        verify(resp).sendRedirect("/crud-manager/enrollments?page=3");
    }

    @Test
    @DisplayName("MATR-07 par aluno-curso repetido reexibe o formulário com erro no campo course e valores digitados")
    void duplicate() throws Exception {
        doThrow(new DuplicateException("Este aluno já está matriculado neste curso."))
                .when(enrollments).create(any(Enrollment.class));
        route("/enrollment/insert", params(Map.of("progress", "55")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(req).setAttribute(eq("enrollment"), captor.capture());
        assertEquals(55, captor.getValue().getProgress());
        assertEquals(5, captor.getValue().getCourse().getId());
        verify(req).setAttribute("errors", Map.of("course", "Este aluno já está matriculado neste curso."));
        verify(req).setAttribute("users", allUsers);
        verify(req).setAttribute("courses", allCourses);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("MATR-07 duplicidade na edição reexibe o formulário com action update")
    void duplicateOnUpdate() throws Exception {
        when(enrollments.find(4)).thenReturn(enrollment(4));
        doThrow(new DuplicateException("Este aluno já está matriculado neste curso."))
                .when(enrollments).update(any(Enrollment.class));
        route("/enrollment/update", params(Map.of("enrollmentId", "4")));

        controller.doPost(req, resp);

        verify(req).setAttribute("errors", Map.of("course", "Este aluno já está matriculado neste curso."));
        verify(req).setAttribute("action", "update");
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("MATR-08 campos vazios chegam nulos ao serviço, que gera uma mensagem por campo, e o form reexibe")
    void invalidFields() throws Exception {
        Map<String, String> errors = Map.of("student", "Informe o aluno.", "course", "Informe o curso.",
                "enrolledOn", "Informe a data da matrícula.", "status", "Informe a situação.");
        doThrow(new ValidationException(errors)).when(enrollments).create(any(Enrollment.class));
        route("/enrollment/insert", params(Map.of("student", "", "course", "", "enrolledOn", "", "status", "")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).create(captor.capture());
        assertNull(captor.getValue().getStudent());
        assertNull(captor.getValue().getCourse());
        assertNull(captor.getValue().getEnrolledOn());
        assertNull(captor.getValue().getStatus());
        verify(req).setAttribute("errors", errors);
        verify(req).setAttribute("action", "insert");
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("MATR-08 progresso fora de 0 a 100 é repassado ao serviço, que o rejeita")
    void progressOutOfRange() throws Exception {
        Map<String, String> errors = Map.of("progress", "O progresso deve estar entre 0 e 100.");
        doThrow(new ValidationException(errors)).when(enrollments).create(any(Enrollment.class));
        route("/enrollment/insert", params(Map.of("progress", "150")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).create(captor.capture());
        assertEquals(150, captor.getValue().getProgress());
        verify(req).setAttribute("errors", errors);
    }

    @Test
    @DisplayName("MATR-08 progresso não numérico e data inválida viram erros de campo sem chamar o serviço")
    void unparsableProgressAndDate() throws Exception {
        route("/enrollment/insert", params(Map.of("progress", "abc", "enrolledOn", "31/02/2026")));

        controller.doPost(req, resp);

        verify(enrollments, never()).create(any());
        verify(req).setAttribute("errors", Map.of("enrolledOn", EnrollmentsController.INVALID_DATE,
                "progress", EnrollmentsController.INVALID_PROGRESS));
        verify(dispatcher).forward(req, resp);
    }

    @Test
    @DisplayName("MATR-09 concluída com progresso menor que 100 reexibe o formulário com 'Matrícula concluída exige progresso 100.'")
    void completedNeeds100() throws Exception {
        Map<String, String> errors = Map.of("progress", "Matrícula concluída exige progresso 100.");
        doThrow(new ValidationException(errors)).when(enrollments).create(any(Enrollment.class));
        route("/enrollment/insert", params(Map.of("status", "CONCLUIDA", "progress", "80")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).create(captor.capture());
        assertEquals(Enrollment.Status.CONCLUIDA, captor.getValue().getStatus());
        assertEquals(80, captor.getValue().getProgress());
        verify(req).setAttribute("errors", errors);
        verify(dispatcher).forward(req, resp);
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    @DisplayName("MATR-10 checkbox pago ausente grava a matrícula como não paga")
    void paidUnchecked() throws Exception {
        route("/enrollment/insert", params(without("paid")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).create(captor.capture());
        assertFalse(captor.getValue().isPaid());
    }

    @Test
    @DisplayName("MATR-10 checkbox pago marcado na edição grava a matrícula como paga")
    void paidChecked() throws Exception {
        Enrollment existing = enrollment(4);
        existing.setPaid(false);
        when(enrollments.find(4)).thenReturn(existing);
        route("/enrollment/update", params(Map.of("enrollmentId", "4")));

        controller.doPost(req, resp);

        ArgumentCaptor<Enrollment> captor = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollments).update(captor.capture());
        assertTrue(captor.getValue().isPaid());
    }

    @Test
    @DisplayName("MATR-12 POST /enrollment/update de matrícula inexistente redireciona com 'Matrícula não encontrada.'")
    void updateUnknown() throws Exception {
        when(enrollments.find(9)).thenThrow(new NotFoundException("x"));
        route("/enrollment/update", params(Map.of("enrollmentId", "9")));

        controller.doPost(req, resp);

        verifyFlash("Matrícula não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/enrollments");
        verify(enrollments, never()).update(any());
    }

    @Test
    @DisplayName("MATR-12 POST /enrollment/delete de matrícula inexistente redireciona com 'Matrícula não encontrada.'")
    void deleteUnknown() throws Exception {
        when(enrollments.find(9)).thenThrow(new NotFoundException("x"));
        route("/enrollment/delete", Map.of("id", "9"));

        controller.doPost(req, resp);

        verifyFlash("Matrícula não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/enrollments?page=1");
    }

    @Test
    @DisplayName("EDGE-2 POST /enrollment/delete com id não numérico redireciona com 'Matrícula não encontrada.' sem excluir")
    void deleteNonNumericId() throws Exception {
        route("/enrollment/delete", Map.of("id", "x", "page", "2"));

        controller.doPost(req, resp);

        verifyFlash("Matrícula não encontrada.", 0);
        verify(resp).sendRedirect("/crud-manager/enrollments?page=2");
        verify(enrollments, never()).delete(anyInt());
    }

    @Test
    @DisplayName("EDGE-2 POST /enrollment/update com id não numérico redireciona com 'Matrícula não encontrada.' sem alterar")
    void updateNonNumericId() throws Exception {
        route("/enrollment/update", params(Map.of("enrollmentId", "x")));

        controller.doPost(req, resp);

        verifyFlash("Matrícula não encontrada.", 0);
        verify(enrollments, never()).update(any());
    }

    @Test
    @DisplayName("ARQ-05 falha de dados ao salvar mostra mensagem genérica sem stack trace e redireciona")
    void insertModelException() throws Exception {
        doThrow(new ModelException("PSQLException")).when(enrollments).create(any(Enrollment.class));
        route("/enrollment/insert", params(Map.of()));

        controller.doPost(req, resp);

        verifyFlash(WebUtil.DB_ERROR_MESSAGE, 0);
        verify(resp).sendRedirect("/crud-manager/enrollments");
    }

    @Test
    @DisplayName("ARQ-05 POST em rota desconhecida responde 405")
    void unknownPost() throws Exception {
        route("/enrollments", Map.of());

        controller.doPost(req, resp);

        verify(resp).sendError(405);
    }
}
