package crudmanager.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crudmanager.domain.error.DuplicateException;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.Enrollment;
import crudmanager.domain.model.User;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Os testes criam matrículas apenas para o aluno 15 (João Pereira), que não tem matrículas no seed. */
class JdbcEnrollmentDAOIT extends PostgresTestBase {

    private static final int FREE_STUDENT = 15;

    private final JdbcEnrollmentDAO dao = new JdbcEnrollmentDAO(connectionFactory());

    @AfterEach
    void cleanUp() {
        execute("DELETE FROM enrollments WHERE student_id = " + FREE_STUDENT);
    }

    private Enrollment newEnrollment(int studentId, int courseId, boolean paid) {
        Enrollment e = new Enrollment();
        e.setStudent(new User(studentId));
        e.setCourse(new Course(courseId));
        e.setEnrolledOn(LocalDate.of(2026, 7, 1));
        e.setStatus(Enrollment.Status.ATIVA);
        e.setProgress(30);
        e.setPaid(paid);
        return e;
    }

    private Enrollment saveAndReload(Enrollment e) {
        assertTrue(dao.save(e));
        return dao.findById((int) scalar("SELECT id FROM enrollments WHERE student_id = "
                + e.getStudent().getId() + " AND course_id = " + e.getCourse().getId()));
    }

    @Test
    @DisplayName("ARQ-06 save grava todos os campos e findById traz nome do aluno e título do curso")
    void saveStoresAllFields() {
        Enrollment e = saveAndReload(newEnrollment(FREE_STUDENT, 2, true));

        assertEquals(FREE_STUDENT, e.getStudent().getId());
        assertEquals("João Pereira", e.getStudent().getName());
        assertEquals(2, e.getCourse().getId());
        assertEquals("Java Avançado", e.getCourse().getTitle());
        assertEquals(LocalDate.of(2026, 7, 1), e.getEnrolledOn());
        assertEquals(Enrollment.Status.ATIVA, e.getStatus());
        assertEquals(30, e.getProgress());
        assertTrue(e.isPaid());
    }

    @Test
    @DisplayName("MATR-10 matrícula gravada com paid=false volta não paga")
    void unpaidPersists() {
        Enrollment e = saveAndReload(newEnrollment(FREE_STUDENT, 2, false));

        assertFalse(e.isPaid());
    }

    @Test
    @DisplayName("MATR-10 update de pago para não pago persiste o valor")
    void paidToUnpaid() {
        Enrollment e = saveAndReload(newEnrollment(FREE_STUDENT, 2, true));
        e.setPaid(false);
        e.setStatus(Enrollment.Status.CONCLUIDA);
        e.setProgress(100);

        assertTrue(dao.update(e));

        Enrollment after = dao.findById(e.getId());
        assertFalse(after.isPaid());
        assertEquals(Enrollment.Status.CONCLUIDA, after.getStatus());
        assertEquals(100, after.getProgress());
    }

    @Test
    @DisplayName("MATR-07 par aluno-curso repetido lança DuplicateException e não cria segunda matrícula")
    void duplicatePairRejected() {
        saveAndReload(newEnrollment(FREE_STUDENT, 2, true));

        assertThrows(DuplicateException.class, () -> dao.save(newEnrollment(FREE_STUDENT, 2, false)));

        assertEquals(1, scalar("SELECT COUNT(*) FROM enrollments WHERE student_id = " + FREE_STUDENT));
    }

    @Test
    @DisplayName("MATR-07 existsByStudentAndCourse acusa o par existente e a edição da própria matrícula não conflita")
    void existsIgnoresOwnEnrollment() {
        Enrollment e = saveAndReload(newEnrollment(FREE_STUDENT, 2, true));

        assertTrue(dao.existsByStudentAndCourse(FREE_STUDENT, 2, null));
        assertFalse(dao.existsByStudentAndCourse(FREE_STUDENT, 2, e.getId()));
        assertFalse(dao.existsByStudentAndCourse(FREE_STUDENT, 3, null));

        e.setProgress(55);
        assertTrue(dao.update(e));
        assertEquals(55, dao.findById(e.getId()).getProgress());
    }

    @Test
    @DisplayName("ARQ-06 delete remove a matrícula")
    void delete() {
        Enrollment e = saveAndReload(newEnrollment(FREE_STUDENT, 2, true));

        assertTrue(dao.delete(e));

        assertNull(dao.findById(e.getId()));
    }

    @Test
    @DisplayName("PAG-01 findPage devolve 10 linhas por página, em ordem crescente de id, e a 11ª fica na página 2")
    void pag01PageOfTen() {
        assertTrue(dao.count() > 10);

        List<Enrollment> first = dao.findPage(0, 10);
        List<Enrollment> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(scalar("SELECT MIN(id) FROM enrollments"), first.get(0).getId());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertEquals(dao.count() - 10, second.size());
        assertEquals(scalar("SELECT id FROM enrollments ORDER BY id OFFSET 10 LIMIT 1"), second.get(0).getId());
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 findPage ordena por id crescente com LIMIT/OFFSET, nomes de aluno e curso; count devolve o total")
    void paging() {
        assertEquals(13, dao.count());

        List<Enrollment> first = dao.findPage(0, 10);
        List<Enrollment> second = dao.findPage(10, 10);

        assertEquals(10, first.size());
        assertEquals(3, second.size());
        assertEquals("Luiza Carvalho", first.get(0).getStudent().getName());
        assertEquals("Java Básico", first.get(0).getCourse().getTitle());
        for (int i = 1; i < first.size(); i++) {
            assertTrue(first.get(i).getId() > first.get(i - 1).getId());
        }
        assertTrue(second.get(0).getId() > first.get(9).getId());
    }

    @Test
    @DisplayName("ARQ-06 listAll devolve todas as matrículas")
    void listAll() {
        assertEquals(13, dao.listAll().size());
    }
}
