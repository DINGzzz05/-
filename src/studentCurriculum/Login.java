package studentCurriculum;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet("/Login")
public class Login extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String role = request.getParameter("status");
        String id = request.getParameter("num");
        String password = request.getParameter("password");
        String query;
        if ("学生".equals(role)) {
            query = "SELECT 学号, 姓名, 登录密码 FROM 学生 WHERE 学号=?";
        } else if ("教师".equals(role)) {
            query = "SELECT 教师编号, 姓名, 登录密码 FROM 教师 WHERE 教师编号=?";
        } else if ("管理员".equals(role)) {
            query = "SELECT 管理员编号, 管理员编号, 登录密码 FROM 管理员 WHERE 管理员编号=?";
        } else {
            showError(request, response, "请选择登录身份。");
            return;
        }

        try (Connection connection = new BaseDao().getConnection();
             PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, id == null ? "" : id.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && result.getString(3).trim().equals(password)) {
                    HttpSession previous = request.getSession(false);
                    if (previous != null) {
                        previous.invalidate();
                    }
                    HttpSession session = request.getSession();
                    session.setAttribute("accountId", result.getString(1).trim());
                    session.setAttribute("accountName", result.getString(2).trim());
                    session.setAttribute("role", role);
                    response.sendRedirect(request.getContextPath() + "/Workspace");
                    return;
                }
            }
            showError(request, response, "账号、密码或登录身份有误。");
        } catch (SQLException e) {
            log("Login database unavailable", e);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            showError(request, response, "暂时无法连接数据库，请稍后重试。");
        }
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("fail", message);
        request.getRequestDispatcher("/index.jsp").forward(request, response);
    }

    public static String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
