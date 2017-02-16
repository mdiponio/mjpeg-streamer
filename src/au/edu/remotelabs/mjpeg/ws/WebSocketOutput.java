/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 23rd June 2016
 */

package au.edu.remotelabs.mjpeg.ws;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import au.edu.remotelabs.mjpeg.dest.FrameTransformer;
import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Outputs frames to the web socket either as JPEG binary blobs or as HTML
 * data URLs. 
 */
public class WebSocketOutput
{
    /** Web socket remote end point. */
    private final RemoteEndpoint.Basic endpoint;
    
    /** Source being sent out. */
    private final SourceStream source;
    
    /** Transformer to generate output frames. */
    private final FrameTransformer transformer;
    
    /** Parameters requested. */
    private Map<String, String> requestParams;
    
    /** Whether to output binary frames or data urls. */
    private final boolean binary;

    public WebSocketOutput(Session session, SourceStream source)
    {
        this.source = source;
        
        Map<String, List<String>> params = session.getRequestParameterMap();
        this.requestParams = new HashMap<>(params.size());
        params.forEach((String k, List<String> v) -> this.requestParams.put(k, v.get(0)));
        
        this.transformer = FrameTransformer.get(this.source, this.requestParams);
        
        this.binary = this.requestParams.containsKey("bin") && this.requestParams.get("bin").charAt(0) == 't';
        
        this.endpoint = session.getBasicRemote();
    }

    /**
     * Setup ready for streaming. 
     * 
     * @return whether setup was successful
     */
    public boolean setup()
    {
        return this.source.register(this);
    }
    
    /**
     * Sends a frame down the web socket connection.
     * 
     * @throws IOException error sending frame
     */
    public void pullFrame() throws Exception
    {
        Frame frame = this.source.nextFrame();
        if (frame == null) return;

        frame = this.transformer.transform(frame);

        if (this.binary)
        {
            OutputStream out = this.endpoint.getSendStream();
            frame.writeTo(out);
            out.close();
        }
        else
        {
            Writer writer = this.endpoint.getSendWriter();
            writer.write("data:");
            writer.write(frame.getContentType());
            writer.write(";base64,");
            frame.writeTo(writer);
            writer.close();
        }
    }
    
    /**
     * Close this output releasing any held open resources.
     */
    public void close()
    {
        this.source.unregister(this);
        FrameTransformer.unget(this.transformer);
    }
}
