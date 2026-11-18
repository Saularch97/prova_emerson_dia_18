package crudmanager.dao.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

    /** Executa um comando de limpeza/preparo direto no banco. */
    protected static void execute(String sql) {
        try (Connection c = connectionFactory().getConnection(); Statement st = c.createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Executa uma consulta de uma linha e uma coluna numérica. */
    protected static long scalar(String sql) {
        try (Connection c = connectionFactory().getConnection();
                Statement st = c.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
