/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 28th September 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.image.BufferedImage;
import java.io.IOException;

import au.edu.remotelabs.mjpeg.source.Frame;

/**
 * Transformation operation which returns a region of the source frame.
 */
public class CropOp implements TransformOp
{
    /** Start offset x coordinate. */
    private int offsetX;
    
    /** Start offset y coordinate. */
    private int offsetY;
    
    /** Width of the region. */
    private int width;
    
    /** Height of region. */
    private int height;

    @Override
    public boolean configure(String param)
    {
        /* Input format is <x>,<y>,<width>,<height>. */
        
        String parts[] = param.split(",");
        if (parts.length != 4) return false;
        
        this.offsetX = Integer.parseInt(parts[0]);
        this.offsetY = Integer.parseInt(parts[1]);
        this.width = Integer.parseInt(parts[2]);
        this.height = Integer.parseInt(parts[3]);
        
        return true;
    }

    @Override
    public BufferedImage apply(BufferedImage image, Frame frame) throws IOException
    {
        if (this.offsetX + this.width > image.getWidth())
        {
            this.width = image.getWidth() - this.offsetX;
        }
        
        if (this.offsetY + this.height > image.getHeight())
        {
            this.height = image.getHeight() - this.offsetY;
        }
        
        return image.getSubimage(this.offsetX, this.offsetY, this.width, this.height);
    }

}
