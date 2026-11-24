package studentCurriculum;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/TeaCourse")
public class TeaCourse extends HttpServlet {
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
        String year = request.getParameter("year");
        if (year == null || !year.matches("[0-9]{4}")) {
            year = OfferingDao.ACTIVE_YEAR;
        }
        String teacher = (String) request.getSession().getAttribute("accountId");
        OfferingDao dao = new OfferingDao();
        try {
            String offering = request.getParameter("colnum");
            if (submit && offering != null) {
                year = OfferingDao.ACTIVE_YEAR;
                boolean removed = dao.cancelOffering(teacher, offering);
                request.setAttribute("flag", removed ? "已取消开课。" : "无法取消开课，教学班不可操作或已有成绩。");
            } else if ("created".equals(request.getParameter("saved"))) {
                request.setAttribute("flag", "开课成功，已保存排课和容量。");
            } else if ("updated".equals(request.getParameter("saved"))) {
                request.setAttribute("flag", "排课与容量已更新。");
            }
            request.setAttribute("clist", dao.teacherCourses(teacher, year));
            request.setAttribute("year", year);
            request.getRequestDispatcher("/WEB-INF/teacourse.jsp").forward(request, response);
        } catch (SQLException e) {
            throw new ServletException("Unable to load or update teacher courses", e);
        }
    }
}
