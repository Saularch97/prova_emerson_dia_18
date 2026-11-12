package crudmanager.adapter.out.persistence;

import crudmanager.application.port.out.CompanyDAO;
import crudmanager.domain.model.Company;
import crudmanager.domain.model.User;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class JdbcCompanyDAO implements CompanyDAO {

    private static final String SELECT = "SELECT c.id, c.name, c.role, c.start, c.\"end\", c.user_id, "
            + "u.nome AS user_name FROM companies c INNER JOIN users u ON u.id = c.user_id ";

    private final JdbcConnectionFactory factory;

    public JdbcCompanyDAO(JdbcConnectionFactory factory) {
        this.factory = factory;
    }

    @Override
    public boolean save(Company company) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("INSERT INTO companies (name, role, start, \"end\", user_id) VALUES (?, ?, ?, ?, ?)");
        db.setString(1, company.getName());
        db.setString(2, company.getRole());
        db.setDate(3, company.getStart());
        bindEnd(db, 4, company);
        db.setInt(5, company.getUser().getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean update(Company company) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("UPDATE companies SET name = ?, role = ?, start = ?, \"end\" = ?, user_id = ? WHERE id = ?");
        db.setString(1, company.getName());
        db.setString(2, company.getRole());
        db.setDate(3, company.getStart());
        bindEnd(db, 4, company);
        db.setInt(5, company.getUser().getId());
        db.setInt(6, company.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public boolean delete(Company company) {
        DBHandler db = new DBHandler(factory);
        db.prepareStatement("DELETE FROM companies WHERE id = ?");
        db.setInt(1, company.getId());
        return db.executeUpdate() > 0;
    }

    @Override
    public Company findById(int id) {
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
    public List<Company> listAll() {
        DBHandler db = new DBHandler(factory);
        try {
            db.prepareStatement(SELECT + "ORDER BY c.id");
            db.executeQuery();
            return readAll(db);
        } finally {
            db.close();
        }
    }

    @Override
    public List<Company> findPage(int offset, int limit) {
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
            db.prepareStatement("SELECT COUNT(*) AS total FROM companies");
            db.executeQuery();
            db.next();
            return db.getLong("total");
        } finally {
            db.close();
        }
    }

    private static void bindEnd(DBHandler db, int index, Company company) {
        if (company.getEnd() == null) {
            db.setNull(index, Types.DATE);
        } else {
            db.setDate(index, company.getEnd());
        }
    }

    private static List<Company> readAll(DBHandler db) {
        List<Company> companies = new ArrayList<>();
        while (db.next()) {
            companies.add(create(db));
        }
        return companies;
    }

    private static Company create(DBHandler db) {
        Company company = new Company(db.getInt("id"));
        company.setName(db.getString("name"));
        company.setRole(db.getString("role"));
        company.setStart(db.getDate("start"));
        company.setEnd(db.getDate("end"));
        User user = new User(db.getInt("user_id"));
        user.setName(db.getString("user_name"));
        company.setUser(user);
        return company;
    }
}
