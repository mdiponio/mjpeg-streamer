/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.logging.Logger;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;

/**
 * Decomposes a M-JPEG stream down to its frames.
 */
public class SourceStream implements Runnable
{
    /** Stream configuration. */
    private final Stream config;
    
    /** Latest frame that has been read. */
    private Frame frame;
    
    /** The boundary to discriminate between frames in M-Jpeg stream. This may
     *  be configured but will be overridden if boundary is present in the response
     *  HTTP header as is typically the case. */
    private String boundary;
    
    /** Whether the connection is being read from. */
    private boolean reading;
    
    /** Whether an error has occurred accessing the stream. */
    private boolean error;
    
    /** The last error that occurred. */
    private String errorReason;
    
    /** Thread which reads the stream. */
    private Thread readThread;
    
    /** Whether to stop reading. */
    private boolean stop;
    
    /** Logger. */
    private final Logger logger;
    
    public SourceStream(Stream config)
    {
        this.config = config;
        this.logger = Logger.getLogger(getClass().getName());
        
        if (!this.config.ondemand)
        {
            this.logger.info("Stream " + this.config.name + " is configured to continuously stream, attempting " +
                    "to connect at start up.");
            this.start();
        }
    }
    
    /**
     * Starts the thread that reads the source stream.
     * 
     * @return true if able to start
     */
    public boolean start()
    {
        if (this.readThread != null && this.readThread.isAlive())
        {
            this.logger.warning("Unable to start stream because the reading thread for the stream is already running.");
            return false;
        }
        
        this.readThread = new Thread(this);
        this.readThread.setName("Stream: " + this.config.name);
        this.readThread.start();
        
        return false;
    }

