package crudmanager.adapter.out.persistence;

import crudmanager.application.port.out.EnrollmentDAO;
import crudmanager.domain.model.Course;
import crudmanager.domain.model.Enrollment;
import crudmanager.domain.model.User;
import java.util.ArrayList;
import java.util.List;

public class JdbcEnrollmentDAO implements EnrollmentDAO {

    private static final String SELECT = "SELECT e.id, e.student_id, u.nome AS student_name, e.course_id, "
            + "c.title AS course_title, e.enrolled_on, e.status, e.progress, e.paid "
            + "FROM enrollments e INNER JOIN users u ON u.id = e.student_id "
            + "INNER JOIN courses c ON c.id = e.course_id ";

    private final JdbcConnectionFactory factory;

    public JdbcEnrollmentDAO(JdbcConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean save(Enrollment enrollment) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("INSERT INTO enrollments (student_id, course_id, enrolled_on, status, progress, paid) "
                + "VALUES (?, ?, ?, ?, ?, ?)");
        bindFields(db, enrollment);
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean update(Enrollment enrollment) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("UPDATE enrollments SET student_id = ?, course_id = ?, enrolled_on = ?, status = ?, "
                + "progress = ?, paid = ? WHERE id = ?");
        bindFields(db, enrollment);
        db.setInt(7, enrollment.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean delete(Enrollment enrollment) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("DELETE FROM enrollments WHERE id = ?");
        db.setInt(1, enrollment.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public Enrollment findById(int id) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "WHERE e.id = ?");
            db.setInt(1, id);
            db.executeQuery();
            return db.next() ? create(db) : null;
        } finally {
            db.close();
        }
    }

    @Override
    public List<Enrollment> listAll() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY e.id");
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public List<Enrollment> findPage(int offset, int limit) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY e.id LIMIT ? OFFSET ?");
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
            db.prepareStatement("SELECT COUNT(*) AS total FROM enrollments");
            db.executeQuery();
            db.next();
            return db.getLong("total");
        } finally {
            db.close();
        }
    }

    @Override
    public boolean existsByStudentAndCourse(int studentId, int courseId, Integer ignoreId) {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement("SELECT COUNT(*) AS total FROM enrollments "
                    + "WHERE student_id = ? AND course_id = ? AND (?::int IS NULL OR id <> ?::int)");
            db.setInt(1, studentId);
            db.setInt(2, courseId);
            if (ignoreId == null) {
                db.setNull(3, java.sql.Types.INTEGER);
                db.setNull(4, java.sql.Types.INTEGER);
            } else {
                db.setInt(3, ignoreId);
                db.setInt(4, ignoreId);
            }
            db.executeQuery();
            db.next();
            return db.getLong("total") > 0;
        } finally {
            db.close();
        }
    }

    private static void bindFields(DBHandler db, Enrollment enrollment) {
        db.setInt(1, enrollment.getStudent().getId());
        db.setInt(2, enrollment.getCourse().getId());
        db.setDate(3, enrollment.getEnrolledOn());
        db.setString(4, enrollment.getStatus().name());
        db.setInt(5, enrollment.getProgress());
        db.setBoolean(6, enrollment.isPaid());
    }

    private static List<Enrollment> readAll(DBHandler db) {
        List<Enrollment> enrollments = new ArrayList<>();
        while (db.next()) {
            enrollments.add(create(db));
        }
        return enrollments;
    }

    private static Enrollment create(DBHandler db) {
        Enrollment enrollment = new Enrollment(db.getInt("id"));
        User student = new User(db.getInt("student_id"));
        student.setName(db.getString("student_name"));
        enrollment.setStudent(student);
        Course course = new Course(db.getInt("course_id"));
        course.setTitle(db.getString("course_title"));
        enrollment.setCourse(course);
        enrollment.setEnrolledOn(db.getDate("enrolled_on"));
        enrollment.setStatus(Enrollment.Status.valueOf(db.getString("status")));
        enrollment.setProgress(db.getInt("progress"));
        enrollment.setPaid(db.getBoolean("paid"));
        return enrollment;
    }
}
