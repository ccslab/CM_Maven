package kr.ac.konkuk.ccslab.cm.manager;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMByteReceiver;
import kr.ac.konkuk.ccslab.cm.thread.CMByteSender;

public class CMCommManager {
	
	public static void terminate(CMInfo cmInfo)
	{
		// close all channels in CM
		
		CMCommInfo commInfo = cmInfo.getCommInfo();
		
		//nonblocking serversocket channel (server)
		ServerSocketChannel ssc = commInfo.getNonBlockServerSocketChannel();
		if(ssc != null && ssc.isOpen())
		{
			try {
				ssc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		// blocking serversocket channel (server)
		ssc = commInfo.getBlockServerSocketChannel();
		if(ssc != null && ssc.isOpen()){
			try {
				ssc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//nonblocking datagram channel
		CMChannelInfo<Integer> dcInfo = commInfo.getNonBlockDatagramChannelInfo();
		dcInfo.removeAllChannels();
		
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
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
		Iterator<CMUser> iterUser = interInfo.getLoginUsers().getAllMembers().iterator();
		while(iterUser.hasNext())
		{
			CMUser tUser = iterUser.next();
			CMChannelInfo<Integer> chInfo = tUser.getNonBlockSocketChannelInfo();
			chInfo.removeAllChannels();
			chInfo = tUser.getBlockSocketChannelInfo();
			chInfo.removeAllChannels();
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
	
	public static String getLocalIP()
	{
		String strIP = null;
		String strIPByGetLocalHost = null;
		InetAddress localAddress = null;
		try {
			localAddress = InetAddress.getLocalHost();
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		strIPByGetLocalHost = localAddress.getHostAddress();
		if(CMInfo._CM_DEBUG)
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
				
				if(CMInfo._CM_DEBUG)
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
						
					//InetAddress inetAddress = enumIA.nextElement();
					if(CMInfo._CM_DEBUG)
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
					if(!inetAddress.isLoopbackAddress())
					{
						 strIP = inetAddress.getHostAddress().toString();
						 if(CMInfo._CM_DEBUG)
							 System.out.println("    :detected as the local IP");
					}
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		if(strIP == null)
		{
			System.err.println("CMCommManager.getLocalIP(), cannot find local IP! use the result of "
					+ "InetAddress.getLocalHost().");
			strIP = strIPByGetLocalHost;
		}
		else
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("------ detected local IP: "+strIP);
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("------ end of detecting IPv4 address");

		return strIP;
	}
	
	public static SelectableChannel openNonBlockChannel(int channelType, String address, int port, CMInfo cmInfo) throws IOException
	{
		SelectableChannel ch = null;
		CMCommInfo commInfo = cmInfo.getCommInfo();
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
	
	public static SelectableChannel openBlockChannel(int channelType, String address, int port, CMInfo cmInfo) throws IOException
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
	
	public static MembershipKey joinMulticastGroup(DatagramChannel dc, String addr)
	{
		NetworkInterface ni = null;
		InetAddress group = null;
		MembershipKey key = null;
		
		try {
			//ni = NetworkInterface.getByName("eth3");
			ni = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
		} catch (SocketException | UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			group = InetAddress.getByName(addr);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		try {
			key = dc.join(group, ni);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return key;
	}
	
	public static CMByteReceiver startReceivingMessage(CMInfo cmInfo)
	{
		Selector sel = cmInfo.getCommInfo().getSelector();
		CMBlockingEventQueue recvQueue = cmInfo.getCommInfo().getRecvBlockingEventQueue();
		CMByteReceiver byteReceiver = new CMByteReceiver(sel, recvQueue);
		byteReceiver.start();
		cmInfo.getCommInfo().setByteReceiver(byteReceiver);
		
		return byteReceiver;
	}
	
	public static CMByteSender startSendingMessage(CMInfo cmInfo)
	{
		CMBlockingEventQueue sendQueue = cmInfo.getCommInfo().getSendBlockingEventQueue();
		CMByteSender byteSender = new CMByteSender(sendQueue);
		byteSender.start();
		cmInfo.getCommInfo().setByteSender(byteSender);
		
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
				// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
}
