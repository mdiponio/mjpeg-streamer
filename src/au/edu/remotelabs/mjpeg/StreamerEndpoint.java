/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Endpoint to stream cameras using web sockets.
 */
@ServerEndpoint("/wsstreams/*")
public class StreamerEndpoint 
{

    @OnOpen
    public void start(Session arg0, EndpointConfig arg1)
    {
        // TODO Auto-generated method stub

    }
    
    @OnMessage
    public void incoming(String message)
    {
        
    }
    
    @OnClose
    public void end()
    {
        
    }    

    @OnError     
    public void error(Throwable thr)
    {
        
    }
}
