/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Configuration loader for streams and 
 */
public class StreamerConfig
{
    /** Administration page username. */
    private String username;

    /** Admin page password. */
    private String password;

    /** API secret. */
    private String secret;

    /** Configured streams. */
    private Map<String, Stream> streams;

    /** Logger. */
    private final Logger logger = Logger.getLogger(getClass().getName());

    public StreamerConfig(String path) throws ServletException
    {
        this(new File(path));
    }

    public StreamerConfig(File configFile) throws ServletException
    {
        if (!(configFile.exists() && configFile.canRead()))
        {
            throw new ServletException("Configuratile file does not exist or is not readable.");
        }

        
    }
    
    /**
     * Parses the configuration file XML.
     * 
     * @param in file input stream
     * @throws ServletException error in file
     */
    private void parse(InputStream in) throws ServletException
    {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        
        try
        {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            
            do
            {
                int evt = reader.nextTag();
                
                if (evt == XMLStreamConstants.START_ELEMENT)
                {
                    switch (reader.getLocalName())
                    {
                    case "streamer": // Root tag
                        break;
                    
                    case "security": // Security config
                        this.parseSecurity(reader);
                        break;
                        
                    case "streams":
                        this.parseStreams(reader);
                        break;
                        
                     default:
                        this.logger.severe("Parse error in configuration file, unexpected tag: " + reader.getLocalName());
                        throw new ServletException("Parse error in configuration file");
                    }
                }
            }
            while (reader.hasNext());
        }
        catch (XMLStreamException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    /**
     * Parse security information section.
     * 
     * @param reader XML reader
     * @throws XMLStreamException parse error
     */
    private void parseSecurity(XMLStreamReader reader) throws XMLStreamException
    {
        
    }
    
    /**
     * Parse streams configuration section.
     * 
     * @param reader XML reader
     * @throws XMLStreamException parse error
     * @throws ServletException missing information
     */
    private void parseStreams(XMLStreamReader reader) throws XMLStreamException, ServletException
    {
        
    }

    public String getAdminUsername() 
    { 
        return this.username; 
        
    }
    
    public String getAdminPassword() 
    {
        return this.password; 
    }
    
    public String getApiSecret()
    {
        return this.secret;
    }
  
    public Map<String, Stream> getStreams()
    {
        return Collections.unmodifiableMap(this.streams);
    }

    public Stream getStream(String name)
    {
        return this.streams.get(name);
    }

    /**
     * Stream configuration.
     */
    public static class Stream
    {
        /** Supported authentication types. */
        enum AuthType {
            NONE, // No Authentication 
            HTTP  // HTTP basic authentication
        }

        /** The name of the stream which forms part of access stream URLs. */
        public final String name;

        /** URL to source stream. */
        public final URL source;
        
        /** Whether this stream is protected by a password. */
        public final boolean protect;

        /** Statically configured password to access this stream. This only 
         *  applies if streams are protected and not using resettable passwords. */
        public final String password;
        
        /** Whether password is resettable on demand via REST API call. If a password
         *  is reset it will override the configured password. */
        public final boolean resettable;
        
        /** Whether the source stream is accessed on demand or continuously. */
        public final boolean ondemand;

        /** Type of authentication to access source stream. */
        public final AuthType authType;

        /** Parameters such as username password pair to authenticate to source stream. */
        public final Map<String, String> authParams; 

        Stream(String name, String url, String pass, boolean protect, boolean resettable, boolean ondemand, 
                String type, Map<String, String> auth)
                throws ServletException
        {
            Logger blog = Logger.getLogger(getClass().getName());

            if ((this.name = name) == null)
            {
                blog.severe("Error in stream configuration, name not set.");
                throw new ServletException("No name set for stream.");
            }
            this.password = pass;
            this.resettable = resettable;
            this.ondemand = ondemand;
            this.protect = protect;
            
            try 
            {
                this.source = new URL(url);
            } 
            catch (MalformedURLException e) 
            {
                blog.severe("Failed configuring stream "  + name + ", URL " + url + " is not valid.");
                throw new ServletException("Invalid source URL " + url + " for " + name, e);
            }

            try
            {
                this.authType = AuthType.valueOf(type);
            }
            catch (NullPointerException | IllegalArgumentException e)
            {
                blog.severe("Failed configuring stream " + name + ", authentication type " + type + " is not supported.");
                throw new ServletException("Invalid authentication " + type + " for " + name, e);
            }

            this.authParams = Collections.unmodifiableMap(auth);
        }

        static class StreamBuilder
        {
            private String name;
            private String url;
            private String pass;
            private String type;
            private Map<String, String> auth = new HashMap<>();
            private boolean protect = false;
            private boolean ondemand = true;
            private boolean resettable = true;

            StreamBuilder setName(String name)
            {
                this.name = name;
                return this;
            }

            StreamBuilder setURL(String url)
            {
                this.url = url;
                return this;
            }
            
            StreamBuilder setProtected(boolean protect)
            {
                this.protect = protect;
                return this;
            }
            
            StreamBuilder setResettable(boolean resettable)
            {
                this.resettable = resettable;
                return this;
            }
            
            StreamBuilder setOnDemand(boolean ondemand)
            {
                this.ondemand = ondemand;
                return this;
            }
            
            StreamBuilder setPassword(String password)
            {
                this.pass = password;
                return this;
            }

            StreamBuilder setAuthType(String type)
            {
                this.type = type;
                return this;
            }

            StreamBuilder addAuthParam(String name, String val)
            {
                this.auth.put(name, val);
                return this;
            }

            public Stream build() throws ServletException
            {
                return new Stream(name, url, pass, protect, resettable, ondemand, type, auth); 
            }
        }
    }
}
