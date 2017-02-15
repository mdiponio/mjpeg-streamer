/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 10th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import au.edu.remotelabs.mjpeg.source.Frame;

/**
 * Adds a time stamp to the top left of a frame.
 */
public class TimestampOp implements TransformOp
{
    /** Time formatter. */
    private final DateTimeFormatter formatter;
    
    /* Font used for drawing time. */
    private final Font font;
    
    public TimestampOp()
    {
        formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm:ss");
        font = Font.decode("Arial-BOLD-14");
    }
    

    @Override
    public boolean configure(String param)
    {
        /* Doesn't require any specific configuration. */
        return true;
    }

    @Override
    public BufferedImage apply(BufferedImage image, Frame frame) throws IOException
    {
        Graphics2D canvas = image.createGraphics();
        canvas.setColor(Color.BLACK);
        canvas.setFont(font);
        canvas.drawString(LocalDateTime.now().format(formatter), 10, 20);
        canvas.dispose();
        
        return image;
    }

}
