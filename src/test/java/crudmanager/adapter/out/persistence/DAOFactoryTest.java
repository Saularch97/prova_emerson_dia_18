package crudmanager.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import crudmanager.application.port.out.CompanyDAO;
import crudmanager.application.port.out.CourseDAO;
import crudmanager.application.port.out.EnrollmentDAO;
import crudmanager.application.port.out.PostDAO;
import crudmanager.application.port.out.UserDAO;
import crudmanager.domain.error.ModelException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DAOFactoryTest {

    /** Porta fechada: qualquer tentativa de conectar falharia, então a criação prova que não conecta. */
    private final DAOFactory factory = new DAOFactory(new JdbcConnectionFactory(
            Map.of("DB_URL", "jdbc:postgresql://127.0.0.1:1/inexistente", "DB_USER", "u", "DB_PASSWORD", "p")));

    @Test
    @DisplayName("ARQ-04 createDAO devolve a implementação JDBC de cada uma das cinco portas, sem abrir conexão")
    void createsJdbcImplementations() {
        assertInstanceOf(JdbcUserDAO.class, factory.<UserDAO>createDAO(UserDAO.class));
        assertInstanceOf(JdbcPostDAO.class, factory.<PostDAO>createDAO(PostDAO.class));
        assertInstanceOf(JdbcCompanyDAO.class, factory.<CompanyDAO>createDAO(CompanyDAO.class));
        assertInstanceOf(JdbcCourseDAO.class, factory.<CourseDAO>createDAO(CourseDAO.class));
        assertInstanceOf(JdbcEnrollmentDAO.class, factory.<EnrollmentDAO>createDAO(EnrollmentDAO.class));
    }

    @Test
    @DisplayName("ARQ-04 createDAO de interface desconhecida lança ModelException")
    void unknownInterface() {
        assertThrows(ModelException.class, () -> factory.createDAO(String.class));
    }
}
