package studentCurriculum;


import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

/**
 * Servlet implementation class Profile
 */
@WebServlet("/StuProfile")
public class StuProfile extends HttpServlet {
	private static final long serialVersionUID = 1L;
	DataSource dataSource;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public StuProfile() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		StuDao stuDao = new StuDao();
		StudentBean studentBean = new StudentBean();
		String num = (String) request.getSession().getAttribute("accountId");
		String num2=(String) request.getAttribute("num");
		if(!num.equals(num2)&&num2!=null) {
			num=num2;
		}
		studentBean = stuDao.getStudent(num);
		request.setAttribute("stu", studentBean);
		
		RequestDispatcher rDispatcher = request.getRequestDispatcher("/WEB-INF/stuindex.jsp");
		rDispatcher.forward(request, response);

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}

