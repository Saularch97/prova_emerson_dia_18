package crudmanager.service;

import crudmanager.model.Course;
import crudmanager.model.Page;
import java.util.List;

/** Contrato da camada de serviço de Course. */
public interface CourseService {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Course> list(int page);

    /** Todos os cursos, sem paginação, para preencher selects de formulários. */
    List<Course> listAll();

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    Course find(int id);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void create(Course entity);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void update(Course entity);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    void delete(int id);
}
