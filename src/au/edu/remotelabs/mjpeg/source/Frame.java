/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg.source;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Base64;

import au.edu.remotelabs.mjpeg.format.EncoderDecoder;

/** 
 * A single frame from the source stream.
 */
public class Frame
{
    /** Image bytes. */
    protected final byte buf[];
    
    /** Size of image in bytes. */
    protected final int size;
    
    /** Timestamp of when the frame was read. */
    private final long timestamp;
    
    /** MIME type of buf. */
    private final String mime;
    
    /** Sequence number of frame. */
    private final int sequence;
    
    /**
     * Creates the frame with the specified content size.
     * 
     * @param mime mime type
     * @param data image data bytes 
     * @param size size of buf
     * @param seq sequence number of frame
     */
    public Frame(String mime, byte data[], int size, int seq)
    {
        this.mime = mime.trim();
        this.buf = data;
        this.timestamp = System.currentTimeMillis();
        this.sequence = seq;
        this.size = size;
    }
    
    /**
     * Returns a buffered image decoded from this frames bytes using AWT.
     * 
     * @return image decoded image
     */
    public BufferedImage decodeImage(EncoderDecoder ed) throws Exception
    {
        return ed.decode(this.buf, size);
    }
    
    /**
     * Write the buf bytes to the output stream. 
     * 
     * @param stream stream to write to
     * @throws IOException error in writing
     */
    public void writeTo(OutputStream stream) throws IOException
    {
        stream.write(this.buf, 0, size);
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
    
    /**
     * Sequence number of frame which is relative from the first frame read.
     * 
     * @return sequence number
     */
    public int getSequence()
    {
        return this.sequence;
    }
}
