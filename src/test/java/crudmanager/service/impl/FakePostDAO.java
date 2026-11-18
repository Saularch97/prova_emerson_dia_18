package crudmanager.service.impl;

import crudmanager.dao.PostDAO;
import crudmanager.exception.RelatedDataException;
import crudmanager.model.Post;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class FakePostDAO implements PostDAO {
    final Map<Integer, Post> store = new TreeMap<>();
    boolean deleteBlocked;
    int nextId = 1000;

    @Override
    public boolean save(Post entity) {
        store.put(nextId++, entity);
        return true;
    }

    @Override
    public boolean update(Post entity) {
        store.put(entity.getId(), entity);
        return true;
    }

    @Override
    public boolean delete(Post entity) {
        if (deleteBlocked) {
            throw new RelatedDataException("fk");
        }
        return store.remove(entity.getId()) != null;
    }

    @Override
    public Post findById(int id) {
        return store.get(id);
    }

    @Override
    public List<Post> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Post> findPage(int offset, int limit) {
        return store.values().stream().skip(offset).limit(limit).toList();
    }

    @Override
    public long count() {
        return store.size();
    }
}
