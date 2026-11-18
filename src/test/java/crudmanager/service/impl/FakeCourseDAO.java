package crudmanager.service.impl;

import crudmanager.dao.CourseDAO;
import crudmanager.exception.RelatedDataException;
import crudmanager.model.Course;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** DAO em memória, ordenado por id. */
class FakeCourseDAO implements CourseDAO {
    final Map<Integer, Course> store = new TreeMap<>();
    boolean deleteBlocked;
    int nextId = 1000;

    @Override
    public boolean save(Course entity) {
        store.put(nextId++, entity);
        return true;
    }

    @Override
    public boolean update(Course entity) {
        store.put(entity.getId(), entity);
        return true;
    }

    @Override
    public boolean delete(Course entity) {
        if (deleteBlocked) {
            throw new RelatedDataException("fk");
        }
        return store.remove(entity.getId()) != null;
    }

    @Override
    public Course findById(int id) {
        return store.get(id);
    }

    @Override
    public List<Course> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Course> findPage(int offset, int limit) {
        return store.values().stream().skip(offset).limit(limit).toList();
    }

    @Override
    public long count() {
        return store.size();
    }
}
