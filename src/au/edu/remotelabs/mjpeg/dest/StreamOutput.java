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
    protected final Map<String, String> requestParams;
    
    /** Source stream that is being returned. */
    protected final SourceStream source;

    /** Transformer to modify acquired frame as requested. */ 
    private final FrameTransformer transformer;
    
    /** Whether to stop the output. */
    private boolean stop;
    
    /** Logger. */
    protected final Logger logger;
    
    public StreamOutput(HttpServletResponse resp, Map<String, String> params, SourceStream source)
    {
        this.logger = Logger.getLogger(getClass().getName());
        
        this.response = resp;
        
        this.requestParams = params;
        this.source = source;
        
        this.transformer = FrameTransformer.get(this.source, this.requestParams);
    }
    
    /**
     * Handles the request.
     */
    public void handle()
    {
        try
        {
            if (!this.source.register(this)) return;
            
            this.response.addHeader("Server", "MJpeg-Streamer/1.0; UTS");
            this.response.setCharacterEncoding("UTF-8");
                
            this.output = this.response.getOutputStream();
            
            /* Write headers. */
            this.writeHeaders();

            Frame frame;
            
            boolean cont = true;
            do
            {
                /* Acquire and stream loop which may be terminated if an
                 * error occurs reading source stream. */
                if ((frame = this.source.nextFrame()) == null) return;
                
                /* Output will drop this frame so short continue acquisition. */
                if (!this.willWrite(frame)) continue;
                
                /* There may be transforms on frame such as size or quality. 
                 * If they are common we transform frame here. */
                if (this.transformer.isTransforming()) frame = transformer.transform(frame);
                
                cont = this.writeFrame(frame);
            }
            while (cont && !this.stop);
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
     * Checks whether will write to the output or discard the frame.
     * 
     * @return true if writing
     */
    protected abstract boolean willWrite(Frame frame);
    
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
    
    /**
     * Cleanup the output, releasing resources.
     */
    public void cleanup()
    {
        FrameTransformer.unget(this.transformer);
    }
    
    /**
     * Stops this output.
     */
    protected void stop()
    {
        this.stop = true;
    }
}
