package crudmanager.service.impl;

import crudmanager.security.PasswordHasher;

/** Hasher reversível só para teste: "hash(" + senha + ")". */
class FakePasswordHasher implements PasswordHasher {
    @Override
    public String hash(String raw) {
        return "hash(" + raw + ")";
    }

    @Override
    public boolean matches(String raw, String stored) {
        return hash(raw).equals(stored);
    }
}
