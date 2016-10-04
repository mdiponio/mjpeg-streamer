/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 28th September 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** 
 * Rotation operation that rotates a frame a specified number of degrees or
 * radians.
 */
public class RotateOp implements TransformOp
{
    /** Amount of rotation radians. */
    private double rotation;
    
    /** New image width. */;
    private int width;
    
    /** New image height. */
    private int height;
    
    /** Whether to clip image. */
    private boolean clip;
    
    /** Transform to rotate image. */
    private AffineTransform transform;
    
    @Override
    public boolean configure(String param)
    {
        /* Param format is <rotation>[rad][,clip]. */
        int p = param.indexOf(',');
        if (p > 0)
        {
            this.clip = ",clip".equals(param.substring(p));
            param = param.substring(0, p);
        }
        
        p = param.indexOf("rad");
        if (p < 0)
        {
            /* Input parameter is specifying rotation in degrees. */
            this.rotation =  Double.parseDouble(param) * Math.PI / 180;
        }
        else
        {
            /* Input parameter is specifying rotation in radians. */
            this.rotation = Double.parseDouble(param.substring(0, p));
        }
        return true;
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws IOException
    {
        if (this.transform == null)
        {
            
            if (this.clip)
            {
                this.width = image.getWidth();
                this.height = image.getHeight();
            }
            else
            {
                this.width = (int)Math.round(Math.abs(image.getWidth() * Math.cos(this.rotation) + image.getHeight() * Math.sin(this.rotation)));
                this.height = (int)Math.round(Math.abs(image.getWidth() * Math.sin(this.rotation) + image.getHeight() * Math.cos(this.rotation)));
            }

            this.transform = new AffineTransform();
            this.transform.translate(this.width / 2, this.height / 2);
            this.transform.rotate(this.rotation);
            this.transform.translate(-image.getWidth() / 2, -image.getHeight() / 2);
        }
            
        BufferedImage rotated = new BufferedImage(this.width, this.height, image.getType());
        Graphics2D canvas = rotated.createGraphics();

        canvas.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        canvas.drawImage(image, this.transform, null);
        canvas.dispose();
        
        return rotated;
    }
}
