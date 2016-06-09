/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 8th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.FrameTransformer;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Stream output which reconstructs a source stream into a M-JPEG for delivery to a requesting
 * client.
 */
public abstract class StreamOutput
{
    /** Servlet response. */
    protected final HttpServletResponse response;
    
    /** Response output stream. */
    protected OutputStream output;
    
    /** Request parameters. */
    protected final Map<String, String[]> requestParams;
    
    /** Source stream that is being returned. */
    protected final SourceStream source;

    /** Whether we are apply transforms to the buf. */
    private final boolean transform;

    /** Requested width of frames. */
    private final int width;
    
    /** Requested height of frames. */
    private final int height;
    
    /** Requested quality of frames. */
    private final int quality;
    
    /** Whether to add timestamp to buf. */
    private boolean timestamp;
    
    /** Logger. */
    protected final Logger logger;
    
    public StreamOutput(HttpServletResponse resp, Map<String, String[]> params, SourceStream source)
    {
        this.logger = Logger.getLogger(getClass().getName());
        
        this.response = resp;
        
        this.requestParams = params;
        this.source = source;

        /* Quality is a percentage from 1 to 100 with 0 being no quality change. */ 
        int q = 0;
        if (params.containsKey("q")) q = Integer.parseInt(params.get("q")[0]);
        if (params.containsKey("quality")) q = Integer.parseInt(params.get("quality")[0]);

        if (q > 100) q = 100;
        if (q < 0) q = 0;
        this.quality = q;
        
        /* A negative value indicates maintain aspect ratio so only one 
         * dimension needs to be set. */
        int wid = -1, hei = -1;
        if (params.containsKey("w")) wid = Integer.parseInt(params.get("w")[0]);
        if (params.containsKey("width")) wid = Integer.parseInt(params.get("width")[0]);        
        if (params.containsKey("h")) hei = Integer.parseInt(params.get("h")[0]);
        if (params.containsKey("height")) hei = Integer.parseInt(params.get("height")[0]);        
        this.width = wid;
        this.height = hei;
        
        this.timestamp = (params.containsKey("ts") && params.get("ts")[0].charAt(0) == 't') ||
                         (params.containsKey("timestamp") && params.get("timestamp")[0].charAt(0) == 't');
        
        this.transform = this.quality > 0 ||                  // Changing quality 
                         this.width > 0 || this.height > 0 || // Changing dimensioning
                         this.timestamp;                      // Adding a time stamp
    }
    
    /**
     * Handles the request.
     */
    public void handle()
    {
        try
        {
            this.source.register(this);
            
            /* Write generic headers. */
            this.response.addHeader("Server", "MJpeg-Streamer/1.0; UTS");
            this.response.setCharacterEncoding("UTF-8");
            
            /* Write headers. */
            this.writeHeaders();
            
            this.output = this.response.getOutputStream();
            
            Frame frame;
            do
            {
                /* Acquire and stream loop which may be terminated if an
                 * error occurs reading source stream. */
                if ((frame = this.source.nextFrame()) == null) return;
                
                /* There may be transforms on frame such as size or quality. 
                 * If they are common we transform frame here. */
                if (this.transform)
                {
                    FrameTransformer transformer = frame.transformer();
                    
                    if (this.timestamp) transformer.addTime();
                    if (this.width > 0 || this.height > 0) transformer.resize(this.width, this.height);
                    if (this.quality > 0 || this.quality < 100) transformer.setQuality(this.quality);
                    
                    frame = transformer.encode();
                }
            }
            while (this.writeFrame(frame));
        }
        catch (IOException ex)
        {
            /* An IO exception is expected when the client has stopped the 
             * connection to indicate they no longer want to display any
             * more of the stream. */
        }
        catch (ServletException ex)
        {
            this.logger.warning("Error handling connection for " + this.getSuffix() + ", error " + 
                    ex.getClass().getName() + ": " + ex.getMessage());
        }
        finally
        {
            this.source.unregister(this);
        }
    }

    /**
     * Writes response headers required for the returned response to be 
     * understood by the requesting web browser.
     * 
     * @throws ServletException error writing response
     * @throws IOException error writing response
     */
    protected abstract void writeHeaders() throws ServletException, IOException;
    
    /**
     * Writes a frame to the response stream. The method response indicates 
     * if further frames are to be written to the output (true) or if the connection
     * should be terminated (false)
     * 
     * @param frame the write to write
     * @return whether more frames should be written back
     * @throws ServletException error writing response
     * @throws IOException error writing response
     */
    protected abstract boolean writeFrame(Frame frame) throws ServletException, IOException;
    
    /**
     * Writes a line to the output, terminating the line with a carriage return and a new 
     * line character.
     * 
     * @param parts parts to write out
     * @throws IOExceptionn exception writing line
     */
    protected void writeln(Object... parts) throws IOException
    {
        for (Object o : parts)
        {
            this.output.write(o.toString().getBytes(Charset.forName("UTF-8")));
        }
        
        this.output.write('\r');
        this.output.write('\n');
    }
    
    /**
     * Specified the request file suffix used for the format.
     * 
     * @return format suffix
     */
    public abstract String getSuffix();
}
