package crudmanager.dao;

import crudmanager.model.Enrollment;
import java.util.List;

/** Contrato de acesso a dados de Enrollment. findById devolve null quando não existe. */
public interface EnrollmentDAO {
    boolean save(Enrollment entity);

    boolean update(Enrollment entity);

    boolean delete(Enrollment entity);

    Enrollment findById(int id);

    List<Enrollment> listAll();

    List<Enrollment> findPage(int offset, int limit);

    long count();

    /** ignoreId exclui uma matrícula da checagem (edição); null na criação. */
    boolean existsByStudentAndCourse(int studentId, int courseId, Integer ignoreId);
}
