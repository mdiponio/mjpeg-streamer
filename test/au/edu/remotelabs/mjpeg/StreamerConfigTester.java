/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 20th May 2016
 */

package au.edu.remotelabs.mjpeg;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import au.edu.remotelabs.mjpeg.StreamerConfig.Stream;

/**
 * Tests the StreamerConfig class.
 */
public class StreamerConfigTester
{
    private StreamerConfig config;
    
    @Before
    public void setUp() throws Exception
    {
    }

    @Test
    public void test() throws Exception
    {
        this.config = new StreamerConfig("./WebContent/META-INF/streams-config.xml");
        
        assertEquals("admin", this.config.getAdminUsername());
        assertEquals("passwd", this.config.getAdminPassword());
        assertEquals("asecret", this.config.getApiSecret());
        
        Map<String, Stream> streams = this.config.getStreams();
        assertEquals(2, streams.size());
        
    }

}
