package studentCurriculum;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/TeaSetup")
public class TeaSetup extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showPage(request, response, false);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        showPage(request, response, true);
    }

    private void showPage(HttpServletRequest request, HttpServletResponse response, boolean submit)
            throws ServletException, IOException {
        OfferingDao dao = new OfferingDao();
        String teacher = (String) request.getSession().getAttribute("accountId");
        String id = request.getParameter("offering");
        try {
            CourseBean existing = id == null ? null : dao.teacherOffering(teacher, id);
            if (id != null && existing == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            String selectedCourse = request.getParameter("colnum");
            CourseSchedule schedule = existing == null ? null : existing.getSchedule();
            if (submit) {
                schedule = readSchedule(request);
                String error = dao.saveSchedule(teacher, id, selectedCourse, schedule);
                if (error == null) {
                    response.sendRedirect(request.getContextPath() + "/TeaCourse?saved="
                            + (existing == null ? "created" : "updated"));
                    return;
                }
                request.setAttribute("flag", error);
            }
            request.setAttribute("existing", existing);
            request.setAttribute("clist", existing == null ? new TeaDao().getNoSet(teacher) : List.of());
            request.setAttribute("rooms", dao.rooms());
            request.setAttribute("selectedCourse", selectedCourse);
            request.setAttribute("schedule", schedule == null ? new CourseSchedule("", 1, 1, 2, 30) : schedule);
            request.getRequestDispatcher("/WEB-INF/teasetup.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Unable to load or save course schedule", e);
        }
    }

    private CourseSchedule readSchedule(HttpServletRequest request) {
        try {
            return new CourseSchedule(request.getParameter("room"),
                    Integer.parseInt(request.getParameter("weekday")),
                    Integer.parseInt(request.getParameter("startPeriod")),
                    Integer.parseInt(request.getParameter("endPeriod")),
                    Integer.parseInt(request.getParameter("capacity")));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
