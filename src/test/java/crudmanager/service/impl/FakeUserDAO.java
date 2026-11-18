package crudmanager.service.impl;

import crudmanager.dao.UserDAO;
import crudmanager.exception.RelatedDataException;
import crudmanager.model.User;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** DAO em memória; ids dados pelo teste, ordem por id. */
class FakeUserDAO implements UserDAO {
    final Map<Integer, User> store = new TreeMap<>();
    boolean deleteBlocked;
    int nextId = 1000;

    User add(int id, String name, String email, String hash) {
        User u = new User(id);
        u.setName(name);
        u.setEmail(email);
        u.setGender("M");
        u.setPasswordHash(hash);
        store.put(id, u);
        return u;
    }

    @Override
    public boolean save(User entity) {
        User copy = new User(nextId++);
        copy.setName(entity.getName());
        copy.setGender(entity.getGender());
        copy.setEmail(entity.getEmail());
        copy.setPasswordHash(entity.getPasswordHash());
        store.put(copy.getId(), copy);
        return true;
    }

    @Override
    public boolean update(User entity) {
        store.put(entity.getId(), entity);
        return true;
    }

    @Override
    public boolean delete(User entity) {
        if (deleteBlocked) {
            throw new RelatedDataException("fk");
        }
        return store.remove(entity.getId()) != null;
    }

    @Override
    public User findById(int id) {
        return store.get(id);
    }

    @Override
    public List<User> listAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<User> findPage(int offset, int limit) {
        return store.values().stream().skip(offset).limit(limit).toList();
    }

    @Override
    public long count() {
        return store.size();
    }

    @Override
    public User findByEmail(String email) {
        return store.values().stream().filter(u -> u.getEmail().equals(email)).findFirst().orElse(null);
    }

    @Override
    public boolean updatePasswordHash(int userId, String passwordHash) {
        store.get(userId).setPasswordHash(passwordHash);
        return true;
    }
}
