/**
 * 
 */
package junit;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;

/**
 * @author mlim
 *
 */
public class CMConfiguratorTest {

	private CMConfigurator conf;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("called setUpBeforeClass().");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		System.out.println("called tearDownAfterClass().");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("called setUp().");
		conf = new CMConfigurator();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		System.out.println("called tearDown().");
	}

	@Test
	public void test() {
		//fail("Not yet implemented");
		System.out.println("called test().");
		String testStr = "testStr";
		assertEquals(testStr, "testStr");		
	}
	
	@Test
	public void testInit() {
		assertEquals("test", "test");
	}

}
