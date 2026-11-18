package crudmanager.service;

import crudmanager.model.Page;
import crudmanager.model.User;
import java.util.List;

/** Contrato da camada de serviço de User. */
public interface UserService {
    /** Página solicitada, já normalizada (acima da última vira a última). */
    Page<User> list(int page);

    /** Todos os usuários, sem paginação, para preencher selects de formulários. */
    List<User> listAll();

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    User find(int id);

    /** A senha em texto é obrigatória; só o hash é gravado. */
    void create(User user, String password);

    /** Senha em branco mantém a atual. */
    void update(User user, String password);

    /** @throws crudmanager.exception.NotFoundException se o id não existe */
    void delete(int id);
}
