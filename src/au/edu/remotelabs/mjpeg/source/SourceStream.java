/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
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
    
    /** Whether an error has occurred accessing the stream. */
    private boolean error;
    
    /** Whether this stream has been disabled. */
    private volatile boolean disabled;
    
    /** The last error that occurred. */
    private String errorReason;
    
    /** Thread which reads the stream. */
    private Thread readThread;
    
    /** Whether to stop reading. */
    private boolean stop;

    /** List of destination streams that provides M-JPEG streams to clients. */
    private final List<Object> destinations;
    
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
        
        this.destinations = Collections.synchronizedList(new ArrayList<Object>());
    }
    
    /**
     * Enable this stream so it can be accessed.
     */
    public void enable()
    {
        synchronized (this.destinations)
        {
            if (this.disabled)
            {
                this.disabled = false;
                if (!this.config.ondemand) this.start();
            }
        }
    }
    
    /**
     * Disables this stream so it can not be accessed.
     */
    public void disable()
    {   
        synchronized (this.destinations)
        {
            if (!this.disabled)
            {
                this.disabled = true;
                
                if (this.destinations.size() > 0)
                {
                    this.destinations.clear();
                    if (this.isReading()) this.stop();
                }
            }
        }
    }
    
    /**
     * Register to receive frames when acquired from this stream source.
     * 
     * @param output the destination output being registered
     * @return whether successfully registered
     */
    public boolean register(Object output)
    {
        if (this.disabled) return false;
        
        synchronized (this.destinations)
        {
            if (this.disabled) return false;
            
            /* If not actively reading from the stream, spool up connection. */
            this.destinations.add(output);
            if (!this.isReading()) this.start();        
        }
        
        return true;
    }
    
    /**
     * Unregister from receiving frames.
     * 
     * @param output the destination output being unregistered
     */
    public void unregister(Object output)
    {
        synchronized (this.destinations)
        {
            this.destinations.remove(output);
            if (this.destinations.size() == 0 && this.config.ondemand) this.stop();
        }
    }
    
    /**
     * Blocking call to get the next frame when it is read from the source
     * stream. If an error has occurred, null will be returned.
     * 
     * @return next frame that is read
     */
    public Frame nextFrame()
    {
        synchronized (this)
        {
            try
            {
                this.wait();
            }
            catch (InterruptedException e)
            {  }
        }

        return this.error || this.stop ? null : this.frame;
    }
    
    /**
     * Starts the thread that reads the source stream.
     */
    private void start()
    {
        this.readThread = new Thread(this);
        this.readThread.setName("Stream: " + this.config.name);
        this.readThread.start();
    }

    @Override
    public void run()
    {
        this.stop = false;
        this.error = false;
        this.errorReason = null;
        int sequence = 0;

        try
        {
            this.logger.fine("Starting stream reading for " + this.config.name);
            
            /* Open connection. */
            HttpURLConnection conn = (HttpURLConnection) this.config.source.openConnection();
            
            /* The timeout ensure that we don't indefinitely block destinations because the
             * source is not currently available. */
            conn.setConnectTimeout(1000);
            
            /* If authentication is configured, add authentication headers to request. */
            switch (this.config.authType)
            {
            case BASIC:
                if (!this.addBasicAuth(conn)) return;
                break;
                
            case NONE:
                /* Falls through. */
            default:
                /* No authentication is required. */
            }
            
            /* Make sure the response status does not indicate an error. */
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK)
            {
                this.logger.warning("Failed to read camera M-JPEG stream, response code is " + conn.getResponseCode());
                this.error = true;
                this.errorReason = "HTTP response code is " + conn.getResponseCode();
                return;
            }
            
            /* Read boundary from content type header. */
            String contentType = conn.getContentType();
            if (contentType != null)
            {
                String boundaryTag = "boundary=";
                int pos = contentType.indexOf(boundaryTag);
                if (pos > 0)
                {
                    this.boundary = "--" + contentType.substring(pos + boundaryTag.length());
                    
                    /* If there are further contain type information, strip from boundary tag. */
                    int s = this.boundary.indexOf(';');
                    if (s > 0) this.boundary = this.boundary.substring(0, s);
                    
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
                 * FF D8<buf bytes>
                 * ...
                 * ...
                 * <buf bytes>FF D9
                 * ---
                 * where FF D8 and FF D9 are start and buf markers respectively.
                 */
                
                if (!this.skipToNextFrame(in)) break;
                
                String mime = this.readContentType(in);
                if (mime == null) break;
                
                int size = this.readContentLength(in);
                if (size < 0) break;
                
                /* An addition blank line. */
                this.readStreamLine(in);
                
                /* Read buf bytes. */
                byte image[] = new byte[size];
                int r, read = 0;
                
                while (read < size && (r = in.read(image, read, size - read)) > 0)
                {
                    read += r;
                    if (this.stop) break;
                }
                
                if (size != read)
                {
                    this.logger.warning("Failed to fully read buf bytes for stream " + this.config.name + 
                            ", read " + read + " of " + size + " bytes.");
                    this.error = true;
                    this.errorReason = "Failed to read buf bytes";
                    break;
                }
                
                /* Validate received frame is correct. */
                if (mime.equalsIgnoreCase("jpeg") &&
                    !(image[0] == 0xFF && image[1] == 0xD8 && image[size - 2] == 0xFF && image[size - 1] == 0xD9))
                {
                    this.logger.info("Received JPEG buf for " + this.config.name + " has incorrect SOI and EOI "
                            + "marker bytes, discarding frame as it may be corrupt.");
                    continue;
                }
                
                synchronized (this)
                {                
                    this.frame = new Frame(mime, image, sequence++);
                    this.notifyAll();
                }
            }
            
            /* Finished reading, through clean shutdown or otherwise, close stream. */
            conn.getInputStream().close();
        }
        catch (IOException e)
        {
            this.logger.warning("Error reading source stream " + this.config.name + ", error " + 
                    e.getClass().getSimpleName() + ": " + e.getMessage());
            this.error = true;
            this.errorReason = "Error reading stream " + this.config.name + ", error " + e.getClass().getSimpleName() + 
                    ": " + e.getMessage(); 
        }
        finally 
        {
            synchronized (this)
            {
                /* If any listeners are still waiting, wake them up. */
                this.notifyAll();
            }
        }
    }
    
    /**
     * Adds basic authentication header to camera connection.
     * 
     * @param conn connection to camera
     * @return whether authentication header added
     */
    private boolean addBasicAuth(HttpURLConnection conn)
    {
        if (!(this.config.authParams.containsKey("username") && this.config.authParams.containsKey("password")))
        {
            this.logger.severe("Cannot add basic authentication to camera request because the username or " +
                     "password was not correctly configured.");
            this.error = true;
            this.errorReason = "";
            return false;
        }
       
        String encoded = Base64.getMimeEncoder().encodeToString((
                this.config.authParams.get("username") + ':' + this.config.authParams.get("password")).getBytes());
       
        conn.addRequestProperty("Authorization", "Basic " + encoded); 
        return true;
    }

    /**
     * Stops the reading source stream.
     */
    public void stop()
    {
        if (!this.isReading()) return;
        
        try
        {
            this.logger.info("Stopping reading thread for " + this.config.name);
            this.stop = true;
            this.readThread.join(30000);
            
            if (this.readThread.isAlive()) this.logger.warning("Failed to stop reading thread for " + 
                    this.config.name + " in 30 seconds.");
        }
        catch (Exception e)
        { 
            this.logger.severe("Error stopping stream reading, " + e.getClass().getName() + ": " + e.getMessage());
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
     * Gets the name of this stream.
     * 
     * @return name of stream
     */
    public String getName()
    {
        return this.config.name;
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
        return this.readThread != null && this.readThread.isAlive();
    }
    
    /**
     * Returns the number of reading streams.
     * 
     * @return number of accessing streams
     */
    public int numStreams()
    {
        return this.destinations.size();
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
     * Checks whether the stream has been disabled.
     * 
     * @return true if disabled
     */
    public boolean isDisabled()
    {
        return this.disabled;
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
