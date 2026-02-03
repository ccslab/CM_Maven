package kr.ac.konkuk.ccslab.cm.manager;
import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.info.*;
import kr.ac.konkuk.ccslab.cm.thread.CMByteReceiver;
import kr.ac.konkuk.ccslab.cm.thread.CMByteSender;

public class CMCommManager {
	
	public static void terminate()
	{
		// close all channels in CM
		
		CMCommInfo commInfo = CMCommInfo.getInstance();
		
		//nonblocking serversocket channel (server)
		ServerSocketChannel ssc = commInfo.getNonBlockServerSocketChannel();
		if(ssc != null && ssc.isOpen())
		{
			try {
				ssc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		// blocking serversocket channel (server)
		ssc = commInfo.getBlockServerSocketChannel();
		if(ssc != null && ssc.isOpen()){
			try {
				ssc.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		//nonblocking datagram channel
		CMChannelInfo<Integer> dcInfo = commInfo.getNonBlockDatagramChannelInfo();
		dcInfo.removeAllChannels();
		
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		// socket channels to the default server (client, additional server)
		CMChannelInfo<Integer> scInfo = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
		scInfo.removeAllChannels();
		scInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
		scInfo.removeAllChannels();
		
		// socket channels to additional servers (client)
		Iterator<CMServer> iterServer = interInfo.getAddServerList().iterator();
		while(iterServer.hasNext())
		{
			CMServer tServer = iterServer.next();
			CMChannelInfo<Integer> tscInfo = tServer.getNonBlockSocketChannelInfo();
			tscInfo.removeAllChannels();
			tscInfo = tServer.getBlockSocketChannelInfo();
			tscInfo.removeAllChannels();
		}
		
		// socket channel of users (server)
		// [MODIFIED START]
		// Adapted iteration logic to support the change in CMMember structure (Hashtable<String, List<CMUser>>)
		// for multi-login.
		// The getAllMembers() now returns a Hashtable, so we iterate through the values (List<CMUser>).
		Hashtable<String, List<CMUser>> loginUserTable = interInfo.getLoginUsers().getAllMembers();
		for(List<CMUser> userList : loginUserTable.values())
		{
			for(CMUser tUser : userList)
			{
				CMChannelInfo<Integer> chInfo = tUser.getNonBlockSocketChannelInfo();
				chInfo.removeAllChannels();
				chInfo = tUser.getBlockSocketChannelInfo();
				chInfo.removeAllChannels();
			}
		}

		// multicast channel
		Iterator<CMSession> iterSession = interInfo.getSessionList().iterator();
		while(iterSession.hasNext())
		{
			CMSession tSession = iterSession.next();
			Iterator<CMGroup> iterGroup = tSession.getGroupList().iterator();
			while(iterGroup.hasNext())
			{
				CMGroup tGroup = iterGroup.next();
				CMChannelInfo<InetSocketAddress> mcInfo = tGroup.getMulticastChannelInfo();
				mcInfo.removeAllChannels();
			}
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMCommManager.terminate(), close and remove all channels.");
	}
	
	public static List<String> getLocalIPList()
	{
		String strIPByGetLocalHost = null;
		InetAddress localAddress = null;
		List<InetAddress> myInetAddressList = new ArrayList<InetAddress>();
		List<String> myInetAddressStrList = null;
		
		try {
			localAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		strIPByGetLocalHost = localAddress.getHostAddress();
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("------ detecting IPv4 addresses");
			System.out.println("local address by InetAddress.getLocalHost(): "+strIPByGetLocalHost);
		}
		
		try{

			/// enumerate IP addresses bound to the local host
			Enumeration<NetworkInterface> nienum = NetworkInterface.getNetworkInterfaces();
			//while (nienum.hasMoreElements())
			for(NetworkInterface ni : Collections.list(nienum))
			{
				//NetworkInterface ni = nienum.nextElement();
				
				if(ni.isPointToPoint())
					continue;
				
				if(CMInfo._CM_DEBUG_2)
				{
					System.out.println("network interface name: "+ni.getName());
					System.out.println("  :isLoopback("+ni.isLoopback()+"), isPointToPoint("+ni.isPointToPoint()+
							"), isUp("+ni.isUp()+"), isVirtual("+ni.isVirtual()+")");
				}
				
				Enumeration<InetAddress> enumIA = ni.getInetAddresses();
				//while (enumIA.hasMoreElements())
				for(InetAddress inetAddress : Collections.list(enumIA))
				{
					if( !(inetAddress instanceof Inet4Address) )
						continue;
						
					// add to myInetAddressList
					myInetAddressList.add(inetAddress);
					
					//InetAddress inetAddress = enumIA.nextElement();
					if(CMInfo._CM_DEBUG_2)
					{
						System.out.println("  detected inetAddress: " + inetAddress.getHostAddress());
						System.out.println("    :isLoopback("+inetAddress.isLoopbackAddress()+"), isLinkLocal("
								+inetAddress.isLinkLocalAddress()+"), isSiteLocal("+inetAddress.isSiteLocalAddress()
								+")");
						if(inetAddress instanceof Inet4Address)
							System.out.println("    :detected as the IP4 address");
						else if(inetAddress instanceof Inet6Address)
							System.out.println("    :detected as the IP6 address");
					}
					
					//if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && 
					//inetAddress.isSiteLocalAddress())
//					if(!inetAddress.isLoopbackAddress())
//					{
//						 strIP = inetAddress.getHostAddress().toString();
//						 if(CMInfo._CM_DEBUG_2)
//							 System.out.println("    :detected as the local IP");
//					}
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		if(myInetAddressList.isEmpty())
		{
			System.err.println("CMCommManager.getLocalIP(), Inet4Address not found!");
			return null;
		}

		// sort the InetAddress list
		// public address > site local address > loopback address
		myInetAddressStrList = myInetAddressList.stream()
				.sorted(
					(addr1, addr2) -> {
						int score1 = 0;
						int score2 = 0;
						score1 += addr1.isLoopbackAddress() ? 3 : 0;
						score1 += addr1.isSiteLocalAddress() ? 2 : 0;
						score1 += addr1.isLinkLocalAddress() ? 1 : 0;
						score2 += addr2.isLoopbackAddress() ? 3 : 0;
						score2 += addr2.isSiteLocalAddress() ? 2 : 0;
						score2 += addr2.isLinkLocalAddress() ? 1 : 0;
						return score1-score2;
					}
				)
				.map(addr -> addr.getHostAddress().toString())
				.collect(Collectors.toList());
		
		if(CMInfo._CM_DEBUG_2) {
			System.out.print("------ detected local IP list: ");
			for(String strAddr : myInetAddressStrList)
				System.out.print(strAddr+" ");
			System.out.println();
		}

		
		return myInetAddressStrList;
	}
	
	public static SelectableChannel openNonBlockChannel(int channelType, String address, int port) throws IOException
	{
		SelectableChannel ch = null;
		CMCommInfo commInfo = CMCommInfo.getInstance();
		Selector sel = commInfo.getSelector();
		
		switch(channelType)
		{
		case CMInfo.CM_SERVER_CHANNEL: // address not used
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			ssc.socket().bind(new InetSocketAddress(port));
			ssc.configureBlocking(false);
			ssc.register(sel, SelectionKey.OP_ACCEPT);
			//commInfo.setNonBlockServerSocketChannel(ssc);
			ch = ssc;
			break;
		case CMInfo.CM_SOCKET_CHANNEL:
			SocketChannel sc = SocketChannel.open(new InetSocketAddress(address, port));
			sc.configureBlocking(false);
			sc.register(sel, SelectionKey.OP_READ);
			//commInfo.addSocketChannel(sc);
			ch = sc;
			break;
		case CMInfo.CM_DATAGRAM_CHANNEL:
			DatagramChannel dc = DatagramChannel.open();
			dc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			dc.socket().bind(new InetSocketAddress(address, port));
			dc.configureBlocking(false);
			dc.register(sel, SelectionKey.OP_READ);
			//commInfo.addDatagramChannel(dc);
			ch = dc;
			break;
		case CMInfo.CM_MULTICAST_CHANNEL:
			//NetworkInterface ni = NetworkInterface.getByName("eth3");
			NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			if(ni == null)
			{
				System.out.println("CMCommManager.openNonBlockChannel(), MULTICAST failed!");
				return null;
			}
			DatagramChannel mc = DatagramChannel.open(StandardProtocolFamily.INET)
					.setOption(StandardSocketOptions.SO_REUSEADDR, true)
					.bind(new InetSocketAddress(port))
					.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
			mc.configureBlocking(false);
			mc.register(sel, SelectionKey.OP_READ);
			ch = mc;
			break;
		default:
			System.out.println("CMCommManager.openNonBlockChannel(), unknown channel type: "+channelType);
			return null;
		}
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMCommManager.openNonBlockChannel(), Ok, type("+channelType+"), address("
					+address+"), port("+port+") hashcode("+ch.hashCode()+").");
			System.out.println("# registered selection keys: "+sel.keys().size());
		}
		
		return ch;
	}
	
	public static SelectableChannel openBlockChannel(int channelType, String address, int port) throws IOException
	{
		SelectableChannel ch = null;
		
		switch(channelType)
		{
		case CMInfo.CM_SERVER_CHANNEL: // address not used
			ServerSocketChannel ssc = ServerSocketChannel.open();
			ssc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			ssc.socket().bind(new InetSocketAddress(port));
			ssc.configureBlocking(true);
			ch = ssc;
			break;
		case CMInfo.CM_SOCKET_CHANNEL:
			SocketChannel sc = SocketChannel.open(new InetSocketAddress(address, port));
			sc.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
			sc.configureBlocking(true);
			ch = sc;
			break;
		case CMInfo.CM_DATAGRAM_CHANNEL:
			DatagramChannel dc = DatagramChannel.open();
			dc.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			dc.socket().bind(new InetSocketAddress(address, port));
			dc.configureBlocking(true);
			ch = dc;
			break;
		case CMInfo.CM_MULTICAST_CHANNEL:
			NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
			if(ni == null)
			{
				System.out.println("CMCommManager.openBlockSocketChannel(), MULTICAST failed!");
				return null;
			}
			DatagramChannel mc = DatagramChannel.open(StandardProtocolFamily.INET)
					.setOption(StandardSocketOptions.SO_REUSEADDR, true)
					.bind(new InetSocketAddress(port))
					.setOption(StandardSocketOptions.IP_MULTICAST_IF, ni);
			mc.configureBlocking(true);
			ch = mc;
			break;
		default:
			System.out.println("CMCommManager.openBlockSocketChannel(), unknown channel type: "+channelType);
			return null;
		}
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMCommManager.openBlockSocketChannel(), Ok, type("+channelType+"), address("
					+address+"), port("+port+") hashcode("+ch.hashCode()+").");
		}
		
		return ch;		
	}
	
	public static SocketChannel addBlockSocketChannel(int nChKey, String strTarget, UUID targetUuid)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMUser myself = interInfo.getMyself();
		CMServer serverInfo = null;
		CMUser targetUser = null;
		SocketChannel sc = null;
		String strTargetSSCAddress = null;
		int nTargetSSCPort = -1;
		CMChannelInfo<Integer> scInfo = null;
		boolean bRet = false;

		//if(getMyself().getState() == CMInfo.CM_INIT || getMyself().getState() == CMInfo.CM_CONNECT)
		if(myself.getState() < CMInfo.CM_LOGIN)
		{
			System.err.println("CMCommManager.addBlockSocketChannel(), you must log in to the default server!");
			return null;
		}
		
		serverInfo = CMInteractionManager.findServer(strTarget);
		if(serverInfo != null)
		{
			scInfo = serverInfo.getBlockSocketChannelInfo();
			strTargetSSCAddress = serverInfo.getServerAddress();
			nTargetSSCPort = serverInfo.getServerPort();			
		}
		else
		{
			targetUser = CMInteractionManager.findGroupMemberOfClient(strTarget, targetUuid);
			if(targetUser == null)
			{
				System.err.println("CMCommManager.addBlockSocketChannel(), target user("
						+strTarget+") with UUID("+targetUuid+") not found!");
				return null;
			}
			
			scInfo = targetUser.getBlockSocketChannelInfo();
			strTargetSSCAddress = targetUser.getHost();
			nTargetSSCPort = targetUser.getSSCPort();
		}
			
		sc = (SocketChannel) scInfo.findChannel(nChKey);
		if(sc != null)
		{
			System.err.println("CMCommManager.addBlockSocketChannel(), failed!: key("
					+nChKey+"), target("+strTarget+"), UUID("+targetUuid+")");
			return null;
		}

		try {
			sc = (SocketChannel) openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, strTargetSSCAddress, 
					nTargetSSCPort);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(sc == null)
		{
			System.err.println("CMCommManager.addBlockSocketChannel(), failed!: key("
					+nChKey+"), target("+strTarget+")");
			return null;
		}
		
		scInfo.addChannel(nChKey, sc);
		
		CMSessionEvent se = new CMSessionEvent();
		se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
		se.setChannelName(myself.getName());
		se.setChannelNum(nChKey);
		se.setChannelUuid(myself.getUuid());
		bRet = CMEventManager.unicastEvent(se, strTarget, targetUuid, CMInfo.CM_STREAM, nChKey, true);

		if(bRet && CMInfo._CM_DEBUG)
		{
			System.out.println("CMCommManager.addBlockSocketChannel(),successfully requested to add the channel "
					+ "with the key("+nChKey+") to the target("+strTarget+") with UUID("+targetUuid+")");
		}

		return sc;
	}
	
	public static boolean removeBlockSocketChannel(int nChKey, String strTarget, UUID targetUuid)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMUser myself = interInfo.getMyself();
		CMServer serverInfo = null;
		CMUser targetUser = null;
		CMChannelInfo<Integer> scInfo = null;
		boolean result = false;
		SocketChannel sc = null;
		CMSessionEvent se = null;
		String strDefServer = interInfo.getDefaultServerInfo().getServerName();

		if(myself.getState() < CMInfo.CM_LOGIN)
		{
			System.err.println("CMCommManager.removeBlockSocketChannel(), you must log in to the default server!");
			return false;
		}
		
		if(strTarget.equals(strDefServer))
		{
			serverInfo = interInfo.getDefaultServerInfo();
			scInfo = serverInfo.getBlockSocketChannelInfo();
		}
		else if( (serverInfo = interInfo.findAddServer(strTarget)) != null ) 
		{
			scInfo = serverInfo.getBlockSocketChannelInfo();
		}
		else
		{
			targetUser = CMInteractionManager.findGroupMemberOfClient(strTarget, targetUuid);
			if(targetUser == null)
			{
				System.err.println("CMCommManager.removeBlockSocketChannel(), target user("
						+strTarget+") not found!");
				return false;
			}
			
			scInfo = targetUser.getBlockSocketChannelInfo();			
		}
		
		sc = (SocketChannel) scInfo.findChannel(nChKey);
		if(sc == null)
		{
			System.err.println("CMCommManager.removeBlockSocketChannel(), "
					+ "socket channel not found! key("+nChKey+"), target("+strTarget+"), uuid("+targetUuid+")!");
			return false;
		}
		
		se = new CMSessionEvent();
		se.setID(CMSessionEvent.REMOVE_BLOCK_SOCKET_CHANNEL);
		se.setChannelNum(nChKey);
		se.setChannelName(myself.getName());
		se.setChannelUuid(myself.getUuid());
		
		// If targetUser is not null, (that is, if the target is a client instead of a server,)
		// the request event should be forwarded by the default server (internal forwarding of CM).
		if(targetUser != null)
		{
			// set distribution fields
			se.setDistributionSession("CM_ONE_USER");
			se.setDistributionGroup(strTarget);
			se.setDistributionUuid(targetUuid);

			// send the event to the default server
			result = CMEventManager.unicastEvent(se, strDefServer);
		}
		else
		{
			// send the event to the target
			result = CMEventManager.unicastEvent(se, strTarget, targetUuid);
		}

		// The channel will be closed and removed after the client receives the ACK event at the event handler.
		return result;
	}
	
	public static MembershipKey joinMulticastGroup(DatagramChannel dc, String addr)
	{
		NetworkInterface ni = null;
		InetAddress group = null;
		MembershipKey key = null;
		
		try {
			//ni = NetworkInterface.getByName("eth3");
			ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
		} catch (SocketException | UnknownHostException e) {
			e.printStackTrace();
		}

		try {
			group = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		try {
			key = dc.join(group, ni);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return key;
	}
	
	public static CMByteReceiver startReceivingMessage()
	{
		CMInfo cmInfo = CMInfo.getInstance();
		ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
		CMByteReceiver byteReceiver = new CMByteReceiver();
		//byteReceiver.start();
		Future<?> future = es.submit(byteReceiver);
		CMCommInfo.getInstance().setByteReceiver(byteReceiver);
		CMCommInfo.getInstance().setByteReceiverFuture(future);
		
		return byteReceiver;
	}
	
	public static CMByteSender startSendingMessage()
	{
		CMInfo cmInfo = CMInfo.getInstance();
		ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
		CMByteSender byteSender = new CMByteSender();
		//byteSender.start();
		Future<?> future = es.submit(byteSender);
		CMCommInfo.getInstance().setByteSender(byteSender);
		CMCommInfo.getInstance().setByteSenderFuture(future);
		
		return byteSender;
	}

	
	public static int sendMessage(ByteBuffer buf, SocketChannel sc)
	{
		int nTotalSentByteNum = 0;
		int nRet = 0;
		
		// initialize the byte buffer
		buf.clear();
		
		while(buf.hasRemaining())
		{
			try {
				nRet = sc.write(buf);
			} catch (IOException e) {
				e.printStackTrace();
				return nTotalSentByteNum;
			}
			
			nTotalSentByteNum += nRet;

			if(CMInfo._CM_DEBUG_2)
			{
				System.out.println("CMCommManager.sendMessage(), SocketChannel has sent "
						+nRet+" bytes.");
			}
		}

		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMCommManager.sendMessage(), SocketChannel completes to send "
					+nTotalSentByteNum+" byets.");
		}
		
		if(buf != null)
			buf = null;
		
		return nTotalSentByteNum;
	}
	
	public static int sendMessage(ByteBuffer buf, DatagramChannel dc, String addr, int port)
	{
		int nTotalSentByteNum = 0;
		int nRet = 0;
		
		// initialize the byte buffer
		buf.clear();
		
		while(buf.hasRemaining())
		{
			try {
				nRet = dc.send(buf, new InetSocketAddress(addr, port));
			} catch (IOException e) {
				e.printStackTrace();
				return nTotalSentByteNum;
			}
			
			nTotalSentByteNum += nRet;

			if(CMInfo._CM_DEBUG_2)
			{
				System.out.println("CMCommManager.sendMessage(), DatagramChannel has sent "
						+nRet+" bytes to ("+addr+", "+port+").");
			}
		}

		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMCommManager.sendMessage(), DatagramChannel completes to send "
					+nTotalSentByteNum+" byets to ("+addr+", "+port+").");
		}
		
		if(buf != null)
			buf = null;
		
		return nTotalSentByteNum;
	}

	public static double measureInputThroughput(String target, UUID targetUuid) {
		if(CMInfo._CM_DEBUG) {
			System.out.println("=== CMCommManager.measureInputThroughput() called..");
			System.out.println("target (" + target + ", target uuid(" + targetUuid + ").");
		}

		// check the current thread id
		long threadId = Thread.currentThread().getId();
		CMThreadInfo threadInfo = Objects.requireNonNull(CMThreadInfo.getInstance());
		if(threadId == threadInfo.getEventReceiverId()) {
			System.err.println("The current thread is the event-receiver thread ("+threadId+")!");
			return -1;
		}

		boolean bReturn = false;
		double speed = 0.0;
		long lFileSize = -1;	// the size of a file to measure the transmission delay
		long lTransDelay = -1;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMEventInfo eInfo = CMEventInfo.getInstance();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMFileEvent replyEvent = null;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();

		bReturn = CMFileTransferManager.requestPermitForPullFile(CMInfo.THROUGHPUT_TEST_FILE,
				target, targetUuid, CMInfo.FILE_OVERWRITE, -1);

		if(!bReturn)
			return -1;

		eventSync.init();
		if(confInfo.isFileTransferScheme())
			eventSync.setWaitedEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER_CHAN, target, targetUuid);
		else
			eventSync.setWaitedEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER, target, targetUuid);

		synchronized(eventSync)
		{
			try {
				eventSync.wait(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			replyEvent = (CMFileEvent) eventSync.getReplyEvent();
			if(replyEvent == null)
			{
				System.err.println("CMCommManager.measureInputThroughput(), timeout expired!");
				CMFileTransferManager.cancelPullFile(target, targetUuid);
				return -1;
			}

			lFileSize = replyEvent.getFileSize();
		}

		if(replyEvent.getID() == CMFileEvent.REPLY_PERMIT_PULL_FILE)
		{
			return -1;
		}

		lTransDelay = fInfo.getEndRecvTime() - fInfo.getStartRecvTime();	// millisecond
		speed = ((double)lFileSize / 1000000) / ((double)lTransDelay / 1000);	// MBps

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMCommManager.measureInputThroughput(); received file size("
					+lFileSize+"), delay("+lTransDelay+" ms), speed("+speed+" MBps)");
		}

		return speed;
	}

	public static double measureOutputThroughput(String target, UUID targetUuid) {
		if(CMInfo._CM_DEBUG) {
			System.out.println("=== CMCommManager.measureOutputThroughput() called..");
			System.out.println("target = " + target);
			System.out.println("target (" + target + ", target uuid(" + targetUuid + ").");
		}

		// check the current thread id
		long threadId = Thread.currentThread().getId();
		CMThreadInfo threadInfo = Objects.requireNonNull(CMThreadInfo.getInstance());
		if(threadId == threadInfo.getEventReceiverId()) {
			System.err.println("The current thread is the event-receiver thread ("+threadId+")!");
			return -1;
		}

		boolean bReturn = false;
		double speed = -1;
		long lFileSize = -1;	// the size of a file to measure the transmission delay
		long lTransDelay = -1;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMEventInfo eInfo = CMEventInfo.getInstance();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMFileEvent replyEvent = null;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		String strFilePath = confInfo.getTransferedFileHome().toString() + File.separator + CMInfo.THROUGHPUT_TEST_FILE;

		//bReturn = CMFileTransferManager.pushFile(strFilePath, strTarget, CMInfo.FILE_OVERWRITE, m_cmInfo);
		bReturn = CMFileTransferManager.requestPermitForPushFile(strFilePath, target, targetUuid,
				CMInfo.FILE_OVERWRITE, -1);

		if(!bReturn)
			return -1;

		eventSync.init();
		if(confInfo.isFileTransferScheme())
			eventSync.setWaitedEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER_CHAN_ACK, target, targetUuid);
		else
			eventSync.setWaitedEvent(CMInfo.CM_FILE_EVENT, CMFileEvent.END_FILE_TRANSFER_ACK, target, targetUuid);

		synchronized(eventSync)
		{
			try {
				eventSync.wait(20000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			replyEvent = (CMFileEvent) eventSync.getReplyEvent();
			if(replyEvent == null)
			{
				System.err.println("CMStub.measureOutputThroughput(), timeout expired!");
				CMFileTransferManager.cancelPushFile(target, targetUuid);
				return -1;
			}

			lFileSize = replyEvent.getFileSize();
		}

		lTransDelay = fInfo.getEndSendTime() - fInfo.getStartSendTime();	// millisecond
		speed = ((double)lFileSize / 1000000) / ((double)lTransDelay / 1000);	// MBps

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMStub.measureOutputThroughput(); received file size("+lFileSize+"), delay("
					+lTransDelay+" ms), speed("+speed+" MBps)");
		}

		return speed;
	}
}
