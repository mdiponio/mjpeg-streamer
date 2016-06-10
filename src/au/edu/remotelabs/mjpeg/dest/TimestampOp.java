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
    public void configure(String param)
    {
        /* Doesn't require any specific configuration. */
    }

    @Override
    public void apply(BufferedImage image, Graphics2D canvas) throws IOException
    {
        canvas.setColor(Color.BLACK);
        canvas.setFont(font);
        canvas.drawString(LocalDateTime.now().format(formatter), 10, 20);
        canvas.dispose();
    }

}
