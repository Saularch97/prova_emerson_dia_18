package crudmanager.dao.jdbc;

import crudmanager.dao.CompanyDAO;
import crudmanager.dao.CourseDAO;
import crudmanager.dao.EnrollmentDAO;
import crudmanager.dao.PostDAO;
import crudmanager.dao.UserDAO;
import crudmanager.exception.ModelException;
import java.util.HashMap;
import java.util.Map;

/** Cria os DAOs JDBC por interface. Os DAOs só abrem conexão quando uma operação é executada. */
public class DAOFactory {

    private final Map<Class<?>, Object> daos = new HashMap<>();

    public DAOFactory(JdbcConnectionFactory connectionFactory) {
        daos.put(UserDAO.class, new JdbcUserDAO(connectionFactory));
        daos.put(PostDAO.class, new JdbcPostDAO(connectionFactory));
        daos.put(CompanyDAO.class, new JdbcCompanyDAO(connectionFactory));
        daos.put(CourseDAO.class, new JdbcCourseDAO(connectionFactory));
        daos.put(EnrollmentDAO.class, new JdbcEnrollmentDAO(connectionFactory));
    }

    @SuppressWarnings("unchecked")
    public <DAOInterface> DAOInterface createDAO(Class<?> entity) {
        Object dao = daos.get(entity);
        if (dao == null) {
            throw new ModelException("DAO não registrado para " + entity.getName());
        }
        return (DAOInterface) dao;
    }
}
