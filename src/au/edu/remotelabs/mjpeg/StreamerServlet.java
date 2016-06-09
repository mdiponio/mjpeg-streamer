/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
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
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Servlet to serve MJpeg streams. 
 */
@WebServlet(name="StreamsServlet",
            urlPatterns = StreamerServlet.PATH + "*", 
            loadOnStartup = 1,
            initParams = { @WebInitParam(name = "streams-config", value = "./WebContent/META-INF/streams-config.xml") })
public class StreamerServlet extends HttpServlet 
{
    /** Url pattern for servlet. */
    public final static String PATH = "/streams/";
    
    private static final long serialVersionUID = 1L;
    
    /** Streams. */
    private final Map<String, SourceStream> streams;
    
    /** Streamer configuration. */
    private StreamerConfig config;
    
    /** Authenticator of streams. */
    private Authenticator authenticator;
    
    /** Logger. */
    private final Logger logger;
    
    public StreamerServlet() 
    {
        super();
        
        this.logger = Logger.getLogger(getClass().getName());
        
        this.streams = new HashMap<>();
    }

    @Override
    public void init(ServletConfig config) throws ServletException 
    {
        String conf = config.getInitParameter("streams-config");
        if (conf == null)
        {
            this.logger.severe("Configuration file for streamer application has not been configured.");
            throw new ServletException("Configuration file location not configured.");
        }
    
        this.config = new StreamerConfig(conf);
        
        for (Stream stream : this.config.getStreams().values())
        {
            this.logger.fine("Loaded configuration for stream: " + stream.name);
            this.streams.put(stream.name, new SourceStream(stream));
        }
        
        this.authenticator = new Authenticator(this.config);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException 
    {
        /*
         * URL format will be: 
         *  <PATH>/<camera>.[jpeg|mjpg][?<options>]
         */
        String url = request.getRequestURI();
        url = url.substring(url.indexOf(PATH) + PATH.length());
        
        int s = url.indexOf('.');
        if (s < 1)
        {
            /* Stream format not specified, resource not found. */
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        Stream stream = this.config.getStream(url.substring(0, s));
        if (stream == null)
        {
            /* Camera with name not found, 404 response. */
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        if (!this.authenticator.authenticate(stream, request.getParameter("pw")))
        {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }


        String format = url.substring(s + 1);
        if (!("jpeg".equalsIgnoreCase(format) || 
              "mjpg".equalsIgnoreCase(format)))
        {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        StreamOutput out;
        switch (format)
        {
        case "jpeg":
            out = new JpegOutput(response, request.getParameterMap(), this.streams.get(stream.name));
            break;
            
        case "mjpg":
            out = new MJpegOutput(response, request.getParameterMap(), this.streams.get(stream.name));
            break;
            
        default:
            /* Whatever format was requested was not understood, return bad request. */
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        
        /* Handle response. */
        out.handle();
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
        for (Entry<String, SourceStream> e : this.streams.entrySet())
        {
            e.getValue().stop();
        }
        
        super.destroy();
    }

}
