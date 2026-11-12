package crudmanager.adapter.out.persistence;

import crudmanager.domain.error.ModelException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/** Abre conexões PostgreSQL a partir de DB_URL, DB_USER e DB_PASSWORD (sem valores padrão). */
public class JdbcConnectionFactory {

    private final String url;
    private final String user;
    private final String password;

    public JdbcConnectionFactory(Map<String, String> env) {
        this.url = require(env, "DB_URL");
        this.user = require(env, "DB_USER");
        this.password = require(env, "DB_PASSWORD");
    }

    public static JdbcConnectionFactory fromEnvironment() {
        return new JdbcConnectionFactory(System.getenv());
    }

    private static String require(Map<String, String> env, String name) {
        String value = env.get(name);
        if (value == null || value.isBlank()) {
            throw new ModelException("Variável de ambiente " + name + " não definida.");
        }
        return value;
    }

    public String getUrl() {
        return url;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public Connection getConnection() {
        try {
            // No Tomcat o DriverManager não enxerga o driver de WEB-INF/lib sem o registro explícito.
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, user, password);
        } catch (ClassNotFoundException | SQLException e) {
            throw new ModelException("Falha na conexão com o BD", e);
        }
    }
}
