/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;
import au.edu.remotelabs.mjpeg.dest.JpegOutput;
import au.edu.remotelabs.mjpeg.dest.MJpegOutput;
import au.edu.remotelabs.mjpeg.dest.StreamOutput;
import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Servlet to serve MJpeg streams. 
 */
@WebServlet(name="StreamerServlet",
            urlPatterns = StreamerServlet.PATH + "*", 
            loadOnStartup = 1,
            initParams = { @WebInitParam(name = "streamer-config", value = "/etc/streamer-config.xml") })
public class StreamerServlet extends HttpServlet 
{
    private static final long serialVersionUID = 1L;
    
    /** Url pattern for servlet. */
    public final static String PATH = "/streams/";
    
    /** Holder for streamer objects. */
    private final StreamerHolder holder;
    
    /** Logger. */
    private final Logger logger;
    
    public StreamerServlet() 
    {
        super();
        
        this.logger = Logger.getLogger(getClass().getName());
        this.holder = StreamerHolder.get();
    }

    @Override
    public void init(ServletConfig config) throws ServletException 
    {
        String conf = config.getInitParameter("streamer-config");
        if (conf == null || !(new File(conf)).canRead())
        {
            conf = System.getenv("STREAMER_CONFIG");
            if (conf == null || !(new File(conf).canRead()))
            {
                this.logger.severe("Configuration file for streamer application has not been configured.");
                throw new ServletException("Configuration file location not configured.");
            }
        }
    
        this.holder.init(conf);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        String url = request.getRequestURI();
        url = url.substring(url.indexOf(PATH) + PATH.length());
        
        /*
         * URL format will be: 
         *  <PATH>/<camera>.[jpeg|mjpg][?<options>]
         */
        
        int s = url.indexOf('.');
        if (s < 1)
        {
            /* Stream format not specified, resource not found. */
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        Stream stream = this.holder.getStreamConfig(url.substring(0, s));
        if (stream == null)
        {
            /* Camera with name not found, 404 response. */
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        if (!this.holder.getAuthenticator().authenticate(stream, request.getParameter("pw")))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        
        SourceStream source = this.holder.getStream(stream.name);
        if (source.isDisabled())
        {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }

        String format = url.substring(s + 1);
        if (!("jpeg".equalsIgnoreCase(format) || 
              "mjpg".equalsIgnoreCase(format) ||
              "last".equalsIgnoreCase(format)))
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        StreamOutput out;
        switch (format)
        {
        case "jpeg":
            out = new JpegOutput(response, this.getParams(request), source);
            break;
            
        case "mjpg":
            out = new MJpegOutput(response, this.getParams(request), source);
            break;
            
        case "last": // Special output format where only the last frame acquired is returned 
            Frame last = source.getLastFrame();
            if (last != null)
            {
                response.setContentType(last.getContentType());
                response.setContentLength(last.getContentLength());
                last.writeTo(response.getOutputStream());
            }
            return;
            
        default:
            /* Whatever format was requested was not understood, return bad request. */
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        /* Handle response. */
        out.handle();
        out.cleanup();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        doGet(request, response);
    }
    
    @Override
    public void destroy()
    {
        this.holder.destroy();
        super.destroy();
    }

    private Map<String, String> getParams(HttpServletRequest request)
    {
        Map<String, String[]> p = request.getParameterMap();
        Map<String, String> cp = new HashMap<>(p.size());
        p.forEach((String k, String v[]) -> cp.put(k, v[0]));
        return cp;
    }
}
