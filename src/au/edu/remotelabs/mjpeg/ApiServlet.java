/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date
 */

package au.edu.remotelabs.mjpeg;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;

/** 
 * REST calls that allows management operations to be executed.
 */
@WebServlet(name = "StreamsAPIServlet",
            urlPatterns = ApiServlet.PATH + "*")
public class ApiServlet extends HttpServlet
{
    private static final long serialVersionUID = 2272398629108535153L;

    /** URL pattern for servlet. */
    public final static String PATH = "/api/";
    
    /** Holder for streamer objects. */
    private StreamerHolder holder;
    
    /** Authorization hash which uses the HTTP BASIC authentication scheme. */
    private String secret;
    
    /** Logger for this object. */
    private final Logger logger;
    
    public ApiServlet()
    {
        super();
        
        this.logger = Logger.getLogger(getClass().getName());
    }
    
    @Override
    public void init(ServletConfig config)
    {
        this.holder = StreamerHolder.get();
        
        this.secret = this.holder.getConfig().getApiSecret();
    }
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        if (!this.isAuthenticated(request))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        String op = request.getRequestURI().substring(request.getRequestURI().indexOf(PATH) + PATH.length()); 
        switch (op)
        {
        case "streams":  // List out the streams.
            this.handleGetStreams(response);
            break;
        
        case "enabled": // Return whether stream is enabled.
            Stream stream = this.getStream(request);
            if (stream != null)
            {
                response.getWriter().println(!this.holder.getStream(stream.name).isDisabled());
            }
            else
            {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            }
            break;

        default:
            this.logger.info("Unknown operation " + op);
            break;
        }
    }

    /**
     * Handle request to get list of streams.
     * 
     * @param response the request response
     * @throws IOException error writing output
     */
    private void handleGetStreams(HttpServletResponse response) throws IOException
    {
        response.setContentType("application/json");
        
        PrintWriter out = response.getWriter();
        out.print('{');
        Iterator<String> it = this.holder.getConfig().getStreams().keySet().iterator();
        while (it.hasNext())
        {
            String name = it.next();
            out.print('"');
            out.print(name);
            out.print('"');
            out.print(':');
            out.print(!this.holder.getStream(name).isDisabled());
            
            if (it.hasNext()) out.print(',');
        }
        out.print('}');
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        if (!this.isAuthenticated(request))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        Stream stream = this.getStream(request);
        if (stream == null)
        {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        String operation = request.getRequestURI().substring(request.getRequestURI().indexOf(PATH) + PATH.length()); 
        switch (operation)
        {
        case "resetPassword": // Reset the password.
            String password = this.holder.getAuthenticator().reset(stream);
            if (password == null)
            {
                this.logger.warning("Cannot reset password for " + stream.name + ", probably because password reset is disabled.");
                response.setStatus(HttpServletResponse.SC_PRECONDITION_FAILED);
            }
            else
            {
                this.logger.info("Reset access password of " + stream.name + " to " + password);
                response.setStatus(HttpServletResponse.SC_OK);
                System.out.println(password);
                response.getWriter().println(password);
            }
            break;
            
        case "enable":  // Enable access to the stream
            this.logger.info("Enabling stream " + stream.name);
            this.holder.getStream(stream.name).enable();
            break;
            
        case "disable": // Disable access to the stream
            this.logger.info("Disabling stream " + stream.name);
            this.holder.getStream(stream.name).disable();
            break;
            
        default:
            this.logger.info("Unknown operation " + operation);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            break;
        }
        
    }
    
    /**
     * Gets the stream from the request parameter.
     * 
     * @param request the request
     * @return stream or null if not found
     */
    private Stream getStream(HttpServletRequest request)
    {
        String stream = request.getParameter("stream");
        return stream != null ? this.holder.getStreamConfig(stream) : null;
    }
    
    /**
     * Checks whether the request is appropriately authenticated.
     * 
     * @param request the request
     * @return whether authenticated
     */
    private boolean isAuthenticated(HttpServletRequest request)
    {
        return this.secret.equals(request.getHeader("Authorization"));
    }
}
