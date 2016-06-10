/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 9th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * Operation that modifies image.
 */
public interface TransformOp
{
    /**
     * Configure operation using supplied request parameters.
     * 
     * @param param request parameter
     */
    void configure(String param);
    
    /**
     * Apply transformation to image.
     * 
     * @param image
     * @param canvas
     */
    void apply(BufferedImage image, Graphics2D canvas) throws IOException;
}
