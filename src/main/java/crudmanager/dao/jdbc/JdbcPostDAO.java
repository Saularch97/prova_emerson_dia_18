package crudmanager.dao.jdbc;

import crudmanager.dao.PostDAO;
import crudmanager.model.Post;
import crudmanager.model.User;
import java.util.ArrayList;
import java.util.List;

public class JdbcPostDAO implements PostDAO {

    private static final String SELECT = "SELECT p.id, p.content, p.post_date, p.user_id, u.nome AS user_name "
            + "FROM posts p INNER JOIN users u ON u.id = p.user_id ";

    private final JdbcConnectionFactory factory;

    public JdbcPostDAO(JdbcConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean save(Post post) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("INSERT INTO posts (content, post_date, user_id) VALUES (?, CURRENT_DATE, ?)");
        db.setString(1, post.getContent());
        db.setInt(2, post.getUser().getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean update(Post post) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("UPDATE posts SET content = ?, user_id = ? WHERE id = ?");
        db.setString(1, post.getContent());
        db.setInt(2, post.getUser().getId());
        db.setInt(3, post.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean delete(Post post) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("DELETE FROM posts WHERE id = ?");
        db.setInt(1, post.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public Post findById(int id) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "WHERE p.id = ?");
            db.setInt(1, id);
            db.executeQuery();
            return db.next() ? create(db) : null;
        } finally {
            db.close();
        }
    }

    @Override
    public List<Post> listAll() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY p.id");
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public List<Post> findPage(int offset, int limit) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY p.id LIMIT ? OFFSET ?");
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
            db.prepareStatement("SELECT COUNT(*) AS total FROM posts");
            db.executeQuery();
            db.next();
            return db.getLong("total");
        } finally {
            db.close();
        }
    }

    private static List<Post> readAll(DBHandler db) {
        List<Post> posts = new ArrayList<>();
        while (db.next()) {
            posts.add(create(db));
        }
        return posts;
    }

    private static Post create(DBHandler db) {
        Post post = new Post(db.getInt("id"));
        post.setContent(db.getString("content"));
        post.setPostDate(db.getDate("post_date"));
        User user = new User(db.getInt("user_id"));
        user.setName(db.getString("user_name"));
        post.setUser(user);
        return post;
    }
}
