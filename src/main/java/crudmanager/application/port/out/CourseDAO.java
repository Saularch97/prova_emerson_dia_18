package crudmanager.application.port.out;

import crudmanager.domain.model.Course;
import java.util.List;

/** Porta de saída de persistência de Course. findById devolve null quando não existe. */
public interface CourseDAO {
    boolean save(Course entity);

    boolean update(Course entity);

    boolean delete(Course entity);

    Course findById(int id);

    List<Course> listAll();

    List<Course> findPage(int offset, int limit);

    long count();
}
