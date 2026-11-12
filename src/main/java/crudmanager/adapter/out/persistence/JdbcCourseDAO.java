package crudmanager.adapter.out.persistence;

import crudmanager.application.port.out.CourseDAO;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.User;
import java.util.ArrayList;
import java.util.List;

public class JdbcCourseDAO implements CourseDAO {

    private static final String SELECT = "SELECT c.id, c.title, c.description, c.price, c.start_date, c.level, "
            + "c.active, c.instructor_id, u.nome AS instructor_name "
            + "FROM courses c INNER JOIN users u ON u.id = c.instructor_id ";

    private final JdbcConnectionFactory factory;

    public JdbcCourseDAO(JdbcConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean save(Course course) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("INSERT INTO courses (title, description, price, start_date, level, active, "
                + "instructor_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
        bindFields(db, course);
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean update(Course course) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("UPDATE courses SET title = ?, description = ?, price = ?, start_date = ?, level = ?, "
                + "active = ?, instructor_id = ? WHERE id = ?");
        bindFields(db, course);
        db.setInt(8, course.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean delete(Course course) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("DELETE FROM courses WHERE id = ?");
        db.setInt(1, course.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public Course findById(int id) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "WHERE c.id = ?");
            db.setInt(1, id);
            db.executeQuery();
            return db.next() ? create(db) : null;
        } finally {
            db.close();
        }
    }

    @Override
    public List<Course> listAll() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY c.title, c.id");
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public List<Course> findPage(int offset, int limit) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY c.id LIMIT ? OFFSET ?");
            db.setInt(1, limit);
            db.setInt(2, offset);
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public long count() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT COUNT(*) AS total FROM courses");
            db.executeQuery();
            db.next();
            return db.getLong("total");
        } finally {
            db.close();
        }
    }

    private static void bindFields(DBHandler db, Course course) {
        db.setString(1, course.getTitle());
        db.setString(2, course.getDescription());
        db.setDecimal(3, course.getPrice());
        db.setDate(4, course.getStartDate());
        db.setString(5, course.getLevel().name());
        db.setBoolean(6, course.isActive());
        db.setInt(7, course.getInstructor().getId());
    }

    private static List<Course> readAll(DBHandler db) {
        List<Course> courses = new ArrayList<>();
        while (db.next()) {
            courses.add(create(db));
        }
        return courses;
    }

    private static Course create(DBHandler db) {
        Course course = new Course(db.getInt("id"));
        course.setTitle(db.getString("title"));
        course.setDescription(db.getString("description"));
        course.setPrice(db.getDecimal("price"));
        course.setStartDate(db.getDate("start_date"));
        course.setLevel(Course.Level.valueOf(db.getString("level")));
        course.setActive(db.getBoolean("active"));
        User instructor = new User(db.getInt("instructor_id"));
        instructor.setName(db.getString("instructor_name"));
        course.setInstructor(instructor);
        return course;
    }
}
