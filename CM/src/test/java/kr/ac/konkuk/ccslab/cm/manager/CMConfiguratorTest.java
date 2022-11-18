package kr.ac.konkuk.ccslab.cm.manager;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMConfigurator;

/**
 * @author mlim
 *
 */
public class CMConfiguratorTest {

	private CMInfo cmInfo;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		System.out.println("===== called setUpBeforeClass().");
	}

	@AfterClass
	public static void tearDownAfterClass() {
		System.out.println("===== called tearDownAfterClass().");
	}

	@Before
	public void setUp() {
		System.out.println("===== called setUp().");
		cmInfo = new CMInfo();
	}

	@After
	public void tearDown() {
		System.out.println("===== called tearDown().");
	}

	@Test
	public void testInit() {
		System.out.println("===== called testInit().");
		String confFilePath = "cm-server.conf";
		boolean ret = CMConfigurator.init(confFilePath, cmInfo);
		assertTrue(ret);
	}

}
