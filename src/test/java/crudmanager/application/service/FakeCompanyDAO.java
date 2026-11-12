package crudmanager.application.service;

import crudmanager.application.port.out.CompanyDAO;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.model.Company;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** DAO em memória, ordenado por id. */
class FakeCompanyDAO implements CompanyDAO {
    final Map<Integer, Company> store = new TreeMap<>();
    boolean deleteBlocked;
    int nextId = 1000;

    @Override
    public boolean save(Company entity) {
        store.put(nextId++, entity);
        return true;
    }

    @Override
    public boolean update(Company entity) {
        store.put(entity.getId(), entity);
        return true;
    }

    @Override
    public boolean delete(Company entity) {
        if (deleteBlocked) {
            throw new RelatedDataException("fk");
        }
        return store.remove(entity.getId()) != null;
    }

    @Override
    public Company findById(int id) {
        return store.get(id);
    }

    @Override
    public List<Company> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Company> findPage(int offset, int limit) {
        return store.values().stream().skip(offset).limit(limit).toList();
    }

    @Override
    public long count() {
        return store.size();
    }
}
