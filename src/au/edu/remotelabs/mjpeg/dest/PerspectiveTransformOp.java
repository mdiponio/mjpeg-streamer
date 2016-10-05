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
 * Transform that applies a perspective transformation to image.
 */
public class PerspectiveTransformOp implements TransformOp
{
    /** Perspective matrix. */
    private final double m[][] = new double[3][3];
    
    /** Width of image. */
    private int width;
    
    /** Height of image. */
    private int height;

    @Override
    public boolean configure(String param)
    {
        /* Format is m00, m01, m02, ..., m22. */
        String parts[] = param.split(",");
        if (parts.length != 9)
        {
            return false;
        }
        
        for (int i = 0; i < 3; i++)
        {
            for (int j = 0; j < 3; j++)
            {
                m[i][j] = Double.parseDouble(parts[i * 3 + j]);
            }
        }
        
        return true;
    }

    @Override
    public BufferedImage apply(BufferedImage image) throws IOException
    {
        this.width = image.getWidth(); 
        this.height = image.getHeight();
        
        int rgb[] = image.getRGB(0, 0, width, height, null, 0, width);
        
        int trans[] = new int[rgb.length];
        
        /* Perspective transform. */
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                double x1 = m[0][0] * x + m[0][1] * y + m[0][2];
                double y1 = m[1][0] * x + m[1][1] * y + m[1][2];
                double w =  m[2][0] * x + m[2][1] * y + m[2][2];
                
                int xh = (int)Math.floor(x1 / w);
                int yh = (int)Math.floor(y1 / w);

                if (xh < 0) xh = 0;
                if (xh >= width) xh = width - 1;
                if (yh < 0) yh = 0;
                if (yh >= height) yh = height - 1;
                
                trans[yh * width + xh] = rgb[y * width + x];
            }
        }
        
        /* Bilinear interpolation of fill missing lines. */
        int adjacent[] = new int[8];

        for (int y = 1; y < height - 1; y++)
        {
            for (int x = 1; x < width - 1; x++)
            {
                if (trans[y * width + x] != 0) continue;

                adjacent[0] = this.pick(trans, x, y, -1, -1);
                adjacent[1] = this.pick(trans, x, y, -1,  0);
                adjacent[2] = this.pick(trans, x, y, -1, -1);
                adjacent[3] = this.pick(trans, x, y,  0, -1);
                adjacent[4] = this.pick(trans, x, y,  0,  1);
                adjacent[5] = this.pick(trans, x, y,  1, -1);
                adjacent[6] = this.pick(trans, x, y,  1,  0);
                adjacent[7] = this.pick(trans, x, y,  1,  1);


                int px = 0;
                for (int m = 0; m < 3; m++)
                {
                    int ch = 0;
                    for (int i = 0; i < adjacent.length; i++)
                    {
                        ch += (adjacent[i] >> (8 * m)) & 0xFF;
                    }

                    px |= (ch / adjacent.length) << (8 * m);
                }

                trans[y * width + x] = px;
            }
        }
       
        BufferedImage transformed = new BufferedImage(width, height, image.getType());
        transformed.setRGB(0, 0, width, height, trans, 0, width);
        return transformed;
    }
    
    int pick(int[] rgb, int x, int y, int xd, int yd)
    {   
        int v = 0;
        do
        {
            x += xd;
            y += yd;
        }
        while (x > 0 && x < this.width && y > 0 && y < this.height && (v = rgb[y * this.width + x]) == 0);
        return v;
    }
}
