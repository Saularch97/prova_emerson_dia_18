package crudmanager.config;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import crudmanager.dao.jdbc.JdbcConnectionFactory;
import crudmanager.service.AuthService;
import crudmanager.service.CompanyService;
import crudmanager.service.CourseService;
import crudmanager.service.EnrollmentService;
import crudmanager.service.PostService;
import crudmanager.service.UserService;
import crudmanager.service.impl.AuthServiceImpl;
import crudmanager.service.impl.CompanyServiceImpl;
import crudmanager.service.impl.CourseServiceImpl;
import crudmanager.service.impl.EnrollmentServiceImpl;
import crudmanager.service.impl.PostServiceImpl;
import crudmanager.service.impl.UserServiceImpl;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ServiceLocatorTest {

    private final ServiceLocator useCases = new ServiceLocator(new JdbcConnectionFactory(
            Map.of("DB_URL", "jdbc:postgresql://127.0.0.1:1/inexistente", "DB_USER", "u", "DB_PASSWORD", "p")));

    @Test
    @DisplayName("ARQ-04 ServiceLocator monta os seis serviços sem abrir conexão e reutiliza as instâncias")
    void assemblesServices() {
        assertInstanceOf(UserServiceImpl.class, useCases.users());
        assertInstanceOf(PostServiceImpl.class, useCases.posts());
        assertInstanceOf(CompanyServiceImpl.class, useCases.companies());
        assertInstanceOf(CourseServiceImpl.class, useCases.courses());
        assertInstanceOf(EnrollmentServiceImpl.class, useCases.enrollments());
        assertInstanceOf(AuthServiceImpl.class, useCases.auth());
        assertSame(useCases.users(), useCases.get(UserService.class));
        assertSame(useCases.posts(), useCases.get(PostService.class));
        assertSame(useCases.companies(), useCases.get(CompanyService.class));
        assertSame(useCases.courses(), useCases.get(CourseService.class));
        assertSame(useCases.enrollments(), useCases.get(EnrollmentService.class));
        assertSame(useCases.auth(), useCases.get(AuthService.class));
    }
}
