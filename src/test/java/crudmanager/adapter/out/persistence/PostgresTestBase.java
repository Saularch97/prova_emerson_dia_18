package crudmanager.adapter.out.persistence;

import java.util.Map;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Base dos ITs: um único container postgres:16 por JVM, inicializado com script-sql.sql (o mesmo mecanismo do
 * docker-compose). Testes que alteram dados devem limpar o que criam para não quebrar contagens dos demais.
 */
public abstract class PostgresTestBase {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withCopyFileToContainer(
                    MountableFile.forHostPath("script-sql.sql"), "/docker-entrypoint-initdb.d/script-sql.sql");

    static {
        POSTGRES.start();
    }

    protected static JdbcConnectionFactory connectionFactory() {
        return new JdbcConnectionFactory(Map.of(
                "DB_URL", POSTGRES.getJdbcUrl(),
                "DB_USER", POSTGRES.getUsername(),
                "DB_PASSWORD", POSTGRES.getPassword()));
    }
}
