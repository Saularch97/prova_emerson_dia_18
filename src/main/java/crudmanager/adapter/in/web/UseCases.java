package crudmanager.adapter.in.web;

import crudmanager.adapter.out.persistence.DAOFactory;
import crudmanager.adapter.out.persistence.JdbcConnectionFactory;
import crudmanager.adapter.out.security.Pbkdf2PasswordHasher;
import crudmanager.application.port.in.AuthUseCase;
import crudmanager.application.port.in.CompanyUseCase;
import crudmanager.application.port.in.CourseUseCase;
import crudmanager.application.port.in.EnrollmentUseCase;
import crudmanager.application.port.in.PostUseCase;
import crudmanager.application.port.in.UserUseCase;
import crudmanager.application.port.out.CompanyDAO;
import crudmanager.application.port.out.CourseDAO;
import crudmanager.application.port.out.EnrollmentDAO;
import crudmanager.application.port.out.PasswordHasher;
import crudmanager.application.port.out.PostDAO;
import crudmanager.application.port.out.UserDAO;
import crudmanager.application.service.AuthService;
import crudmanager.application.service.CompanyService;
import crudmanager.application.service.CourseService;
import crudmanager.application.service.EnrollmentService;
import crudmanager.application.service.PostService;
import crudmanager.application.service.UserService;
import java.util.HashMap;
import java.util.Map;

/** Composition root: monta serviços, DAOs e hasher. {@link #instance()} lê o ambiente na primeira chamada. */
public class UseCases {

    private static UseCases instance;

    private final Map<Class<?>, Object> useCases = new HashMap<>();

    public UseCases(JdbcConnectionFactory connectionFactory) {
        DAOFactory daos = new DAOFactory(connectionFactory);
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        UserDAO userDAO = daos.createDAO(UserDAO.class);
        PostDAO postDAO = daos.createDAO(PostDAO.class);
        CompanyDAO companyDAO = daos.createDAO(CompanyDAO.class);
        CourseDAO courseDAO = daos.createDAO(CourseDAO.class);
        EnrollmentDAO enrollmentDAO = daos.createDAO(EnrollmentDAO.class);
        useCases.put(UserUseCase.class, new UserService(userDAO, hasher));
        useCases.put(PostUseCase.class, new PostService(postDAO));
        useCases.put(CompanyUseCase.class, new CompanyService(companyDAO));
        useCases.put(CourseUseCase.class, new CourseService(courseDAO));
        useCases.put(EnrollmentUseCase.class, new EnrollmentService(enrollmentDAO));
        useCases.put(AuthUseCase.class, new AuthService(userDAO, hasher));
    }

    /** Instância única criada sob demanda a partir de DB_URL, DB_USER e DB_PASSWORD. */
    public static synchronized UseCases instance() {
        if (instance == null) {
            instance = new UseCases(JdbcConnectionFactory.fromEnvironment());
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> useCase) {
        return (T) useCases.get(useCase);
    }

    public UserUseCase users() {
        return get(UserUseCase.class);
    }

    public PostUseCase posts() {
        return get(PostUseCase.class);
    }

    public CompanyUseCase companies() {
        return get(CompanyUseCase.class);
    }

    public CourseUseCase courses() {
        return get(CourseUseCase.class);
    }

    public EnrollmentUseCase enrollments() {
        return get(EnrollmentUseCase.class);
    }

    public AuthUseCase auth() {
        return get(AuthUseCase.class);
    }
}
