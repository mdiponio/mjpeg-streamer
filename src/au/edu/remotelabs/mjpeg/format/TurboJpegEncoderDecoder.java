/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 17th February 2017
 */

package au.edu.remotelabs.mjpeg.format;

import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.libjpegturbo.turbojpeg.TJ;
import org.libjpegturbo.turbojpeg.TJCompressor;
import org.libjpegturbo.turbojpeg.TJDecompressor;
import org.libjpegturbo.turbojpeg.TJException;

public class TurboJpegEncoderDecoder implements EncoderDecoder
{

    /** Turbo JPEG Compressor. */
    private TJCompressor compressor;
    
    /** Turbo JPEG Decompressor. */
    private TJDecompressor decompressor;
    
    @Override
    public byte[] encode(BufferedImage image, int quality) throws Exception
    {
        if (this.compressor == null) this.compressor = new TJCompressor();
        this.compressor.setSourceImage(image, 0, 0, 0, 0);
        this.compressor.setSubsamp(1);
        this.compressor.setJPEGQuality(quality);
        return this.compressor.compress(TJ.FLAG_FASTDCT | TJ.SAMP_444);
    }

    @Override
    public BufferedImage decode(byte[] buf, int size) throws Exception
    {
       if (this.decompressor == null) this.decompressor = new TJDecompressor();
       this.decompressor.setSourceImage(buf, size);
       return this.decompressor.decompress(0, 0, BufferedImage.TYPE_INT_RGB, TJ.FLAG_FASTDCT | TJ.FLAG_FASTUPSAMPLE);
    }

    @Override
    public void dispose()
    {
        if (this.compressor != null) try
        {
            this.compressor.close();
        }
        catch (TJException e)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed disposing compressor", e);
        }
        
        if (this.decompressor != null) try
        {
            this.decompressor.close();
        }
        catch (TJException e)
        {
            Logger.getLogger(getClass().getName()).log(Level.WARNING, "Failed disposing decompressor", e);
        }
    }

}
