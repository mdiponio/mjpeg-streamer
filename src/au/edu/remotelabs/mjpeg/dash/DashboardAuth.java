/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 1st July 2016
 */

package au.edu.remotelabs.mjpeg.dash;

import java.util.Base64;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import au.edu.remotelabs.mjpeg.StreamerConfig;
import au.edu.remotelabs.mjpeg.StreamerHolder;

/** 
 * Basic authentication for the dash board pages.
 */
public class DashboardAuth
{   
    /**
     * Checks whether the request is authenticated. A request is authenticated
     * when either a session exists or a the browser has sent a HTTP BASIC login
     * with appropriate username and password configured.
     * 
     * @param request request object
     * @return whether authenticated
     */
    public static boolean authenticate(HttpServletRequest request)
    {
        HttpSession session = request.getSession();
        
        Object start = session.getAttribute("start");
        if (start != null && System.currentTimeMillis() - ((Date)start).getTime() / 1000 < 1800)
        {
            /* The session was previously logged in. */
            return true;
        }
        

        String auth = request.getHeader("Authorization");
        
        /* Only BASIC authentication is being supported. */
        if (auth == null || !auth.contains("Basic")) return false;

        /* Expected format is 'Basic <base64 encoded username:password>'. */
        int pos = auth.lastIndexOf(' ');
        if (pos < 0) return false;
        auth = auth.substring(pos + 1);
        
        StreamerConfig config = StreamerHolder.get().getConfig();
        String pass = Base64.getMimeEncoder().encodeToString(
                (config.getAdminUsername() + ':' + config.getAdminPassword()).getBytes());
        
        if (auth.equals(pass))
        {
            /* Auth is correct. */
            session.setAttribute("start", new Date());
            return true;
        }
        else
        {
            /* Authentication has failed, either bad username or password. */
            return false;
        }
    }
}
