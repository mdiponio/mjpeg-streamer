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
    public BufferedImage apply(BufferedImage image, Frame frame) throws IOException
    {
        /* Algorithm from:
         * http://www.tannerhelland.com/4743/simple-algorithm-correcting-lens-distortion/. */   
        int wid = image.getWidth();
        int hei = image.getHeight();
        
        int halfWid = wid / 2; 
        int halfHei = hei / 2;
        
        double rad = Math.sqrt(wid * wid + hei * hei) / this.strength;
        
        int orig[] = image.getRGB(0, 0, wid, hei, null, 0, wid);
        int corrected[] = orig.clone();
        
        for (int y = 0; y < hei; y++)
        {
            for (int x = 0; x < wid; x++)
            {
                int nx = x - halfWid;
                int ny = y - halfHei;
                
                double r = Math.sqrt(nx * nx + ny * ny) / rad;
                
                double th;
                if (r == 0) 
                {
                    th = 1;
                }
                else
                {
                    th = Math.atan(r) / r;
                }
                
                int sx = (int)(halfWid + th * nx * this.zoom);
                int sy = (int)(halfHei + th * ny * this.zoom);
                
                /* Range check if correct position is outside frame, if so clamp to 
                 * boundary pixel. e*/
                if (sx < 0) sx = 0;
                if (sx >= wid) sx = wid - 1;
                if (sy < 0) sy = 0;
                if (sy >= hei) sy = hei - 1;
                corrected[y * wid + x] = orig[sy * wid + sx];
            }
        }
        
        BufferedImage newImage = new BufferedImage(wid, hei, image.getType());
        newImage.setRGB(0, 0, wid, hei, corrected, 0, wid);
        return newImage;
    }
}
