package crudmanager.service;

import crudmanager.model.Enrollment;
import crudmanager.model.Page;

public interface EnrollmentService {
    Page<Enrollment> list(int page);

    Enrollment find(int id);

    void create(Enrollment entity);

    void update(Enrollment entity);

    void delete(int id);
}
