/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 8th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Stream output format that returns a single JPEG frame. 
 */
public class JpegOutput extends StreamOutput
{

    public JpegOutput(HttpServletResponse resp, Map<String, String[]> params, SourceStream source)
    {
        super(resp, params, source);
    }
    
    @Override 
    public void writeHeaders()
    {
        /* Content type and length headers will be set once frame is received. */
    }

    @Override
    public boolean writeFrame(Frame frame) throws IOException
    {
        response.setContentType(frame.getContentType());
        response.setContentLength(frame.getContentLength());
        frame.writeTo(this.output);
        
        /* In a JPEG request we are only returning a single frame. */
        return false;
    }
    
    @Override
    protected boolean willWrite(Frame frame)
    {
        /* Always write frame. */
        return true;
    }
    
    @Override
    public String getSuffix()
    {
        return "jpeg";
    }

    
}
