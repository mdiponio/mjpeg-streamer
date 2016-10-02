/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 28th September 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Barrel distortion correction operation. 
 */
public class BarrelCorrectionOp implements TransformOp
{
    /** Strength of distortion correction. */
    private double strength;
    
    /** Zoom factor. */
    private double zoom;

    @Override
    public boolean configure(String param)
    {
        /* Format is <strength>[,<zoom>]. */
        
        int p = param.indexOf(',');
        if (p > 0)
        {
            this.zoom = Double.parseDouble(param.substring(p + 1));
            param = param.substring(0, p);
        }
        else
        {
            this.zoom = 1;
        }
        
        this.strength = Double.parseDouble(param);
        return this.strength > 0;
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws IOException
    {
        /* Algorithm from:
         * http://www.tannerhelland.com/4743/simple-algorithm-correcting-lens-distortion/. */      
        int halfWid = image.getWidth() / 2; 
        int halfHei = image.getHeight() / 2;
        
        double rad = Math.sqrt(image.getWidth() * image.getWidth() + image.getHeight() * image.getHeight()) / this.strength;
        
        BufferedImage fixed = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        for (int y = 0; y < image.getHeight(); y++)
        {
            for (int x = 0; x < image.getWidth(); x++)
            {
                int nx = x - halfWid;
                int ny = y - halfHei;
                
                double r = Math.sqrt(nx * nx + ny + ny) / rad;
                
                double th;
                if (r == 0) th = 1;
                else
                {
                    th = Math.atan(r) / r;
                }
                
                int sx = (int)(halfWid + th * nx * this.zoom);
                int sy = (int)(halfHei + th * ny * this.zoom);
                
                if (sx >= image.getWidth()) sx = image.getWidth() - 1;
                if (sy >= image.getHeight()) sy = image.getHeight() - 1;
                fixed.setRGB(x, y, image.getRGB(sx, sy));
            }
        }

        return fixed;
    }
}
