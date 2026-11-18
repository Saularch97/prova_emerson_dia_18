package crudmanager.dao;

import crudmanager.model.Post;
import java.util.List;

public interface PostDAO {
    boolean save(Post entity);

    boolean update(Post entity);

    boolean delete(Post entity);

    Post findById(int id);

    List<Post> listAll();

    List<Post> findPage(int offset, int limit);

    long count();
}
