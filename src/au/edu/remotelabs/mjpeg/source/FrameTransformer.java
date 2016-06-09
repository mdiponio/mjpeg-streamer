/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 9th June 2016
 */

package au.edu.remotelabs.mjpeg.source;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.imageio.ImageIO;

/**
 * A mutable frame that allows transforms to be applied
 */
public class FrameTransformer
{
    /** Decoded image. */
    private BufferedImage image;
    
    /** Time formatter. */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy kk:mm:ss"); 
    
    /* Font used for drawing time. */
    private static final Font font = Font.decode("Arial-BOLD-14");
    
    public FrameTransformer(Frame source) throws IOException
    {
        this.image = ImageIO.read(new ByteArrayInputStream(source.buf));
    }

    /** 
     * Add a time stamp to the frame.
     */
    public void addTime() throws IOException
    {
        Graphics2D canvas = this.image.createGraphics();    
        canvas.setColor(Color.BLACK);
        canvas.setFont(font);
        canvas.drawString(LocalDateTime.now().format(formatter), 10, 20);
        canvas.dispose();
    }
    
    /**
     * Resize the image to the desired width or height. 
     * 
     * @param width new image width
     * @param height new image height
     */
    public void resize(int width, int height)
    {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        
        AffineTransform tr = new AffineTransform();
        
        this.image = resized;
    }
    
    public void setQuality(int quality)
    {
        
    }
 
    /**
     * Encodes the image as JPEG ready for output transmission.
     *  
     * @return frame encoded frame
     * @throws IOException error in encoding
     */
    public Frame encode() throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(this.image, "jpg", out);
        byte buf[] = out.toByteArray();
        return new Frame("image/jpeg", buf);
    }
}
