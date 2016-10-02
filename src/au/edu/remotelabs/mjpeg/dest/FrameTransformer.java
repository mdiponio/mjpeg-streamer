/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 9th June 2016
 */

package au.edu.remotelabs.mjpeg.dest;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import au.edu.remotelabs.mjpeg.source.Frame;
import au.edu.remotelabs.mjpeg.source.SourceStream;

/**
 * Applies transforms to a frame. 
 */
public class FrameTransformer
{    
    private static final Map<String, Class<? extends TransformOp>> TRANSFORMS = new HashMap<>(4);
    static {
        TRANSFORMS.put("barrel",    BarrelCorrectionOp.class);
        TRANSFORMS.put("quality",   QualityOp.class);
        TRANSFORMS.put("size",      ResizeOp.class);
        TRANSFORMS.put("timestamp", TimestampOp.class);
        TRANSFORMS.put("rotate",    RotateOp.class);
    }
    
    /** Name of source stream that is being transformed. */
    private final String name;
    
    /** Params list. */
    private final Map<String, String> params;
    
    /** Operations list. */
    private final List<TransformOp> ops;
    
    /** Encode quality of transformed frame. */
    private float encodeQuality;
    
    /** Time stamp of cached transformed frame. */
    private long timestamp;
    
    /** Cache of transformed frame. */
    private Frame cachedFrame;
    
    /** Frame transformer instances. */
    private static Map<FrameTransformer, Integer> instances = new HashMap<>();
    
    private FrameTransformer(String name, Map<String, String> request)
    {
        this.name = name;
        
        /* Default encode quality is source quality. */
        this.encodeQuality = 1.f;
        
        List<TransformOp> opsList = new ArrayList<>();
        Map<String, String> paramMap = new HashMap<>();
        
        for (Entry<String, String> p : request.entrySet())
        {
            if (TRANSFORMS.containsKey(p.getKey()))
            {
                try
                {
                    /* Create the transform. */
                    TransformOp op = TRANSFORMS.get(p.getKey()).newInstance();
                    op.configure(p.getValue());
                    opsList.add(op);
                    
                    /* Store the params parameter to allow transformer instances to
                     * be reused across identical params. */
                    paramMap.put(p.getKey(), p.getValue());
                }
                catch (InstantiationException | IllegalAccessException e)
                {
                    Logger.getLogger(getClass().getName()).severe("Bug, error instantiating transform operation '" +
                            p.getKey() + "', error " + e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        
        for (int i = 0; i < opsList.size(); i++)
        {
            Class<? extends TransformOp> opClass = opsList.get(i).getClass(); 
            if (opClass.equals(TimestampOp.class))
            {
                /* Time stamping should always be last because if it is sized or scaled, 
                 * the time stamp might be illegible. */
                TransformOp last = opsList.set(opsList.size() - 1, opsList.get(i));
                opsList.set(i, last);
            }
            else if (opClass.equals(QualityOp.class))
            {
                this.encodeQuality = ((QualityOp)opsList.get(i)).getEncodeQuality();
            }
        }
        
        this.ops = Collections.unmodifiableList(opsList);
        this.params = Collections.unmodifiableMap(paramMap);
    }
    
    /**
     * Checks whether transforms are required.  
     *  
     * @return true if transformations are required
     */
    public boolean isTransforming()
    {
        return this.ops.size() > 0;
    }
    
    /**
     * Apply all transforms.
     * 
     * @param frame frame to transform
     * @return transformed frame
     * @throws IOException error transforming
     */
    public synchronized Frame transform(Frame frame) throws IOException
    {
        /* If nothing to do no need to decode source. */
        if (!this.isTransforming()) return frame;
        
        /* If we have already transformed to frame, return the result instead of 
         * recomputing the frame. */
        if (this.cachedFrame != null && this.timestamp == frame.getTimestamp())
        {
            return this.cachedFrame;
        }
        
        BufferedImage image = frame.decodeImage();
        
        for (TransformOp op : this.ops)
        {
            image = op.apply(image);
        }
        
        this.timestamp = frame.getTimestamp();
        return this.cachedFrame = this.encode(frame, image);
    }
    
    /**
     * Return the source that is being transformed.
     * 
     * @return source name
     */
    public String getSourceName()
    {
        return this.name;
    }

    /**
     * Encodes the image as JPEG ready for output transmission.
     *  
     * @param image image to encode
     * @return frame encoded frame
     * @throws IOException error in encoding
     */
    private Frame encode(Frame orig, BufferedImage image) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        if (this.encodeQuality < 1)
        {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(this.encodeQuality);
        }
        
        writer.setOutput(new MemoryCacheImageOutputStream(out));
        writer.write(null, new IIOImage(image, null, null), param);

        return new Frame("image/jpeg", out.toByteArray());
    }
    
    /**
     * Match parameters used to create a transformer instance to allow reuse
     * of the same transformer in requests asking for the same thing. This is 
     * a strategy to remove duplicated computations.
     * 
     * @param name name of source stream
     * @param params request params
     * @return true if this matches requests
     */
    private boolean match(String name, Map<String, String> params)
    {
        /* Must be same source. */
        if (!this.name.equals(name)) return false;
        
        /* To match we need to do all operations requested and no more. */
        if (this.params.size() > params.size()) return false;
        
        Map<String, String> cp = new HashMap<>(params);
        for (Entry<String, String> p : this.params.entrySet())
        {
            if (!cp.containsKey(p.getKey()) || 
                !cp.get(p.getKey()).equals(p.getValue())) return false;
            
            cp.remove(p.getKey());
        }
        
        /* Make sure no operations remain. */
        for (String op : cp.keySet())
        {
            if (TRANSFORMS.containsKey(op)) return false;
        }
        
        /* Name and parameters match, request matches. */
        return true;
    }
    
    /**
     * Gets a frame transformer for the source and request.
     * 
     * @param source source stream
     * @param params request parameters
     * @return transformer instances
     */
    public static synchronized FrameTransformer get(SourceStream source, Map<String, String> params)
    {
        for (FrameTransformer tr : instances.keySet())
        {
            if (tr.match(source.getName(), params)) 
            {
                instances.replace(tr, instances.get(tr) + 1);
                return tr;
            }
        }
        
        /* Transformer does not exist, create it. */
        FrameTransformer tr = new FrameTransformer(source.getName(), params);
        instances.put(tr, 1);
        return tr;
    }
    
    /**
     * Release a transformer. 
     * 
     * @param FrameTransformer instance to remove
     */
    public static synchronized void unget(FrameTransformer instance)
    {
        int num = instances.get(instance);
        if (num > 1)
        {
            instances.replace(instance, num - 1);
        }
        else
        {
            instances.remove(instance);
        }
    }
}
