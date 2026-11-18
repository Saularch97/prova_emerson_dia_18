package crudmanager.dao;

import crudmanager.model.Course;
import java.util.List;

/** Contrato de acesso a dados de Course. findById devolve null quando não existe. */
public interface CourseDAO {
    boolean save(Course entity);

    boolean update(Course entity);

    boolean delete(Course entity);

    Course findById(int id);

    List<Course> listAll();

    List<Course> findPage(int offset, int limit);

    long count();
}
