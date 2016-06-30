/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
 * Configuration for application.
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
    private Map<String, Stream> streams = new HashMap<>();

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
            this.logger.severe("Configuration file does not exist or is not readable. Configuration location: " + 
                    configFile.getAbsolutePath());
            throw new ServletException("Configuration file does not exist or is not readable.");
        }

        FileInputStream fis = null;
        try
        {
            fis = new FileInputStream(configFile);
            this.parse(fis);
        }
        catch (IOException ex)
        {
            this.logger.severe("Failed to read configuration file, error " + ex.getClass().getSimpleName() + 
                    ": " + ex.getMessage());
            throw new ServletException("Failed reading file.");
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch (IOException e)
            {
                this.logger.warning("Failed to close configuration file, error " + 
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
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
                if (reader.next() == XMLStreamConstants.START_ELEMENT)
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
            e.printStackTrace();
            this.logger.severe("Failed to parse configuration file, error " + e.getClass().getSimpleName() + 
                    ": " + e.getMessage());
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
        do 
        {
            if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)            
            {
                switch (reader.getLocalName())
                {
                case "username":
                    this.username = reader.getElementText();
                    break;
                    
                case "password":
                    this.password = reader.getElementText();
                    break;
                    
                case "apiSecret":
                    this.secret = reader.getElementText();
                    break;
                }
            }
        }
        while (reader.hasName() && !"security".equals(reader.getLocalName()));
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
        do
        {
            if (reader.nextTag() == XMLStreamConstants.START_ELEMENT &&
                    "stream".equals(reader.getLocalName()))
            {
                this.parseStream(reader);
            }
        }
        while (reader.hasNext() && !"streams".equals(reader.getLocalName()));
    }
    
    /**
     * Parse a specific stream configuration section.
     * 
     * @return reader XML reader
     * @throws XMLStreamException parse error
     * @throws ServletException missing information
     */
    public void parseStream(XMLStreamReader reader) throws XMLStreamException, ServletException
    {
        Stream.Builder builder = new Stream.Builder();
     
        do 
        {
            if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
            {
                switch (reader.getLocalName())
                {
                case "name":
                    builder.setName(reader.getElementText());
                    break;
                    
                case "url":
                    builder.setURL(reader.getElementText());
                    break;
                    
                case "access":
                    builder.setPassword(reader.getElementText());
                    break;
                    
                case "resettable":
                    builder.setResettable("true".equals(reader.getElementText()));
                    break;
                    
                case "ondemand":
                    builder.setOnDemand("true".equals(reader.getElementText()));
                    break;
                    
                case "protect":
                    builder.setProtected("true".equals(reader.getElementText()));
                    break;
                    
                case "auth":
                    do
                    {
                        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
                        {
                            if ("type".equals(reader.getLocalName()))
                            {
                                builder.setAuthType(reader.getElementText());
                            }
                            else 
                            {
                                builder.addAuthParam(reader.getLocalName(), reader.getElementText());
                            }
                        }
                    }
                    while (reader.hasNext() && !"auth".equals(reader.getLocalName()));
                    break;
                    
                case "format":
                    do
                    {
                        if (reader.nextTag() == XMLStreamConstants.START_ELEMENT)
                        {
                            builder.addFormatParam(reader.getLocalName(), reader.getElementText());
                        }
                    }
                    while (reader.hasNext() && "!format".equals(reader.getLocalName()));
                    break;
                    
                default:
                    this.logger.severe("Error in configuration file, unexpected tag: " + reader.getLocalName());
                    throw new ServletException("Unexpected tag reading configuration file.");
                }
            }
        }
        while (reader.hasNext() && !"stream".equals(reader.getLocalName()));
  
        
        this.streams.put(builder.name, builder.build());
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
        public enum AuthType {
            NONE,  // No Authentication 
            BASIC  // HTTP basic authentication
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
        
        /** Format specific options that help reading source stream. */
        public final Map<String, String> formatParams;

        Stream(String name, String url, String pass, boolean protect, boolean resettable, boolean ondemand, 
                String type, Map<String, String> auth, Map<String, String> format)
                throws ServletException
        {
            Logger lg = Logger.getLogger(getClass().getName());

            if ((this.name = name) == null)
            {
                lg.severe("Error in configuration, name not set.");
                throw new ServletException("No name set for stream.");
            }
            
            this.password = pass;
            this.resettable = resettable;
            this.ondemand = ondemand;
            this.protect = protect;

            if (url == null)
            {
                lg.severe("Error in configuration, source stream URL not set.");
                throw new ServletException("No source stream URL set.");
            }
            
            try 
            {
                this.source = new URL(url);
            } 
            catch (MalformedURLException e) 
            {
                lg.severe("Failed configuring stream "  + name + ", URL " + url + " is not valid.");
                throw new ServletException("Invalid source URL " + url + " for " + name, e);
            }

            try
            {
                this.authType = AuthType.valueOf(type);
            }
            catch (NullPointerException | IllegalArgumentException e)
            {
                lg.severe("Failed configuring stream " + name + ", authentication type " + type + " is not supported.");
                throw new ServletException("Invalid authentication " + type + " for " + name, e);
            }

            this.authParams = Collections.unmodifiableMap(auth);
            this.formatParams = Collections.unmodifiableMap(format);
        }

        static class Builder
        {
            private String name;
            private String url;
            private String pass;
            private String type = "NONE";      // Default source authentication is none 
            private Map<String, String> auth = new HashMap<>();
            private boolean protect = false;   // Default is not to protect streams
            private boolean ondemand = true;   // Default is on demand stream connection management
            private boolean resettable = true; // Default is resettable passwords
            private Map<String, String> format = new HashMap<>();

            Builder setName(String name)
            {
                this.name = name;
                return this;
            }

            Builder setURL(String url)
            {
                this.url = url;
                return this;
            }
            
            Builder setProtected(boolean protect)
            {
                this.protect = protect;
                return this;
            }
            
            Builder setResettable(boolean resettable)
            {
                this.resettable = resettable;
                return this;
            }
            
            Builder setOnDemand(boolean ondemand)
            {
                this.ondemand = ondemand;
                return this;
            }
            
            Builder setPassword(String password)
            {
                this.pass = password;
                return this;
            }

            Builder setAuthType(String type)
            {
                this.type = type;
                return this;
            }

            Builder addAuthParam(String name, String val)
            {
                this.auth.put(name, val);
                return this;
            }
            
            Builder addFormatParam(String name, String val)
            {
                this.format.put(name, val);
                return this;
            }

            public Stream build() throws ServletException
            {
                return new Stream(name, url, pass, protect, resettable, ondemand, type, auth, format); 
            }
        }
    }
}
