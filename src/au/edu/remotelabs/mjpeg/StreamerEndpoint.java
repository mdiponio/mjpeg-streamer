/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

/**
 * Web socket end point that streams M-Jpeg source frames (with transformations
 * as the HTTP version) either binary encoded or based 64 encoded (to generate
 * data URLs).
 */
@ServerEndpoint("/ws")
public class StreamerEndpoint 
{
    /** Holder of streamer objects. */
    private final StreamerHolder holder;
    
    /** Logger. */
    private final Logger logger;
    
    public StreamerEndpoint()
    {
        this.logger = Logger.getLogger(getClass().getName());
        this.holder = StreamerHolder.get();
    }

    @OnOpen
    public void start(Session session, EndpointConfig config)
    {
        
    }
    
    @OnMessage
    public void incoming(String message, Session session)
    {   
        System.out.println("Incoming message: " + message);
        try
        {
            session.getBasicRemote().sendText("Received: " + message);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    @OnClose
    public void end()
    {
        System.out.println("Conversion has finished.");
    }    

    @OnError     
    public void error(Throwable thr)
    {
        System.out.println("Conversation error");
        thr.printStackTrace();
    }
}
