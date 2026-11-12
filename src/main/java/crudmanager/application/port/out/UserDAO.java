package crudmanager.application.port.out;

import crudmanager.domain.model.User;
import java.util.List;

/** Porta de saída de persistência de User. findById devolve null quando não existe. */
public interface UserDAO {
    boolean save(User entity);

    boolean update(User entity);

    boolean delete(User entity);

    User findById(int id);

    List<User> listAll();

    List<User> findPage(int offset, int limit);

    long count();

    User findByEmail(String email);

    boolean updatePasswordHash(int userId, String passwordHash);
}
