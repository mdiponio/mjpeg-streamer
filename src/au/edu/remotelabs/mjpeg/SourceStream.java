/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;

/**
 * Decomposes a M-JPEG stream down to its frames.
 */
public class SourceStream implements Runnable
{
    /** Stream configuration. */
    private final Stream config;
    
    /** The boundary to discriminate between frames in M-Jpeg stream. This may
     *  be configured but will be overridden if boundary is present in the response
     *  HTTP header as is typically the case. */
    private String boundary;
    
    /** Whether the connection is being read from. */
    private boolean reading;
    
    /** Whether an error has occurred accessing the stream. */
    private boolean errored;
    
    /** The last error that occurred. */
    private String lastError;
    
    /** Thread which reads the stream. */
    private Thread readThread;
    
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
                    this.boundary = contentType.substring(pos + boundaryTag.length() + 1);
                    this.logger.info("Loaded stream " + this.config.name + " boundary as " + boundary);
                }
            }
            
            
            
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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
        return this.errored;
    }
    
    /**
     * Gets the error that occurred during reading source stream. 
     * 
     * @return reading error
     */
    public String getError()
    {
        return this.lastError;
    }
}
