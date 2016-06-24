/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 18th May 2016
 */

package au.edu.remotelabs.mjpeg;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;
import au.edu.remotelabs.mjpeg.ws.WebSocketOutput;

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
    
    /** Currently open connections. */
    private final Map<String, WebSocketOutput> connections;
    
    /** Logger. */
    private final Logger logger;
    
    public StreamerEndpoint()
    {
        this.logger = Logger.getLogger(getClass().getName());
        this.holder = StreamerHolder.get();
        this.connections = new HashMap<>();
    }

    @OnOpen
    public void start(Session session, EndpointConfig config)
    {
        Map<String, List<String>> params = session.getRequestParameterMap();
        
        try
        {
            if (!params.containsKey("stream"))
            {
                session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Missing stream param"));
                return;
            }
            
            Stream stream = this.holder.getStreamConfig(params.get("stream").get(0));
            if (stream == null)
            {
                session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Stream not found"));
                return;
            }
            
            String pw = params.containsKey("pw") ? params.get("pw").get(0) : null;
            if (!this.holder.getAuthenticator().authenticate(stream, pw))
            {
                session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, "Auth failed"));
                return;
            }
            
            
            this.logger.fine("Accepting web socket stream request for stream " + stream.name + 
                    ", session " + session.getId());
            
            WebSocketOutput out = new WebSocketOutput(session, this.holder.getStream(stream.name));
            out.setup();
            this.connections.put(session.getId(), out);
            
        }
        catch (IOException e)
        {
            this.logger.warning("Error accepting connection for session " + session.getId() + ", error" +
                    e.getClass().getName() + ": " + e.getMessage());
            try
            {
                session.close();
            }
            catch (IOException e1) { }
            this.release(session);
        }
    }
    
    @OnMessage
    public void incoming(String message, Session session)
    {   
        try
        {
            WebSocketOutput out = this.connections.get(session.getId());
            if (out == null)
            {
                session.close(new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "Stream not found"));
            }
            
            if ("p".equals(message)) out.pullFrame();
        }
        catch (IOException e)
        {
             this.logger.warning("Error responding to message for session " + session.getId() + ", error" +
                    e.getClass().getName() + ": " + e.getMessage());
            try
            {
                session.close();
            }
            catch (IOException e1) { }
            this.release(session);
        }
    }
    
    @OnClose
    public void end(Session session)
    {
        this.logger.fine("Closing streaming session " + session.getId());
        this.release(session);
    }    

    @OnError     
    public void error(Session session, Throwable thr)
    {
        this.logger.fine("Error for streaming session " + session.getId() + ", error " + 
                thr.getClass().getName() + ": " + thr.getMessage());
        this.release(session);
    }
    
    /**
     * Close the session.
     * 
     * @param session session that has been closed
     */
    private void release(Session session)
    {
        WebSocketOutput out = this.connections.remove(session.getId());
        if (out != null) out.close();
    }
}
