/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg.source;

import java.io.IOException;
import java.io.OutputStream;

/** 
 * A single frame from the source stream.
 */
public class Frame
{
    /** Image bytes. */
    protected byte buf[];
    
    /** Timestamp of when the frame was read. */
    private final long timestamp;
    
    /** MIME type of buf. */
    private final String mime;
    
    /**
     * Creates the frame with the specified content size.
     * 
     * @param mime buf mime type
     * @param data buf data bytes 
     */
    public Frame(String mime, byte data[])
    {
        this.mime = mime;
        this.buf = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns a mutable copy of this frame.
     * 
     * @return mutable frame.
     */
    public FrameTransformer transformer() throws IOException
    {
        return new FrameTransformer(this);
    }
    
    /**
     * Write the buf bytes to the output stream. 
     * 
     * @param stream stream to write to
     * @throws IOException error in writing
     */
    public void writeTo(OutputStream stream) throws IOException
    {
        stream.write(this.buf);
    }
    
    /**
     * Returns the length of the frame buf in bytes.
     * 
     * @return buf length in bytes
     */
    public int getContentLength()
    {
        return this.buf.length;
    }
    
    /**
     * Returns the content type MIME of the frame. 
     * 
     * @return MIME type
     */
    public String getContentType()
    {
        return this.mime;
    }
    
    /**
     * Gets the time stamp of when this frame was read.
     * 
     * @return time stamp of read
     */
    public long getTimestamp()
    {
        return this.timestamp;
    }
}
