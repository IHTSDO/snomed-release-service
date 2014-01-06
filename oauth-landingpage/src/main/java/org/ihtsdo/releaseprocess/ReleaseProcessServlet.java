package org.ihtsdo.releaseprocess;

import javax.servlet.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
//import java.net.URL;
//import java.net.URLClassLoader;
//import java.util.Arrays;

import org.apache.commons.io.IOUtils; 

/**
 * ReleaseProcessServlet is the abstract class that all Servlets in this package
 * will inherit from, so that they can take advantage of some common methods for
 * working with streams, rather than creating utility classes
 * @author Peter G. Williams, IHTSDO
 * @version     %I%, %G%
 * @since       1.0
 */
public abstract class ReleaseProcessServlet extends GenericServlet {


	private static final long serialVersionUID = -1043811473175588885L;

	@Override
    public abstract void service(ServletRequest req, ServletResponse res) throws ServletException, IOException;
	
	protected void out (ServletResponse res, String msg) throws IOException {
		out(res, msg, false); //default behaviour, newline for each thing output
	}
	
	protected void out (ServletResponse res, String msg, boolean raw) throws IOException {
		PrintWriter out = res.getWriter();
		out.println ( (raw?"":"<br/>") + msg );
	}	
	
	protected void include (ServletResponse res, String filename) throws IOException {
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
