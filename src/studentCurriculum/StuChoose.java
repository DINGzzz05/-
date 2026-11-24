package studentCurriculum;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/StuChoose")
public class StuChoose extends HttpServlet {
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
                request.setAttribute("flag", dao.choose(student, offering));
            }
            List<CourseBean> courses = dao.availableCourses(student);
            StuDao studentDao = new StuDao();
            List<TeacherBean> teachers = new ArrayList<>();
            for (CourseBean course : courses) {
                teachers.add(studentDao.getTeacher(course.getTeachernum()));
            }
            request.setAttribute("clist", courses);
            request.setAttribute("tlist", teachers);
            request.getRequestDispatcher("/WEB-INF/stuchoose.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Unable to load or update course selection", e);
        }
    }
}
