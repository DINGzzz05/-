package studentCurriculum;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/StuCourse")
public class StuCourse extends HttpServlet {
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
        String student = (String) request.getSession().getAttribute("accountId");
        OfferingDao dao = new OfferingDao();
        try {
            String offering = request.getParameter("colnum");
            if (submit && offering != null) {
                String message = "cancel-waitlist".equals(request.getParameter("action"))
                        ? dao.cancelWaitlist(student, offering) : dao.withdraw(student, offering);
                request.setAttribute("flag", message);
            }
            request.setAttribute("clist", dao.studentCourses(student));
            request.setAttribute("waitlist", dao.studentWaitlist(student));
            request.getRequestDispatcher("/WEB-INF/stucourse.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Unable to load or update student courses", e);
        }
    }
}
