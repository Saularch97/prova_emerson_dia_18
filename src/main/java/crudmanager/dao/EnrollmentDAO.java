package crudmanager.dao;

import crudmanager.model.Enrollment;
import java.util.List;

public interface EnrollmentDAO {
    boolean save(Enrollment entity);

    boolean update(Enrollment entity);

    boolean delete(Enrollment entity);

    Enrollment findById(int id);

    List<Enrollment> listAll();

    List<Enrollment> findPage(int offset, int limit);

    long count();

    boolean existsByStudentAndCourse(int studentId, int courseId, Integer ignoreId);
}
