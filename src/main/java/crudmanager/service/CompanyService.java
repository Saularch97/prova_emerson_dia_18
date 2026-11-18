package crudmanager.service;

import crudmanager.model.Company;
import crudmanager.model.Page;

/** Contrato da camada de serviço de Company. */
public interface CompanyService {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Company> list(int page);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    Company find(int id);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void create(Company entity);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void update(Company entity);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    void delete(int id);
}
