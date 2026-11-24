package studentCurriculum;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class TeaAvg
 */
@WebServlet("/TeaAve")
public class TeaAvg extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public TeaAvg() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String num = (String) request.getSession().getAttribute("accountId");
		
		String year=request.getParameter("year");
		TeaDao teaDao=new TeaDao();
		ArrayList<CourseBean> cList=new ArrayList<>();
		if(year!=null) {
			cList=teaDao.getAvg(num, year);
			if(cList.isEmpty()) {
				request.setAttribute("flag", year+"年查无数据");
			}
		}
		request.setAttribute("year", year);
		request.setAttribute("clist", cList);
		RequestDispatcher rDispatcher = request.getRequestDispatcher("/WEB-INF/teaavg.jsp");
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
