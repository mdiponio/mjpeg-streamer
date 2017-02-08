/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg.source;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Base64;

import javax.imageio.ImageIO;

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
        this.mime = mime.trim();
        this.buf = data;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Returns a buffered image decoded from this frames bytes.
     * 
     * @return image decoded image
     */
    public BufferedImage decodeImage() throws IOException
    {
        return ImageIO.read(new ByteArrayInputStream(this.buf));
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
        stream.flush();
    }
    
    /**
     * Write a Base64 encoded string to the stream.
     * 
     * @param stream writer stream to output to
     */
    public void writeTo(Writer stream) throws IOException
    {
        stream.write(Base64.getMimeEncoder().encodeToString(this.buf));
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
