/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 17th February 2017
 */

package au.edu.remotelabs.mjpeg.format;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * Encoded / decoder that uses Java native AWT API.
 */
public class AWTJpegEncoderDecoder implements EncoderDecoder
{
    @Override
    public BufferedImage decode(byte buf[]) throws Exception
    {
        return ImageIO.read(new ByteArrayInputStream(buf));
    }
    
    @Override
    public byte[] encode(BufferedImage image, int quality) throws Exception
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        if (quality < 1)
        {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        
        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), param);
        return out.toByteArray();
    }
    
    @Override
    public void dispose()
    {
        /* Nothing to dispose. */
    }
}
