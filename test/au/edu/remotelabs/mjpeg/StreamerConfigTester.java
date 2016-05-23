/**
 * MJPEG streaming application.
 * 
 * @author Michael Diponio <michael.diponio@uts.edu.au>
 * @date 20th May 2016
 */

package au.edu.remotelabs.mjpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Map;

import javax.servlet.ServletException;

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
        this.config = new StreamerConfig("./resources/test/good-config.xml");
        
        assertEquals("admin", this.config.getAdminUsername());
        assertEquals("passwd", this.config.getAdminPassword());
        assertEquals("asecret", this.config.getApiSecret());
        
        Map<String, Stream> streams = this.config.getStreams();
        assertEquals(2, streams.size());
        
        Stream shaker1 = streams.get("shaker1");
        assertNotNull(shaker1);
        
        assertEquals("shaker1", shaker1.name);
        assertEquals("http://138.25.49.22/camera1.mjpg", shaker1.source.toString());
        
        assertEquals(Stream.AuthType.HTTP, shaker1.authType);
        assertEquals(2, shaker1.authParams.size());
        assertEquals("user", shaker1.authParams.get("username"));
        assertEquals("pass", shaker1.authParams.get("password"));
        
        assertEquals(1, shaker1.formatParams.size());
        assertEquals("abc123", shaker1.formatParams.get("boundary"));
        
        assertTrue(shaker1.protect);
        assertTrue(shaker1.resettable);
        assertEquals("shake", shaker1.password);
        assertTrue(shaker1.ondemand);
        
        Stream ct1 = streams.get("coupledtanks1");
        assertNotNull(ct1);
        
        assertEquals("coupledtanks1", ct1.name);
        assertEquals("http://138.25.49.21/camera1.mjpg", ct1.source.toString());
        assertEquals(Stream.AuthType.NONE, ct1.authType);
        assertEquals(0, ct1.authParams.size());
        assertEquals(0, ct1.formatParams.size());
        assertFalse(ct1.protect);
        assertTrue(ct1.resettable);
        assertTrue(ct1.ondemand);
        assertNull(ct1.password);
    }

    @Test
    public void testMinimalConfig() throws Exception
    {
        this.config = new StreamerConfig("./resources/test/minimal-config.xml");
        
        assertEquals("admin", this.config.getAdminUsername());
        assertEquals("passwd", this.config.getAdminPassword());
        assertEquals("asecret", this.config.getApiSecret());
        
        Map<String, Stream> streams = this.config.getStreams();
        assertEquals(1, streams.size());
        
        Stream c = streams.get("ct");
        assertNotNull(c);
        
        assertEquals("ct", c.name);
        assertEquals("http://localhost/camera1.mjpg", c.source.toString());
        
        assertEquals(0, c.authParams.size());
        assertEquals(0, c.formatParams.size());
        assertFalse(c.protect);
        assertTrue(c.resettable);
        assertTrue(c.ondemand);
        assertNull(c.password);
    }
    
    @Test
    public void testBadConfig()
    {
        try
        {
            this.config = new StreamerConfig("./resources/test/bad-config.xml");
            fail("Servlet exception not thrown");
        }
        catch (ServletException e)
        {
            /* Expected result. */
        }
    }
}
