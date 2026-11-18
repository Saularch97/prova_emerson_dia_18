package crudmanager.security;

public interface PasswordHasher {
    String hash(String raw);

    boolean matches(String raw, String stored);
}
