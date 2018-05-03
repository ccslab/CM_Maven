import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Console;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;

import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMGroupInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMPosition;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMSessionInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;

public class CMWinClient extends JFrame {

	private static final long serialVersionUID = 1L;
	//private JTextArea m_outTextArea;
	private JTextPane m_outTextPane;
	private JTextField m_inTextField;
	private JButton m_startStopButton;
	private JButton m_loginLogoutButton;
	private JPanel m_leftButtonPanel;
	private JScrollPane m_westScroll;
	private JButton m_composeSNSContentButton;
	private JButton m_readNewSNSContentButton;
	private JButton m_readNextSNSContentButton;
	private JButton m_readPreviousSNSContentButton;
	private JButton m_findUserButton;
	private JButton m_addFriendButton;
	private JButton m_removeFriendButton;
	private JButton m_friendsButton;
	private JButton m_friendRequestersButton;
	private JButton m_biFriendsButton;
	private MyMouseListener cmMouseListener;
	private CMClientStub m_clientStub;
	private CMWinClientEventHandler m_eventHandler;
	
	CMWinClient()
	{		
		MyKeyListener cmKeyListener = new MyKeyListener();
		MyActionListener cmActionListener = new MyActionListener();
		cmMouseListener = new MyMouseListener();
		setTitle("CM Client");
		setSize(600, 600);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		setLayout(new BorderLayout());

		m_outTextPane = new JTextPane();
		m_outTextPane.setBackground(new Color(245,245,245));
		//m_outTextPane.setForeground(Color.WHITE);
		m_outTextPane.setEditable(false);

		StyledDocument doc = m_outTextPane.getStyledDocument();
		addStylesToDocument(doc);
		add(m_outTextPane, BorderLayout.CENTER);
		JScrollPane centerScroll = new JScrollPane (m_outTextPane, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//add(centerScroll);
		getContentPane().add(centerScroll, BorderLayout.CENTER);
		
		m_inTextField = new JTextField();
		m_inTextField.addKeyListener(cmKeyListener);
		add(m_inTextField, BorderLayout.SOUTH);
		
		JPanel topButtonPanel = new JPanel();
		topButtonPanel.setBackground(new Color(220,220,220));
		topButtonPanel.setLayout(new FlowLayout());
		add(topButtonPanel, BorderLayout.NORTH);
		
		m_startStopButton = new JButton("Start Client CM");
		//m_startStopButton.setBackground(Color.LIGHT_GRAY);	// not work on Mac
		m_startStopButton.addActionListener(cmActionListener);
		//add(startStopButton, BorderLayout.NORTH);
		topButtonPanel.add(m_startStopButton);
		
		m_loginLogoutButton = new JButton("Login");
		m_loginLogoutButton.addActionListener(cmActionListener);
		topButtonPanel.add(m_loginLogoutButton);
		
		m_leftButtonPanel = new JPanel();
		m_leftButtonPanel.setBackground(new Color(220,220,220));
		m_leftButtonPanel.setLayout(new BoxLayout(m_leftButtonPanel, BoxLayout.Y_AXIS));
		add(m_leftButtonPanel, BorderLayout.WEST);
		m_westScroll = new JScrollPane (m_leftButtonPanel, 
				   JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		//add(westScroll);
		getContentPane().add(m_westScroll, BorderLayout.WEST);

		Border lineBorder = BorderFactory.createLineBorder(Color.BLACK);
		TitledBorder titledBorder = BorderFactory.createTitledBorder(lineBorder, "SNS");
		JPanel snsPanel = new JPanel();
		snsPanel.setLayout(new BoxLayout(snsPanel, BoxLayout.Y_AXIS));
		snsPanel.setBorder(titledBorder);
		
		m_composeSNSContentButton = new JButton("Compose");
		m_composeSNSContentButton.addActionListener(cmActionListener);
		m_readNewSNSContentButton = new JButton("Read New");
		m_readNewSNSContentButton.addActionListener(cmActionListener);
		m_readNextSNSContentButton = new JButton("Read Next");
		m_readNextSNSContentButton.addActionListener(cmActionListener);
		m_readPreviousSNSContentButton = new JButton("Read Prev");
		m_readPreviousSNSContentButton.addActionListener(cmActionListener);
		m_findUserButton = new JButton("Find user");
		m_findUserButton.addActionListener(cmActionListener);
		m_addFriendButton = new JButton("Add Friend");
		m_addFriendButton.addActionListener(cmActionListener);
		m_removeFriendButton = new JButton("Remove Friend");
		//m_removeFriendButton.setMaximumSize(new Dimension(150,10));
		m_removeFriendButton.addActionListener(cmActionListener);
		m_friendsButton = new JButton("Friends");
		m_friendsButton.addActionListener(cmActionListener);
		m_friendRequestersButton = new JButton("Friend requests");
		//m_friendRequestersButton.setMaximumSize(new Dimension(150,10));
		m_friendRequestersButton.addActionListener(cmActionListener);
		m_biFriendsButton = new JButton("Bi-friends");
		m_biFriendsButton.addActionListener(cmActionListener);
		snsPanel.add(m_composeSNSContentButton);
		snsPanel.add(m_readNewSNSContentButton);
		snsPanel.add(m_readNextSNSContentButton);
		snsPanel.add(m_readPreviousSNSContentButton);
		snsPanel.add(m_findUserButton);
		snsPanel.add(m_addFriendButton);
		snsPanel.add(m_removeFriendButton);
		snsPanel.add(m_friendsButton);
		snsPanel.add(m_friendRequestersButton);
		snsPanel.add(m_biFriendsButton);
		m_leftButtonPanel.add(snsPanel);
		
		m_leftButtonPanel.setVisible(false);
		m_westScroll.setVisible(false);
		setVisible(true);

		m_clientStub = new CMClientStub();
		m_eventHandler = new CMWinClientEventHandler(m_clientStub, this);

		boolean bRet = m_clientStub.startCM();
		if(!bRet)
		{
			printStyledMessage("CM initialization error!\n", "bold");
		}
		else
		{
			printStyledMessage("Client CM starts.\n", "bold");
			printStyledMessage("Type \"0\" for menu.\n", "regular");
			// change the appearance of buttons in the client window frame
			setButtonsAccordingToClientState();
		}
		
		m_inTextField.requestFocus();
	}
	
	private void addStylesToDocument(StyledDocument doc)
	{
		Style defStyle = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

		Style regularStyle = doc.addStyle("regular", defStyle);
		StyleConstants.setFontFamily(regularStyle, "SansSerif");
		
		Style boldStyle = doc.addStyle("bold", defStyle);
		StyleConstants.setBold(boldStyle, true);
		
		Style linkStyle = doc.addStyle("link", defStyle);
		StyleConstants.setForeground(linkStyle, Color.BLUE);
		StyleConstants.setUnderline(linkStyle, true);
	}
	
	public CMClientStub getClientStub()
	{
		return m_clientStub;
	}
	
	public CMWinClientEventHandler getClientEventHandler()
	{
		return m_eventHandler;
	}
	
	// initialize button titles
	public void initializeButtons()
	{
		m_startStopButton.setText("Start Client CM");
		m_loginLogoutButton.setText("Login");
		m_leftButtonPanel.setVisible(false);
		m_westScroll.setVisible(false);
		revalidate();
		repaint();
	}
	
	// set button titles
	public void setButtonsAccordingToClientState()
	{
		int nClientState;
		nClientState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();
		
		// nclientState: CMInfo.CM_INIT, CMInfo.CM_CONNECT, CMInfo.CM_LOGIN, CMInfo.CM_SESSION_JOIN
		switch(nClientState)
		{
		case CMInfo.CM_INIT:
			m_startStopButton.setText("Stop Client CM");
			m_loginLogoutButton.setText("Login");
			m_leftButtonPanel.setVisible(false);
			m_westScroll.setVisible(false);
			break;
		case CMInfo.CM_CONNECT:
			m_startStopButton.setText("Stop Client CM");
			m_loginLogoutButton.setText("Login");
			m_leftButtonPanel.setVisible(false);
			m_westScroll.setVisible(false);
			break;
		case CMInfo.CM_LOGIN:
			m_startStopButton.setText("Stop Client CM");
			m_loginLogoutButton.setText("Logout");
			m_leftButtonPanel.setVisible(false);
			m_westScroll.setVisible(false);
			break;
		case CMInfo.CM_SESSION_JOIN:
			m_startStopButton.setText("Stop Client CM");
			m_loginLogoutButton.setText("Logout");
			m_leftButtonPanel.setVisible(true);
			m_westScroll.setVisible(true);
			break;
		default:
			m_startStopButton.setText("Start Client CM");
			m_loginLogoutButton.setText("Login");
			m_leftButtonPanel.setVisible(false);
			m_westScroll.setVisible(false);
			break;
		}
		revalidate();
		repaint();
	}
	
	public void printMessage(String strText)
	{
		/*
		m_outTextArea.append(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
		*/
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, null);
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public void printStyledMessage(String strText, String strStyleName)
	{
		StyledDocument doc = m_outTextPane.getStyledDocument();
		try {
			doc.insertString(doc.getLength(), strText, doc.getStyle(strStyleName));
			m_outTextPane.setCaretPosition(m_outTextPane.getDocument().getLength());

		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	public void printImage(String strPath)
	{
		int nTextPaneWidth = m_outTextPane.getWidth();
		int nImageWidth;
		int nImageHeight;
		int nNewWidth;
		int nNewHeight;

		File f = new File(strPath);
		if(!f.exists())
		{
			printMessage(strPath+"\n");
			return;
		}
		
		ImageIcon icon = new ImageIcon(strPath);
		Image image = icon.getImage();
		nImageWidth = image.getWidth(m_outTextPane);
		nImageHeight = image.getHeight(m_outTextPane);
		
		if(nImageWidth > nTextPaneWidth/2)
		{
			nNewWidth = nTextPaneWidth / 2;
			float fRate = (float)nNewWidth/(float)nImageWidth;
			nNewHeight = (int)(nImageHeight * fRate);
			Image newImage = image.getScaledInstance(nNewWidth, nNewHeight, java.awt.Image.SCALE_SMOOTH);
			icon = new ImageIcon(newImage);
		}
		
		m_outTextPane.insertIcon ( icon );
		printMessage("\n");
	}
	
	public void printFilePath(String strPath)
	{
		JLabel pathLabel = new JLabel(strPath);
		pathLabel.addMouseListener(cmMouseListener);
		m_outTextPane.insertComponent(pathLabel);
		printMessage("\n");
	}

	/*
	private void setMessage(String strText)
	{
		m_outTextArea.setText(strText);
		m_outTextArea.setCaretPosition(m_outTextArea.getDocument().getLength());
	}
	*/
	
	public void processInput(String strInput)
	{
		int nCommand = -1;
		try {
			nCommand = Integer.parseInt(strInput);
		} catch (NumberFormatException e) {
			//System.out.println("Incorrect command number!");
			printMessage("Incorrect command number!\n");
			return;
		}
		
		switch(nCommand)
		{
		case 0:
			printMessage("---------------------------------------------------\n");
			printMessage("0: help, 1: connect to default server, 2: disconnect from default server\n");
			printMessage("3: login to default server, 4: logout from default server\n");
			printMessage("5: request session info from default server, 6: join session of defalut server, 7: leave session of default server\n");
			printMessage("8: user position, 9: chat, 10: test CMDummyEvent, 11: test datagram message\n");
			printMessage("12: test CMUserEvent, 13: print group info, 14: print current user status\n");
			printMessage("15: change group, 16: add additional channel, 17: remove additional channel\n");
			printMessage("18: set file path, 19: request file, 20: push file\n");
			printMessage("62: cancel receiving file, 63: cancel sending file\n");
			printMessage("21: test forwarding schemes, 22: test delay of forwarding schemes\n");
			printMessage("---------------------------------------------------\n");
			printMessage("73: synchronously login to default server, 74: synchronously request session info\n");
			printMessage("75: synchronously join session of default server\n");
			printMessage("---------------------------------------------------\n");
			printMessage("23: SNS content download, 50: request attached file of SNS content\n");
			printMessage("24: test repeated downloading of SNS content, 25: SNS content upload\n");
			printMessage("26: register user, 27: deregister user\n");
			printMessage("28: find registered user, 29: add a new friend, 30: remove a friend\n");
			printMessage("31: request current friend list, 32: request friend requester list\n");
			printMessage("33: request bi-directional friends\n");
			printMessage("---------------------------------------------------\n");
			printMessage("34: request additional server info\n");
			printMessage("35: connect to a designated server, 36: disconnect from a designated server\n");
			printMessage("37: log in to a designated server, 38: log out from a designated server\n");
			printMessage("39: request session info from a designated server\n");
			printMessage("40: join a session of a designated server, 41: leave a session of a designated server\n");
			printMessage("42: print group info of a designated server\n");
			printMessage("---------------------------------------------------\n");
			printMessage("43: pull/push multiple files, 44: split a file, 45: merge files\n");
			printMessage("46: distribute a file and merge\n");
			printMessage("---------------------------------------------------\n");
			printMessage("47: multicast chat in current group\n");
			printMessage("48: get additional blocking socket channel\n");
			printMessage("60: test input network throughput, 61: test output network throughput\n");
			printMessage("99: terminate CM\n");
			break;
		case 1: // connect to default server
			testConnectionDS();
			break;
		case 2: // disconnect from default server
			testDisconnectionDS();
			break;
		case 3: // asynchronous login to default server
			testLoginDS();
			break;
		case 73: // synchronously login to default server
			testSyncLoginDS();
			break;
		case 4: // logout from default server
			testLogoutDS();
			break;
		case 5: // request session info from default server
			testSessionInfoDS();
			break;
		case 74: // synchronously request session info from default server
			testSyncSessionInfoDS();
			break;
		case 6: // join a session
			testJoinSession();
			break;
		case 75: // synchronously join a session
			testSyncJoinSession();
			break;
		case 7: // leave the current session
			testLeaveSession();
			break;
		case 8: // user position
			testUserPosition();
			break;
		case 9: // chat
			testChat();
			break;
		case 10: // test CMDummyEvent
			testDummyEvent();
			break;
		case 11: // test datagram message
			testDatagram();
			break;
		case 12: // test CMUserEvent
			testUserEvent();
			break;
		case 13: // print group info
			testPrintGroupInfo();
			break;
		case 14: // print current information about the client
			testCurrentUserStatus();
			break;
		case 15: // change current group
			testChangeGroup();
			break;
		case 16: // add additional channel
			testAddChannel();
			break;
		case 17: // remove additional channel
			testRemoveChannel();
			break;
		case 18: // set file path
			testSetFilePath();
			break;
		case 19: // request a file
			testRequestFile();
			break;
		case 20: // push a file
			testPushFile();
			break;
		case 21: // test forwarding schemes (typical vs. internal)
			testForwarding();
			break;
		case 22: // test delay of forwarding schemes
			testForwardingDelay();
			break;
		case 23: // test SNS content download
			testDownloadNewSNSContent();
			break;
		case 24: // test repeated downloading of SNS content
			testRepeatedSNSContentDownload();
			break;
		case 25: // test SNS content upload
			testSNSContentUpload();
			break;
		case 26: // register user
			testRegisterUser();
			break;
		case 27: // deregister user
			testDeregisterUser();
			break;
		case 28: // find user
			testFindRegisteredUser();
			break;
		case 29: // add a new friend
			testAddNewFriend();
			break;
		case 30: // remove a friend
			testRemoveFriend();
			break;
		case 31: // request current friends list
			testRequestFriendsList();
			break;
		case 32: // request friend requesters list
			testRequestFriendRequestersList();
			break;
		case 33: // request bi-directional friends
			testRequestBiFriendsList();
			break;
		case 34: // request additional server info
			testRequestServerInfo();
			break;
		case 35: // connect to a designated server
			testConnectToServer();
			break;
		case 36: // disconnect from a designated server
			testDisconnectFromServer();
			break;
		case 37: // log in to a designated server
			testLoginServer();
			break;
		case 38: // log out from a designated server
			testLogoutServer();
			break;
		case 39: // request session information from a designated server
			testRequestSessionInfoOfServer();
			break;
		case 40: // join a session of a designated server
			testJoinSessionOfServer();
			break;
		case 41: // leave a session of a designated server
			testLeaveSessionOfServer();
			break;
		case 42: // print current group info of a designated server
			testPrintGroupInfoOfServer();
			break;
		case 43: // pull or push multiple files
			testSendMultipleFiles();
			break;
		case 44: // split a file
			testSplitFile();
			break;
		case 45: // merge files
			testMergeFiles();
			break;
		case 46: // distribute a file and merge
			testDistFileProc();
			break;
		case 47: // test multicast chat in current group
			testMulticastChat();
			break;
		case 48: // get additional blocking socket channel
			testGetBlockSocketChannel();
			break;
		case 50: // request an attached file of SNS content
			testRequestAttachedFileOfSNSContent();
			break;
		case 60: // test input network throughput
			testMeasureInputThroughput();
			break;
		case 61: // test output network throughput
			testMeasureOutputThroughput();
			break;
		case 62:	// test cancel receiving a file
			cancelRecvFile();
			break;
		case 63:	// test cancel sending a file
			cancelSendFile();
			break;
		case 99: // terminate CM
			testTermination();
			break;
		default:
			System.out.println("Unknown command.");
			break;
		}
	}
	
	public void testConnectionDS()
	{
		//System.out.println("====== connect to default server\n");
		printMessage("====== connect to default server\n");
		boolean ret = m_clientStub.connectToServer();
		if(ret)
		{
			printMessage("Successfully connected to the default server.\n");
		}
		else
		{
			printMessage("Cannot connect to the default server!\n");
		}
		//System.out.println("======");
		printMessage("======\n");
	}
	
	public void testDisconnectionDS()
	{
		//System.out.println("====== disconnect from default server");
		printMessage("====== disconnect from default server\n");
		boolean ret = m_clientStub.disconnectFromServer();
		if(ret)
		{
			printMessage("Successfully disconnected from the default server.\n");
		}
		else
		{
			printMessage("Error while disconnecting from the default server!");
		}
		//System.out.println("======");
		printMessage("======\n");
		
		setButtonsAccordingToClientState();
		setTitle("CM Client");
	}
	
	public void testLoginDS()
	{
		String strUserName = null;
		String strPassword = null;
		String strEncPassword = null;
		boolean bRequestResult = false;

		printMessage("====== login to default server\n");
		JTextField userNameField = new JTextField();
		JPasswordField passwordField = new JPasswordField();
		Object[] message = {
				"User Name:", userNameField,
				"Password:", passwordField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Login Input", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION)
		{
			strUserName = userNameField.getText();
			strPassword = new String(passwordField.getPassword()); // security problem?
			// encrypt password
			strEncPassword = CMUtil.getSHA1Hash(strPassword);
			
			m_eventHandler.setStartTime(System.currentTimeMillis());
			bRequestResult = m_clientStub.loginCM(strUserName, strEncPassword);
			long lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
			if(bRequestResult)
			{
				printMessage("successfully sent the login request.\n");
				printMessage("return delay: "+lDelay+" ms.\n");
			}
			else
			{
				printStyledMessage("failed the login request!\n", "bold");
				m_eventHandler.setStartTime(0);
			}
		}
		
		printMessage("======\n");
	}
	
	public void testSyncLoginDS()
	{
		String strUserName = null;
		String strPassword = null;
		String strEncPassword = null;
		CMSessionEvent loginAckEvent = null;

		printMessage("====== synchronous login to default server\n");
		JTextField userNameField = new JTextField();
		JPasswordField passwordField = new JPasswordField();
		Object[] message = {
				"User Name:", userNameField,
				"Password:", passwordField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Login Input", JOptionPane.OK_CANCEL_OPTION);
		if (option == JOptionPane.OK_OPTION)
		{
			strUserName = userNameField.getText();
			strPassword = new String(passwordField.getPassword()); // security problem?
			// encrypt password
			strEncPassword = CMUtil.getSHA1Hash(strPassword);
			
			m_eventHandler.setStartTime(System.currentTimeMillis());
			loginAckEvent = m_clientStub.syncLoginCM(strUserName, strEncPassword);
			long lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
			if(loginAckEvent != null)
			{
				// print login result
				if(loginAckEvent.isValidUser() == 0)
				{
					printMessage("This client fails authentication by the default server!\n");		
				}
				else if(loginAckEvent.isValidUser() == -1)
				{
					printMessage("This client is already in the login-user list!\n");
				}
				else
				{
					printMessage("return delay: "+lDelay+" ms.\n");
					printMessage("This client successfully logs in to the default server.\n");
					CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
					
					// Change the title of the client window
					setTitle("CM Client ("+interInfo.getMyself().getName()+")");

					// Set the appearance of buttons in the client frame window
					setButtonsAccordingToClientState();
				}				
			}
			else
			{
				printStyledMessage("failed the login request!\n", "bold");
			}
			
		}
		
		printMessage("======\n");		
	}

	public void testLogoutDS()
	{
		boolean bRequestResult = false;
		printMessage("====== logout from default server\n");
		bRequestResult = m_clientStub.logoutCM();
		if(bRequestResult)
			printMessage("successfully sent the logout request.\n");
		else
			printStyledMessage("failed the logout request!\n", "bold");
		printMessage("======\n");

		// Change the title of the login button
		setButtonsAccordingToClientState();
		setTitle("CM Client");
	}

	public void testTermination()
	{
		m_clientStub.disconnectFromServer();
		m_clientStub.terminateCM();
		printMessage("Client CM terminates.\n");
		// change the appearance of buttons in the client window frame
		initializeButtons();
		setTitle("CM Client");
	}

	public void testSessionInfoDS()
	{
		boolean bRequestResult = false;
		printMessage("====== request session info from default server\n");
		m_eventHandler.setStartTime(System.currentTimeMillis());
		bRequestResult = m_clientStub.requestSessionInfo();
		long lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
		if(bRequestResult)
		{
			printMessage("successfully sent the session-info request.\n");
			printMessage("return delay: "+ lDelay +" ms.\n");
		}
		else
			printStyledMessage("failed the session-info request!\n", "bold");
		printMessage("======\n");
	}
	
	public void testSyncSessionInfoDS()
	{
		CMSessionEvent se = null;
		printMessage("====== synchronous request session info from default server\n");
		m_eventHandler.setStartTime(System.currentTimeMillis());
		se = m_clientStub.syncRequestSessionInfo();
		long lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
		if(se == null)
		{
			printStyledMessage("failed the session-info request!\n", "bold");
			return;
		}
		
		printMessage("return delay: "+ lDelay +" ms.\n");
		
		// print the request result
		Iterator<CMSessionInfo> iter = se.getSessionInfoList().iterator();

		printMessage(String.format("%-60s%n", "------------------------------------------------------------"));
		printMessage(String.format("%-20s%-20s%-10s%-10s%n", "name", "address", "port", "user num"));
		printMessage(String.format("%-60s%n", "------------------------------------------------------------"));

		while(iter.hasNext())
		{
			CMSessionInfo tInfo = iter.next();
			printMessage(String.format("%-20s%-20s%-10d%-10d%n", tInfo.getSessionName(), tInfo.getAddress(), 
					tInfo.getPort(), tInfo.getUserNum()));
		}
	
		printMessage("======\n");		
	}

	public void testJoinSession()
	{
		String strSessionName = null;
		boolean bRequestResult = false;
		printMessage("====== join a session\n");
		strSessionName = JOptionPane.showInputDialog("Session Name:");
		if(strSessionName != null)
		{
			m_eventHandler.setStartTime(System.currentTimeMillis());
			bRequestResult = m_clientStub.joinSession(strSessionName);
			long lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
			if(bRequestResult)
			{
				printMessage("successfully sent the session-join request.\n");
				printMessage("return delay: "+lDelay+" ms.\n");
			}
			else
				printStyledMessage("failed the session-join request!\n", "bold");
		}
		printMessage("======\n");
	}
	
	public void testSyncJoinSession()
	{
		CMSessionEvent se = null;
		String strSessionName = null;
		printMessage("====== join a session\n");
		strSessionName = JOptionPane.showInputDialog("Session Name:");
		if(strSessionName != null)
		{
			m_eventHandler.setStartTime(System.currentTimeMillis());
			se = m_clientStub.syncJoinSession(strSessionName);
			long lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
			if(se != null)
			{
				setButtonsAccordingToClientState();
				// print result of the request
				printMessage("successfully joined a session that has ("+se.getGroupNum()+") groups.\n");
				printMessage("return delay: "+lDelay+" ms.\n");
			}
			else
			{
				printStyledMessage("failed the session-join request!\n", "bold");
			}
		}
				
		printMessage("======\n");		
	}

	public void testLeaveSession()
	{
		boolean bRequestResult = false;
		printMessage("====== leave the current session\n");
		bRequestResult = m_clientStub.leaveSession();
		if(bRequestResult)
			printMessage("successfully sent the leave-session request.\n");
		else
			printStyledMessage("failed the leave-session request!\n", "bold");
		printMessage("======\n");
		setButtonsAccordingToClientState();
	}

	public void testUserPosition()
	{
		CMPosition position = new CMPosition();
		//String strLine = null;
		//String strDelim = "\\s+";
		//String[] strTokens;
		//System.out.println("====== send user position");
		printMessage("====== send user position\n");
		/*
		System.out.print("pos (x,y,z): ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			strLine = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		strLine.trim();
		strTokens = strLine.split(strDelim);
		position.m_p.m_x = Float.parseFloat(strTokens[0]);
		position.m_p.m_y = Float.parseFloat(strTokens[1]);
		position.m_p.m_z = Float.parseFloat(strTokens[2]);
		System.out.println("Pos input: ("+position.m_p.m_x+", "+position.m_p.m_y+", "+position.m_p.m_z+")");
		*/
		JTextField xField = new JTextField();
		JTextField yField = new JTextField();
		JTextField zField = new JTextField();
		JTextField quatWField = new JTextField();
		JTextField quatXField = new JTextField();
		JTextField quatYField = new JTextField();
		JTextField quatZField = new JTextField();
		Object[] message = {
				"pos(x): ", xField, "pos(y): ", yField, "pos(z): ", zField,
				"quat(w): ", quatWField, "quat(x): ", quatXField, "quat(y): ", quatYField,
				"quat(z): ", quatZField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Position (x,y,z), Quat (w,x,y,z) Input"
				, JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.CANCEL_OPTION) return;
		position.m_p.m_x = Float.parseFloat(xField.getText());
		position.m_p.m_y = Float.parseFloat(yField.getText());
		position.m_p.m_z = Float.parseFloat(zField.getText());
		position.m_q.m_w = Float.parseFloat(quatWField.getText());
		position.m_q.m_x = Float.parseFloat(quatXField.getText());
		position.m_q.m_y = Float.parseFloat(quatYField.getText());
		position.m_q.m_z = Float.parseFloat(quatZField.getText());
		printMessage("Pos input: ("+position.m_p.m_x+", "+position.m_p.m_y+", "+position.m_p.m_z+")\n");
		printMessage("Quat input: ("+position.m_q.m_w+", "+position.m_q.m_x+", "
				+position.m_q.m_y+", "+position.m_q.m_z+")\n");
		/*
		System.out.print("quat (w,x,y,z): ");
		try {
			strLine = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		strLine.trim();
		strTokens = strLine.split(strDelim);
		position.m_q.m_w = Float.parseFloat(strTokens[0]);
		position.m_q.m_x = Float.parseFloat(strTokens[1]);
		position.m_q.m_y = Float.parseFloat(strTokens[2]);
		position.m_q.m_z = Float.parseFloat(strTokens[3]);
		System.out.println("Quat input: ("+position.m_q.m_w+", "+position.m_q.m_x+", "+position.m_q.m_y+", "+position.m_q.m_z+")");
		*/
		
		m_clientStub.sendUserPosition(position);
		
		//System.out.println("======");
		printMessage("======\n");
	}

	public void testChat()
	{
		String strTarget = null;
		String strMessage = null;
		//System.out.println("====== chat");
		printMessage("====== chat\n");
		/*
		System.out.print("target(/b, /s, /g, or /username): ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			strTarget = br.readLine();
			strTarget = strTarget.trim();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("message: ");
		try {
			strMessage = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		JTextField targetField = new JTextField();
		JTextField messageField = new JTextField();
		Object[] message = {
				"Target(/b, /s, /g, or /username): ", targetField,
				"Message: ", messageField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Chat Input", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strTarget = targetField.getText();
			strMessage = messageField.getText();
			m_clientStub.chat(strTarget, strMessage);
		}
		
		//System.out.println("======");
		printMessage("======\n");
	}

	public void testDummyEvent()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		String strInput = null;
		
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You should join a session and a group!");
			printMessage("You should join a session and a group!\n");
			return;
		}
		
		//System.out.println("====== test CMDummyEvent in current group");
		printMessage("====== test CMDummyEvent in current group\n");
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("input message: ");
		try {
			strInput = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		strInput = JOptionPane.showInputDialog("Input Message: ");
		if(strInput == null) return;
		
		
		CMDummyEvent due = new CMDummyEvent();
		due.setHandlerSession(myself.getCurrentSession());
		due.setHandlerGroup(myself.getCurrentGroup());
		due.setDummyInfo(strInput);
		m_clientStub.cast(due, myself.getCurrentSession(), myself.getCurrentGroup());
		due = null;
		
		//System.out.println("======");
		printMessage("======\n");
	}

	public void testDatagram()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();

		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You should join a session and a group!");
			printMessage("You should join a session and a group!\n");
			return;
		}
		
		String strReceiver = null;
		String strMessage = null;
		//System.out.println("====== test unicast chatting with datagram");
		printMessage("====== test unicast chatting with datagram\n");
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("receiver: ");
		try {
			strReceiver = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.print("message: ");
		try {
			strMessage = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		JTextField receiverField = new JTextField();
		JTextField messageField = new JTextField();
		Object[] message = {
				"Receiver: ", receiverField, "Message: ", messageField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Message Input", JOptionPane.OK_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strReceiver = receiverField.getText();
			strMessage = messageField.getText();
			
			CMInterestEvent ie = new CMInterestEvent();
			ie.setID(CMInterestEvent.USER_TALK);
			ie.setHandlerSession(myself.getCurrentSession());
			ie.setHandlerGroup(myself.getCurrentGroup());
			ie.setUserName(myself.getName());
			ie.setTalk(strMessage);
			m_clientStub.send(ie, strReceiver, CMInfo.CM_DATAGRAM);
			ie = null;
		}
		
		//System.out.println("======");
		printMessage("======\n");
		return;
	}

	public void testUserEvent()
	{
		//String strInput = null;
		String strReceiver = null;
		//boolean bEnd = false;
		//String[] strTokens = null;
		int nValueByteNum = -1;
		CMUser myself = m_clientStub.getCMInfo().getInteractionInfo().getMyself();
		
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You should join a session and a group!");
			printMessage("You should join a session and a group!\n");
			return;
		}

		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== test CMUserEvent");
		System.out.println("data type: CM_INT(0) CM_LONG(1) CM_FLOAT(2) CM_DOUBLE(3) CM_CHAR(4) CM_STR(5) CM_BYTES(6)");
		System.out.println("Type \"end\" to stop.");
		
		CMUserEvent ue = new CMUserEvent();
		ue.setStringID("testID");
		ue.setHandlerSession(myself.getCurrentSession());
		ue.setHandlerGroup(myself.getCurrentGroup());
		while(!bEnd)
		{
			System.out.println("If the data type is CM_BYTES, the number of bytes must be given "
					+ "in the third parameter.");
			System.out.print("(data type, field name, value): ");
			try {
				strInput = br.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				ue.removeAllEventFields();
				ue = null;
				return;
			}
			
			if(strInput.equals("end"))
			{
				bEnd = true;
			}
			else
			{
				strInput.trim();
				strTokens = strInput.split("\\s+");
				if(Integer.parseInt(strTokens[0]) == CMInfo.CM_BYTES)
				{
					nValueByteNum = Integer.parseInt(strTokens[2]);
					if(nValueByteNum < 0)
					{
						System.out.println("CMClientApp.testUserEvent(), Invalid nValueByteNum("
								+nValueByteNum+")");
						ue.removeAllEventFields();
						ue = null;
						return;
					}
					byte[] valueBytes = new byte[nValueByteNum];
					for(int i = 0; i < nValueByteNum; i++)
						valueBytes[i] = 1;	// dummy data
					ue.setEventBytesField(strTokens[1], nValueByteNum, valueBytes);	
				}
				else
					ue.setEventField(Integer.parseInt(strTokens[0]), strTokens[1], strTokens[2]);
			}
		}
		
		System.out.print("receiver: ");
		try {
			strReceiver = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		printMessage("====== test CMUserEvent\n");
		
		String strFieldNum = null;
		int nFieldNum = -1;

		strFieldNum = JOptionPane.showInputDialog("Field Numbers:");
		if(strFieldNum == null) return;
		try{
			nFieldNum = Integer.parseInt(strFieldNum);
		}catch(NumberFormatException e){
			printMessage("Input must be an integer number greater than 0!");
			return;
		}
		
		String strID = null;
		JTextField strIDField = new JTextField();
		JTextField strReceiverField = new JTextField();
		String[] dataTypes = {"CM_INT", "CM_LONG", "CM_FLOAT", "CM_DOUBLE", "CM_CHAR", "CH_STR", "CM_BYTES"};
		JComboBox<String>[] dataTypeBoxes = new JComboBox[nFieldNum]; 
		JTextField[] eventFields = new JTextField[nFieldNum*2];
		Object[] message = new Object[4+nFieldNum*3*2];
		
		for(int i = 0; i < nFieldNum; i++)
		{
			dataTypeBoxes[i] = new JComboBox<String>(dataTypes);
		}
		
		for(int i = 0; i < nFieldNum*2; i++)
		{
			eventFields[i] = new JTextField();
		}
		
		message[0] = "event ID: ";
		message[1] = strIDField;
		message[2] = "Receiver Name: ";
		message[3] = strReceiverField;
		for(int i = 4, j = 0, k = 1; i < 4+nFieldNum*3*2; i+=6, j+=2, k++)
		{
			message[i] = "Data type "+k+":";
			message[i+1] = dataTypeBoxes[k-1];
			message[i+2] = "Field Name "+k+":";
			message[i+3] = eventFields[j];
			message[i+4] = "Field Value "+k+":";
			message[i+5] = eventFields[j+1];
		}
		int option = JOptionPane.showConfirmDialog(null, message, "User Event Input", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strID = strIDField.getText();
			strReceiver = strReceiverField.getText();
			
			CMUserEvent ue = new CMUserEvent();
			ue.setStringID(strID);
			ue.setHandlerSession(myself.getCurrentSession());
			ue.setHandlerGroup(myself.getCurrentGroup());
			
			for(int i = 0, j = 0; i < nFieldNum*2; i+=2, j++)
			{
				//if(Integer.parseInt(eventFields[i].getText()) == CMInfo.CM_BYTES)
				if(dataTypeBoxes[j].getSelectedIndex() == CMInfo.CM_BYTES)
				{
					nValueByteNum = Integer.parseInt(eventFields[i+1].getText());
					if(nValueByteNum < 0)
					{
						//System.out.println("CMClientApp.testUserEvent(), Invalid nValueByteNum("
						//		+nValueByteNum+")");
						printMessage("CMClientApp.testUserEvent(), Invalid nValueByteNum("
								+nValueByteNum+")\n");
						ue.removeAllEventFields();
						ue = null;
						return;
					}
					byte[] valueBytes = new byte[nValueByteNum];
					for(int k = 0; k < nValueByteNum; k++)
						valueBytes[k] = 1;	// dummy data
					ue.setEventBytesField(eventFields[i].getText(), nValueByteNum, valueBytes);	
				}
				else
				{
					ue.setEventField(dataTypeBoxes[j].getSelectedIndex(),
							eventFields[i].getText(), eventFields[i+1].getText());
				}
				
			}
			
			m_clientStub.send(ue, strReceiver);
			ue.removeAllEventFields();
			ue = null;
		}
		

		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	// print group information provided by the default server
	public void testPrintGroupInfo()
	{
		// check local state
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You should join a session and a group.");
			printMessage("You should join a session and a group.\n");
			return;
		}
		
		CMSession session = interInfo.findSession(myself.getCurrentSession());
		Iterator<CMGroup> iter = session.getGroupList().iterator();
		//System.out.println("---------------------------------------------------------");
		//System.out.format("%-20s%-20s%-20s%n", "group name", "multicast addr", "multicast port");
		//System.out.println("---------------------------------------------------------");
		printMessage("---------------------------------------------------------\n");
		printMessage(String.format("%-20s%-20s%-20s%n", "group name", "multicast addr", "multicast port"));
		printMessage("---------------------------------------------------------\n");
		
		while(iter.hasNext())
		{
			CMGroupInfo gInfo = iter.next();
			//System.out.format("%-20s%-20s%-20d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
			//		, gInfo.getGroupPort());
			printMessage(String.format("%-20s%-20s%-20d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
					, gInfo.getGroupPort()));
		}
		
		return;
	}
	
	public void testCurrentUserStatus()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		//System.out.println("------ for the default server");
		printMessage("------ for the default server\n");
		//System.out.println("name("+myself.getName()+"), session("+myself.getCurrentSession()+"), group("
		//		+myself.getCurrentGroup()+"), udp port("+myself.getUDPPort()+"), state("
		//		+myself.getState()+").");
		printMessage("name("+myself.getName()+"), session("+myself.getCurrentSession()+"), group("
				+myself.getCurrentGroup()+"), udp port("+myself.getUDPPort()+"), state("
				+myself.getState()+"), attachment download scheme("+confInfo.getAttachDownloadScheme()+").\n");
		
		// for additional servers
		Iterator<CMServer> iter = interInfo.getAddServerList().iterator();
		while(iter.hasNext())
		{
			CMServer tserver = iter.next();
			if(tserver.getNonBlockSocketChannelInfo().findChannel(0) != null)
			{
				//System.out.println("------ for additional server["+tserver.getServerName()+"]");
				printMessage("------ for additional server["+tserver.getServerName()+"]\n");
				//System.out.println("current session("+tserver.getCurrentSessionName()+
				//		"), current group("+tserver.getCurrentGroupName()+"), state("
				//		+tserver.getClientState()+").");
				printMessage("current session("+tserver.getCurrentSessionName()+
						"), current group("+tserver.getCurrentGroupName()+"), state("
						+tserver.getClientState()+").");
				
			}
		}
		
		return;
	}

	public void testChangeGroup()
	{
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String strGroupName = null;
		//System.out.println("====== change group");
		printMessage("====== change group\n");
		/*
		try {
			System.out.print("group name: ");
			strGroupName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		strGroupName = JOptionPane.showInputDialog("Group Name: ");
		if(strGroupName != null)
			m_clientStub.changeGroup(strGroupName);
		
		//System.out.println("======");
		printMessage("======\n");
		return;
	}

	// ServerSocketChannel is not supported.
	// A server cannot add SocketChannel.
	// For the SocketChannel, available server name must be given as well.
	// For the MulticastChannel, session name and group name known by this client/server must be given. 
	public void testAddChannel()
	{
		int nChType = -1;
		int nChKey = -1; // the channel key for the socket channel
		String strServerName = null;
		String strChAddress = null; // the channel key for the multicast address is the (address, port) pair
		int nChPort = -1; // the channel key for the datagram socket channel, or the multicast port number
		String strSessionName = null;
		String strGroupName = null;
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		boolean result = false;
		boolean isBlock = false;
		SocketChannel sc = null;
		boolean isSyncCall = false;
		long lDelay = -1;
		
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMUser myself = interInfo.getMyself();
			if(myself.getState() != CMInfo.CM_SESSION_JOIN && myself.getState() != CMInfo.CM_LOGIN)
			{
				printMessage("You should login to the default server.\n");
				return;
			}
		}
		
		printMessage("====== add additional channel\n");
		
		// ask channel type, (server name), channel index (integer greater than 0), addr, port
		
		String[] chTypes = {"SocketChannel", "DatagramChannel", "MulticastChannel"};
		JComboBox<String> chTypeBox = new JComboBox<String>(chTypes);
		Object[] message = {
				"Channel Type: ", chTypeBox
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Channel type", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		nChType = chTypeBox.getSelectedIndex() + 2;

		if(nChType == CMInfo.CM_SOCKET_CHANNEL)
		{
			JRadioButton blockRadioButton = new JRadioButton("Blocking Channel");
			JRadioButton nonBlockRadioButton = new JRadioButton("NonBlocking Channel");
			nonBlockRadioButton.setSelected(true);
			ButtonGroup bGroup = new ButtonGroup();
			bGroup.add(blockRadioButton);
			bGroup.add(nonBlockRadioButton);
			String[] syncAsync = {"synchronous call", "asynchronous call"};
			JComboBox syncAsyncComboBox = new JComboBox(syncAsync);
			syncAsyncComboBox.setSelectedIndex(1); // default value is asynchronous call
			
			JTextField chIndexField = new JTextField();
			JTextField strServerField = new JTextField();
			Object[] scMessage = {
					"", blockRadioButton,
					"", nonBlockRadioButton,
					"syncronous or asynchronous call", syncAsyncComboBox,
					"Channel key (> 0 for nonblocking ch, >=0 for blocking ch)", chIndexField,
					"Server name(empty for the default server)", strServerField
			};
			
			int scResponse = JOptionPane.showConfirmDialog(null, scMessage, "Socket Channel", JOptionPane.OK_CANCEL_OPTION);

			if(scResponse != JOptionPane.OK_OPTION) return;
			nChKey = Integer.parseInt(chIndexField.getText());

			if(blockRadioButton.isSelected()) isBlock = true;
			else isBlock = false;
			
			if(!isBlock && nChKey <= 0)
			{
				printMessage("testAddChannel(), invalid nonblocking socket channel key ("+nChKey+")!\n");
				return;
			}
			else if(isBlock && nChKey < 0)
			{
				printMessage("testAddChannel(), invalid blocking socket channel key ("+nChKey+")!\n");
				return;
			}
			
			if(syncAsyncComboBox.getSelectedIndex() == 0)
				isSyncCall = true;
			else
				isSyncCall = false;
			
			strServerName = strServerField.getText();
			if(strServerName == null || strServerName.equals(""))
				strServerName = "SERVER"; // default server name
		}
		else if(nChType == CMInfo.CM_DATAGRAM_CHANNEL)
		{
			String strUDP = JOptionPane.showInputDialog("Port number (key of the datagram channel): ");
			if(strUDP == null) return;
			try{
				nChPort = Integer.parseInt(strUDP);
			}catch(NumberFormatException e){
				printMessage("The channel UDP port must be a number!\n");
				return;
			}			
		}
		else if(nChType == CMInfo.CM_MULTICAST_CHANNEL)
		{
			JTextField snameField = new JTextField();
			JTextField gnameField = new JTextField();
			JTextField chAddrField = new JTextField();
			JTextField chPortField = new JTextField();
			Object[] multicastMessage = {
					"Target Session Name: ", snameField,
					"Target Group Name: ", gnameField,
					"Channel Multicast Address: ", chAddrField,
					"Channel Multicast Port: ", chPortField
			};
			int multicastResponse = JOptionPane.showConfirmDialog(null, multicastMessage, "Additional Multicast Input",
					JOptionPane.OK_CANCEL_OPTION);
			if(multicastResponse != JOptionPane.OK_OPTION) return;
			
			strSessionName = snameField.getText();
			strGroupName = gnameField.getText();
			strChAddress = chAddrField.getText();
			nChPort = Integer.parseInt(chPortField.getText());			
		}
	    
		switch(nChType)
		{
		case CMInfo.CM_SOCKET_CHANNEL:
			if(isBlock)
			{
				if(isSyncCall)
				{
					m_eventHandler.setStartTime(System.currentTimeMillis());
					sc = m_clientStub.syncAddBlockSocketChannel(nChKey, strServerName);
					lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
					if(sc != null)
					{
						printMessage("Successfully added a blocking socket channel both "
								+ "at the client and the server: key("+nChKey+"), server("+strServerName+")\n");
						printMessage("return delay: "+lDelay+" ms.\n");
					}
					else
						printMessage("Failed to add a blocking socket channel both at "
								+ "the client and the server: key("+nChKey+"), server("+strServerName+")\n");					
				}
				else
				{
					m_eventHandler.setStartTime(System.currentTimeMillis());
					result = m_clientStub.addBlockSocketChannel(nChKey, strServerName);
					lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
					if(result)
					{
						printMessage("Successfully added a blocking socket channel at the client and "
								+"requested to add the channel info to the server: key("+nChKey+"), server("
								+strServerName+")\n");
						printMessage("return delay: "+lDelay+" ms.\n");
					}
					else
						printMessage("Failed to add a blocking socket channel at the client or "
								+"failed to request to add the channel info to the server: key("+nChKey
								+"), server("+strServerName+")\n");
				}
			}
			else
			{
				if(isSyncCall)
				{
					sc = m_clientStub.syncAddNonBlockSocketChannel(nChKey, strServerName);
					if(sc != null)
						printMessage("Successfully added a nonblocking socket channel both at the client "
								+ "and the server: key("+nChKey+"), server("+strServerName+")\n");
					else
						printMessage("Failed to add a nonblocking socket channel both at the client "
								+ "and the server: key("+nChKey+") to server("+strServerName+")\n");														
				}
				else
				{
					result = m_clientStub.addNonBlockSocketChannel(nChKey, strServerName);
					if(result)
						printMessage("Successfully added a nonblocking socket channel at the client and "
								+ "requested to add the channel info to the server: key("+nChKey+"), server("
								+strServerName+")\n");
					else
						printMessage("Failed to add a nonblocking socket channe at the client or "
								+ "failed to request to add the channel info to the server: key("+nChKey
								+") to server("+strServerName+")\n");									
				}
			}
				
			break;
		case CMInfo.CM_DATAGRAM_CHANNEL:
			result = m_clientStub.addDatagramChannel(nChPort);
			if(result)
				printMessage("Successfully added a datagram socket channel: port("+nChPort+")\n");
			else
				printMessage("Failed to add a datagram socket channel: port("+nChPort+")\n");
			break;
		case CMInfo.CM_MULTICAST_CHANNEL:
			result = m_clientStub.addMulticastChannel(strSessionName, strGroupName, strChAddress, nChPort);
			if(result)
			{
				printMessage("Successfully added a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")\n");
			}
			else
			{
				printMessage("Failed to add a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")\n");
			}
			break;
		default:
			printMessage("Channel type is incorrect!\n");
			break;
		}
		
		printMessage("======\n");
	}

	public void testRemoveChannel()
	{
		int nChType = -1;
		int nChKey = -1;
		int nChPort = -1;
		String strChAddress = null;
		String strServerName = null;
		String strSessionName = null;
		String strGroupName = null;
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		boolean result = false;
		boolean isBlock = false;
		boolean isSyncCall = false;
		long lDelay = 0;
		
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMUser myself = interInfo.getMyself();
			if(myself.getState() != CMInfo.CM_SESSION_JOIN && myself.getState() != CMInfo.CM_LOGIN)
			{
				printMessage("You should login to the default server.\n");
				return;
			}
		}
		
		printMessage("====== remove additional channel\n");
				
		String[] chTypes = {"SocketChannel", "DatagramChannel", "MulticastChannel"};
		JComboBox<String> chTypeBox = new JComboBox<String>(chTypes);
		Object[] message = {
				"Channel Type: ", chTypeBox
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Removal of Additional Channel", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		nChType = chTypeBox.getSelectedIndex() + 2;

		if(nChType == CMInfo.CM_SOCKET_CHANNEL)
		{
			JRadioButton blockRadioButton = new JRadioButton("Blocking Channel");
			JRadioButton nonBlockRadioButton = new JRadioButton("NonBlocking Channel");
			nonBlockRadioButton.setSelected(true);
			ButtonGroup bGroup = new ButtonGroup();
			bGroup.add(blockRadioButton);
			bGroup.add(nonBlockRadioButton);
			String syncAsync[] = {"synchronous call", "asynchronous call"};
			JComboBox syncAsyncComboBox = new JComboBox(syncAsync);
			syncAsyncComboBox.setSelectedIndex(1);	//default value is asynchronous call

			JTextField chIndexField = new JTextField();
			JTextField strServerField = new JTextField();
			Object[] scMessage = {
					"", blockRadioButton,
					"", nonBlockRadioButton,
					"Synchronous or asynchronous call", syncAsyncComboBox,
					"Channel key (> 0 for nonblocking ch, >=0 for blocking ch)", chIndexField,
					"Server name(empty for the default server)", strServerField
			};
			
			int scResponse = JOptionPane.showConfirmDialog(null, scMessage, "Socket Channel", JOptionPane.OK_CANCEL_OPTION);

			if(scResponse != JOptionPane.OK_OPTION) return;
			nChKey = Integer.parseInt(chIndexField.getText());

			if(blockRadioButton.isSelected()) isBlock = true;
			else isBlock = false;

			if(!isBlock && nChKey <= 0)
			{
				printMessage("testRemoveChannel(), invalid nonblocking socket channel key ("+nChKey+")!\n");
				return;
			}
			else if(isBlock && nChKey < 0)
			{
				printMessage("testRemoveChannel(), invalid blocking socket channel key ("+nChKey+")!\n");
				return;
			}
			
			if(syncAsyncComboBox.getSelectedIndex() == 0)
				isSyncCall = true;
			else
				isSyncCall = false;
			
			strServerName = strServerField.getText();
			if(strServerName == null || strServerName.equals(""))
				strServerName = "SERVER"; // default server name
		}
		else if(nChType == CMInfo.CM_DATAGRAM_CHANNEL)
		{
			String strUDP = JOptionPane.showInputDialog("Port number (key of the datagram channel): ");
			if(strUDP == null) return;
			try{
				nChPort = Integer.parseInt(strUDP);
			}catch(NumberFormatException e){
				printMessage("The channel UDP port must be a number!\n");
				return;
			}						
		}
		else if(nChType == CMInfo.CM_MULTICAST_CHANNEL)
		{
			JTextField snameField = new JTextField();
			JTextField gnameField = new JTextField();
			JTextField chAddrField = new JTextField();
			JTextField chPortField = new JTextField();
			Object[] sgMessage = { 
					"Target Session Name: ", snameField,
					"Target Group Name: ", gnameField,
					"Channel Multicast Address: ", chAddrField,
					"Channel Multicast Port: ", chPortField
			};
			int sgOption = JOptionPane.showConfirmDialog(null, sgMessage, "Target Session and Group", JOptionPane.OK_CANCEL_OPTION);
			if(sgOption != JOptionPane.OK_OPTION) return;
			strSessionName = snameField.getText();
			strGroupName = gnameField.getText();
			strChAddress = chAddrField.getText();
			nChPort = Integer.parseInt(chPortField.getText());			
		}

		switch(nChType)
		{
		case CMInfo.CM_SOCKET_CHANNEL:
			if(isBlock)
			{
				if(isSyncCall)
				{
					m_eventHandler.setStartTime(System.currentTimeMillis());
					result = m_clientStub.syncRemoveBlockSocketChannel(nChKey, strServerName);
					lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
					if(result)
					{
						printMessage("Successfully removed a blocking socket channel both "
								+ "at the client and the server: key("+nChKey+"), server ("+strServerName+")\n");
						printMessage("return delay: "+lDelay+" ms.\n");
					}
					else
						printMessage("Failed to remove a blocking socket channel both at the client "
								+ "and the server: key("+nChKey+"), server ("+strServerName+")\n");					
				}
				else
				{
					m_eventHandler.setStartTime(System.currentTimeMillis());
					result = m_clientStub.removeBlockSocketChannel(nChKey, strServerName);
					lDelay = System.currentTimeMillis() - m_eventHandler.getStartTime();
					if(result)
					{
						printMessage("Successfully removed a blocking socket channel at the client and " 
								+ "requested to remove it at the server: key("+nChKey+"), server("+strServerName+")\n");
						printMessage("return delay: "+lDelay+" ms.\n");
					}
					else
						printMessage("Failed to remove a blocking socket channel at the client or "
								+ "failed to request to remove it at the server: key("+nChKey+"), server("
								+strServerName+")\n");
				}
			}
			else
			{
				result = m_clientStub.removeNonBlockSocketChannel(nChKey, strServerName);
				if(result)
					printMessage("Successfully removed a nonblocking socket channel: key("+nChKey
							+"), server("+strServerName+")\n");
				else
					printMessage("Failed to remove a nonblocing socket channel: key("+nChKey
							+"), server("+strServerName+")\n");			
			}
	
			break;
		case CMInfo.CM_DATAGRAM_CHANNEL:
			result = m_clientStub.removeAdditionalDatagramChannel(nChPort);
			if(result)
				printMessage("Successfully removed a datagram socket channel: port("+nChPort+")\n");
			else
				printMessage("Failed to remove a datagram socket channel: port("+nChPort+")\n");

			break;
		case CMInfo.CM_MULTICAST_CHANNEL:
			result = m_clientStub.removeAdditionalMulticastChannel(strSessionName, strGroupName, strChAddress, nChPort);
			if(result)
			{
				printMessage("Successfully removed a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")\n");
			}
			else
			{
				printMessage("Failed to remove a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")\n");
			}
			break;
		default:
			printMessage("Channel type is incorrect!\n");
			break;
		}
		
		printMessage("======\n");
	}

	public void testSetFilePath()
	{
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//System.out.println("====== set file path");
		printMessage("====== set file path\n");
		String strPath = null;
		
		/*
		System.out.print("file path (must end with \'/\'): ");
		try {
			strPath = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		strPath = JOptionPane.showInputDialog("file path: ");
		if(strPath == null) return;
		
		/*
		if(!strPath.endsWith("/"))
		{
			//System.out.println("Invalid file path!");
			printMessage("Invalid file path! (must end with \'/\')");
			return;
		}
		*/
		
		//CMFileTransferManager.setFilePath(strPath, m_clientStub.getCMInfo());
		m_clientStub.setFilePath(strPath);
		
		//System.out.println("======");
		printMessage("======\n");
	}

	public void testRequestFile()
	{
		boolean bReturn = false;
		String strFileName = null;
		String strFileOwner = null;
		String strFileAppendMode = null;
		
		printMessage("====== request a file\n");

		JTextField fnameField = new JTextField();
		JTextField fownerField = new JTextField();
		String[] fAppendMode = {"Default", "Overwrite", "Append"};		
		JComboBox<String> fAppendBox = new JComboBox<String>(fAppendMode);

		Object[] message = { 
				"File Name: ", fnameField, "File Owner: ", fownerField,
				"File Append Mode: ", fAppendBox 
				};
		int option = JOptionPane.showConfirmDialog(null, message, "File Request", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strFileName = fnameField.getText();
			strFileOwner = fownerField.getText();
			strFileAppendMode = (String) fAppendBox.getSelectedItem();
			
			if(strFileAppendMode.equals("Default"))
				bReturn = m_clientStub.requestFile(strFileName, strFileOwner);
			else if(strFileAppendMode.equals("Overwrite"))
				bReturn = m_clientStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_OVERWRITE);
			else
				bReturn = m_clientStub.requestFile(strFileName, strFileOwner, CMInfo.FILE_APPEND);
			
			if(!bReturn)
				printMessage("Request file error! file("+strFileName+"), owner("+strFileOwner+").\n");
		}
		
		printMessage("======\n");
	}

	public void testPushFile()
	{
		String strFilePath = null;
		File[] files = null;
		String strReceiver = null;
		printMessage("====== push a file\n");
		
		/*
		JTextField fnameField = new JTextField();
		JTextField freceiverField = new JTextField();
		Object[] message = { "File Path Name: ", fnameField, "File Receiver: ", freceiverField };
		int option = JOptionPane.showConfirmDialog(null, message, "File Push", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strFilePath = fnameField.getText();
			strReceiver = freceiverField.getText();
			CMFileTransferManager.pushFile(strFilePath, strReceiver, m_clientStub.getCMInfo());
		}
		*/
		
		strReceiver = JOptionPane.showInputDialog("Receiver Name: ");
		if(strReceiver == null) return;
		JFileChooser fc = new JFileChooser();
		fc.setMultiSelectionEnabled(true);
		CMFileTransferInfo fInfo = m_clientStub.getCMInfo().getFileTransferInfo();
		File curDir = new File(fInfo.getFilePath());
		fc.setCurrentDirectory(curDir);
		int fcRet = fc.showOpenDialog(this);
		if(fcRet != JFileChooser.APPROVE_OPTION) return;
		files = fc.getSelectedFiles();
		if(files.length < 1) return;
		for(int i=0; i < files.length; i++)
		{
			strFilePath = files[i].getPath();
			//CMFileTransferManager.pushFile(strFilePath, strReceiver, m_clientStub.getCMInfo());
			m_clientStub.pushFile(strFilePath, strReceiver);
		}
		
		printMessage("======\n");
	}

	public void cancelRecvFile()
	{
		String strSender = null;
		boolean bReturn = false;
		printMessage("====== cancel receiving a file\n");
		
		strSender = JOptionPane.showInputDialog("Input sender name (enter for all senders)");
		if(strSender.isEmpty())
			strSender = null;
		
		bReturn = m_clientStub.cancelRequestFile(strSender);
		
		if(bReturn)
		{
			if(strSender == null)
				strSender = "all senders";
			printMessage("Successfully requested to cancel receiving a file to ["+strSender+"].\n");
		}
		else
			printMessage("Request failed to cancel receiving a file to ["+strSender+"]!\n");
		
		return;
	}
	
	public void cancelSendFile()
	{
		String strReceiver = null;
		boolean bReturn = false;
		printMessage("====== cancel sending a file\n");

		strReceiver = JOptionPane.showInputDialog("Input receiver name (enter for all receivers)");
		if(strReceiver.isEmpty())
			strReceiver = null;
		
		bReturn = m_clientStub.cancelPushFile(strReceiver);
		
		if(bReturn)
		{
			if(strReceiver == null)
				strReceiver = "all receivers";
			printMessage("Successfully requested to cancel sending a file to ["+strReceiver+"].\n");
		}
		else
			printMessage("Request failed to cancel sending a file to ["+strReceiver+"]!\n");
		
		return;
	}
	
	public void testForwarding()
	{
		int nForwardType = 0;
		float fForwardRate = 0;
		int nSimNum = 0;
		int nEventTypeNum = 10;
		int nEventRange = -1;
		int nEventID = -1;
		String strUserName = null;
		CMUserEvent ue = null;
		
		int nUserState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();
		if(nUserState != CMInfo.CM_LOGIN && nUserState != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You must log in to the default server.");
			printMessage("You must log in to the default server.\n");
			return;
		}
		
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		//System.out.println("====== typical/internal forwarding test");
		printMessage("====== typical/internal forwarding test\n");
		
		/*
		try {
			System.out.print("Forwarding type (0: typical, 1: internal): ");
			nForwardType = Integer.parseInt(br.readLine());
			System.out.print("Forwarding rate (0 ~ 1): ");
			fForwardRate = Float.parseFloat(br.readLine());
			System.out.print("Simulation num: ");
			nSimNum = Integer.parseInt(br.readLine());
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		*/
		String[] ftypes = {"Typical", "Internal"};
		JComboBox<String> ftypeBox = new JComboBox<String>(ftypes);
		JTextField frateField = new JTextField();
		JTextField simnumField = new JTextField();
		Object[] message = {
				"Forwarding Type: ", ftypeBox,
				"Forwarding Rate (0 ~ 1): ", frateField,
				"Simulation Number: ", simnumField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Event Forwarding Test", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		nForwardType = ftypeBox.getSelectedIndex();
		fForwardRate = Float.parseFloat(frateField.getText());
		nSimNum = Integer.parseInt(simnumField.getText());
		
		nEventRange = (int) (nEventTypeNum * fForwardRate); // number of event types which must be forwarded
		strUserName = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();
		Random rnd = new Random();
		ue = new CMUserEvent();
		
		for(int i = 0; i < nSimNum; i++)
		{
			for(int j = 0; j < 100; j++)
			{
				ue = new CMUserEvent();
				nEventID = rnd.nextInt(10);	// 0 ~ 9
				if(nEventID >= 0 && nEventID < nEventRange)
					ue.setStringID("testForward");
				else
					ue.setStringID("testNotForward");
				ue.setEventField(CMInfo.CM_INT, "id", String.valueOf(nEventID));
				ue.setEventField(CMInfo.CM_INT, "ftype", String.valueOf(nForwardType));
				ue.setEventField(CMInfo.CM_STR, "user", strUserName);
				
				// send the event to a server
				if(nForwardType == 0)
					m_clientStub.send(ue, "SERVER");
				else if(nForwardType == 1)
				{
					if(ue.getStringID().equals("testForward"))
						m_clientStub.send(ue, strUserName);
					else
						m_clientStub.send(ue, "SERVER");
				}
				else
				{
					//System.out.println("Invalid forwarding type: "+nForwardType);
					printMessage("Invalid forwarding type: "+nForwardType+"\n");
					return;
				}
			}
		}
		
		// send an end event to a server (id: EndSim, int: simnum)
		ue = new CMUserEvent();
		ue.setStringID("EndSim");
		ue.setEventField(CMInfo.CM_INT, "simnum", String.valueOf(nSimNum));
		m_clientStub.send(ue, "SERVER");
		
		ue = null;
		return;
	}

	public void testForwardingDelay()
	{
		int nForwardType = 0;
		int nSendNum = 0;
		String strUserName = null;
		long lSendTime = 0;
		CMUserEvent ue = null;

		int nUserState = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getState();
		if(nUserState != CMInfo.CM_LOGIN && nUserState != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You must log in to the default server.");
			printMessage("You must log in to the default server.\n");
			return;
		}

		//System.out.println("====== test delay of forwarding schemes (typical vs. internal");
		printMessage("====== test delay of forwarding schemes (typical vs. internal\n");
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("forward type(0:typical, 1:internal): ");
			nForwardType = Integer.parseInt(br.readLine());
			System.out.print("Send num: ");
			nSendNum = Integer.parseInt(br.readLine());
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		*/
		String[] fTypes = {"Typical", "Internal"};
		JComboBox<String> forwardTypeBox = new JComboBox<String>(fTypes);
		JTextField sendNumField = new JTextField();
		Object[] message = {
				"Forward Type: ", forwardTypeBox,
				"Number of Transmission: ", sendNumField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Test Forwarding Delay", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		nForwardType = forwardTypeBox.getSelectedIndex();
		nSendNum = Integer.parseInt(sendNumField.getText());

		strUserName = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();

		for(int i=0; i < nSendNum; i++)
		{
			
			// generate a test event
			ue = new CMUserEvent();
			ue.setStringID("testForwardDelay");
			ue.setEventField(CMInfo.CM_INT, "id", String.valueOf(i));
			ue.setEventField(CMInfo.CM_INT, "ftype", String.valueOf(nForwardType));
			ue.setEventField(CMInfo.CM_STR, "user", strUserName);
				
			lSendTime = System.currentTimeMillis();
			ue.setEventField(CMInfo.CM_LONG, "stime", String.valueOf(lSendTime));

			// send an event to a server
			if(nForwardType == 0)
				m_clientStub.send(ue, "SERVER");
			else if(nForwardType == 1)
			{
				m_clientStub.send(ue, strUserName);
			}
			else
			{
				//System.out.println("Invalid forward type: "+nForwardType);
				printMessage("Invalid forward type: "+nForwardType+"\n");
				return;
			}
		}
		
		// send end event to a server (id: EndSim, int: simnum)
		ue = new CMUserEvent();
		ue.setStringID("EndForwardDelay");
		ue.setEventField(CMInfo.CM_INT, "ftype", String.valueOf(nForwardType));
		ue.setEventField(CMInfo.CM_STR, "user", strUserName);
		ue.setEventField(CMInfo.CM_INT, "sendnum", String.valueOf(nSendNum));
		
		if(nForwardType == 0)
			m_clientStub.send(ue, "SERVER");
		else
			m_clientStub.send(ue, strUserName);
		
		//System.out.println("======");
		printMessage("======\n");
		
		ue = null;
		return;
	}

	public void testDownloadNewSNSContent()
	{
		//System.out.println("====== request downloading of SNS content (offset 0)");
		printMessage("====== request downloading of SNS content (offset 0)\n");

		int nContentOffset = 0;
		String strWriterName = null;
		String strUserName = m_clientStub.getMyself().getName();
		
		JTextField offsetField = new JTextField();
		JTextField writerField = new JTextField();
		Object[] message = {
				"Offset ( >= 0 ): ", offsetField,
				"Content Writer (Empty for no designation, CM_MY_FRIEND for my friends, CM_BI_FRIEND for my bi-friends, "
				+ "or a specific name): ", 
				writerField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Request content download", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		if(offsetField.getText().isEmpty())
			nContentOffset = 0;
		else
			nContentOffset = Integer.parseInt(offsetField.getText());
		strWriterName = writerField.getText();

		// start time of downloading contents
		m_eventHandler.setStartTime(System.currentTimeMillis());

		m_clientStub.requestSNSContent(strWriterName, nContentOffset);
		if(CMInfo._CM_DEBUG)
		{
			printMessage("["+strUserName+"] requests content of writer["+strWriterName+"] with offset("
					+nContentOffset+").\n");
		}

		printMessage("======\n");
		return;
	}
	
	public void testRequestAttachedFileOfSNSContent()
	{
		printMessage("====== request an attached file of SNS content\n");
//		int nContentID = 0;
//		String strWriterName = null;
		String strFileName = null;

//		JTextField contentIDField = new JTextField();
//		JTextField writerField = new JTextField();
		JTextField fileNameField = new JTextField();
		Object[] message = {
//				"Content ID: ", contentIDField,
//				"Content Writer Name: ", writerField,
				"Attachment File Name: ", fileNameField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Request an attached file of SNS content", 
				JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;

//		nContentID = Integer.parseInt(contentIDField.getText());
//		strWriterName = writerField.getText();
		strFileName = fileNameField.getText();
		
//		m_clientStub.requestAttachedFileOfSNSContent(nContentID, strWriterName, strFileName);
		m_clientStub.requestAttachedFileOfSNSContent(strFileName);
		return;
	}

	public void testRepeatedSNSContentDownload()
	{
		//System.out.println("====== Repeated downloading of SNS content");
		printMessage("====== Repeated downloading of SNS content\n");
		
		// open a file for writing the access delay and # downloaded contents
		FileOutputStream fos = null;
		PrintWriter pw = null;
		try {
			fos = new FileOutputStream("SNSContentDownload.txt");
			pw = new PrintWriter(fos);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_eventHandler.setFileOutputStream(fos);
		m_eventHandler.setPrintWriter(pw);
		m_eventHandler.setSimNum(100);

		m_clientStub.requestSNSContent("", 0);	// no specific writer, offset = 0

		return;
	}

	// download the next SNS content list
	// if this method is called without any previous download request, it requests the most recent list
	public void testDownloadNextSNSContent()
	{
		printMessage("===== Request the next SNS content list\n");
		// start time of downloading contents
		m_eventHandler.setStartTime(System.currentTimeMillis());
		m_clientStub.requestNextSNSContent();
		
		return;		
	}
	
	// download the previous SNS content list
	// if this method is called without any previous download request, it requests the most recent list
	public void testDownloadPreviousSNSContent()
	{
		printMessage("===== Request the previous SNS content list\n");
		// start time of downloading contents
		m_eventHandler.setStartTime(System.currentTimeMillis());
		m_clientStub.requestPreviousSNSContent();
		
		return;		
	}

	public void testSNSContentUpload()
	{
		String strMessage = null;
		ArrayList<String> filePathList = null;
		int nNumAttachedFiles = 0;
		int nReplyOf = 0;
		int nLevelOfDisclosure = 0;
		File[] files = null;

		printMessage("====== test SNS content upload\n");
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("Input message: ");
			strMessage = br.readLine();
			System.out.print("Input attached file path (0 for no attachment): ");
			strFilePath = br.readLine();
			if(strFilePath.equals("0"))
				strFilePath="";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		JTextField msgField = new JTextField();
		JCheckBox attachedFilesBox = new JCheckBox();
		JTextField replyOfField = new JTextField();
		String[] lod = {"Everyone", "My Followers", "Bi-Friends", "Nobody"};
		JComboBox<String> lodBox = new JComboBox<String>(lod);
		//JTextField lodField = new JTextField();
		Object[] message = {
				"Input Message: ", msgField,
				"File Attachment: ", attachedFilesBox,
				"Content ID to which this content replies(0 for no reply): ", replyOfField,
				//"Level of Disclosure(0: to everyone, 1: to my followers, 2: to bi-friends, 3: nobody): ", lodField
				"Level of Disclosure: ", lodBox
		};
		int option = JOptionPane.showConfirmDialog(null, message, "SNS Content Upload", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strMessage = msgField.getText();
			String strReplyOf = replyOfField.getText();
			if(!strReplyOf.isEmpty())
				nReplyOf = Integer.parseInt(strReplyOf);
			else
				nReplyOf = 0;
			/*
			String strLod = lodField.getText();
			if(strLod != null)
				nLevelOfDisclosure = Integer.parseInt(strLod);
			else
				nLevelOfDisclosure = 0;
			*/
			nLevelOfDisclosure = lodBox.getSelectedIndex();
			System.out.println("selected lod: "+nLevelOfDisclosure);
			
			if(attachedFilesBox.isSelected())
			{
				JFileChooser fc = new JFileChooser();
				fc.setMultiSelectionEnabled(true);
				CMFileTransferInfo fInfo = m_clientStub.getCMInfo().getFileTransferInfo();
				File curDir = new File(fInfo.getFilePath());
				fc.setCurrentDirectory(curDir);
				int fcRet = fc.showOpenDialog(this);
				if(fcRet == JFileChooser.APPROVE_OPTION)
				{
					files = fc.getSelectedFiles();
					if(files.length > 0)
					{
						nNumAttachedFiles = files.length;
						filePathList = new ArrayList<String>();
						for(int i = 0; i < nNumAttachedFiles; i++)
						{
							String strPath = files[i].getPath();
							filePathList.add(strPath);
						}
					}
				}
			}			
			
			String strUser = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();
			m_clientStub.requestSNSContentUpload(strUser, strMessage, nNumAttachedFiles, nReplyOf, nLevelOfDisclosure, 
					filePathList);
		}

		return;
	}

	public void testRegisterUser()
	{
		String strName = null;
		String strPasswd = null;
		String strRePasswd = null;
		String strEncPasswd = null;
		
		/*
		Console console = System.console();
		if(console == null)
		{
			System.err.println("Unable to obtain console.");
		}
		*/

		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== register a user");
		try {
			System.out.print("Input user name: ");
			strName = br.readLine();
			if(console == null)
			{
				System.out.print("Input password: ");
				strPasswd = br.readLine();
				System.out.print("Retype password: ");
				strRePasswd = br.readLine();
			}
			else
			{
				strPasswd = new String(console.readPassword("Input password: "));
				strRePasswd = new String(console.readPassword("Retype password: "));
			}
			
			if(!strPasswd.equals(strRePasswd))
			{
				System.err.println("Password input error");
				return;
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== register a user\n");
		JTextField nameField = new JTextField();
		JPasswordField passwordField = new JPasswordField();
		JPasswordField rePasswordField = new JPasswordField();
		Object[] message = {
				"Input User Name: ", nameField,
				"Input Password: ", passwordField,
				"Retype Password: ", rePasswordField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "User Registration", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		strName = nameField.getText();
		strPasswd = new String(passwordField.getPassword());	// security problem?
		strRePasswd = new String(rePasswordField.getPassword());// security problem?

		if(!strPasswd.equals(strRePasswd))
		{
			//System.err.println("Password input error");
			printMessage("Password input error!\n");
			return;
		}

		// encrypt password
		strEncPasswd = CMUtil.getSHA1Hash(strPasswd);

		//m_clientStub.registerUser(strName, strPasswd);
		m_clientStub.registerUser(strName, strEncPasswd);
		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testDeregisterUser()
	{
		String strName = null;
		String strPasswd = null;
		String strEncPasswd = null;
		
		/*
		Console console = System.console();
		if(console == null)
		{
			System.err.println("Unable to obtain console.");
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("====== Deregister a user");
		try {
			System.out.print("Input user name: ");
			strName = br.readLine();
			if(console == null)
			{
				System.out.print("Input password: ");
				strPasswd = br.readLine();
			}
			else
			{
				strPasswd = new String(console.readPassword("Input password: "));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== Deregister a user\n");
		JTextField nameField = new JTextField();
		JPasswordField passwdField = new JPasswordField();
		Object[] message = {
				"Input User Name: ", nameField,
				"Input Password: ", passwdField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "User Deregistration", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		strName = nameField.getText();
		strPasswd = new String(passwdField.getPassword());	// security problem?
		
		// encrypt password
		strEncPasswd = CMUtil.getSHA1Hash(strPasswd);
		//m_clientStub.deregisterUser(strName, strPasswd);
		m_clientStub.deregisterUser(strName, strEncPasswd);

		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testFindRegisteredUser()
	{
		String strName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== search for a registered user");
		try {
			System.out.print("Input user name: ");
			strName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== search for a registered user\n");
		strName = JOptionPane.showInputDialog("Input User Name: ");
		if(strName != null)
			m_clientStub.findRegisteredUser(strName);

		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testAddNewFriend()
	{
		String strFriendName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== add a new friend");
		System.out.println("A friend must be a registered user in CM");
		try {
			System.out.print("Input a friend name: ");
			strFriendName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		*/
		
		printMessage("====== add a new friend\n");
		printMessage("A friend must be a registered user in CM\n");
		strFriendName = JOptionPane.showInputDialog("Input a friend name: ");
		if(strFriendName != null)
			m_clientStub.addNewFriend(strFriendName);
		
		return;
	}

	public void testRemoveFriend()
	{
		String strFriendName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== remove a friend");
		try {
			System.out.print("Input a friend name: ");
			strFriendName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		*/
		
		printMessage("====== remove a friend\n");
		strFriendName = JOptionPane.showInputDialog("Input a friend name: ");
		if(strFriendName != null)
			m_clientStub.removeFriend(strFriendName);
		
		return;
	}
	
	public void testRequestFriendsList()
	{
		//System.out.println("====== request current friends list");
		printMessage("====== request current friends list\n");
		m_clientStub.requestFriendsList();
		return;
	}

	public void testRequestFriendRequestersList()
	{
		//System.out.println("====== request friend requesters list");
		printMessage("====== request friend requesters list\n");
		m_clientStub.requestFriendRequestersList();
		return;
	}

	public void testRequestBiFriendsList()
	{
		//System.out.println("====== request bi-directional friends list");
		printMessage("====== request bi-directional friends list\n");
		m_clientStub.requestBiFriendsList();
		return;
	}

	public void testRequestServerInfo()
	{
		//System.out.println("====== request additional server information");
		printMessage("====== request additional server information\n");
		m_clientStub.requestServerInfo();
	}

	public void testConnectToServer()
	{
		//System.out.println("====== connect to a designated server");
		printMessage("====== connect to a designated server\n");
		String strServerName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input a server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		strServerName = JOptionPane.showInputDialog("Input a server name: ");
		if(strServerName != null)
			m_clientStub.connectToServer(strServerName);
		
		return;
	}

	public void testDisconnectFromServer()
	{
		//System.out.println("===== disconnect from a designated server");
		printMessage("===== disconnect from a designated server\n");
		
		String strServerName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input a server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		strServerName = JOptionPane.showInputDialog("Input a server name: ");
		if(strServerName != null)
			m_clientStub.disconnectFromServer(strServerName);

		return;
	}

	public void testLoginServer()
	{
		String strServerName = null;
		String user = null;
		String password = null;
		String strEncPasswd = null;
		
		/*
		Console console = System.console();
		if(console == null)
		{
			System.err.println("Unable to obtain console.");
		}

		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		System.out.println("====== log in to a designated server");
		try {
			System.out.print("Input server name: ");
			strServerName = br.readLine();
			if( strServerName.equals("SERVER") )	// login to a default server
			{
				System.out.print("User name: ");
				user = br.readLine();
				if(console == null)
				{
					System.out.print("Password: ");
					password = br.readLine();
				}
				else
				{
					password = new String(console.readPassword("Password: "));
				}
				// encrypt password
				strEncPasswd = CMUtil.getSHA1Hash(password);
				
				//m_clientStub.loginCM(user, password);
				m_clientStub.loginCM(user, strEncPasswd);
			}
			else // use the login info for the default server
			{
				CMUser myself = m_clientStub.getCMInfo().getInteractionInfo().getMyself();
				user = myself.getName();
				password = myself.getPasswd();
				m_clientStub.loginCM(strServerName, user, password);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== log in to a designated server\n");
		strServerName = JOptionPane.showInputDialog("Server Name: ");
		if(strServerName == null) return;

		if( strServerName.equals("SERVER") )	// login to a default server
		{
			JTextField userNameField = new JTextField();
			JPasswordField passwordField = new JPasswordField();
			Object[] message = {
					"User Name:", userNameField,
					"Password:", passwordField
			};
			int option = JOptionPane.showConfirmDialog(null, message, "Login Input", JOptionPane.OK_CANCEL_OPTION);
			if (option == JOptionPane.OK_OPTION)
			{
				user = userNameField.getText();
				String strPassword = new String(passwordField.getPassword()); // security problem?
				// encrypt password
				strEncPasswd = CMUtil.getSHA1Hash(strPassword);

				m_clientStub.loginCM(user, strEncPasswd);
			}
		}
		else // use the login info for the default server
		{
			CMUser myself = m_clientStub.getCMInfo().getInteractionInfo().getMyself();
			user = myself.getName();
			password = myself.getPasswd();
			m_clientStub.loginCM(strServerName, user, password);
		}
		
		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testLogoutServer()
	{
		String strServerName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== log out from a designated server");
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== log out from a designated server\n");
		strServerName = JOptionPane.showInputDialog("Server Name: ");
		if(strServerName != null)
			m_clientStub.logoutCM(strServerName);
		
		//System.out.println("======");
		printMessage("======\n");
	}

	public void testRequestSessionInfoOfServer()
	{
		String strServerName = null;
		//System.out.println("====== request session informatino of a designated server");
		printMessage("====== request session informatino of a designated server\n");
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		strServerName = JOptionPane.showInputDialog("Server Name: ");
		if(strServerName != null)
			m_clientStub.requestSessionInfo(strServerName);
		
		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testJoinSessionOfServer()
	{
		String strServerName = null;
		String strSessionName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== join a session of a designated server");
		try {
			System.out.print("Input server name: ");
			strServerName = br.readLine();
			System.out.print("Input session name: ");
			strSessionName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== join a session of a designated server\n");
		JTextField serverField = new JTextField();
		JTextField sessionField = new JTextField();
		Object[] message = {
				"Server Name", serverField, "Session Name", sessionField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Join Session", JOptionPane.OK_CANCEL_OPTION);
		if(option == JOptionPane.OK_OPTION)
		{
			strServerName = serverField.getText();
			strSessionName = sessionField.getText();
			m_clientStub.joinSession(strServerName, strSessionName);
		}
		
		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testLeaveSessionOfServer()
	{
		String strServerName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== leave a session of a designated server");
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== leave a session of a designated server\n");
		strServerName = JOptionPane.showInputDialog("Input a server name: ");
		if(strServerName != null)
			m_clientStub.leaveSession(strServerName);
		
		//System.out.println("======");
		printMessage("======\n");
		
		return;
	}

	public void testPrintGroupInfoOfServer()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		
		String strServerName = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== print group information a designated server");
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		printMessage("====== print group information a designated server\n");
		strServerName = JOptionPane.showInputDialog("Input a server name: ");
		if(strServerName == null) return;
		
		if(strServerName.equals("SERVER"))
		{
			testPrintGroupInfo();
			return;
		}
		
		CMServer server = interInfo.findAddServer(strServerName);
		if(server == null)
		{
			//System.out.println("server("+strServerName+") not found in the add-server list!");
			printMessage("server("+strServerName+") not found in the add-server list!\n");
			return;
		}
		
		CMSession session = server.findSession(myself.getCurrentSession());
		Iterator<CMGroup> iter = session.getGroupList().iterator();
		//System.out.println("---------------------------------------------------------");
		//System.out.format("%-20s%-20s%-20s%n", "group name", "multicast addr", "multicast port");
		//System.out.println("---------------------------------------------------------");
		printMessage("---------------------------------------------------------\n");
		printMessage(String.format("%-20s%-20s%-20s%n", "group name", "multicast addr", "multicast port"));
		printMessage("---------------------------------------------------------\n");
		
		while(iter.hasNext())
		{
			CMGroupInfo gInfo = iter.next();
			//System.out.format("%-20s%-20s%-20d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
			//		, gInfo.getGroupPort());
			
			printMessage(String.format("%-20s%-20s%-20d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
					, gInfo.getGroupPort()));
		}

		return;
	}

	public void testSendMultipleFiles()
	{
		String[] strFiles = null;
		String strFileList = null;
		int nMode = -1; // 1: push, 2: pull
		int nFileNum = -1;
		String strTarget = null;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== pull/push multiple files");
		try {
			System.out.print("Select mode (1: push, 2: pull): ");
			nMode = Integer.parseInt(br.readLine());
			if(nMode == 1)
			{
				System.out.print("Input receiver name: ");
				strTarget = br.readLine();
			}
			else if(nMode == 2)
			{
				System.out.print("Input file owner name: ");
				strTarget = br.readLine();
			}
			else
			{
				System.out.println("Incorrect transmission mode!");
				return;
			}

			System.out.print("Number of files: ");
			nFileNum = Integer.parseInt(br.readLine());
			System.out.print("Input file names separated with space: ");
			strFileList = br.readLine();
			
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		*/
		
		printMessage("====== pull/push multiple files\n");
		
		String[] modes = {"Push", "Pull"};
		JComboBox<String> modeBox = new JComboBox<String>(modes);
		JTextField targetField = new JTextField();
		JTextField fileNumField = new JTextField();
		JTextField fileNamesField = new JTextField();
		Object[] message = {
				"Transmission Mode", modeBox,
				"File Receiver or Owner", targetField,
				"Number of Files", fileNumField,
				"File Names Separated with Space", fileNamesField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Push/Pull Multiple Files", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		nMode = modeBox.getSelectedIndex();
		strTarget = targetField.getText();
		try{
			nFileNum = Integer.parseInt(fileNumField.getText());
		}catch(NumberFormatException e){
			printMessage("Number of files must be an integer!\n");
			return;
		}
		strFileList = fileNamesField.getText();
		
		strFileList.trim();
		strFiles = strFileList.split("\\s+");
		if(strFiles.length != nFileNum)
		{
			//System.out.println("The number of files incorrect!");
			printMessage("The number of files incorrect!\n");
			return;
		}
		
		for(int i = 0; i < nFileNum; i++)
		{
			switch(nMode)
			{
			case 0: // push
				CMFileTransferManager.pushFile(strFiles[i], strTarget, m_clientStub.getCMInfo());
				break;
			case 1: // pull
				CMFileTransferManager.requestFile(strFiles[i], strTarget, m_clientStub.getCMInfo());
				break;
			}
		}
		
		return;
	}

	public void testSplitFile()
	{
		String strSrcFile = null;
		String strSplitFile = null;
		long lFileSize = -1;
		long lFileOffset = 0;
		long lSplitSize = -1;
		long lSplitRemainder = -1;
		int nSplitNum = -1;
		RandomAccessFile raf = null;
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		//System.out.println("====== split a file");
		
		printMessage("====== split a file\n");
		
		try {
			/*
			System.out.print("Input source file name: ");
			strSrcFile = br.readLine();
			System.out.print("Input the number of split files: ");
			nSplitNum = Integer.parseInt(br.readLine());
			*/
			
			JTextField fileField = new JTextField();
			JTextField splitNumField = new JTextField();
			Object[] message = {
					"Source File Name", fileField,
					"Number of Split Files", splitNumField
			};
			int option = JOptionPane.showConfirmDialog(null, message, "Split a File", JOptionPane.OK_CANCEL_OPTION);
			if(option != JOptionPane.OK_OPTION) return;
			strSrcFile = fileField.getText();
			try{
				nSplitNum = Integer.parseInt(splitNumField.getText());
			}catch(NumberFormatException ne){
				printMessage("Number of split files must be an integer!");
				return;
			}
			
			raf = new RandomAccessFile(strSrcFile, "r");
			lFileSize = raf.length();

			lSplitSize = lFileSize / nSplitNum;
			lSplitRemainder = lFileSize % lSplitSize;
			
			for(int i = 0; i < nSplitNum; i++)
			{
				// get the name of split file ('srcfile'-i.split)
				int index = strSrcFile.lastIndexOf(".");
				strSplitFile = strSrcFile.substring(0, index)+"-"+(i+1)+".split";
				
				// update offset
				lFileOffset = i*lSplitSize;
				
				if(i+1 != nSplitNum)
					CMFileTransferManager.splitFile(raf, lFileOffset, lSplitSize, strSplitFile);
				else
					CMFileTransferManager.splitFile(raf, lFileOffset, lSplitSize+lSplitRemainder, strSplitFile);
				
			}
			
			raf.close();
		} catch (FileNotFoundException fe) {
			fe.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		return;
	}

	public void testMergeFiles()
	{
		String[] strFiles = null;
		//String strFileList = null;
		String strFilePrefix = null;
		String strMergeFileName = null;
		int nFileNum = -1;
		long lMergeFileSize = -1;
		
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("====== merge split files");
		try {
			System.out.print("Number of split files: ");
			nFileNum = Integer.parseInt(br.readLine());
			//System.out.print("Input split files in order: ");
			//strFileList = br.readLine();
			System.out.print("Input prefix of split files: ");
			strFilePrefix = br.readLine();
			System.out.print("Input merged file name: ");
			strMergeFileName = br.readLine();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		*/
		
		printMessage("====== merge split files\n");
		
		JTextField splitNumField = new JTextField();
		JTextField prefixField = new JTextField();
		JTextField mergeFileNameField = new JTextField();
		Object[] message = {
				"Number of split files", splitNumField,
				"Prefix name of split files", prefixField,
				"Merge file name", mergeFileNameField
		};
		int option = JOptionPane.showConfirmDialog(null, message, "Merge Split Files", JOptionPane.OK_CANCEL_OPTION);
		if(option != JOptionPane.OK_OPTION) return;
		try{
			nFileNum = Integer.parseInt(splitNumField.getText());
		}catch(NumberFormatException e){
			printMessage("Number of split files must be an integer!");
			return;
		}
		strFilePrefix = prefixField.getText();
		strMergeFileName = mergeFileNameField.getText();
				
		// make list of split file names
		strFiles = new String[nFileNum];
		for(int i = 0; i < nFileNum; i++)
		{
			strFiles[i] = strFilePrefix + "-" + (i+1) + ".split";
		}
		
		lMergeFileSize = CMFileTransferManager.mergeFiles(strFiles, nFileNum, strMergeFileName);
		//System.out.println("Size of merged file("+strMergeFileName+"): "+lMergeFileSize+" Bytes.");
		printMessage("Size of merged file("+strMergeFileName+"): "+lMergeFileSize+" Bytes.\n");
		return;
	}

	public void testDistFileProc()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		String strFile = null;
		long lFileSize = 0;
		CMFileEvent fe = null;
		//BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		//System.out.println("====== split a file, distribute to multiple servers, and merge");
		printMessage("====== split a file, distribute to multiple servers, and merge\n");
		
		// check if the client logs in to all available servers
		int nClientState = interInfo.getMyself().getState();
		if(nClientState == CMInfo.CM_INIT || nClientState == CMInfo.CM_CONNECT)
		{
			//System.out.println("You must log in the default server!");
			printMessage("You must log in the default server!\n");
			return;
		}
		Iterator<CMServer> iter = interInfo.getAddServerList().iterator();
		while(iter.hasNext())
		{
			CMServer tserver = iter.next();
			nClientState = tserver.getClientState();
			if(nClientState == CMInfo.CM_INIT || nClientState == CMInfo.CM_CONNECT)
			{
				//System.out.println("You must log in the additional server("+tserver.getServerName()
				//		+")!");
				printMessage("You must log in the additional server("+tserver.getServerName()
						+")!\n");
				return;
			}
		}

		// input file name
		/*
		try {
			System.out.println("A source file must exists in the file path configured in CM");
			System.out.print("Input a source file name: ");
			strFile = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		//printMessage("A source file must exists in the file path configured in CM\n");
		strFile = JOptionPane.showInputDialog("Source file path");
		if(strFile == null) return;

		// print the file size
		//strFile = fileInfo.getFilePath()+"/"+strFile;
		File srcFile = new File(strFile);
		lFileSize = srcFile.length();
		//System.out.println("Source file ("+strFile+"): "+lFileSize+" Bytes.");
		printMessage("Source file ("+strFile+"): "+lFileSize+" Bytes.\n");

		// get current number of servers ( default server + add servers )
		m_eventHandler.setCurrentServerNum(interInfo.getAddServerList().size() + 1);
		String[] filePieces = new String[interInfo.getAddServerList().size()+1];
		m_eventHandler.setFilePieces(filePieces);
		
		// initialize the number of modified pieces
		m_eventHandler.setRecvPieceNum(0);

		// set m_bDistSendRecv to true
		m_eventHandler.setDistFileProc(true);

		// set send time
		m_eventHandler.setStartTime(System.currentTimeMillis());

		// extract the extension of the file
		String strPrefix = null;
		String strExt = null;
		int index = strFile.lastIndexOf(".");
		strPrefix = strFile.substring(0, index);
		strExt = strFile.substring(index+1);
		m_eventHandler.setFileExtension(strExt);
		//System.out.println("Source file extension: "+m_eventHandler.getFileExtension());
		printMessage("Source file extension: "+m_eventHandler.getFileExtension()+"\n");

		// split a file into pieces with the number of servers. each piece has the name of 'file name'-x.split
		// and send each piece to different server
		long lPieceSize = lFileSize / m_eventHandler.getCurrentServerNum();
		int i = 0;
		String strPieceName = null;
		long lOffset = 0;
		RandomAccessFile raf;
		try {
			raf = new RandomAccessFile(strFile, "r");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}

		// make a file event (REQUEST_DIST_FILE_PROC)
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_DIST_FILE_PROC);
		fe.setReceiverName(interInfo.getMyself().getName());

		// for pieces except the last piece
		for( i = 0; i < m_eventHandler.getCurrentServerNum()-1; i++)
		{
			// get the piece name
			strPieceName = strPrefix+"-"+(i+1)+".split";
			//System.out.println("File piece name: "+strPieceName);
			printMessage("File piece name: "+strPieceName+"\n");

			// split the file with a piece
			CMFileTransferManager.splitFile(raf, lOffset, lPieceSize, strPieceName);
			// update offset
			lOffset += lPieceSize;

			// send piece to the corresponding additional server
			String strAddServer = interInfo.getAddServerList().elementAt(i).getServerName();
			
			m_clientStub.send(fe, strAddServer);
			
			CMFileTransferManager.pushFile(strPieceName, strAddServer, m_clientStub.getCMInfo());
		}
		// for the last piece
		if( i == 0 )
		{
			// no split
			strPieceName = strFile;
		}
		else
		{
			// get the last piece name
			strPieceName = strPrefix+"-"+(i+1)+".split";
			//System.out.println("File piece name: "+strPieceName);
			printMessage("File piece name: "+strPieceName+"\n");

			// get the last piece
			CMFileTransferManager.splitFile(raf, lOffset, lFileSize-lPieceSize*i, strPieceName);
		}
		// send the last piece to the default server
		m_clientStub.send(fe, "SERVER");
		CMFileTransferManager.pushFile(strPieceName, "SERVER", m_clientStub.getCMInfo());
		
		try {
			raf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// The next process proceeds when a modified piece is transferred from a server.

		// Whenever a modified piece(m-'file name'-x.split) is transferred, if m_bDistSendRecv is true, 
		// increase the number of pieces and its name is stored in an array.
		// When all modified pieces arrive, they are merged to a file (m-'file name').
		// After the file is merged, set the received time, calculate the elapsed time, set m_bDistSendRecv to false
		// and print the result.

		fe = null;
		return;
	}

	public void testMulticastChat()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		//System.out.println("====== test multicast chat in current group");
		printMessage("====== test multicast chat in current group\n");

		// check user state
		CMUser myself = interInfo.getMyself();
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			//System.out.println("You must join a session and a group for multicasting.");
			printMessage("You must join a session and a group for multicasting.\n");
			return;
		}

		// check communication architecture
		if(!confInfo.getCommArch().equals("CM_PS"))
		{
			//System.out.println("CM must start with CM_PS mode which enables multicast per group!");
			printMessage("CM must start with CM_PS mode which enables multicast per group!\n");
			return;
		}

		// receive a user input message
		/*
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input message: ");
		String strMessage = null;
		try {
			strMessage = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		
		String strMessage = JOptionPane.showInputDialog("Chat Message");
		if(strMessage == null) return;
		
		// make a CMInterestEvent.USER_TALK event
		CMInterestEvent ie = new CMInterestEvent();
		ie.setID(CMInterestEvent.USER_TALK);
		ie.setHandlerSession(myself.getCurrentSession());
		ie.setHandlerGroup(myself.getCurrentGroup());
		ie.setUserName(myself.getName());
		ie.setTalk(strMessage);
		
		m_clientStub.multicast(ie, myself.getCurrentSession(), myself.getCurrentGroup());

		ie = null;
		return;
	}
	
	public void testGetBlockSocketChannel()
	{
		int nChKey = -1;
		String strServerName = null;
		SocketChannel sc = null;
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMUser myself = interInfo.getMyself();
			if(myself.getState() != CMInfo.CM_SESSION_JOIN && myself.getState() != CMInfo.CM_LOGIN)
			{
				printMessage("You should login to the default server.\n");
				return;
			}
		}
		
		printMessage("============= get blocking socket channel\n");

		JTextField chKeyField = new JTextField();
		JTextField serverField = new JTextField();
		Object[] scMessage = {
				"Channel key (>=0)", chKeyField,
				"Server name(empty for the default server)", serverField
		};
		
		int scResponse = JOptionPane.showConfirmDialog(null, scMessage, "find blocking socket channel", 
				JOptionPane.OK_CANCEL_OPTION);
		if(scResponse != JOptionPane.OK_OPTION) return;
		
		nChKey = Integer.parseInt(chKeyField.getText());
		if(nChKey < 0)
		{
			System.err.println("Invalid channel key: "+nChKey);
			return;
		}

		
		strServerName = serverField.getText();
		if(strServerName == null || strServerName.equals(""))
			strServerName = "SERVER"; // default server name

		sc = m_clientStub.getBlockSocketChannel(nChKey, strServerName);
		
		if(sc == null)
		{
			printMessage("Blocking socket channel not found: key("+nChKey+"), server("+strServerName+")\n");
		}
		else
		{
			printMessage("Blocking socket channel found: key("+nChKey+"), server("+strServerName+")\n");
		}
		
		return;
	}

	public void testMeasureInputThroughput()
	{
		String strTarget = null;
		float fSpeed = -1; // MBps
		printMessage("========== test input network throughput\n");
		
		strTarget = JOptionPane.showInputDialog("Target node (empty for the default server)");
		if(strTarget == null) 
			return;
		else if(strTarget.equals(""))
			strTarget = "SERVER";

		fSpeed = m_clientStub.measureInputThroughput(strTarget);
		if(fSpeed == -1)
			printMessage("Test failed!\n");
		else
			printMessage(String.format("Input network throughput from [%s] : %.2f MBps%n", strTarget, fSpeed));
	}
	
	public void testMeasureOutputThroughput()
	{
		String strTarget = null;
		float fSpeed = -1; // MBps
		printMessage("========== test output network throughput\n");
		
		strTarget = JOptionPane.showInputDialog("Target node (empty for the default server)");
		if(strTarget == null) 
			return;
		else if(strTarget.equals(""))
			strTarget = "SERVER";

		fSpeed = m_clientStub.measureOutputThroughput(strTarget);
		if(fSpeed == -1)
			printMessage("Test failed!\n");
		else
			printMessage(String.format("Output network throughput to [%s] : %.2f MBps%n", strTarget, fSpeed));
	}
	
	private void requestAttachedFile(String strFileName)
	{
		/*
		int nContentID = -1;
		String strWriterName = null;
		// A downloaded file name may be a thumbnail file name instead of original name
		int index = strFileName.lastIndexOf(".");
		String strThumbnail = strFileName.substring(0, index) + "-thumbnail"
				+ strFileName.substring(index, strFileName.length());
		// search for content ID and writer name
		CMSNSInfo snsInfo = m_clientStub.getCMInfo().getSNSInfo();
		CMSNSContentList contentList = snsInfo.getSNSContentList();
		Vector<CMSNSContent> contentVector = contentList.getContentList();
		Iterator<CMSNSContent> iter = contentVector.iterator();
		boolean bFound = false;
		while(iter.hasNext() && !bFound)
		{
			CMSNSContent content = iter.next();
			if(content.containsFileName(strFileName) || content.containsFileName(strThumbnail))
			{
				nContentID = content.getContentID();
				strWriterName = content.getWriterName();
				bFound = true;
			}
		}		
		
		if(bFound)
		{
			// set a flag of the request
			m_eventHandler.setReqAttachedFile(true);
			// send request for the attachment download
			m_clientStub.requestAttachedFileOfSNSContent(nContentID, strWriterName, strFileName);			
		}
		else
		{
			printMessage(strFileName+" not found in the downloaded content list!\n");
		}
		*/
		
		boolean bRet = m_clientStub.requestAttachedFileOfSNSContent(strFileName);
		if(bRet)
			m_eventHandler.setReqAttachedFile(true);
		else
			printMessage(strFileName+" not found in the downloaded content list!\n");
			
		return;
	}

	private void accessAttachedFile(String strFileName)
	{
		boolean bRet = m_clientStub.accessAttachedFileOfSNSContent(strFileName);
		if(!bRet)
			printMessage(strFileName+" not found in the downloaded content list!\n");
		
		return;
	}
	
	public class MyKeyListener implements KeyListener {
		public void keyPressed(KeyEvent e)
		{
			int key = e.getKeyCode();
			if(key == KeyEvent.VK_ENTER)
			{
				JTextField input = (JTextField)e.getSource();
				String strText = input.getText();
				printMessage(strText+"\n");
				// parse and call CM API
				processInput(strText);
				input.setText("");
				input.requestFocus();
			}
		}
		
		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
	}
	
	public class MyActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e)
		{
			JButton button = (JButton) e.getSource();
			if(button.getText().equals("Start Client CM"))
			{
				// start cm
				boolean bRet = m_clientStub.startCM();
				if(!bRet)
				{
					printStyledMessage("CM initialization error!\n", "bold");
				}
				else
				{
					printStyledMessage("Client CM starts.\n", "bold");
					printStyledMessage("Type \"0\" for menu.\n", "regular");
					// change the appearance of buttons in the client window frame
					setButtonsAccordingToClientState();
				}
			}
			else if(button.getText().equals("Stop Client CM"))
			{
				testTermination();
			}
			else if(button.getText().equals("Login"))
			{
				// login to the default cm server
				testLoginDS();
			}
			else if(button.getText().equals("Logout"))
			{
				// logout from the default cm server
				testLogoutDS();
			}
			else if(button.equals(m_composeSNSContentButton))
			{
				testSNSContentUpload();
			}
			else if(button.equals(m_readNewSNSContentButton))
			{
				testDownloadNewSNSContent();
			}
			else if(button.equals(m_readNextSNSContentButton))
			{
				testDownloadNextSNSContent();
			}
			else if(button.equals(m_readPreviousSNSContentButton))
			{
				testDownloadPreviousSNSContent();
			}
			else if(button.equals(m_findUserButton))
			{
				testFindRegisteredUser();
			}
			else if(button.equals(m_addFriendButton))
			{
				testAddNewFriend();
			}
			else if(button.equals(m_removeFriendButton))
			{
				testRemoveFriend();
			}
			else if(button.equals(m_friendsButton))
			{
				testRequestFriendsList();
			}
			else if(button.equals(m_friendRequestersButton))
			{
				testRequestFriendRequestersList();
			}
			else if(button.equals(m_biFriendsButton))
			{
				testRequestBiFriendsList();
			}

			m_inTextField.requestFocus();
		}
	}
	
	public class MyMouseListener implements MouseListener {

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
			if(e.getSource() instanceof JLabel)
			{
				JLabel pathLabel = (JLabel)e.getSource();
				String strPath = pathLabel.getText();
				File fPath = new File(strPath);
				try {
					int index = strPath.lastIndexOf(File.separator);
					String strFileName = strPath.substring(index+1, strPath.length()); 
					if(fPath.exists())
					{
						accessAttachedFile(strFileName);
						Desktop.getDesktop().open(fPath);
					}
					else
					{
						requestAttachedFile(strFileName);
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			if(e.getSource() instanceof JLabel)
			{
				Cursor cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
				setCursor(cursor);
			}
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			if(e.getSource() instanceof JLabel)
			{
				Cursor cursor = Cursor.getDefaultCursor();
				setCursor(cursor);
			}
		}
		
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CMWinClient client = new CMWinClient();
		CMClientStub cmStub = client.getClientStub();
		cmStub.setEventHandler(client.getClientEventHandler());
	}

}
