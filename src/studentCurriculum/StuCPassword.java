package studentCurriculum;


import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class StuCPassword
 */
@WebServlet("/StuCPassword")
public class StuCPassword extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public StuCPassword() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String passcode=request.getParameter("num");
		if ("POST".equals(request.getMethod()) && passcode != null) {
			String num = (String) request.getSession().getAttribute("accountId");
			StuDao stuDao = new StuDao();
			boolean flag=stuDao.changePasscode(passcode, num);
			if(flag) {
				request.setAttribute("flag", "修改密码成功！");
				
			}
			else {
				request.setAttribute("flag", "修改密码失败！");
			}
		}
		RequestDispatcher rDispatcher = request.getRequestDispatcher("/WEB-INF/stucpasscode.jsp");
		rDispatcher.forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
