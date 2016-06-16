/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 16th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * This class represents a quality transform but does not actually perform 
 * transformation work as quality manipulation occurs during encode phase
 * of transformation. It is used as a placeholder for request parameter 
 * parsing and matching.
 */
public class QualityOp implements TransformOp
{
    /** The quality of output encode between 0 and 1. */
    private float quality;

    @Override
    public boolean configure(String param)
    {
        /* Parameter should just be a integer specified quality percentage between 0 and
         * 100. */
        try
        {
            this.quality = Integer.parseInt(param) / 100.f;
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }
    
    /**
     * Gets the encode quality parameter.
     * 
     * @return encode quality
     */
    public float getEncodeQuality()
    {
        return this.quality;
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws IOException
    {
        /* This actually doesn't perform any tasks, as it quality occurs during
         * output encode. */
        return image;
    }

}
