/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 22nd June 2016
 */

package au.edu.remotelabs.mjpeg;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.servlet.ServletException;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Holder for objects required to process stream requests with both HTTP
 * and web sockets.
 */
public class StreamerHolder
{
    /** Streams. */
    private final Map<String, SourceStream> streams;
    
    /** Streamer configuration. */
    private StreamerConfig config;
    
    /** Authenticator of streams. */
    private Authenticator authenticator;
    
    /** Logger. */
    private final Logger logger;
    
    /** Singleton instance. */
    private final static StreamerHolder holder = new StreamerHolder();
    
    public StreamerHolder()
    {
        this.logger = Logger.getLogger(getClass().getName());
        
        this.streams = new HashMap<>();
    }
    
    /**
     * Initialises holder.
     * 
     * @param configPath configuration file path
     * @throws ServletException 
     */
    public void init(String configPath) throws ServletException
    {
        this.config = new StreamerConfig(configPath);
        
        for (Stream stream : this.config.getStreams().values())
        {
            this.logger.fine("Loaded configuration for stream: " + stream.name);
            this.streams.put(stream.name, new SourceStream(stream));
        }
        
        this.authenticator = new Authenticator(this.config);
    }
    
    /**
     * Gets configuration for a stream.
     *  
     * @param stream stream name
     * @return stream configuration
     */
    public Stream getStreamConfig(String stream)
    {
        return this.config.getStream(stream);
    }
    
    /**
     * Gets the source stream for a stream. 
     * 
     * @param stream stream name
     * @return source stream
     */
    public SourceStream getStream(String stream)
    {
        return this.streams.get(stream);
    }
    
    /**
     * Gets authenticator to authenticate requests.
     * 
     * @return stream authenticator
     */
    public Authenticator getAuthenticator()
    {
        return this.authenticator;
    }
    
    /**
     * Gets all configuration.
     * 
     * @return configuration
     */
    public StreamerConfig getConfig()
    {
        return this.config;
    }
    
    /**
     * Destroys held objects.
     */
    public void destroy()
    {
        for (Entry<String, SourceStream> e : this.streams.entrySet())
        {
            e.getValue().stop();
        }
        
        this.streams.clear();
    }
    
    /**
     * Gets the singleton instance of the holder.
     */
    public static StreamerHolder get()
    {
        return holder;
    }
}
