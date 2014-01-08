package org.ihtsdo.releaseprocess;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;

import java.io.IOException;


@WebServlet(value="/hello", name="helloServlet")
public class HelloServlet extends ReleaseProcessServlet {


	private static final long serialVersionUID = -8953798908945384391L;

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

		//out(res,"Hello world 4!!");
		include (res, "google_login.html");
	}
	
}
