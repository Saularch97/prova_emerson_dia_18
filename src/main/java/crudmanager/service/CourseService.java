package crudmanager.service;

import crudmanager.model.Course;
import crudmanager.model.Page;
import java.util.List;

public interface CourseService {
    Page<Course> list(int page);

    List<Course> listAll();

    Course find(int id);

    void create(Course entity);

    void update(Course entity);

    void delete(int id);
}
