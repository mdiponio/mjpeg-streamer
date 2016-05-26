/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;

/**
 * Class that provides password protection for a stream.
 */
public class StreamAuthenticator
{
    /** Passwords to authenticate. */
    private final Map<String, String> passwords;
    
    public StreamAuthenticator(StreamerConfig config)
    {
        this.passwords = new HashMap<>();
        
        for (Stream s : config.getStreams().values())
        {
            if (s.protect)
            {
                if (s.password != null) 
                {
                    this.passwords.put(s.name, s.password);
                }
                else 
                {
                    this.passwords.put(s.name, this.generatePassword());
                }
            }
        }
    }
    
    /**
     * Checks whether the stream is required to be authenticated before returned.
     * 
     * @param stream stream configuration
     * @return true if authentication is required
     */
    public boolean requiresAuth(Stream stream)
    {
        return this.passwords.containsKey(stream.name);
    }
    
    /**
     * AUthenticates a request using the specified name and password. 
     * 
     * @param stream stream configuration
     * @param password password to authenticate stream
     * @return true if authenticated
     */
    public boolean authenticate(Stream stream, String password)
    {
        return !this.requiresAuth(stream) || 
               password != null && 
               password.equals(this.passwords.get(stream.name));
    }
    
    /**
     * Resets the password of a stream, generating and returning the
     * random password. If the stream is not configured to be resettable,
     * null is returned
     * 
     * 
     * @param stream stream configuration
     * @return newly generated password
     */
    public String reset(Stream stream)
    {
        if (!(stream.protect && stream.resettable)) return null;
            
        String pw = this.generatePassword();
        this.passwords.put(stream.name, pw);
        return pw;
    }
    
    /**
     * Generates a random password with 8 characters consisting numeric, lower
     * and upper case characters.
     * 
     * @return generated password
     */
    private String generatePassword()
    {
        Random random = new Random();
        char pwd[] = new char[8];
        
        for (int i = 0; i < 8; i++)
        {
            switch (random.nextInt(3))
            {
                case 0: pwd[i] = (char) (random.nextInt(10) + 48); break; /* Numeric characters. */
                case 1: pwd[i] = (char) (random.nextInt(26) + 65); break; /* Upper case characters. */
                case 2: pwd[i] = (char) (random.nextInt(26) + 97); break; /* Lower case characters. */
            }
        }
        return String.valueOf(pwd);
    }
}
