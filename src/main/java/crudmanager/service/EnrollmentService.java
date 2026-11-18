package crudmanager.service;

import crudmanager.model.Enrollment;
import crudmanager.model.Page;

/** Contrato da camada de serviço de Enrollment. */
public interface EnrollmentService {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Enrollment> list(int page);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    Enrollment find(int id);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void create(Enrollment entity);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void update(Enrollment entity);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    void delete(int id);
}
