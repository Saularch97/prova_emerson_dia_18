package crudmanager.application.service;

import crudmanager.application.port.out.EnrollmentDAO;
import crudmanager.domain.error.RelatedDataException;
import crudmanager.domain.model.Enrollment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** DAO em memória, ordenado por id. */
class FakeEnrollmentDAO implements EnrollmentDAO {
    final Map<Integer, Enrollment> store = new TreeMap<>();
    boolean deleteBlocked;
    int nextId = 1000;
    Integer lastIgnoreId;
    int lastStudentId;
    int lastCourseId;
    boolean existsCalled;

    @Override
    public boolean existsByStudentAndCourse(int studentId, int courseId, Integer ignoreId) {
        existsCalled = true;
        lastStudentId = studentId;
        lastCourseId = courseId;
        lastIgnoreId = ignoreId;
        return store.entrySet().stream().anyMatch(e -> e.getValue().getStudent().getId() == studentId
                && e.getValue().getCourse().getId() == courseId
                && (ignoreId == null || e.getValue().getId() != ignoreId));
    }

    @Override
    public boolean save(Enrollment entity) {
        store.put(nextId++, entity);
        return true;
    }

    @Override
    public boolean update(Enrollment entity) {
        store.put(entity.getId(), entity);
        return true;
    }

    @Override
    public boolean delete(Enrollment entity) {
        if (deleteBlocked) {
            throw new RelatedDataException("fk");
        }
        return store.remove(entity.getId()) != null;
    }

    @Override
    public Enrollment findById(int id) {
        return store.get(id);
    }

    @Override
    public List<Enrollment> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Enrollment> findPage(int offset, int limit) {
        return store.values().stream().skip(offset).limit(limit).toList();
    }

    @Override
    public long count() {
        return store.size();
    }
}
