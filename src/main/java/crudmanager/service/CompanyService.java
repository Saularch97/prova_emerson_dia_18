package crudmanager.service;

import crudmanager.model.Company;
import crudmanager.model.Page;

public interface CompanyService {
    Page<Company> list(int page);

    Company find(int id);

    void create(Company entity);

    void update(Company entity);

    void delete(int id);
}
