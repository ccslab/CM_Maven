import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMGroupInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMPosition;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;
import kr.ac.konkuk.ccslab.cm.stub.CMClientStub;
import kr.ac.konkuk.ccslab.cm.util.CMUtil;


public class CMClientApp {
	private CMClientStub m_clientStub;
	private CMClientEventHandler m_eventHandler;
	private boolean m_bRun;
	private Scanner m_scan = null;
	
	public CMClientApp()
	{
		m_clientStub = new CMClientStub();
		m_eventHandler = new CMClientEventHandler(m_clientStub);
		m_bRun = true;
	}
	
	public CMClientStub getClientStub()
	{
		return m_clientStub;
	}
	
	public CMClientEventHandler getClientEventHandler()
	{
		return m_eventHandler;
	}
	
	///////////////////////////////////////////////////////////////
	// test methods

	public void startTest()
	{
		System.out.println("client application starts.");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		m_scan = new Scanner(System.in);
		String strInput = null;
		int nCommand = -1;
		while(m_bRun)
		{
			System.out.println("Type \"0\" for menu.");
			System.out.print("> ");
			try {
				strInput = br.readLine();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			
			try {
				nCommand = Integer.parseInt(strInput);
			} catch (NumberFormatException e) {
				System.out.println("Incorrect command number!");
				continue;
			}
			
			switch(nCommand)
			{
			case 0:
				System.out.println("---------------------------------------------------");
				System.out.println("0: help, 1: connect to default server, 2: disconnect from default server");
				System.out.println("3: login to default server, 4: logout from default server");
				System.out.println("5: request session info from default server, 6: join session of defalut server, 7: leave session of default server");
				System.out.println("8: user position, 9: chat, 10: test CMDummyEvent, 11: test datagram message");
				System.out.println("12: test CMUserEvent, 13: print group info, 14: print current user status");
				System.out.println("15: change group, 16: add additional channel, 17: remove additional channel");
				System.out.println("18: set file path, 19: request file, 20: push file");
				System.out.println("62: cancel receiving file, 63: cancel sending file");
				System.out.println("21: test forwarding schemes, 22: test delay of forwarding schemes");
				System.out.println("---------------------------------------------------");
				System.out.println("23: download new SNS content, 50: request attached file of SNS content");
				System.out.println("51: download next SNS content, 52: download previous SNS content");
				System.out.println("24: test repeated downloading of SNS content, 25: SNS content upload");
				System.out.println("26: register user, 27: deregister user");
				System.out.println("28: find registered user, 29: add a new friend, 30: remove a friend");
				System.out.println("31: request current friend list, 32: request friend requester list");
				System.out.println("33: request bi-directional friends");
				System.out.println("---------------------------------------------------");
				System.out.println("34: request additional server info");
				System.out.println("35: connect to a designated server, 36: disconnect from a designated server");
				System.out.println("37: log in to a designated server, 38: log out from a designated server");
				System.out.println("39: request session info from a designated server");
				System.out.println("40: join a session of a designated server, 41: leave a session of a designated server");
				System.out.println("42: print group info of a designated server");
				System.out.println("---------------------------------------------------");
				System.out.println("43: pull/push multiple files, 44: split a file, 45: merge files");
				System.out.println("46: distribute a file and merge");
				System.out.println("---------------------------------------------------");
				System.out.println("47: multicast chat in current group");
				System.out.println("48: get additional blocking socket channel");
				System.out.println("60: test input network throughput, 61: test output network throughput");
				System.out.println("99: terminate CM");
				break;
			case 1: // connect to default server
				testConnectionDS();
				break;
			case 2: // disconnect from default server
				testDisconnectionDS();
				break;
			case 3: // login to default server
				testLoginDS();
				break;
			case 4: // logout from default server
				testLogoutDS();
				break;
			case 5: // request session info from default server
				testSessionInfoDS();
				break;
			case 6: // join a session
				testJoinSession();
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
			case 50: // test request for an attached file of SNS content
				testRequestAttachedFileOfSNSContent();
				break;
			case 51: // test download next SNS content
				testDownloadNextSNSContent();
				break;
			case 52: // test download previous SNS content
				testDownloadPreviousSNSContent();
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
		
		try {
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_scan.close();
	}
	
	public void testConnectionDS()
	{
		System.out.println("====== connect to default server");
		m_clientStub.connectToServer();
		System.out.println("======");
	}
	
	public void testDisconnectionDS()
	{
		System.out.println("====== disconnect from default server");
		m_clientStub.disconnectFromServer();
		System.out.println("======");
	}
	
	public void testLoginDS()
	{
		String strUserName = null;
		String strPassword = null;
		String strEncPassword = null;
		Console console = System.console();
		if(console == null)
		{
			System.err.println("Unable to obtain console.");
		}
		
		System.out.println("====== login to default server");
		System.out.print("user name: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			strUserName = br.readLine();
			if(console == null)
			{
				System.out.print("password: ");
				strPassword = br.readLine();
			}
			else
				strPassword = new String(console.readPassword("password: "));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// encrypt password
		strEncPassword = CMUtil.getSHA1Hash(strPassword);
		
		//m_clientStub.loginCM(strUserName, strPassword);
		m_clientStub.loginCM(strUserName, strEncPassword);
		System.out.println("======");
	}
	
	public void testLogoutDS()
	{
		System.out.println("====== logout from default server");
		m_clientStub.logoutCM();
		System.out.println("======");
	}
	
	public void testTermination()
	{
		m_clientStub.terminateCM();
		m_bRun = false;
	}

	public void testSessionInfoDS()
	{
		System.out.println("====== request session info from default server");
		m_clientStub.requestSessionInfo();
		System.out.println("======");
	}
	
	public void testJoinSession()
	{
		String strSessionName = null;
		System.out.println("====== join a session");
		System.out.print("session name: ");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			strSessionName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_clientStub.joinSession(strSessionName);
		System.out.println("======");
	}
	
	public void testLeaveSession()
	{
		System.out.println("====== leave the current session");
		m_clientStub.leaveSession();
		System.out.println("======");
	}
	
	public void testUserPosition()
	{
		CMPosition position = new CMPosition();
		String strLine = null;
		String strDelim = "\\s+";
		String[] strTokens;
		System.out.println("====== send user position");
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
		
		m_clientStub.sendUserPosition(position);
		
		System.out.println("======");
	}
	
	public void testChat()
	{
		String strTarget = null;
		String strMessage = null;
		System.out.println("====== chat");
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
		
		m_clientStub.chat(strTarget, strMessage);
		
		System.out.println("======");
	}

	public void testDummyEvent()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.out.println("You should join a session and a group!");
			return;
		}
		
		System.out.println("====== test CMDummyEvent in current group");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("input message: ");
		String strInput = null;
		try {
			strInput = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		CMDummyEvent due = new CMDummyEvent();
		due.setHandlerSession(myself.getCurrentSession());
		due.setHandlerGroup(myself.getCurrentGroup());
		due.setDummyInfo(strInput);
		m_clientStub.cast(due, myself.getCurrentSession(), myself.getCurrentGroup());
		due = null;
		
		System.out.println("======");
	}
	
	public void testDatagram()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();

		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.out.println("You should join a session and a group!");
			return;
		}
		
		String strReceiver = null;
		String strMessage = null;
		System.out.println("====== test unicast chatting with datagram");
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
		
		CMInterestEvent ie = new CMInterestEvent();
		ie.setID(CMInterestEvent.USER_TALK);
		ie.setHandlerSession(myself.getCurrentSession());
		ie.setHandlerGroup(myself.getCurrentGroup());
		ie.setUserName(myself.getName());
		ie.setTalk(strMessage);
		m_clientStub.send(ie, strReceiver, CMInfo.CM_DATAGRAM);
		ie = null;
		
		System.out.println("======");
		return;
	}
	
	public void testUserEvent()
	{
		String strInput = null;
		String strReceiver = null;
		boolean bEnd = false;
		String[] strTokens = null;
		int nValueByteNum = -1;
		CMUser myself = m_clientStub.getCMInfo().getInteractionInfo().getMyself();
		
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.out.println("You should join a session and a group!");
			return;
		}
		
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
		
		m_clientStub.send(ue, strReceiver);

		System.out.println("======");
		
		ue.removeAllEventFields();
		ue = null;
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
			System.out.println("You should join a session and a group.");
			return;
		}
		
		CMSession session = interInfo.findSession(myself.getCurrentSession());
		Iterator<CMGroup> iter = session.getGroupList().iterator();
		System.out.println("---------------------------------------------------------");
		System.out.format("%-20s%-20s%-20s%n", "group name", "multicast addr", "multicast port");
		System.out.println("---------------------------------------------------------");
		while(iter.hasNext())
		{
			CMGroupInfo gInfo = iter.next();
			System.out.format("%-20s%-20s%-20d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
					, gInfo.getGroupPort());
		}
		
		return;
	}
	
