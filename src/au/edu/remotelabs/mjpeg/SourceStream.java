/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.util.logging.Logger;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;

/**
 * Decomposes a M-JPEG stream down to its frames.
 */
public class SourceStream implements Runnable
{
    /** Stream configuration. */
    private final Stream config;
    
    /** Whether the connection is being read from. */
    private boolean isReading;
    
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
            
            this.readThread = new Thread(this);
            this.readThread.setName("Stream: " + this.config.name);
            this.readThread.start();
        }
    }

    @Override
    public void run()
    {
        
        
        
        
    }

}