    @Override
    public void run()
    {
        this.reading = true;
        this.stop = false;
        this.error = false;
        this.errorReason = null;

        try
        {
            /* Open connection. */
            HttpURLConnection conn = (HttpURLConnection) this.config.source.openConnection();
            
            /* Read boundary from content type header. */
            String contentType = conn.getContentType();
            if (contentType != null)
            {
                String boundaryTag = "boundary=";
                int pos = contentType.indexOf(boundaryTag);
                if (pos > 0)
                {
                    this.boundary = "--" + contentType.substring(pos + boundaryTag.length());
                    this.logger.info("Loaded stream " + this.config.name + " boundary as " + boundary);
                }
            }
            
            /* Read loop to acquire M-JPEG frames from source stream. */
            BufferedInputStream in = new BufferedInputStream(conn.getInputStream());
            while (!this.stop)
            {
                /*
                 * The M-Jpeg stream format is as follows:
                 * ---
                 * <boundary>
                 * content-type: <MIME>
                 * content-length: <length>
                 * [blank line]
                 * FF D8<image bytes>
                 * ...
                 * ...
                 * <image bytes>FF D9
                 * ---
                 * where FF D8 and FF D9 are start and image markers respectively.
                 */
                
                if (!this.skipToNextFrame(in)) break;
                
                String mime = this.readContentType(in);
                if (mime == null) break;
                
                int size = this.readContentLength(in);
                if (size < 0) break;
                
                /* An addition blank line. */
                this.readStreamLine(in);
                
                /* Read image bytes. */
                byte image[] = new byte[size];
                int r, read = 0;
                
                while (read < size && (r = in.read(image, read, size - read)) > 0)
                {
                    read += r;
                    if (this.stop) break;
                }
                
                if (size != read)
                {
                    this.logger.warning("Failed to fully read image bytes for stream " + this.config.name + 
                            ", read " + read + " of " + size + " bytes.");
                    this.error = true;
                    this.errorReason = "Failed to read image bytes";
                    break;
                }
                
                /* Validate received frame is correct. */
                if (mime.equalsIgnoreCase("jpeg") &&
                    !(image[0] == 0xFF && image[1] == 0xD8 && image[size - 2] == 0xFF && image[size - 1] == 0xD9))
                {
                    this.logger.info("Received JPEG image for " + this.config.name + " has incorrect SOI and EOI "
                            + "marker bytes, discarding frame as it may be corrupt.");
                    continue;
                }
                
                this.frame = new Frame(mime, image);
            }
            
            /* Finished reading, through clean shutdown or otherwise, close stream. */
        }
        catch (IOException e)
        {
            this.logger.warning("Error reading source stream " + this.config.name + ", error " + 
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            this.error = true;
            this.errorReason = "Error reading stream " + this.config.name + ", error " + e.getClass().getSimpleName() + 
                    ": " + e.getMessage(); 
        }
    }
    
    /**
     * Stops the reading source stream.
     * 
     * @return whether stopping was successful
     */
    public boolean stop()
    {
        if (this.readThread == null || !this.readThread.isAlive()) return true;
        
        try
        {
            this.logger.info("Stopping reading thread for " + this.config.name);
            this.stop = true;
            this.readThread.join(30000);
            
            if (this.readThread.isAlive()) this.logger.warning("Failed to stop reading thread for " + 
                    this.config.name + " in 30 seconds.");
            return !this.readThread.isAlive();
        }
        catch (InterruptedException e)
        {
            return true;
        }
    }
    
    /**
     * Reads a line from the input stream. The line characters are assumed to be 
     * ASCII characters.
     * 
     * @param in stream input
     * @return 
     * @throws IOException
     */
    private String readStreamLine(BufferedInputStream in) throws IOException
    {
        char buf[] = new char[255];
        int len = 0;
        
        buf[len++] = (char) in.read();
        buf[len++] = (char) in.read();
        
        while (buf[len - 2] != '\r' && buf[len - 1] != '\n')
        {
            if (len == buf.length) buf = Arrays.copyOf(buf, buf.length * 2);
            buf[len++]= (char) in.read();
        }
        
        return String.valueOf(buf, 0, len).trim();
    }
    
    /**
     * Reads through the M-JPEG stream until the boundary has been read. 
     * 
     * Pre-condition: Reader is connected
     * Post-condition: Reader is a position ready to read the next line 
     * after the boundary line
     * 
     * @param in stream input
     * @return true if successfully a position just past the boundary
     * @throws IOException error reading stream
     */
    private boolean skipToNextFrame(BufferedInputStream in) throws IOException
    {
        String line;
        do
        {
            if (this.stop) return false;

            line = this.readStreamLine(in);
            if (line == null)
            {
                /* End of stream. */
                this.logger.warning("Reached end of stream for " + this.config.name + " unexpectedly. ");
                this.error = true;
                this.errorReason = "Reached end of stream";
                return false;
            }
        }
        while (!this.boundary.equals(line));
        
        /* Successfully skipped past frame boundary. */
        return true;
    }
    
    /**
     * Reads the MIME type label from the content-type line in the frame details.
     * 
     * Pre-condition: Reader is at a position to read the line after the boundary
     * Post-condition: Reader is at a position to read the line after content type
     * 
     * @param in stream input
     * @return mime or null if error has occurred
     * @throws IOException error reading stream
     */
    private String readContentType(BufferedInputStream in) throws IOException
    {
        String line = this.readStreamLine(in);
        if (line == null || !line.toLowerCase().startsWith("content-type:"))
        {
            this.logger.warning("Unexpected stream format for " + this.config.name + ", did not receive " +
                    "content type mime at the expected position.");
            this.error = true;
            this.errorReason = "Did not recieve content type";
            return null;
        }
        
        return line.substring(line.indexOf(':') + 1);
    }
    
    /**
     * Reads the content length in the frame details.
     * 
     * Pre-condition: Reader is at a position to read content length line
     * Post-condition: Reader is at a position to read line after content length
     * 
     * @param in stream input
     * @return content length or -1 if error
     * @throws IOException error reading stream
     */
    private int readContentLength(BufferedInputStream in) throws IOException
    {
        String line = this.readStreamLine(in);
        if (line == null || !line.toLowerCase().startsWith("content-length:"))
        {
            this.logger.warning("Unexpected stream format for " + this.config.name + ", did not receive " +
                    "frame content length.");
            this.error = true;
            this.errorReason = "Did not recieve content length";
            return -1;
        }

        try
        {
            return Integer.parseInt(line.substring(line.indexOf(':') + 1).trim());
        }
        catch (NumberFormatException e)
        {
            this.logger.warning("Unexpected stream format for " + this.config.name + ", did not recieve " +
                    "valid content length");
            return -1;
        }
    }
    
    
    
    /**
     * Gets the last frame read which may be null if no frame has been read.
     * 
     * @return last frame read
     */
    public Frame getLastFrame()
    {
        return this.frame;
    }

    /**
     * Checks whether the source stream is being read.
     * 
     * @return true if reading source
     */
    public boolean isReading()
    {
        return this.reading;
    }
    
    /**
     * Checks whether there has been an error reading source stream.
     * 
     * @return true if reading error
     */
    public boolean isErrored()
    {
        return this.error;
    }
    
    /**
     * Gets the error that occurred during reading source stream. 
     * 
     * @return reading error
     */
    public String getError()
    {
        return this.errorReason;
    }
}
