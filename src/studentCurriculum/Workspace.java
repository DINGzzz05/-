package studentCurriculum;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/Workspace")
public class Workspace extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("role") == null) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        String role = (String) session.getAttribute("role");
        String account = (String) session.getAttribute("accountId");
        request.setAttribute("role", role);
        request.setAttribute("year", OfferingDao.ACTIVE_YEAR);

        try {
            switch (role) {
                case "学生": loadStudent(request, account); break;
                case "教师": loadTeacher(request, account); break;
                case "管理员": loadAdmin(request); break;
                default:
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
            }
            request.getRequestDispatcher("/WEB-INF/workspace.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Unable to load workspace", e);
        }
    }

    private void loadStudent(HttpServletRequest request, String account) throws SQLException {
        OfferingDao dao = new OfferingDao();
        List<CourseBean> available = dao.availableCourses(account).stream()
                .filter(course -> course.getSchedule() != null).toList();
        List<OfferingDao.WaitlistItem> waitlist = dao.studentWaitlist(account);
        request.setAttribute("available", available.stream().limit(5).toList());
        request.setAttribute("waitlist", waitlist.stream().limit(4).toList());
        request.setAttribute("counts", new long[]{available.size(),
                dao.studentCourses(account).stream().filter(CourseBean::isActiveOffering).count(),
                waitlist.stream().filter(item -> !item.conflict()).count()});
    }

    private void loadTeacher(HttpServletRequest request, String account) throws SQLException {
        List<CourseBean> courses = new OfferingDao().teacherCourses(account, OfferingDao.ACTIVE_YEAR)
                .stream().filter(CourseBean::isActiveOffering).toList();
        request.setAttribute("courses", courses.stream().limit(5).toList());
        request.setAttribute("counts", new long[]{courses.size(),
                courses.stream().filter(course -> course.getSchedule() == null).count(),
                courses.stream().mapToInt(CourseBean::getWaitingCount).sum()});
    }

    private void loadAdmin(HttpServletRequest request) throws SQLException {
        String sql = """
                SELECT (SELECT COUNT(*) FROM 教师), (SELECT COUNT(*) FROM 学生), (SELECT COUNT(*) FROM 课程)
                """;
        try (Connection connection = new BaseDao().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            result.next();
            request.setAttribute("counts", new long[]{result.getLong(1), result.getLong(2), result.getLong(3)});
        }
    }
}
