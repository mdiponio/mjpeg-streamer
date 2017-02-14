/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 7th February 2017
 */

package au.edu.remotelabs.mjpeg.dest;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Stream output that provides a M-JPEG format, buffering frames if there are delays.
 * <br />
 * Takes the parameter 'wait', specifies the maximum number of frames to buffer before 
 * dropping frames.
 */
public class BufferedMJpegOutput extends MJpegOutput
{
    /** Queue for frames that need to sent. */
    private final ArrayBlockingQueue<Frame> sendQueue;
    
    /** Thread for sending frames. */
    private final Thread sendThread;
    
    public BufferedMJpegOutput(HttpServletResponse resp, Map<String, String> params, SourceStream source)
    {
        super(resp, params, source);
        
        /* Wait parameter specifies the maximum amount of frames to buffer before dropping. */
        this.sendQueue = new ArrayBlockingQueue<>(Integer.parseInt(params.getOrDefault("wait", "20")));
        
        this.sendThread = new Thread(() -> { sendFrames(); });
        this.sendThread.setDaemon(true);
        this.sendThread.start();
    }

    @Override
    protected void writeHeaders() throws ServletException, IOException
    {
        this.response.setContentType("multipart/x-mixed-replace;boundary=" + MJpegOutput.BOUNDARY);
    }


    @Override
    public boolean writeFrame(Frame frame) throws IOException
    {
        if (!this.sendQueue.offer(frame))
        {
            /* If no more space in the queue, we will need to drop frames to make space. */
            this.logger.info("Buffered M-Jpeg queue is full dropping oldest frame.");
            this.sendQueue.poll();      
            this.sendQueue.offer(frame);

        }

        return true;
    }
    
    private void sendFrames()
    { 
        try
        {
            for (;;)
            {
                Frame frame = this.sendQueue.take();
                
                this.sendFrame(frame);
            }
        }
        catch (InterruptedException | IOException e)
        {
            /* Expected if client disconnects. */
            this.stop();
        }
    }

    @Override
    public String getSuffix()
    {
        return "bjpg";
    }

}
