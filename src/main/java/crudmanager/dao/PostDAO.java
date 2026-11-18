package crudmanager.dao;

import crudmanager.model.Post;
import java.util.List;

/** Contrato de acesso a dados de Post. findById devolve null quando não existe. */
public interface PostDAO {
    boolean save(Post entity);

    boolean update(Post entity);

    boolean delete(Post entity);

    Post findById(int id);

    List<Post> listAll();

    List<Post> findPage(int offset, int limit);

    long count();
}
