package crudmanager.application.port.out;

import crudmanager.domain.model.Post;
import java.util.List;

/** Porta de saída de persistência de Post. findById devolve null quando não existe. */
public interface PostDAO {
    boolean save(Post entity);

    boolean update(Post entity);

    boolean delete(Post entity);

    Post findById(int id);

    List<Post> listAll();

    List<Post> findPage(int offset, int limit);

    long count();
}
