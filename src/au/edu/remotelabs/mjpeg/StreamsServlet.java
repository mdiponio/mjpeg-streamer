/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation to serve MJpeg streams 
 */
@WebServlet(name="StreamsServlet",
            urlPatterns = "/streams/*", 
            initParams = { @WebInitParam(name = "config-file", value = "./META-INF/streams.xml") })
public class StreamsServlet extends HttpServlet 
{
    private static final long serialVersionUID = 1L;
    
    

    public StreamsServlet() 
    {
        super();
    }

    public void init(ServletConfig config) throws ServletException 
    {
        // TODO Auto-generated method stub
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        // TODO Auto-generated method stub
        response.getWriter().append("Served at: ").append(request.getContextPath());
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        // TODO Auto-generated method stub
        doGet(request, response);
    }
    
    
    public void destroy()
    {
        super.destroy();
    }

}
