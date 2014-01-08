package org.ihtsdo.releaseprocess;

import javax.servlet.*;
import javax.servlet.annotation.WebServlet;

import java.io.IOException;
import java.util.Map;

import org.json.JSONObject;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;




@WebServlet(value="/serverLogin", name="serverLoginServlet")
/**
 * ServerLoginServlet expects a Google API Access token to have been previously
 * negotiated for it (eg using client side javascript), and will use that token
 * to access the other required information about a user and set this up in 
 * TBA datastore.
 * @author Peter G. Williams, IHTSDO
 * @version     %I%, %G%
 * @since       1.0
 *
 */
public class ServerLoginServlet extends ReleaseProcessServlet {


	private static final long serialVersionUID = -4738122290293989871L;

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {

		//Pull out everything sent to the servlet, and return it as a JSON object
		Map<String, String[]> requestParameters = req.getParameterMap();
		JSONObject jsonObj = new JSONObject();
		for (Map.Entry<String,String[]> entry : requestParameters.entrySet()) {
			jsonObj.append(entry.getKey(), entry.getValue()[0]);
		}


		Person person = getUserDetails(req);
		for (Map.Entry<String,Object> entry : person.entrySet()) {
			jsonObj.append(entry.getKey(), entry.getValue().toString());
		}
		
		res.setContentType("application/json");
		out (res, jsonObj.toString(), true);		

	}
	
	private Person getUserDetails(ServletRequest req) throws ServletException, IOException{
		
		//Check we received an accessToken from the front end
		String accessToken = req.getParameter("AccessToken");
		GoogleCredential credential = new GoogleCredential().setAccessToken(accessToken);
		Plus plus = new Plus.Builder(new NetHttpTransport(), new JacksonFactory(), credential)
			.setApplicationName("snomed-release-system")
			.setHttpRequestInitializer(credential)
			.build();
		return plus.people().get("me").execute();		
	}
	
}
