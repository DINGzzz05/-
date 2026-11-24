package studentCurriculum;

import java.io.IOException;
import java.util.ArrayList;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/TeaIn")
public class TeaIn extends HttpServlet {
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

    private void showPage(HttpServletRequest request, HttpServletResponse response, boolean save)
            throws ServletException, IOException {
        String teacher = (String) request.getSession().getAttribute("accountId");
        String course = request.getParameter("num");
        String year = request.getParameter("year");
        TeaDao dao = new TeaDao();
        ArrayList<StudentBean> students = new ArrayList<>();

        if (course != null && year != null) {
            int start = course.lastIndexOf('（');
            if (start >= 0 && course.endsWith("）")) {
                course = course.substring(start + 1, course.length() - 1);
            }
            students = dao.getSingle(teacher, course, year, 1);
            if (save) {
                String message = saveGrades(request, dao, teacher, students);
                request.setAttribute("flag", message);
                students = dao.getSingle(teacher, course, year, 1);
            } else if (students.isEmpty()) {
                request.setAttribute("flag", "该课程暂无选课记录");
            }
        }

        request.setAttribute("year", year);
        request.setAttribute("num", course);
        request.setAttribute("alist", dao.getACourse(teacher));
        request.setAttribute("slist", students);
        request.getRequestDispatcher("/WEB-INF/teain.jsp").forward(request, response);
    }

    private String saveGrades(HttpServletRequest request, TeaDao dao, String teacher,
                              ArrayList<StudentBean> students) {
        ArrayList<StudentBean> grades = new ArrayList<>();
        for (StudentBean student : students) {
            String value = request.getParameter("grade." + student.getTmpnum() + "." + student.getNum());
            if (value == null || value.trim().isEmpty()) {
                continue;
            }
            float grade;
            try {
                grade = Float.parseFloat(value);
            } catch (NumberFormatException e) {
                return "请输入 0 到 100 之间的有效成绩";
            }
            if (!Float.isFinite(grade) || grade < 0 || grade > 100) {
                return "请输入 0 到 100 之间的有效成绩";
            }
            student.setGpa(grade);
            grades.add(student);
        }
        if (grades.isEmpty()) {
            return "请至少录入一项成绩";
        }
        return dao.saveGrades(teacher, grades) ? "成绩已保存，学生学分已更新" : "保存失败，请重新查询后再试";
    }
}
