package org.hive2hive.core.integration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.hive2hive.core.H2HJUnitTest;
import org.hive2hive.core.api.H2HNode;
import org.hive2hive.core.api.configs.FileConfiguration;
import org.hive2hive.core.api.configs.NetworkConfiguration;
import org.hive2hive.core.api.interfaces.IFileConfiguration;
import org.hive2hive.core.api.interfaces.IH2HNode;
import org.hive2hive.core.api.interfaces.INetworkConfiguration;
import org.hive2hive.core.exceptions.IllegalFileLocation;
import org.hive2hive.core.exceptions.NoPeerConnectionException;
import org.hive2hive.core.exceptions.NoSessionException;
import org.hive2hive.core.file.IFileAgent;
import org.hive2hive.core.security.UserCredentials;
import org.hive2hive.core.utils.FileTestUtil;
import org.hive2hive.core.utils.NetworkTestUtil;
import org.hive2hive.core.utils.TestExecutionUtil;
import org.hive2hive.core.utils.helper.TestFileAgent;
import org.hive2hive.processframework.interfaces.IProcessComponent;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Integration tests for the H2HNode.
 * 
 * @author Nico
 * 
 */
public class H2HNodeTest extends H2HJUnitTest {

	private static final int NETWORK_SIZE = 5;
	private static List<IH2HNode> network;
	private final Random random = new Random();

	private IH2HNode loggedInNode;
	private UserCredentials credentials;
	private TestFileAgent fileAgent;

	@BeforeClass
	public static void initTest() throws Exception {
		testClass = H2HNodeTest.class;
		beforeClass();
		network = NetworkTestUtil.createH2HNetwork(NETWORK_SIZE);
	}

	@AfterClass
	public static void cleanAfterClass() {
		NetworkTestUtil.shutdownH2HNetwork(network);
		afterClass();
	}

	@Before
	public void testRegisterLogin() throws IOException, NoPeerConnectionException {
		credentials = generateRandomCredentials();

		IH2HNode registerNode = network.get(random.nextInt(NETWORK_SIZE));
		IProcessComponent<?> registerProcess = registerNode.getUserManager().register(credentials);
		TestExecutionUtil.executeProcessTillSucceded(registerProcess);

		fileAgent = new TestFileAgent();
		loggedInNode = network.get(random.nextInt(NETWORK_SIZE / 2));
		IProcessComponent<?> loginProcess = loggedInNode.getUserManager().login(credentials, fileAgent);
		TestExecutionUtil.executeProcessTillSucceded(loginProcess);
	}

	@Test
	public void testAddDeleteFile() throws IOException, IllegalFileLocation, NoSessionException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException, NoPeerConnectionException {
		File testFile = new File(fileAgent.getRoot(), "test-file");
		FileUtils.write(testFile, "Hello World");

		IProcessComponent<?> process = loggedInNode.getFileManager().add(testFile);
		TestExecutionUtil.executeProcessTillSucceded(process);

		// is now added; delete it
		process = loggedInNode.getFileManager().delete(testFile);
		TestExecutionUtil.executeProcessTillSucceded(process);
	}

	@Test(expected = IllegalFileLocation.class)
	public void testAddFileWrongDir() throws IOException, NoSessionException, IllegalFileLocation, NoPeerConnectionException {
		File testFile = new File(FileTestUtil.getTempDirectory(), "test-file");
		FileUtils.write(testFile, "Hello World");

		loggedInNode.getFileManager().add(testFile);
	}

	@Test
	public void testAddFileTree() throws IOException, IllegalFileLocation, NoSessionException, NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException, NoPeerConnectionException {
		// /folder1/
		// /folder1/test1.txt
		// /folder1/folder2/test2.txt
		File folder1 = new File(fileAgent.getRoot(), "folder1");
		folder1.mkdir();
		File test1File = new File(folder1, "test1.txt");
		FileUtils.write(test1File, "Hello World 1");

		File folder2 = new File(folder1, "folder2");
		folder2.mkdir();
		File test2File = new File(folder2, "test2.txt");
		FileUtils.write(test2File, "Hello World 2");

		IProcessComponent<?> process = loggedInNode.getFileManager().add(folder1);
		TestExecutionUtil.executeProcessTillSucceded(process);

		// TODO wait for all async process to upload

		// then start 2nd client and login
		IFileAgent fileAgentUser2 = new TestFileAgent();
		IH2HNode newNode = network.get((random.nextInt(NETWORK_SIZE / 2) + NETWORK_SIZE / 2));

		IProcessComponent<?> loginProcess = newNode.getUserManager().login(credentials, fileAgentUser2);
		TestExecutionUtil.executeProcessTillSucceded(loginProcess);

		// TODO wait for login process to download all files

		// verfiy that all files are here
		folder1 = new File(fileAgentUser2.getRoot(), "folder1");
		Assert.assertTrue(folder1.exists());

		test1File = new File(folder1, "test1.txt");
		Assert.assertEquals("Hello World 1", FileUtils.readFileToString(test1File));

		folder2 = new File(folder1, "folder2");
		Assert.assertTrue(folder2.exists());

		test2File = new File(folder2, "test2.txt");
		Assert.assertEquals("Hello World 2", FileUtils.readFileToString(test2File));
	}

	@Test
	public void getPeer() {
		// a unconnected node does not provide a peer
		INetworkConfiguration config = NetworkConfiguration.createInitial();
		IFileConfiguration fileConfig = FileConfiguration.createDefault();
		IH2HNode node = H2HNode.createNode(config, fileConfig);
		Assert.assertNull(node.getPeer());

		// connected nodes return a peer
		Assert.assertNotNull(network.get(random.nextInt(NETWORK_SIZE)).getPeer());
	}

	@After
	public void logoutAndUnregister() throws NoSessionException, NoPeerConnectionException {
		IProcessComponent<?> process = loggedInNode.getUserManager().logout();
		TestExecutionUtil.executeProcessTillSucceded(process);

		// TODO unregister
	}
}
