package crudmanager.service;

import crudmanager.model.Page;
import crudmanager.model.Post;

/** Contrato da camada de serviço de Post. */
public interface PostService {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Post> list(int page);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    Post find(int id);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void create(Post entity);

    /** @throws crudmanager.exception.ValidationException se os dados são inválidos */
    void update(Post entity);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    void delete(int id);
}
