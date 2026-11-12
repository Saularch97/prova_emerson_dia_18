package crudmanager.application.service;

import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.port.out.PasswordHasher;
import crudmanager.application.port.out.UserDAO;
import crudmanager.domain.Page;
import crudmanager.domain.error.NotFoundException;
import crudmanager.domain.error.ValidationException;
import crudmanager.domain.model.User;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserService implements UserUseCase {

    private static final int MAX_LENGTH = 150;

    private final UserDAO users;
    private final PasswordHasher hasher;

    public UserService(UserDAO users, PasswordHasher hasher) {
        this.users = users;
        this.hasher = hasher;
    }

    @Override
    public Page<User> list(int page) {
        long total = users.count();
        int current = Pagination.clamp(page, total);
        List<User> items = users.findPage(Pagination.offset(current), Pagination.PAGE_SIZE);
        return Page.of(items, current, Pagination.PAGE_SIZE, total);
    }

    @Override
    public List<User> listAll() {
        return users.listAll();
    }

    @Override
    public User find(int id) {
        User user = users.findById(id);
        if (user == null) {
            throw new NotFoundException("Usuário não encontrado.");
        }
        return user;
    }

    @Override
    public void create(User user, String password) {
        Map<String, String> errors = validate(user);
        if (password == null || password.isBlank()) {
            errors.put("password", "Informe a senha.");
        }
        throwIfAny(errors);
        user.setPasswordHash(hasher.hash(password));
        users.save(user);
    }

    @Override
    public void update(User user, String password) {
        User current = find(user.getId());
        throwIfAny(validate(user));
        if (password == null || password.isBlank()) {
            user.setPasswordHash(current.getPasswordHash());
        } else {
            user.setPasswordHash(hasher.hash(password));
        }
        users.update(user);
    }

    @Override
    public void delete(int id) {
        users.delete(find(id));
    }

    private static Map<String, String> validate(User user) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (isBlank(user.getName())) {
            errors.put("name", "Informe o nome.");
        } else if (user.getName().length() > MAX_LENGTH) {
            errors.put("name", "O nome deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }
        if (!"M".equals(user.getGender()) && !"F".equals(user.getGender())) {
            errors.put("gender", "Informe o sexo (M ou F).");
        }
        if (isBlank(user.getEmail())) {
            errors.put("email", "Informe o e-mail.");
        } else if (user.getEmail().length() > MAX_LENGTH) {
            errors.put("email", "O e-mail deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }
        return errors;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static void throwIfAny(Map<String, String> errors) {
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }
}
