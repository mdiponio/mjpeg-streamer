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
    public static String BOUNDARY = "ffserver";
    
    /** Delay in milliseconds between frames to maintain target frame rate. */
    protected final int delay;
    
    /** Timestamp when a frame was last sent. */
    protected long sent;
    
    /** Whether to send sequence number. */
    protected final boolean sendSequence;
    
    /** Sequence offset from first frame in this stream. */
    protected int sequenceOffset = -1;

    public MJpegOutput(HttpServletResponse resp, Map<String, String> params, SourceStream source)
    {
        super(resp, params, source);
        
        int rate = 0;
        if (params.containsKey("fr")) rate = Integer.parseInt(params.get("fr"));
        if (params.containsKey("frame_rate")) rate = Integer.parseInt(params.get("frame_rate"));
        
        /* Sequence specifies a number for each frame starting at 0. */
        this.sendSequence = params.containsKey("sequence") || params.containsKey("seq");
        
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
        if (this.sequenceOffset < 0) this.sequenceOffset = frame.getSequence();
        this.sent = System.currentTimeMillis();
        return true;
    }
    
    /**
     * Sends the frame header and frame which includes boundary and content type and length
     * fields plus image bytes.
     * 
     * @param frame frame being sent
     * @throws IOException error sending
     */
    protected void sendFrame(Frame frame) throws IOException
    {
        this.writeln("--", BOUNDARY);
        this.writeln("Content-type: ", frame.getContentType());
        this.writeln("Content-length: ", frame.getContentLength());
        if (this.sendSequence) this.writeln("frame-sequence: ", frame.getSequence() - this.sequenceOffset);
        this.writeln();
        
        frame.writeTo(this.output);
    }

    @Override
    public boolean writeFrame(Frame frame) throws IOException
    {
        this.sendFrame(frame);
        return true;
    }
    
    @Override
    public String getSuffix()
    {
        return "mjpg";
    }
}
