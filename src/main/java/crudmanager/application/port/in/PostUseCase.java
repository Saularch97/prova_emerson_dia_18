package crudmanager.application.port.in;

import crudmanager.domain.Page;
import crudmanager.domain.model.Post;

/** Porta de entrada dos casos de uso de Post. */
public interface PostUseCase {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<Post> list(int page);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    Post find(int id);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void create(Post entity);

    /** @throws crudmanager.domain.error.ValidationException se os dados são inválidos */
    void update(Post entity);

    /** @throws crudmanager.domain.error.NotFoundException se o id não existe */
    void delete(int id);
}
