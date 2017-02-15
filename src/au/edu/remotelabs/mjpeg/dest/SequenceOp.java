/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 15th February 2017
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import au.edu.remotelabs.mjpeg.source.Frame;

/**
 * Operation to add sequence number to frame image.
 */
public class SequenceOp implements TransformOp
{
    /* Font used for drawing number. */
    private final Font font;
    
    /** Color for text. */
    private Color color;
    
    public SequenceOp()
    {
        this.font = Font.decode("Arial-BOLD-24");
    }
    
    @Override
    public boolean configure(String param)
    {
        try
        {
            int rgba[] = { 0xFF, 0xFF, 0xFF, 0xFF };
            
            if (param != null && param.length() > 0)
            {
                if (param.startsWith("rgb("))
                {
                    int s = 0, e = 3;
                    for (int i = 0; i < 3; i++)
                    {
                        s = e + 1;
                        e = param.indexOf(',', s);
                        if (e < s) e = param.length() - 1;
                        rgba[i] = Integer.parseInt(param.substring(s, e).trim());       
                    }
                }
                else if (param.startsWith("rgba("))
                {
                    int s = 0, e = 4;
                    for (int i = 0; i < 4; i++)
                    {
                        s = e + 1;
                        e = param.indexOf(',', s);
                        if (e < s) e = param.length() - 1;
                        rgba[i] = Integer.parseInt(param.substring(s, e).trim());
                    }
                }
                else
                {
                    /* Assume hex format. */
                    for (int i = 0; i < 3; i++)
                    {
                        rgba[i] = Integer.parseInt(param.substring(2 * i, 2 * i + 2), 16);   
                    }
                }
            }
            
            this.color = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
            return true;
        }
        catch (NumberFormatException | IndexOutOfBoundsException ex)
        {
            /* Invalid color format. */
            return false;
        }
    }    

    @Override
    public BufferedImage apply(BufferedImage image, Frame frame) throws IOException
    {
        Graphics2D canvas = image.createGraphics();
        canvas.setColor(this.color);
        canvas.setFont(font);
        canvas.drawString(String.valueOf(frame.getSequence()), 40, 40);
        canvas.dispose();
        
        return image;
    }

}
