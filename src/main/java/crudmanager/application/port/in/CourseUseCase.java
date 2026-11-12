package crudmanager.application.port.in;

import crudmanager.domain.Page;
import crudmanager.domain.model.Course;

/** Porta de entrada dos casos de uso de Course. */
public interface CourseUseCase {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Course> list(int page);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    Course find(int id);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void create(Course entity);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void update(Course entity);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    void delete(int id);
}
