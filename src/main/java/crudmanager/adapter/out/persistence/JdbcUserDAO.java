package crudmanager.adapter.out.persistence;

import crudmanager.application.port.out.UserDAO;
import crudmanager.domain.model.User;
import java.util.ArrayList;
import java.util.List;

public class JdbcUserDAO implements UserDAO {

    private static final String COLUMNS = "id, nome, sexo, email, password_hash";

    private final JdbcConnectionFactory factory;

    public JdbcUserDAO(JdbcConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean save(User user) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("INSERT INTO users (nome, sexo, email, password_hash) VALUES (?, ?, ?, ?)");
        db.setString(1, user.getName());
        db.setString(2, user.getGender());
        db.setString(3, user.getEmail());
        db.setString(4, user.getPasswordHash());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean update(User user) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement(
                "UPDATE users SET nome = ?, sexo = ?, email = ?, password_hash = COALESCE(?, password_hash) WHERE id = ?");
        db.setString(1, user.getName());
        db.setString(2, user.getGender());
        db.setString(3, user.getEmail());
        db.setString(4, user.getPasswordHash());
        db.setInt(5, user.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean delete(User user) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("DELETE FROM users WHERE id = ?");
        db.setInt(1, user.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean updatePasswordHash(int userId, String passwordHash) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("UPDATE users SET password_hash = ? WHERE id = ?");
        db.setString(1, passwordHash);
        db.setInt(2, userId);
        return db.executeUpdate() > 0;
    }

    @Override
    public User findById(int id) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE id = ?");
            db.setInt(1, id);
            db.executeQuery();
            return db.next() ? create(db) : null;
        } finally {
            db.close();
        }
    }

    @Override
    public User findByEmail(String email) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT " + COLUMNS + " FROM users WHERE email = ?");
            db.setString(1, email);
            db.executeQuery();
            return db.next() ? create(db) : null;
        } finally {
            db.close();
        }
    }

    @Override
    public List<User> listAll() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT " + COLUMNS + " FROM users ORDER BY nome, id");
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public List<User> findPage(int offset, int limit) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT " + COLUMNS + " FROM users ORDER BY id LIMIT ? OFFSET ?");
            db.setInt(1, limit);
            db.setInt(2, offset);
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public long count() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT COUNT(*) AS total FROM users");
            db.executeQuery();
            db.next();
            return db.getLong("total");
        } finally {
            db.close();
        }
    }

    private static List<User> readAll(DBHandler db) {
        List<User> users = new ArrayList<>();
        while (db.next()) {
            users.add(create(db));
        }
        return users;
    }

    private static User create(DBHandler db) {
        User user = new User(db.getInt("id"));
        user.setName(db.getString("nome"));
        user.setGender(db.getString("sexo"));
        user.setEmail(db.getString("email"));
        user.setPasswordHash(db.getString("password_hash"));
        return user;
    }
}
