package studentCurriculum;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class OfferingDao extends BaseDao {
    public static final String ACTIVE_YEAR = "2019";
    private static final String ACTIVE_TERM = "%1$s.开课学年='" + ACTIVE_YEAR + "' AND %1$s.开课学期=0";
    private static final String OVERLAP = "a.星期=? AND a.开始节次<=? AND a.结束节次>=?";
    private static final String COURSE_COLUMNS = """
            o.*, a.教室编号, a.星期, a.开始节次, a.结束节次, a.容量, r.教室名称,
                   (SELECT COUNT(*) FROM 选课情况 e WHERE e.开课编号=o.开课编号) AS 已选人数,
                   (SELECT COUNT(*) FROM 候补记录 w WHERE w.开课编号=o.开课编号 AND w.状态='WAITING') AS 候补人数
            """;
    private static final String COURSE_TABLES = """
            FROM 教师任课查询 o LEFT JOIN 教学班安排 a ON a.开课编号=o.开课编号
            LEFT JOIN 教室 r ON r.教室编号=a.教室编号
            """;

    public record Room(String id, String name, int seats) {}

    public record WaitlistItem(String offering, String course, String teacher, String schedule,
                               int position, boolean conflict) {}

    private record Offering(String id, String course, String teacher, int capacity, int weekday,
                            int start, int end) {}

    @FunctionalInterface
    private interface Transaction<T> {
        T run(Connection connection) throws SQLException;
    }

    private <T> T transact(Transaction<T> transaction) throws SQLException {
        try (Connection connection = getConnection()) {
            connection.setAutoCommit(false);
            try {
                lockSemester(connection);
                T result = transaction.run(connection);
                connection.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    // Operations on different offerings also share students, teachers and rooms.
    // Serialize these writes so each overlap or capacity check sees the preceding commit.
    static void lockSemester(Connection connection) throws SQLException {
        if (execute(connection, "UPDATE 排课选课锁 SET 版本=版本+1 WHERE 编号=1") != 1) {
            throw new SQLException("Missing scheduling lock");
        }
    }

    public List<Room> rooms() throws SQLException {
        List<Room> rooms = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM 教室 ORDER BY 教室编号");
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                rooms.add(new Room(rows.getString("教室编号"), rows.getString("教室名称"), rows.getInt("座位数")));
            }
        }
        return rooms;
    }

    public List<CourseBean> teacherCourses(String teacher, String year) throws SQLException {
        return courses("WHERE o.教师编号=? AND o.开课学年=? ORDER BY o.开课学期, o.课程编号", teacher, year);
    }

    public CourseBean teacherOffering(String teacher, String id) throws SQLException {
        List<CourseBean> found = courses("WHERE o.教师编号=? AND o.开课编号=? AND " + ACTIVE_TERM.formatted("o"), teacher, id);
        return found.isEmpty() ? null : found.get(0);
    }

    public List<CourseBean> studentCourses(String student) throws SQLException {
        List<CourseBean> found = new ArrayList<>();
        String sql = "SELECT e.成绩, " + COURSE_COLUMNS + COURSE_TABLES + """
                JOIN 选课情况 e ON e.开课编号=o.开课编号 WHERE e.学号=?
                ORDER BY o.开课学年, o.开课学期 DESC, o.课程编号
                """;
        try (Connection connection = getConnection();
             PreparedStatement statement = prepare(connection, sql, student);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                CourseBean course = readCourse(rows);
                course.setGraded(rows.getObject("成绩") != null);
                found.add(course);
            }
        }
        return found;
    }

    public List<CourseBean> availableCourses(String student) throws SQLException {
        return courses("""
                WHERE %s
                  AND NOT EXISTS (
                      SELECT 1 FROM 选课情况 e JOIN 开课情况 x ON x.开课编号=e.开课编号
                      WHERE e.学号=? AND x.课程编号=o.课程编号 AND %s
                  )
                  AND NOT EXISTS (
                      SELECT 1 FROM 候补记录 w JOIN 开课情况 x ON x.开课编号=w.开课编号
                      WHERE w.学号=? AND w.状态='WAITING' AND x.课程编号=o.课程编号 AND %s
                  )
                ORDER BY CASE WHEN a.容量 IS NULL THEN 1 ELSE 0 END, o.课程编号, o.教师编号
                """.formatted(ACTIVE_TERM.formatted("o"), ACTIVE_TERM.formatted("x"), ACTIVE_TERM.formatted("x")), student, student);
    }

    public List<WaitlistItem> studentWaitlist(String student) throws SQLException {
        String sql = """
                SELECT w.开课编号, w.状态, o.课程名称, o.姓名, a.教室编号, a.星期, a.开始节次, a.结束节次, a.容量, r.教室名称,
                       1 + (SELECT COUNT(*) FROM 候补记录 earlier WHERE earlier.开课编号=w.开课编号
                            AND earlier.状态='WAITING' AND earlier.排队编号<w.排队编号) AS 排位
                FROM 候补记录 w JOIN 教师任课查询 o ON o.开课编号=w.开课编号
                JOIN 教学班安排 a ON a.开课编号=w.开课编号 JOIN 教室 r ON r.教室编号=a.教室编号
                WHERE w.学号=? ORDER BY w.排队编号
                """;
        List<WaitlistItem> found = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = prepare(connection, sql, student);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                String schedule = readSchedule(rows).periodLabel() + " · " + rows.getString("教室名称");
                found.add(new WaitlistItem(rows.getString("开课编号"), rows.getString("课程名称"),
                        rows.getString("姓名"), schedule, rows.getInt("排位"), "CONFLICT".equals(rows.getString("状态"))));
            }
        }
        return found;
    }

    public String saveSchedule(String teacher, String id, String course, CourseSchedule schedule) throws SQLException {
        if (schedule == null || !schedule.isValid()) {
            return "请填写有效的教室、容量和上课节次。结束节次须大于或等于开始节次。";
        }
        return transact(connection -> {
            int seats = number(connection, "SELECT 座位数 FROM 教室 WHERE 教室编号=?", schedule.room());
            if (seats == 0 || schedule.capacity() > seats) {
                return "容量不能超过所选教室的座位数。";
            }

            Offering existing = id == null ? null : findOffering(connection, id);
            if (id != null && (existing == null || !teacher.equals(existing.teacher()))) {
                return "该教学班不可调整。";
            }
            boolean creating = id == null;
            if (creating && (course == null || !exists(connection, "SELECT 1 FROM 课程 WHERE 课程编号=?", course))) {
                return "请选择要开设的课程。";
            }
            String offering = creating ? course + teacher + ACTIVE_YEAR + "0" : id;
            if (creating && findOffering(connection, offering) != null) {
                return "该课程已开设，请在任课查询中调整排课。";
            }
            if (enrollmentCount(connection, offering) > schedule.capacity()) {
                return "容量不能低于当前已选人数。";
            }

            String conflictSql = """
                    SELECT 1 FROM 教学班安排 a JOIN 开课情况 o ON o.开课编号=a.开课编号
                    WHERE %s AND a.开课编号<>? AND %s AND
                    """.formatted(ACTIVE_TERM.formatted("o"), OVERLAP);
            if (exists(connection, conflictSql + " o.任课教师编号=?", offering, schedule.weekday(),
                    schedule.endPeriod(), schedule.startPeriod(), teacher)) {
                return "排课冲突：您在该时段已有授课安排。";
            }
            if (exists(connection, conflictSql + " a.教室编号=?", offering, schedule.weekday(),
                    schedule.endPeriod(), schedule.startPeriod(), schedule.room())) {
                return "排课冲突：该教室在所选时段已被占用。";
            }
            if (exists(connection, conflictSql + """
                     EXISTS (SELECT 1 FROM 选课情况 target JOIN 选课情况 other ON target.学号=other.学号
                             WHERE target.开课编号=? AND other.开课编号=a.开课编号)
                    """, offering, schedule.weekday(), schedule.endPeriod(), schedule.startPeriod(), offering)) {
                return "排课冲突：已有选课学生在该时段有其他课程。";
            }

            if (creating) {
                execute(connection, """
                        INSERT INTO 开课情况(开课编号, 课程编号, 任课教师编号, 开课学年, 开课学期)
                        VALUES (?, ?, ?, ?, 0)
                        """, offering, course, teacher, ACTIVE_YEAR);
            }
            int updated = execute(connection, """
                    UPDATE 教学班安排 SET 教室编号=?, 星期=?, 开始节次=?, 结束节次=?, 容量=? WHERE 开课编号=?
                    """, schedule.room(), schedule.weekday(), schedule.startPeriod(), schedule.endPeriod(),
                    schedule.capacity(), offering);
            if (updated == 0) {
                execute(connection, """
                        INSERT INTO 教学班安排(教室编号, 星期, 开始节次, 结束节次, 容量, 开课编号)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """, schedule.room(), schedule.weekday(), schedule.startPeriod(), schedule.endPeriod(),
                        schedule.capacity(), offering);
            }
            fillSeats(connection, new Offering(offering, creating ? course : existing.course(), teacher,
                    schedule.capacity(), schedule.weekday(), schedule.startPeriod(), schedule.endPeriod()));
            return null;
        });
    }

    public String choose(String student, String id) throws SQLException {
        return transact(connection -> {
            Offering offering = findOffering(connection, id);
            if (offering == null || offering.capacity() == 0) {
                return "该教学班尚未排课或已关闭选课。";
            }
            if (!exists(connection, "SELECT 1 FROM 学生 WHERE 学号=?", student)) {
                return "学生账号已停用。";
            }
            if (hasEnrollment(connection, student, offering.course()) || exists(connection, """
                    SELECT 1 FROM 候补记录 w JOIN 开课情况 o ON o.开课编号=w.开课编号
                    WHERE w.学号=? AND w.状态='WAITING' AND o.课程编号=? AND %s
                    """.formatted(ACTIVE_TERM.formatted("o")), student, offering.course())) {
                return "您已选修或正在候补这门课程。";
            }
            if (hasStudentConflict(connection, student, offering)) {
                return "选课冲突：该时段与您的已选课程重叠。";
            }

            execute(connection, "DELETE FROM 候补记录 WHERE 开课编号=? AND 学号=? AND 状态='CONFLICT'", id, student);
            if (enrollmentCount(connection, id) < offering.capacity()) {
                enroll(connection, id, student);
                return "选课成功。";
            }
            execute(connection, "INSERT INTO 候补记录(开课编号, 学号) VALUES (?, ?)", id, student);
            return "教学班已满，您已加入候补队列。";
        });
    }

    public String withdraw(String student, String id) throws SQLException {
        return transact(connection -> {
            Offering offering = findOffering(connection, id);
            if (offering == null || execute(connection,
                    "DELETE FROM 选课情况 WHERE 开课编号=? AND 学号=? AND 成绩 IS NULL", id, student) == 0) {
                return "该课程无法退选，可能已评分或已退选。";
            }
            fillSeats(connection, offering);
            return "退选成功。";
        });
    }

    public String cancelWaitlist(String student, String id) throws SQLException {
        return transact(connection -> execute(connection, "DELETE FROM 候补记录 WHERE 开课编号=? AND 学号=?",
                id, student) == 1 ? "候补记录已移除。" : "该候补记录已结束，请查看个人课程。");
    }

    public boolean cancelOffering(String teacher, String id) throws SQLException {
        return transact(connection -> {
            Offering offering = findOffering(connection, id);
            if (offering == null || !teacher.equals(offering.teacher()) ||
                    exists(connection, "SELECT 1 FROM 选课情况 WHERE 开课编号=? AND 成绩 IS NOT NULL", id)) {
                return false;
            }
            execute(connection, "DELETE FROM 选课情况 WHERE 开课编号=?", id);
            return execute(connection, "DELETE FROM 开课情况 WHERE 开课编号=?", id) == 1;
        });
    }

    public boolean deleteStudent(String student) throws SQLException {
        return transact(connection -> {
            List<String> affected = new ArrayList<>();
            try (PreparedStatement statement = prepare(connection, """
                    SELECT e.开课编号 FROM 选课情况 e JOIN 开课情况 o ON o.开课编号=e.开课编号
                    WHERE e.学号=? AND %s
                    """.formatted(ACTIVE_TERM.formatted("o")), student);
                 ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    affected.add(rows.getString(1));
                }
            }
            if (execute(connection, "DELETE FROM 学生 WHERE 学号=?", student) != 1) {
                return false;
            }
            for (String id : affected) {
                fillSeats(connection, findOffering(connection, id));
            }
            return true;
        });
    }

    private static void fillSeats(Connection connection, Offering offering) throws SQLException {
        int vacancies = offering.capacity() - enrollmentCount(connection, offering.id());
        while (vacancies > 0) {
            String candidate;
            try (PreparedStatement statement = prepare(connection, """
                    SELECT 学号 FROM 候补记录 WHERE 开课编号=? AND 状态='WAITING' ORDER BY 排队编号
                    """, offering.id())) {
                statement.setMaxRows(1);
                try (ResultSet rows = statement.executeQuery()) {
                    if (!rows.next()) {
                        return;
                    }
                    candidate = rows.getString(1);
                }
            }
            if (hasEnrollment(connection, candidate, offering.course()) || hasStudentConflict(connection, candidate, offering)) {
                execute(connection, "UPDATE 候补记录 SET 状态='CONFLICT' WHERE 开课编号=? AND 学号=?",
                        offering.id(), candidate);
            } else {
                enroll(connection, offering.id(), candidate);
                execute(connection, "DELETE FROM 候补记录 WHERE 开课编号=? AND 学号=?", offering.id(), candidate);
                vacancies--;
            }
        }
    }

    private static Offering findOffering(Connection connection, String id) throws SQLException {
        try (PreparedStatement statement = prepare(connection, """
                SELECT o.课程编号, o.任课教师编号, a.容量, a.星期, a.开始节次, a.结束节次
                FROM 开课情况 o LEFT JOIN 教学班安排 a ON a.开课编号=o.开课编号
                WHERE o.开课编号=? AND %s
                """.formatted(ACTIVE_TERM.formatted("o")), id);
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? new Offering(id, rows.getString("课程编号"), rows.getString("任课教师编号"),
                    rows.getInt("容量"), rows.getInt("星期"), rows.getInt("开始节次"), rows.getInt("结束节次")) : null;
        }
    }

    private static boolean hasEnrollment(Connection connection, String student, String course) throws SQLException {
        return exists(connection, """
                SELECT 1 FROM 选课情况 e JOIN 开课情况 o ON o.开课编号=e.开课编号
                WHERE e.学号=? AND o.课程编号=? AND %s
                """.formatted(ACTIVE_TERM.formatted("o")), student, course);
    }

    private static boolean hasStudentConflict(Connection connection, String student, Offering offering) throws SQLException {
        return exists(connection, """
                SELECT 1 FROM 选课情况 e JOIN 开课情况 o ON o.开课编号=e.开课编号
                JOIN 教学班安排 a ON a.开课编号=o.开课编号
                WHERE e.学号=? AND o.开课编号<>? AND %s AND %s
                """.formatted(ACTIVE_TERM.formatted("o"), OVERLAP), student, offering.id(), offering.weekday(),
                offering.end(), offering.start());
    }

    private static int enrollmentCount(Connection connection, String id) throws SQLException {
        return number(connection, "SELECT COUNT(*) FROM 选课情况 WHERE 开课编号=?", id);
    }

    private static void enroll(Connection connection, String id, String student) throws SQLException {
        execute(connection, "INSERT INTO 选课情况(开课编号, 学号) VALUES (?, ?)", id, student);
    }

    private List<CourseBean> courses(String filter, Object... arguments) throws SQLException {
        List<CourseBean> found = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = prepare(connection, "SELECT " + COURSE_COLUMNS + COURSE_TABLES + filter, arguments);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                found.add(readCourse(rows));
            }
        }
        return found;
    }

    private static CourseBean readCourse(ResultSet rows) throws SQLException {
        CourseBean course = new CourseBean();
        course.setTmpnum(rows.getString("开课编号").trim());
        course.setCoursenum(rows.getString("课程编号").trim());
        course.setCoursename(rows.getString("课程名称").trim());
        course.setTeachernum(rows.getString("教师编号").trim());
        course.setTeacher(rows.getString("姓名").trim());
        course.setYear(rows.getString("开课学年").trim());
        course.setTerm(rows.getBoolean("开课学期") ? "上学期" : "下学期");
        course.setTime(rows.getInt("学时"));
        course.setTestway(rows.getBoolean("考试或考查") ? "考查" : "考试");
        course.setCredit(rows.getFloat("学分"));
        course.setEnrolledCount(rows.getInt("已选人数"));
        course.setWaitingCount(rows.getInt("候补人数"));
        String room = rows.getString("教室编号");
        if (room != null) {
            course.setSchedule(readSchedule(rows), rows.getString("教室名称"));
        }
        return course;
    }

    private static CourseSchedule readSchedule(ResultSet rows) throws SQLException {
        return new CourseSchedule(rows.getString("教室编号"), rows.getInt("星期"), rows.getInt("开始节次"),
                rows.getInt("结束节次"), rows.getInt("容量"));
    }

    private static boolean exists(Connection connection, String sql, Object... arguments) throws SQLException {
        return number(connection, sql, arguments) > 0;
    }

    private static int number(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, arguments);
             ResultSet rows = statement.executeQuery()) {
            return rows.next() ? rows.getInt(1) : 0;
        }
    }

    private static int execute(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = prepare(connection, sql, arguments)) {
            return statement.executeUpdate();
        }
    }

    private static PreparedStatement prepare(Connection connection, String sql, Object... arguments) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            for (int i = 0; i < arguments.length; i++) {
                statement.setObject(i + 1, arguments[i]);
            }
            return statement;
        } catch (SQLException e) {
            statement.close();
            throw e;
        }
    }
}