	public void testCurrentUserStatus()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		System.out.println("------ for the default server");
		System.out.println("name("+myself.getName()+"), session("+myself.getCurrentSession()+"), group("
				+myself.getCurrentGroup()+"), udp port("+myself.getUDPPort()+"), state("
				+myself.getState()+"), attachment download scheme("+confInfo.getAttachDownloadScheme()+").");
		
		// for additional servers
		Iterator<CMServer> iter = interInfo.getAddServerList().iterator();
		while(iter.hasNext())
		{
			CMServer tserver = iter.next();
			if(tserver.getNonBlockSocketChannelInfo().findChannel(0) != null)
			{
				System.out.println("------ for additional server["+tserver.getServerName()+"]");
				System.out.println("current session("+tserver.getCurrentSessionName()+
						"), current group("+tserver.getCurrentGroupName()+"), state("
						+tserver.getClientState()+").");
				
			}
		}
		
		return;
	}
	
	public void testChangeGroup()
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String strGroupName = null;
		System.out.println("====== change group");
		try {
			strGroupName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_clientStub.changeGroup(strGroupName);
		
		System.out.println("======");
		return;
	}
	
	// ServerSocketChannel is not supported.
	// A server cannot add SocketChannel.
	// For the SocketChannel, available server name must be given as well.
	// For the MulticastChannel, session name and group name known by this client/server must be given. 
	public void testAddChannel()
	{
		int nChType = -1;
		int nChKey = -1;
		String strServerName = null;
		String strChAddress = null;
		int nChPort = -1;
		String strSessionName = null;
		String strGroupName = null;
		CMConfigurationInfo confInfo = m_clientStub.getCMInfo().getConfigurationInfo();
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		boolean bResult = false;
		String strBlock = null;
		boolean isBlock = false;
		SocketChannel sc = null;
		String strSync = null;
		boolean isSyncCall = false;
		
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMUser myself = interInfo.getMyself();
			if(myself.getState() != CMInfo.CM_SESSION_JOIN && myself.getState() != CMInfo.CM_LOGIN)
			{
				System.out.println("You should login to the default server.");
				return;
			}
		}
		
		System.out.println("====== add additional channel");

		// ask channel type, (server name), channel index (integer greater than 0), addr, port
		try{
			System.out.print("Select channel type (SocketChannel:2, DatagramChannel:3, MulticastChannel:4): ");
			nChType = m_scan.nextInt();
			if(nChType == CMInfo.CM_SOCKET_CHANNEL)
			{
				System.out.print("is it a blocking channel? (\"y\": yes, \"n\": no): ");
				strBlock = m_scan.next();
				if(strBlock.equals("y")) isBlock = true;
				else if(strBlock.equals("n")) isBlock = false;
				else
				{
					System.err.println("invalid answer! : "+strBlock);
					return;
				}
			
				if(isBlock)
				{
					System.out.print("Channel key(>=0): ");
					nChKey = m_scan.nextInt();
					if(nChKey < 0)
					{
						System.err.println("testAddChannel(), invalid blocking socket channel key ("+nChKey+")!");
						return;
					}
				}
				else
				{
					System.out.print("Channel key(integer greater than 0): ");
					nChKey = m_scan.nextInt();
					if(nChKey <= 0)
					{
						System.err.println("testAddChannel(), invalid nonblocking socket channel key ("+nChKey+")!");
						return;
					}
				}
				
				System.out.print("Is the addition synchronous? (\"y\": yes, \"n\": no): ");
				strSync = m_scan.next();
				if(strSync.equals("y")) isSyncCall = true;
				else if(strSync.equals("n")) isSyncCall =false;
				else
				{
					System.err.println("invalid answer! :" + strSync);
					return;
				}
				
				System.out.print("Server name(\"SERVER\" for the default server): ");
				strServerName = m_scan.next();
			}
			else if(nChType == CMInfo.CM_DATAGRAM_CHANNEL)
			{
				System.out.print("Channel udp port: ");
				nChPort = m_scan.nextInt();
			}
			else if(nChType == CMInfo.CM_MULTICAST_CHANNEL)
			{
				System.out.print("Target session name: ");
				strSessionName = m_scan.next();
				System.out.print("Target group name: ");
				strGroupName = m_scan.next();
				System.out.print("Channel multicast address: ");
				strChAddress = m_scan.next();
				System.out.print("Channel multicast port: ");
				nChPort = m_scan.nextInt();
			}
		}catch(InputMismatchException e){
			System.err.println("Invalid input type!");
			m_scan.next();
			return;
		}
					
		switch(nChType)
		{
		case CMInfo.CM_SOCKET_CHANNEL:
			if(isBlock)
			{
				if(isSyncCall)
				{
					sc = m_clientStub.syncAddBlockSocketChannel(nChKey, strServerName);
					if(sc != null)
						System.out.println("Successfully added a blocking socket channel both "
								+ "at the client and the server: key("+nChKey+"), server("+strServerName+")");
					else
						System.err.println("Failed to add a blocking socket channel both at "
								+ "the client and the server: key("+nChKey+"), server("+strServerName+")");					
				}
				else
				{
					bResult = m_clientStub.addBlockSocketChannel(nChKey, strServerName);
					if(bResult)
						System.out.println("Successfully added a blocking socket channel at the client and "
								+"requested to add the channel info to the server: key("+nChKey+"), server("
								+strServerName+")");
					else
						System.err.println("Failed to add a blocking socket channel at the client or "
								+"failed to request to add the channel info to the server: key("+nChKey
								+"), server("+strServerName+")");
					
				}
			}
			else
			{
				if(isSyncCall)
				{
					sc = m_clientStub.syncAddNonBlockSocketChannel(nChKey, strServerName);
					if(sc != null)
						System.out.println("Successfully added a nonblocking socket channel both at the client "
								+ "and the server: key("+nChKey+"), server("+strServerName+")");
					else
						System.err.println("Failed to add a nonblocking socket channel both at the client "
								+ "and the server: key("+nChKey+"), server("+strServerName+")");										
				}
				else
				{
					bResult = m_clientStub.addNonBlockSocketChannel(nChKey, strServerName);
					if(bResult)
						System.out.println("Successfully added a nonblocking socket channel at the client and "
								+ "requested to add the channel info to the server: key("+nChKey+"), server("
								+strServerName+")");
					else
						System.err.println("Failed to add a nonblocking socket channel at the client or "
								+ "failed to request to add the channel info to the server: key("+nChKey
								+"), server("+strServerName+")");					
				}
			}
				
			break;
		case CMInfo.CM_DATAGRAM_CHANNEL:
			bResult = m_clientStub.addDatagramChannel(nChPort);
			if(bResult)
				System.out.println("Successfully added a datagram socket channel: port("+nChPort+")");
			else
				System.err.println("Failed to add a datagram socket channel: port("+nChPort+")");
			break;
		case CMInfo.CM_MULTICAST_CHANNEL:
			bResult = m_clientStub.addMulticastChannel(strSessionName, strGroupName, strChAddress, nChPort);
			if(bResult)
			{
				System.out.println("Successfully added a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")");
			}
			else
			{
				System.err.println("Failed to add a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")");
			}
			break;
		default:
			System.out.println("Channel type is incorrect!");
			break;
		}
		
		System.out.println("======");
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
		String strBlock = null;
		boolean isBlock = false;
		String strSync = null;
		boolean isSyncCall = false;
		
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMUser myself = interInfo.getMyself();
			if(myself.getState() != CMInfo.CM_SESSION_JOIN && myself.getState() != CMInfo.CM_LOGIN)
			{
				System.out.println("You should login to the default server.");
				return;
			}
		}
		
		System.out.println("====== remove additional channel");
		try{
			System.out.print("Select channel type (SocketChannel:2, DatagramChannel:3, MulticastChannel:4): ");
			nChType = m_scan.nextInt();
			if(nChType == CMInfo.CM_SOCKET_CHANNEL)
			{
				System.out.print("is it a blocking channel? (\"y\": yes, \"n\": no): ");
				strBlock = m_scan.next();
				if(strBlock.equals("y")) isBlock = true;
				else if(strBlock.equals("n")) isBlock = false;
				else
				{
					System.err.println("invalid answer! : "+strBlock);
					return;
				}
			
				if(isBlock)
				{
					System.out.print("Channel key(>=0): ");
					nChKey = m_scan.nextInt();
					if(nChKey < 0)
					{
						System.err.println("testRemoveChannel(), invalid socket channel key ("+nChKey+")!");
						return;
					}
					System.out.print("Is the removal synchronous? (\"y\": yes, \"n\": no); ");
					strSync = m_scan.next();
					if(strSync.equals("y")) isSyncCall = true;
					else if(strSync.equals("n")) isSyncCall = false;
					else
					{
						System.err.println("Invalid answer! : "+strSync);
						return;
					}
				}
				else
				{
					System.out.print("Channel key(integer greater than 0): ");
					nChKey = m_scan.nextInt();
					if(nChKey <= 0)
					{
						System.err.println("testRemoveChannel(), invalid socket channel key ("+nChKey+")!");
						return;
					}
				}
				System.out.print("Server name(\"SERVER\" for the default server): ");
				strServerName = m_scan.next();
			}
			else if(nChType ==CMInfo.CM_DATAGRAM_CHANNEL)
			{
			System.out.print("Channel udp port: ");
			nChPort = m_scan.nextInt();			
			}
			else if(nChType == CMInfo.CM_MULTICAST_CHANNEL)
			{
				System.out.print("Target session name: ");
				strSessionName = m_scan.next();
				System.out.print("Target group name: ");
				strGroupName = m_scan.next();
				System.out.print("Multicast address: ");
				strChAddress = m_scan.next();
				System.out.print("Multicast port: ");
				nChPort = m_scan.nextInt();
			}
		}catch(InputMismatchException e){
			System.err.println("Invalid input type!");
			m_scan.next();
			return;
		}

		switch(nChType)
		{
		case CMInfo.CM_SOCKET_CHANNEL:
			if(isBlock)
			{
				if(isSyncCall)
				{
					result = m_clientStub.syncRemoveBlockSocketChannel(nChKey, strServerName);
					if(result)
						System.out.println("Successfully removed a blocking socket channel both "
								+ "at the client and the server: key("+nChKey+"), server ("+strServerName+")");
					else
						System.err.println("Failed to remove a blocking socket channel both at the client "
								+ "and the server: key("+nChKey+"), server ("+strServerName+")");					
				}
				else
				{
					result = m_clientStub.removeBlockSocketChannel(nChKey, strServerName);
					if(result)
						System.out.println("Successfully removed a blocking socket channel at the client and " 
								+ "requested to remove it at the server: key("+nChKey+"), server("+strServerName+")");
					else
						System.err.println("Failed to remove a blocking socket channel at the client or "
								+ "failed to request to remove it at the server: key("+nChKey+"), server("
								+strServerName+")");
				}
			}
			else
			{
				result = m_clientStub.removeNonBlockSocketChannel(nChKey, strServerName);
				if(result)
					System.out.println("Successfully removed a nonblocking socket channel: key("+nChKey
							+"), server("+strServerName+")");
				else
					System.err.println("Failed to remove a nonblocing socket channel: key("+nChKey
							+"), server("+strServerName+")");
			}
			
			break;
		case CMInfo.CM_DATAGRAM_CHANNEL:
			result = m_clientStub.removeAdditionalDatagramChannel(nChPort);
			if(result)
				System.out.println("Successfully removed a datagram socket channel: port("+nChPort+")");
			else
				System.err.println("Failed to remove a datagram socket channel: port("+nChPort+")");

			break;
		case CMInfo.CM_MULTICAST_CHANNEL:
			result = m_clientStub.removeAdditionalMulticastChannel(strSessionName, strGroupName, strChAddress, nChPort);
			if(result)
			{
				System.out.println("Successfully removed a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")");
			}
			else
			{
				System.err.println("Failed to remove a multicast channel: session("+strSessionName+"), group("
						+strGroupName+"), address("+strChAddress+"), port("+nChPort+")");
			}
			break;
		default:
			System.out.println("Channel type is incorrect!");
			break;
		}
		
		System.out.println("======");
	}
	
	public void testSetFilePath()
	{
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== set file path");
		String strPath = null;
		System.out.print("file path: ");
		try {
			strPath = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		if(!strPath.endsWith("/"))
		{
			System.out.println("Invalid file path!");
			return;
		}
		*/
		
		//CMFileTransferManager.setFilePath(strPath, m_clientStub.getCMInfo());
		m_clientStub.setFilePath(strPath);
		
		System.out.println("======");
	}
	
	public void testRequestFile()
	{
		boolean bReturn = false;
		String strFileName = null;
		String strFileOwner = null;
		String strFileAppend = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== request a file");
		try {
			System.out.print("File name: ");
			strFileName = br.readLine();
			System.out.print("File owner(enter for \"SERVER\"): ");
			strFileOwner = br.readLine();
			if(strFileOwner.isEmpty())
				strFileOwner = "SERVER";
			System.out.print("File append mode('y'(append);'n'(overwrite);''(empty for the default configuration): ");
			strFileAppend = br.readLine();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(strFileAppend.isEmpty())
			bReturn = m_clientStub.requestFile(strFileName, strFileOwner);
		else if(strFileAppend.equals("y"))
			bReturn = m_clientStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_APPEND);
		else if(strFileAppend.equals("n"))
			bReturn = m_clientStub.requestFile(strFileName,  strFileOwner, CMInfo.FILE_OVERWRITE);
		else
			System.err.println("wrong input for the file append mode!");
		
		if(!bReturn)
			System.err.println("Request file error! file("+strFileName+"), owner("+strFileOwner+").");
		
		System.out.println("======");
	}
	
	public void testPushFile()
	{
		String strFilePath = null;
		String strReceiver = null;
		boolean bReturn = false;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== push a file");
		
		try {
			System.out.print("File path name: ");
			strFilePath = br.readLine();
			System.out.print("File receiver (enter for \"SERVER\"): ");
			strReceiver = br.readLine();
			if(strReceiver.isEmpty())
				strReceiver = "SERVER";
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		bReturn = m_clientStub.pushFile(strFilePath, strReceiver);
		
		if(!bReturn)
			System.err.println("Push file error! file("+strFilePath+"), receiver("+strReceiver+")");
		
		System.out.println("======");
	}
	
	public void cancelRecvFile()
	{
		String strSender = null;
		boolean bReturn = false;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== cancel receiving a file");
		
		System.out.print("Input sender name (enter for all senders): ");
		try {
			strSender = br.readLine();
			if(strSender.isEmpty())
				strSender = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		bReturn = m_clientStub.cancelRequestFile(strSender);
		
		if(bReturn)
		{
			if(strSender == null)
				strSender = "all senders";
			System.out.println("Successfully requested to cancel receiving a file to ["+strSender+"].");
		}
		else
			System.err.println("Request failed to cancel receiving a file to ["+strSender+"]!");
		
		return;
	}
	
	public void cancelSendFile()
	{
		String strReceiver = null;
		boolean bReturn = false;
		System.out.println("====== cancel sending a file");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input receiver name (enter for all receivers): ");
		
		try {
			strReceiver = br.readLine();
			if(strReceiver.isEmpty())
				strReceiver = null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		bReturn = m_clientStub.cancelPushFile(strReceiver);
		
		if(bReturn)
		{
			if(strReceiver == null)
				strReceiver = "all receivers";
			System.out.println("Successfully requested to cancel sending a file to ["+strReceiver+"].");
		}
		else
			System.err.println("Request failed to cancel sending a file to ["+strReceiver+"]!");
		
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
			System.out.println("You must log in to the default server.");
			return;
		}
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("====== typical/internal forwarding test");
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
					System.out.println("Invalid forwarding type: "+nForwardType);
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
			System.out.println("You must log in to the default server.");
			return;
		}

		System.out.println("====== test delay of forwarding schemes (typical vs. internal");
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
				System.out.println("Invalid forward type: "+nForwardType);
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
		
		System.out.println("======");
		
		ue = null;
		return;
	}
	
	public void testDownloadNewSNSContent()
	{
		System.out.println("====== request downloading of SNS content (offset 0)");

		String strWriterName = null;
		String strInput = null;
		int nContentOffset = 0;
		String strUserName = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("Input offset(>= 0, Enter for 0): ");
			strInput = br.readLine();
			if(strInput.compareTo("") != 0)
				nContentOffset = Integer.parseInt(strInput);
			System.out.print("Content writer(Enter for no designation, "
					+ "CM_MY_FRIEND for my friends, CM_BI_FRIEND for bi-friends, or specify a name): ");
			strWriterName = br.readLine();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			System.err.println("Input data is not a number!");
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// start time of downloading contents
		m_eventHandler.setStartTime(System.currentTimeMillis());

		m_clientStub.requestSNSContent(strWriterName, nContentOffset);
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("["+strUserName+"] requests content of writer["+strWriterName
					+"] with offset("+nContentOffset+").");
		}

		System.out.println("======");
		return;
	}
	
	public void testRequestAttachedFileOfSNSContent()
	{
		System.out.println("===== Request an attached file of SNS content");
//		int nContentID = 0;
//		String strWriterName = null;
		String strFileName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		try {
//			System.out.print("SNS content ID: ");
//			nContentID = Integer.parseInt(br.readLine());
//			System.out.print("Writer name: ");
//			strWriterName = br.readLine();
			System.out.print("Attached file name: ");
			strFileName = br.readLine();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
//		m_clientStub.requestAttachedFileOfSNSContent(nContentID, strWriterName, strFileName);
		m_clientStub.requestAttachedFileOfSNSContent(strFileName);
		return;
	}
	
	public void testRepeatedSNSContentDownload()
	{
		System.out.println("====== Repeated downloading of SNS content");
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
		System.out.println("===== Request the next SNS content list");
		// start time of downloading contents
		m_eventHandler.setStartTime(System.currentTimeMillis());
		m_clientStub.requestNextSNSContent();
		
		return;
	}

	// download the previous SNS content list
	// if this method is called without any previous download request, it requests the most recent list
	public void testDownloadPreviousSNSContent()
	{
		System.out.println("===== Request the previous SNS content list");
		// start time of downloading contents
		m_eventHandler.setStartTime(System.currentTimeMillis());
		m_clientStub.requestPreviousSNSContent();
		
		return;
	}
	
	public void testSNSContentUpload()
	{
		String strMessage = null;
		int nNumAttachedFiles = 0;
		int nReplyOf = 0;
		int nLevelOfDisclosure = 0;
		ArrayList<String> filePathList = null;
		System.out.println("====== test SNS content upload");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.print("Input message: ");
			strMessage = br.readLine();
			System.out.print("Number of attached files: ");
			nNumAttachedFiles = Integer.parseInt(br.readLine());
			System.out.print("Content ID to which this content replies (0 for no reply): ");
			nReplyOf = Integer.parseInt(br.readLine());
			System.out.print("Level of Disclosure (0: to everyone, 1: to my followers, 2: to bi-friends, 3: nobody): ");
			nLevelOfDisclosure = Integer.parseInt(br.readLine());

			if(nNumAttachedFiles > 0)
			{
				String strPath = null;
				filePathList = new ArrayList<String>();
				System.out.println("Input path names of attahced files..");
				for(int i = 0; i < nNumAttachedFiles; i++)
				{
					System.out.print(i+": ");
					strPath = br.readLine();
					filePathList.add(strPath);
				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		String strUser = m_clientStub.getCMInfo().getInteractionInfo().getMyself().getName();
		m_clientStub.requestSNSContentUpload(strUser, strMessage, nNumAttachedFiles, nReplyOf, nLevelOfDisclosure, 
				filePathList);

		return;
	}
	
	public void testRegisterUser()
	{
		String strName = null;
		String strPasswd = null;
		String strRePasswd = null;
		String strEncPasswd = null;
		Console console = System.console();
		if(console == null)
		{
			System.err.println("Unable to obtain console.");
		}

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

		// encrypt password
		strEncPasswd = CMUtil.getSHA1Hash(strPasswd);

		//m_clientStub.registerUser(strName, strPasswd);
		m_clientStub.registerUser(strName, strEncPasswd);
		System.out.println("======");
		return;
	}
	
	public void testDeregisterUser()
	{
		String strName = null;
		String strPasswd = null;
		String strEncPasswd = null;
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
		
		// encrypt password
		strEncPasswd = CMUtil.getSHA1Hash(strPasswd);
		//m_clientStub.deregisterUser(strName, strPasswd);
		m_clientStub.deregisterUser(strName, strEncPasswd);
		System.out.println("======");
		return;
	}
	
	public void testFindRegisteredUser()
	{
		String strName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== search for a registered user");
		try {
			System.out.print("Input user name: ");
			strName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_clientStub.findRegisteredUser(strName);
		System.out.println("======");
		return;
	}
	
	public void testAddNewFriend()
	{
		String strFriendName = null;
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
		
		m_clientStub.addNewFriend(strFriendName);
		return;
	}
	
	public void testRemoveFriend()
	{
		String strFriendName = null;
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
		
		m_clientStub.removeFriend(strFriendName);
		return;
	}
	
	public void testRequestFriendsList()
	{
		System.out.println("====== request current friends list");
		m_clientStub.requestFriendsList();
		return;
	}
	
	public void testRequestFriendRequestersList()
	{
		System.out.println("====== request friend requesters list");
		m_clientStub.requestFriendRequestersList();
		return;
	}
	
	public void testRequestBiFriendsList()
	{
		System.out.println("====== request bi-directional friends list");
		m_clientStub.requestBiFriendsList();
		return;
	}
	
	public void testRequestServerInfo()
	{
		System.out.println("====== request additional server information");
		m_clientStub.requestServerInfo();
	}
	
	public void testConnectToServer()
	{
		System.out.println("====== connect to a designated server");
		String strServerName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input a server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		m_clientStub.connectToServer(strServerName);
		return;
	}
	
	public void testDisconnectFromServer()
	{
		System.out.println("===== disconnect from a designated server");
		String strServerName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input a server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		m_clientStub.disconnectFromServer(strServerName);
		return;
	}
	
	public void testLoginServer()
	{
		String strServerName = null;
		String user = null;
		String password = null;
		String strEncPasswd = null;
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
		
		System.out.println("======");
		return;
	}
	
	public void testLogoutServer()
	{
		String strServerName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== log out from a designated server");
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		m_clientStub.logoutCM(strServerName);
		System.out.println("======");
	}
	
	public void testRequestSessionInfoOfServer()
	{
		String strServerName = null;
		System.out.println("====== request session informatino of a designated server");
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_clientStub.requestSessionInfo(strServerName);
		System.out.println("======");
		return;
	}
	
	public void testJoinSessionOfServer()
	{
		String strServerName = null;
		String strSessionName = null;
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
		m_clientStub.joinSession(strServerName, strSessionName);
		System.out.println("======");
		return;
	}
	
	public void testLeaveSessionOfServer()
	{
		String strServerName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== leave a session of a designated server");
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_clientStub.leaveSession(strServerName);
		System.out.println("======");
		return;
	}
	
	public void testPrintGroupInfoOfServer()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		CMUser myself = interInfo.getMyself();
		
		String strServerName = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("====== print group information a designated server");
		System.out.print("Input server name: ");
		try {
			strServerName = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(strServerName.equals("SERVER"))
		{
			testPrintGroupInfo();
			return;
		}
		
		CMServer server = interInfo.findAddServer(strServerName);
		if(server == null)
		{
			System.out.println("server("+strServerName+") not found in the add-server list!");
			return;
		}
		
		CMSession session = server.findSession(myself.getCurrentSession());
		Iterator<CMGroup> iter = session.getGroupList().iterator();
		System.out.println("---------------------------------------------------------");
		System.out.format("%-20s%-20s%-20s%n", "group name", "multicast addr", "multicast port");
		System.out.println("---------------------------------------------------------");
		while(iter.hasNext())
		{
			CMGroupInfo gInfo = iter.next();
			System.out.format("%-20s%-20s%-20d%n", gInfo.getGroupName(), gInfo.getGroupAddress()
					, gInfo.getGroupPort());
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
		
		strFileList.trim();
		strFiles = strFileList.split("\\s+");
		if(strFiles.length != nFileNum)
		{
			System.out.println("The number of files incorrect!");
			return;
		}
		
		for(int i = 0; i < nFileNum; i++)
		{
			switch(nMode)
			{
			case 1: // push
				CMFileTransferManager.pushFile(strFiles[i], strTarget, m_clientStub.getCMInfo());
				break;
			case 2: // pull
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
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("====== split a file");
		try {
			System.out.print("Input source file name: ");
			strSrcFile = br.readLine();
			System.out.print("Input the number of splitted files: ");
			nSplitNum = Integer.parseInt(br.readLine());
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
		
		/*
		strFileList.trim();
		strFiles = strFileList.split("\\s+");
		if(nFileNum != strFiles.length)
		{
			System.out.println("Wrong number of input files!");
			return;
		}
		*/
		
		// make list of split file names
		strFiles = new String[nFileNum];
		for(int i = 0; i < nFileNum; i++)
		{
			strFiles[i] = strFilePrefix + "-" + (i+1) + ".split";
		}
		
		lMergeFileSize = CMFileTransferManager.mergeFiles(strFiles, nFileNum, strMergeFileName);
		System.out.println("Size of merged file("+strMergeFileName+"): "+lMergeFileSize+" Bytes.");
		return;
	}
	
	public void testDistFileProc()
	{
		CMInteractionInfo interInfo = m_clientStub.getCMInfo().getInteractionInfo();
		//CMFileTransferInfo fileInfo = m_clientStub.getCMInfo().getFileTransferInfo();
		String strFile = null;
		long lFileSize = 0;
		CMFileEvent fe = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("====== split a file, distribute to multiple servers, and merge");
		
		// check if the client logs in to all available servers
		int nClientState = interInfo.getMyself().getState();
		if(nClientState == CMInfo.CM_INIT || nClientState == CMInfo.CM_CONNECT)
		{
			System.out.println("You must log in the default server!");
			return;
		}
		Iterator<CMServer> iter = interInfo.getAddServerList().iterator();
		while(iter.hasNext())
		{
			CMServer tserver = iter.next();
			nClientState = tserver.getClientState();
			if(nClientState == CMInfo.CM_INIT || nClientState == CMInfo.CM_CONNECT)
			{
				System.out.println("You must log in the additional server("+tserver.getServerName()
						+")!");
				return;
			}
		}

		// input file name
		try {
			//System.out.println("A source file must exists in the file path configured in CM");
			System.out.print("Input a source file path: ");
			strFile = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// print the file size
		//strFile = fileInfo.getFilePath()+"/"+strFile;
		File srcFile = new File(strFile);
		lFileSize = srcFile.length();
		System.out.println("Source file ("+strFile+"): "+lFileSize+" Bytes.");

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
		System.out.println("Source file extension: "+m_eventHandler.getFileExtension());

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
			System.out.println("File piece name: "+strPieceName);

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
			System.out.println("File piece name: "+strPieceName);

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
		System.out.println("====== test multicast chat in current group");

		// check user state
		CMUser myself = interInfo.getMyself();
		if(myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.out.println("You must join a session and a group for multicasting.");
			return;
		}

		// check communication architecture
		if(!confInfo.getCommArch().equals("CM_PS"))
		{
			System.out.println("CM must start with CM_PS mode which enables multicast per group!");
			return;
		}

		// receive a user input message
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		System.out.print("Input message: ");
		String strMessage = null;
		try {
			strMessage = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
				System.err.println("You should login to the default server.");
				return;
			}
		}
		
		System.out.println("============= get blocking socket channel");

		System.out.print("Channel key (>=0): ");
		nChKey = m_scan.nextInt();
		
		if(nChKey < 0)
		{
			System.err.println("Invalid channel key: "+nChKey);
			return;
		}
		
		System.out.print("Server name (\"SERVER\" for the default server): ");
		strServerName = m_scan.next();

		/*
		 * scan.next() method never returns before any user given input.
		 * 
		if(strServerName == null || strServerName.equals(""))
			strServerName = "SERVER"; // default server name
		*/

		sc = m_clientStub.getBlockSocketChannel(nChKey, strServerName);
		
		if(sc == null)
		{
			System.err.println("Blocking socket channel not found: key("+nChKey+"), server("+strServerName+")");
		}
		else
		{
			System.out.println("Blocking socket channel found: key("+nChKey+"), server("+strServerName+")");
		}
		
		return;		
	}
	
	public void testMeasureInputThroughput()
	{
		String strTarget = null;
		float fSpeed = -1; // MBps
		System.out.println("========== test input network throughput");
		System.out.print("target node (\"SERVER\" for the default server): ");
		strTarget = m_scan.next();
		fSpeed = m_clientStub.measureInputThroughput(strTarget);
		if(fSpeed == -1)
			System.err.println("Test failed!");
		else
			System.out.format("Input network throughput from [%s] : %.2f%n", strTarget, fSpeed);
	}
	
	public void testMeasureOutputThroughput()
	{
		String strTarget = null;
		float fSpeed = -1; // MBps
		System.out.println("========== test output network throughput");
		System.out.print("target node (\"SERVER\" for the default server): ");
		strTarget = m_scan.next();
		fSpeed = m_clientStub.measureOutputThroughput(strTarget);
		if(fSpeed == -1)
			System.err.println("Test failed!");
		else
			System.out.format("Output network throughput to [%s] : %.2f%n", strTarget, fSpeed);
	}
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		CMClientApp client = new CMClientApp();
		CMClientStub cmStub = client.getClientStub();
		cmStub.setEventHandler(client.getClientEventHandler());
		boolean bRet = cmStub.startCM();
		if(!bRet)
		{
			System.err.println("CM initialization error!");
			return;
		}
		client.startTest();
		
		System.out.println("Client application is terminated.");
	}

}
