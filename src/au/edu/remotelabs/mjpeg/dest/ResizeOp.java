/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 16th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Resize operation that resizes a frame to new dimensions optionally 
 * preserving aspect ratio of source frame. 
 */
public class ResizeOp implements TransformOp
{
    /** Desired width of output image. */
    private int width;
    
    /** Desired height of output image. */
    private int height;
    
    /** Whether to preserve aspect ratio of source. */
    private boolean preserveAspect;

    /** The transform configured to scale image by desired scaling factor. */
    private AffineTransform transform;

    @Override
    public boolean configure(String param)
    {
        /* Param format is <width>x<height[,keepRatio]. */
        try
        {
            int xp = param.indexOf('x');
            if (xp == -1) return false;
            
            this.width = Integer.parseInt(param.substring(0, xp));
            
            int cp = param.indexOf(',');
            if (cp > 0)
            {
                this.height = Integer.parseInt(param.substring(xp + 1, cp));
                this.preserveAspect = "keepRatio".equalsIgnoreCase(param.substring(cp + 1));
            }
            else
            {
                this.height = Integer.parseInt(param.substring(xp + 1));
                this.preserveAspect = false;
            }
        
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws IOException
    {
        if (this.transform == null)
        {
            this.transform = new AffineTransform();
            
            double wid = this.width, hei = this.height;
            if (this.preserveAspect)
            {
                /* To preserve aspect ratio, the smaller value between width and height scale will be
                 * used so that the aspect ratio will be kept and the dimension that is larger will 
                 * be ignored. */
                double scale = wid / image.getWidth() < hei / image.getHeight() ?
                        wid / image.getWidth() : hei / image.getHeight();
                this.transform.setToScale(scale, scale);
                this.width = (int)Math.floor(image.getWidth() * scale);
                this.height= (int)Math.floor(image.getHeight() * scale); 
            }
            else
            {
                this.transform.setToScale(wid / image.getWidth(), hei / image.getHeight());
            }
        }
        
        BufferedImage sizedImage = new BufferedImage(this.width, this.height, image.getType());
        Graphics2D canvas = sizedImage.createGraphics();
        canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        canvas.drawImage(image, this.transform, null);
        canvas.dispose();
        
        return sizedImage;
    }

}
