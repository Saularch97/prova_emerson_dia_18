package crudmanager.application.service;

import crudmanager.application.port.in.EnrollmentUseCase;
import crudmanager.application.port.out.EnrollmentDAO;
import crudmanager.domain.Page;
import crudmanager.domain.error.DuplicateException;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.Enrollment;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnrollmentService implements EnrollmentUseCase {

    private final EnrollmentDAO enrollments;

    public EnrollmentService(EnrollmentDAO enrollments) {
        this.enrollments = enrollments;
    }

    @Override
    public Page<Enrollment> list(int page) {
        long total = enrollments.count();
        int current = Pagination.clamp(page, total);
        List<Enrollment> items = enrollments.findPage(Pagination.offset(current), Pagination.PAGE_SIZE);
        return Page.of(items, current, Pagination.PAGE_SIZE, total);
    }

    @Override
    public Enrollment find(int id) {
        Enrollment enrollment = enrollments.findById(id);
        if (enrollment == null) {
            throw new NotFoundException("Matrícula não encontrada.");
        }
        return enrollment;
    }

    @Override
    public void create(Enrollment enrollment) {
        validate(enrollment);
        rejectDuplicate(enrollment, null);
        enrollments.save(enrollment);
    }

    @Override
    public void update(Enrollment enrollment) {
        find(enrollment.getId());
        validate(enrollment);
        rejectDuplicate(enrollment, enrollment.getId());
        enrollments.update(enrollment);
    }

    @Override
    public void delete(int id) {
        enrollments.delete(find(id));
    }

    private void rejectDuplicate(Enrollment enrollment, Integer ignoreId) {
        if (enrollments.existsByStudentAndCourse(
                enrollment.getStudent().getId(), enrollment.getCourse().getId(), ignoreId)) {
            throw new DuplicateException("Este aluno já está matriculado neste curso.");
        }
    }

    private static void validate(Enrollment enrollment) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (enrollment.getStudent() == null || enrollment.getStudent().getId() <= 0) {
            errors.put("student", "Informe o aluno.");
        }
        if (enrollment.getCourse() == null || enrollment.getCourse().getId() <= 0) {
            errors.put("course", "Informe o curso.");
        }
        if (enrollment.getEnrolledOn() == null) {
            errors.put("enrolledOn", "Informe a data da matrícula.");
        }
        if (enrollment.getStatus() == null) {
            errors.put("status", "Informe a situação.");
        }
        if (enrollment.getProgress() < 0 || enrollment.getProgress() > 100) {
            errors.put("progress", "O progresso deve estar entre 0 e 100.");
        } else if (enrollment.getStatus() == Enrollment.Status.CONCLUIDA && enrollment.getProgress() < 100) {
            errors.put("progress", "Matrícula concluída exige progresso 100.");
        }
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
