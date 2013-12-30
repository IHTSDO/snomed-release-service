package org.ihtsdo.releaseprocess;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.Arrays;

import org.apache.commons.io.IOUtils; 

@WebServlet(value="/hello", name="helloServlet")
public class HelloServlet extends GenericServlet {
    /**
	 * 
	 */
	private static final long serialVersionUID = -8953798908945384391L;

	@Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
  		
		//out(res,"Hello world 4!!");
        include (res, "google_login.html");
    }
	
	private void out (ServletResponse res, String msg) throws IOException {
		PrintWriter out = res.getWriter();
		out.println ("<br/>" + msg );
	}
	
	private void include (ServletResponse res, String filename) throws IOException {
        try{

        	//URL[] classPaths = ((URLClassLoader)getServletContext().getClassLoader()).getURLs();
        	//out(res,"Servlet Classsloader path is: " + Arrays.toString(classPaths));
        	//URL url = getServletContext().getClassLoader().getResource(filename);
        	
        	InputStream is = getServletContext().getClassLoader().getResourceAsStream(filename);
        	IOUtils.copy(is, res.getWriter());
        	is.close();
        	
        	//This doesn't work because RequestDispatcher needs a url accessible the same way as the servlet is.
        	//TODO discuss best way to access resource files vs JSP
        	//(/oauth-landingpage/file:/C:/Users/Peter/git/snomed-release-system/oauth-landingpage/target/classes/included.html) is not available
	        //req.getRequestDispatcher(resourceUrl).include(req, res);  
        } catch (Exception e) {
        	out(res,"Failed to load resource for inclusion: " + e.getLocalizedMessage());
        	e.printStackTrace(res.getWriter());
        }
		
	}


}
