/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 9th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

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
     * @return whether configuration was successful from supplied param
     */
    boolean configure(String param);
    
    /**
     * Apply transformation to image.
     * 
     * @param image image to transform
     * @return transform image which may not be the same as param image  
     */
    BufferedImage apply(BufferedImage images) throws IOException;
}
