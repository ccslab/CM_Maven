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

import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;

/**
 * @author mlim
 *
 */
public class CMConfiguratorTest {

	private CMInfo cmInfo;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		System.out.println("-- called setUpBeforeClass().");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		System.out.println("-- called tearDownAfterClass().");
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		System.out.println("-- called setUp().");
		cmInfo = new CMInfo();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		System.out.println("-- called tearDown().");
	}

	@Test
	public void testInit() {
		System.out.println("-- called testInit().");
		String confFilePath = "src/test/resources/cm-server.conf";
		boolean ret = CMConfigurator.init(confFilePath, cmInfo);
		assertTrue(ret);
	}

}
