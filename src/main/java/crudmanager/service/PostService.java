package crudmanager.service;

import crudmanager.model.Page;
import crudmanager.model.Post;

public interface PostService {
    Page<Post> list(int page);

    Post find(int id);

    void create(Post entity);

    void update(Post entity);

    void delete(int id);
}
