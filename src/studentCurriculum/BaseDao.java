package studentCurriculum;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

public class BaseDao {
    protected final DataSource dataSource;

    public BaseDao() {
        try {
            dataSource = InitialContext.doLookup("java:comp/env/jdbc/SCS");
        } catch (NamingException e) {
            throw new IllegalStateException("Data source jdbc/SCS is not configured", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
