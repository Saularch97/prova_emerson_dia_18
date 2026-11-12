package crudmanager.adapter.in.web;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import crudmanager.adapter.out.persistence.JdbcConnectionFactory;
import crudmanager.application.port.in.AuthUseCase;
import crudmanager.application.port.in.CompanyUseCase;
import crudmanager.application.port.in.CourseUseCase;
import crudmanager.application.port.in.EnrollmentUseCase;
import crudmanager.application.port.in.PostUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.service.AuthService;
import crudmanager.application.service.CompanyService;
import crudmanager.application.service.CourseService;
import crudmanager.application.service.EnrollmentService;
import crudmanager.application.service.PostService;
import crudmanager.application.service.UserService;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UseCasesTest {

    private final UseCases useCases = new UseCases(new JdbcConnectionFactory(
            Map.of("DB_URL", "jdbc:postgresql://127.0.0.1:1/inexistente", "DB_USER", "u", "DB_PASSWORD", "p")));

    @Test
    @DisplayName("ARQ-04 UseCases monta os seis serviços sem abrir conexão e reutiliza as instâncias")
    void assemblesServices() {
        assertInstanceOf(UserService.class, useCases.users());
        assertInstanceOf(PostService.class, useCases.posts());
        assertInstanceOf(CompanyService.class, useCases.companies());
        assertInstanceOf(CourseService.class, useCases.courses());
        assertInstanceOf(EnrollmentService.class, useCases.enrollments());
        assertInstanceOf(AuthService.class, useCases.auth());
        assertSame(useCases.users(), useCases.get(UserUseCase.class));
        assertSame(useCases.posts(), useCases.get(PostUseCase.class));
        assertSame(useCases.companies(), useCases.get(CompanyUseCase.class));
        assertSame(useCases.courses(), useCases.get(CourseUseCase.class));
        assertSame(useCases.enrollments(), useCases.get(EnrollmentUseCase.class));
        assertSame(useCases.auth(), useCases.get(AuthUseCase.class));
    }
}
