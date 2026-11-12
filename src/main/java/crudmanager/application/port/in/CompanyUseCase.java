package crudmanager.application.port.in;

import crudmanager.domain.Page;
import crudmanager.domain.model.Company;

/** Porta de entrada dos casos de uso de Company. */
public interface CompanyUseCase {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Company> list(int page);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    Company find(int id);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void create(Company entity);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void update(Company entity);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    void delete(int id);
}
