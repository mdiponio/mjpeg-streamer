/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;

/**
 * Configuration loader for streams and 
 */
public class StreamerConfig
{
	/** Administration page username. */
	private String username;
	
	/** Admin page password. */
	private String password;
	
	/** Whether to password protect stream access. */
	private boolean protect;
	
	/** Whether to access source streams on-demand of requesting streams. */
	private boolean ondemand;
	
	/** Whether stream passwords can be reset or are statically configured.. */
	private boolean allowReset;
	
	/** Password reset password. */
	private String resetPassword;
	
	/** Configured streams. */
	private Map<String, Stream> streams;
	
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
		
		/** Statically configured password to access this stream. This only 
		 *  applies if streams are protected and not using resettable passwords. */
		public final String password;
		
		/** Type of authentication to access source stream. */
		public final AuthType authType;
		
		/** Parameters such as username password pair to authenticate to source stream. */
		public final Map<String, String> authParams; 
		
		public Stream(String name, String url, String pass, String type, Map<String, String> auth)
				throws ServletException
		{
			Logger blog = Logger.getLogger(getClass().getName());
			
			this.name = name;
			this.password = pass;
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
				return new Stream(name, url, pass, type, auth); 
			}
		}
	}
    
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
        
        this.load();
    }
    
    private void load() throws ServletException
    {
    	this.username = "";
    	this.password = "";
    }
    
    public String getAdminUsername() { return this.username; }
    public String getAdminPassword() { return this.password; }
    public boolean isProtectingStreams() { return this.protect; }
    public boolean isOnDemand() { return this.ondemand; }
    public boolean allowPasswordResets() { return this.allowReset; }
    public String getResetPassword() { return this.resetPassword; }
    
    public Map<String, Stream> getStreams()
    {
    	return Collections.unmodifiableMap(this.streams);
    }
    
    public Stream getStream(String name)
    {
    	return this.streams.get(name);
    }
    
}
