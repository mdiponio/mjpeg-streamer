/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 8th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Stream output format provides a M-JPEG format.
 */
public class MJpegOutput extends StreamOutput
{
    /** Boundary this application uses to delimit each frame. */
    public static String BOUNDARY = "mjpeg-streamer";
    
    /** Delay in milliseconds between frames to maintain target frame rate. */
    private final int delay;
    
    /** Timestamp when a frame was last sent. */
    private long sent;

    public MJpegOutput(HttpServletResponse resp, Map<String, String[]> params, SourceStream source)
    {
        super(resp, params, source);
        
        int rate = 0;
        if (params.containsKey("fr")) rate = Integer.parseInt(params.get("fr")[0]);
        if (params.containsKey("frame_rate")) rate = Integer.parseInt(params.get("frame_rate")[0]);
        
        if (rate > 0)
        {
            /* No frame rate has been specified so every frame will be 
             * returned as it read from the source. */
            rate = 1000 / rate;
        }
        
        this.delay = rate;
        this.sent = System.currentTimeMillis();
    }

    @Override
    protected void writeHeaders() throws ServletException, IOException
    {
        this.response.setContentType("multipart/x-mixed-replace;boundary=" + BOUNDARY);
    }
    
    @Override
    protected boolean willWrite(Frame frame)
    {
        /* If acquisition is faster than target framerate, drop frames. */
        if (this.sent + this.delay > System.currentTimeMillis()) return false;
        
        this.sent = System.currentTimeMillis();
        return true;
    }

    @Override
    public boolean writeFrame(Frame frame) throws IOException
    {
        this.writeln();
        this.writeln("--", BOUNDARY);
        this.writeln("content-type: ", frame.getContentType());
        this.writeln("content-length: ", frame.getContentLength());
        this.writeln();
        
        frame.writeTo(this.output);
        return true;
    }
    
    @Override
    public String getSuffix()
    {
        return "mjpg";
    }
}
