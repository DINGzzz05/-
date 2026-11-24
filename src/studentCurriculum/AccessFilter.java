package studentCurriculum;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebFilter("/*")
public class AccessFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest http = (HttpServletRequest) request;
        HttpServletResponse result = (HttpServletResponse) response;
        String path = http.getServletPath();
        String requiredRole = null;
        if (path.startsWith("/Adm") || path.equals("/StuSChange")) {
            requiredRole = "管理员";
        } else if (path.startsWith("/Stu")) {
            requiredRole = "学生";
        } else if (path.startsWith("/Tea")) {
            requiredRole = "教师";
        }
        if (requiredRole != null) {
            HttpSession session = http.getSession(false);
            if (session == null || session.getAttribute("role") == null) {
                result.sendRedirect(http.getContextPath() + "/");
                return;
            }
            if (!requiredRole.equals(session.getAttribute("role"))) {
                result.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            result.setHeader("Cache-Control", "no-store");
        }
        request.setCharacterEncoding("UTF-8");
        chain.doFilter(request, response);
    }
}
