package crudmanager.config;

import crudmanager.dao.CompanyDAO;
import crudmanager.dao.CourseDAO;
import crudmanager.dao.EnrollmentDAO;
import crudmanager.dao.PostDAO;
import crudmanager.dao.UserDAO;
import crudmanager.dao.jdbc.DAOFactory;
import crudmanager.dao.jdbc.JdbcConnectionFactory;
import crudmanager.security.PasswordHasher;
import crudmanager.security.Pbkdf2PasswordHasher;
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
import java.util.HashMap;
import java.util.Map;

/** Composition root: monta serviços, DAOs e hasher. {@link #instance()} lê o ambiente na primeira chamada. */
public class ServiceLocator {

    private static ServiceLocator instance;

    private final Map<Class<?>, Object> useCases = new HashMap<>();

    public ServiceLocator(JdbcConnectionFactory connectionFactory) {
        DAOFactory daos = new DAOFactory(connectionFactory);
        PasswordHasher hasher = new Pbkdf2PasswordHasher();
        UserDAO userDAO = daos.createDAO(UserDAO.class);
        PostDAO postDAO = daos.createDAO(PostDAO.class);
        CompanyDAO companyDAO = daos.createDAO(CompanyDAO.class);
        CourseDAO courseDAO = daos.createDAO(CourseDAO.class);
        EnrollmentDAO enrollmentDAO = daos.createDAO(EnrollmentDAO.class);
        useCases.put(UserService.class, new UserServiceImpl(userDAO, hasher));
        useCases.put(PostService.class, new PostServiceImpl(postDAO));
        useCases.put(CompanyService.class, new CompanyServiceImpl(companyDAO));
        useCases.put(CourseService.class, new CourseServiceImpl(courseDAO));
        useCases.put(EnrollmentService.class, new EnrollmentServiceImpl(enrollmentDAO));
        useCases.put(AuthService.class, new AuthServiceImpl(userDAO, hasher));
    }

    /** Instância única criada sob demanda a partir de DB_URL, DB_USER e DB_PASSWORD. */
    public static synchronized ServiceLocator instance() {
        if (instance == null) {
            instance = new ServiceLocator(JdbcConnectionFactory.fromEnvironment());
        }
        return instance;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> useCase) {
        return (T) useCases.get(useCase);
    }

    public UserService users() {
        return get(UserService.class);
    }

    public PostService posts() {
        return get(PostService.class);
    }

    public CompanyService companies() {
        return get(CompanyService.class);
    }

    public CourseService courses() {
        return get(CourseService.class);
    }

    public EnrollmentService enrollments() {
        return get(EnrollmentService.class);
    }

    public AuthService auth() {
        return get(AuthService.class);
    }
}
