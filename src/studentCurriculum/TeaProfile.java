package studentCurriculum;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class TeaProfile
 */
@WebServlet("/TeaProfile")
public class TeaProfile extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TeaProfile() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		TeaDao teaDao = new TeaDao();
		TeacherBean teacherBean = new TeacherBean();
		String num = (String) request.getSession().getAttribute("accountId");
		String num2=(String) request.getAttribute("num");
		if(!num.equals(num2)&&num2!=null) {
			num=num2;
		}
		teacherBean = teaDao.getTeacher(num);
		request.setAttribute("tea", teacherBean);
		
		RequestDispatcher rDispatcher = request.getRequestDispatcher("/WEB-INF/teaindex.jsp");
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
