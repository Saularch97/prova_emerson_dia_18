package crudmanager.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.error.DuplicateException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.Enrollment;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class EnrollmentServiceTest {

    private final FakeEnrollmentDAO enrollments = new FakeEnrollmentDAO();
    private final EnrollmentService service = new EnrollmentService(enrollments);

    private Enrollment valid(int id, int student, int course) {
        Enrollment e = new Enrollment(id);
        e.setStudent(new User(student));
        e.setCourse(new Course(course));
        e.setEnrolledOn(LocalDate.of(2026, 3, 1));
        e.setStatus(Enrollment.Status.ATIVA);
        e.setProgress(40);
        e.setPaid(true);
        return e;
    }

    private static ValidationException invalid(Executable e) {
        return assertThrows(ValidationException.class, e);
    }

    @Test
    @DisplayName("MATR-03 matrícula válida é gravada")
    void createValid() {
        service.create(valid(0, 1, 1));
        assertEquals(1, enrollments.count());
    }

    @Test
    @DisplayName("MATR-05 update válido atualiza a matrícula")
    void updateValid() {
        enrollments.store.put(5, valid(5, 1, 1));
        Enrollment edited = valid(5, 1, 1);
        edited.setProgress(80);
        service.update(edited);
        assertEquals(80, enrollments.findById(5).getProgress());
    }

    @Test
    @DisplayName("MATR-06 delete remove a matrícula")
    void deleteRemoves() {
        enrollments.store.put(5, valid(5, 1, 1));
        service.delete(5);
        assertNull(enrollments.findById(5));
    }

    @Test
    @DisplayName("MATR-07 par aluno-curso repetido lança DuplicateException com a mensagem do spec e não grava")
    void duplicatePair() {
        enrollments.store.put(5, valid(5, 1, 1));
        DuplicateException e = assertThrows(DuplicateException.class, () -> service.create(valid(0, 1, 1)));
        assertEquals("Este aluno já está matriculado neste curso.", e.getMessage());
        assertEquals(1, enrollments.count());
        service.create(valid(0, 1, 2));
        service.create(valid(0, 2, 1));
        assertEquals(3, enrollments.count());
    }

    @Test
    @DisplayName("MATR-07 na edição a própria matrícula é ignorada; outra com o mesmo par é duplicada")
    void duplicateIgnoresSelf() {
        enrollments.store.put(5, valid(5, 1, 1));
        enrollments.store.put(6, valid(6, 2, 2));
        service.update(valid(5, 1, 1));
        assertEquals(5, enrollments.lastIgnoreId);
        assertThrows(DuplicateException.class, () -> service.update(valid(6, 1, 1)));
    }

    @Test
    @DisplayName("MATR-07 na criação a checagem de duplicidade não ignora nenhum id")
    void createChecksWithoutIgnore() {
        service.create(valid(0, 1, 1));
        assertTrue(enrollments.existsCalled);
        assertNull(enrollments.lastIgnoreId);
    }

    @Test
    @DisplayName("MATR-08 aluno, curso, data e situação vazios geram uma mensagem por campo")
    void requiredFields() {
        Enrollment e = new Enrollment();
        ValidationException ex = invalid(() -> service.create(e));
        assertEquals("Informe o aluno.", ex.getErrors().get("student"));
        assertEquals("Informe o curso.", ex.getErrors().get("course"));
        assertEquals("Informe a data da matrícula.", ex.getErrors().get("enrolledOn"));
        assertEquals("Informe a situação.", ex.getErrors().get("status"));
        assertEquals(4, ex.getErrors().size());
        assertEquals(0, enrollments.count());
    }

    @Test
    @DisplayName("MATR-08 progresso fora de 0 a 100 é rejeitado; 0 e 100 são aceitos")
    void progressRange() {
        for (int bad : new int[] {-1, 101}) {
            Enrollment e = valid(0, 1, 1);
            e.setProgress(bad);
            assertEquals("O progresso deve estar entre 0 e 100.",
                    invalid(() -> service.create(e)).getErrors().get("progress"));
        }
        Enrollment zero = valid(0, 1, 1);
        zero.setProgress(0);
        service.create(zero);
        Enrollment full = valid(0, 2, 1);
        full.setProgress(100);
        service.create(full);
        assertEquals(2, enrollments.count());
    }

    @Test
    @DisplayName("MATR-09 concluída com progresso menor que 100 é rejeitada com a mensagem do spec")
    void completedNeeds100() {
        Enrollment e = valid(0, 1, 1);
        e.setStatus(Enrollment.Status.CONCLUIDA);
        e.setProgress(99);
        ValidationException ex = invalid(() -> service.create(e));
        assertEquals("Matrícula concluída exige progresso 100.", ex.getErrors().get("progress"));
        e.setProgress(100);
        service.create(e);
        assertEquals(1, enrollments.count());
    }

    @Test
    @DisplayName("MATR-10 o valor de paid é gravado como recebido (false permanece false)")
    void unpaidKept() {
        Enrollment e = valid(0, 1, 1);
        e.setPaid(false);
        service.create(e);
        assertEquals(false, enrollments.store.values().iterator().next().isPaid());
    }

    @Test
    @DisplayName("MATR-12 find, update e delete de id inexistente lançam NotFoundException 'Matrícula não encontrada.'")
    void notFound() {
        assertEquals("Matrícula não encontrada.",
                assertThrows(NotFoundException.class, () -> service.find(9)).getMessage());
        assertThrows(NotFoundException.class, () -> service.delete(9));
        assertThrows(NotFoundException.class, () -> service.update(valid(9, 1, 1)));
    }

    @Test
    @DisplayName("ARQ-03 list(page) pagina em 10 e clampa a página")
    void list() {
        for (int i = 1; i <= 25; i++) {
            enrollments.store.put(i, valid(i, i, i));
        }
        assertEquals(10, service.list(1).getItems().size());
        assertEquals(3, service.list(99).getPage());
        assertNotNull(service.find(3));
    }
}
