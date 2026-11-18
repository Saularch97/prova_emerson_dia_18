package crudmanager.service;

import crudmanager.model.Page;
import crudmanager.model.User;
import java.util.List;

public interface UserService {
    Page<User> list(int page);

    List<User> listAll();

    User find(int id);

    void create(User user, String password);

    void update(User user, String password);

    void delete(int id);
}
