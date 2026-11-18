package crudmanager.dao;

import crudmanager.model.Company;
import java.util.List;

/** Contrato de acesso a dados de Company. findById devolve null quando não existe. */
public interface CompanyDAO {
    boolean save(Company entity);

    boolean update(Company entity);

    boolean delete(Company entity);

    Company findById(int id);

    List<Company> listAll();

    List<Company> findPage(int offset, int limit);

    long count();
}
