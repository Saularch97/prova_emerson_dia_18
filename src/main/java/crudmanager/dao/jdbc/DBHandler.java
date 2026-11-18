package crudmanager.dao.jdbc;

import crudmanager.exception.DuplicateException;
import crudmanager.exception.ModelException;
import crudmanager.exception.RelatedDataException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Envolve uma conexão JDBC por operação. Nunca vaza SQLException: erros viram ModelException; violações de
 * chave estrangeira (23503) e de unicidade (23505) viram RelatedDataException e DuplicateException.
 */
public class DBHandler {

    private static final String FK_VIOLATION = "23503";
    private static final String UNIQUE_VIOLATION = "23505";

    private final Connection connection;
    private PreparedStatement preparedStatement;
    private ResultSet resultSet;

    public DBHandler(JdbcConnectionFactory factory) {
        this.connection = factory.getConnection();
    }

    public void prepareStatement(String sql) {
        try {
            preparedStatement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            close();
            throw new ModelException("Erro ao preparar a SQL", e);
        }
    }

    public void setString(int i, String value) {
        bind(() -> preparedStatement.setString(i, value), "string");
    }

    public void setInt(int i, int value) {
        bind(() -> preparedStatement.setInt(i, value), "inteiro");
    }

    public void setLong(int i, long value) {
        bind(() -> preparedStatement.setLong(i, value), "long");
    }

    public void setBoolean(int i, boolean value) {
        bind(() -> preparedStatement.setBoolean(i, value), "booleano");
    }

    public void setDecimal(int i, BigDecimal value) {
        bind(() -> preparedStatement.setBigDecimal(i, value), "decimal");
    }

    public void setDate(int i, LocalDate value) {
        bind(() -> preparedStatement.setObject(i, value), "data");
    }

    public void setNull(int i, int sqlType) {
        bind(() -> preparedStatement.setNull(i, sqlType), "null");
    }

    public int executeUpdate() {
        validatePreparedStatement();
        try {
            return preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            close();
        }
    }

    public void executeQuery() {
        validatePreparedStatement();
        try {
            resultSet = preparedStatement.executeQuery();
        } catch (SQLException e) {
            close();
            throw translate(e);
        }
    }

    public boolean next() {
        if (resultSet == null) {
            throw new ModelException("Result set não foi criado");
        }
        try {
            return resultSet.next();
        } catch (SQLException e) {
            close();
            throw new ModelException("Erro ao chamar próximo registro", e);
        }
    }

    public int getInt(String column) {
        return read(() -> resultSet.getInt(column), "getInt");
    }

    public long getLong(String column) {
        return read(() -> resultSet.getLong(column), "getLong");
    }

    public String getString(String column) {
        return read(() -> resultSet.getString(column), "getString");
    }

    public boolean getBoolean(String column) {
        return read(() -> resultSet.getBoolean(column), "getBoolean");
    }

    public BigDecimal getDecimal(String column) {
        return read(() -> resultSet.getBigDecimal(column), "getDecimal");
    }

    /** Devolve null quando a coluna é NULL. */
    public LocalDate getDate(String column) {
        return read(() -> resultSet.getObject(column, LocalDate.class), "getDate");
    }

    /** Fecha statement, result set e conexão; pode ser chamado várias vezes. */
    public void close() {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            connection.close();
        } catch (SQLException e) {
            throw new ModelException("Erro ao fechar recursos", e);
        }
    }

    private RuntimeException translate(SQLException e) {
        if (FK_VIOLATION.equals(e.getSQLState())) {
            return new RelatedDataException("Há dados relacionados ao registro.");
        }
        if (UNIQUE_VIOLATION.equals(e.getSQLState())) {
            return new DuplicateException("Registro duplicado.");
        }
        return new ModelException("Erro ao executar SQL", e);
    }

    private interface SqlAction {
        void run() throws SQLException;
    }

    private interface SqlReader<T> {
        T get() throws SQLException;
    }

    private void bind(SqlAction action, String what) {
        validatePreparedStatement();
        try {
            action.run();
        } catch (SQLException e) {
            close();
            throw new ModelException("Erro ao atribuir " + what, e);
        }
    }

    private <T> T read(SqlReader<T> reader, String what) {
        try {
            return reader.get();
        } catch (SQLException e) {
            close();
            throw new ModelException("Erro ao chamar " + what, e);
        }
    }

    private void validatePreparedStatement() {
        if (preparedStatement == null) {
            throw new ModelException("Prepared Statement não foi criado");
        }
    }
}
