/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 17th February 2017
 */

package au.edu.remotelabs.mjpeg.format;

import java.awt.image.BufferedImage;

/**
 * Encoder / decoder for image types.
 */
public interface EncoderDecoder
{
    /** Whether encoding / decoding with libjpeg-turbo. */
    static final boolean TURBO = true;
    
    
    /**
     * Encode image to format.
     * 
     * @param image raw image
     * @param quality compression quality
     * @return encoded image
     */
    public byte[] encode(BufferedImage image, int quality) throws Exception;
    
    /**
     * Decodes format to raw image.
     * @param buf format
     * @param size size of image in bytes
     * @return image 
     */
    public BufferedImage decode(byte[] buf, int size) throws Exception;
    
    /**
     * Dispose of encoder.
     */
    public void dispose();
    
    public static EncoderDecoder get()
    {
        if (TURBO) return new TurboJpegEncoderDecoder();
        else return new AWTJpegEncoderDecoder();
    }
}
