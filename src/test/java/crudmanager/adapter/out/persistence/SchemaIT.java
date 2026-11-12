package crudmanager.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SchemaIT extends PostgresTestBase {

    private static final String FKS = "SELECT kcu.column_name, ccu.table_name AS ref_table "
            + "FROM information_schema.table_constraints tc "
            + "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name "
            + "JOIN information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name "
            + "WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_name = ?";

    private static final String UNIQUES = "SELECT kcu.column_name "
            + "FROM information_schema.table_constraints tc "
            + "JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name "
            + "WHERE tc.constraint_type = 'UNIQUE' AND tc.table_name = ? ORDER BY tc.constraint_name, kcu.ordinal_position";

    private List<String> query(String sql, String table, String... columns) throws Exception {
        List<String> rows = new ArrayList<>();
        try (Connection c = connectionFactory().getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StringBuilder sb = new StringBuilder();
                    for (String column : columns) {
                        sb.append(sb.length() == 0 ? "" : "->").append(rs.getString(column));
                    }
                    rows.add(sb.toString());
                }
            }
        }
        return rows;
    }

    @Test
    @DisplayName("DB-01 courses.instructor_id referencia users")
    void courseInstructorFk() throws Exception {
        assertTrue(query(FKS, "courses", "column_name", "ref_table").contains("instructor_id->users"));
    }

    @Test
    @DisplayName("DB-01 enrollments.student_id referencia users e enrollments.course_id referencia courses")
    void enrollmentFks() throws Exception {
        List<String> fks = query(FKS, "enrollments", "column_name", "ref_table");
        assertTrue(fks.contains("student_id->users"));
        assertTrue(fks.contains("course_id->courses"));
    }

    @Test
    @DisplayName("DB-01 enrollments tem unique (student_id, course_id)")
    void enrollmentUnique() throws Exception {
        assertEquals(List.of("student_id", "course_id"), query(UNIQUES, "enrollments", "column_name"));
    }

    @Test
    @DisplayName("DB-01 users tem a coluna password_hash")
    void passwordHashColumn() throws Exception {
        List<String> cols = query(
                "SELECT column_name FROM information_schema.columns WHERE table_name = ?", "users", "column_name");
        assertTrue(cols.contains("password_hash"));
    }
}
