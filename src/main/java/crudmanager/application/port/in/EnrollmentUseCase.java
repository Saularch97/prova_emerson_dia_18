package crudmanager.application.port.in;

import crudmanager.domain.Page;
import crudmanager.domain.model.Enrollment;

/** Porta de entrada dos casos de uso de Enrollment. */
public interface EnrollmentUseCase {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Enrollment> list(int page);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    Enrollment find(int id);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void create(Enrollment entity);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void update(Enrollment entity);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    void delete(int id);
}
