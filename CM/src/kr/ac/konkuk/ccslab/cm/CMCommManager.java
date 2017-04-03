package kr.ac.konkuk.ccslab.cm;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class CMCommManager {
	
	public static void terminate(CMInfo cmInfo)
	{
		// close all channels in CM
		
		CMCommInfo commInfo = cmInfo.getCommInfo();
		//serversocket channel (server)
		ServerSocketChannel ssc = commInfo.getServerSocketChannel();
		if(ssc != null && ssc.isOpen())
		{
			try {
				ssc.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		//datagram channel
		CMChannelInfo dcInfo = commInfo.getDatagramChannelInfo();
		dcInfo.removeAllChannels();
		
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		// socket channels to the default server (client, additional server)
		CMChannelInfo scInfo = interInfo.getDefaultServerInfo().getSocketChannelInfo();
		scInfo.removeAllChannels();
		
		// socket channels to additional servers (client)
		Iterator<CMServer> iterServer = interInfo.getAddServerList().iterator();
		while(iterServer.hasNext())
		{
			CMServer tServer = iterServer.next();
			CMChannelInfo tscInfo = tServer.getSocketChannelInfo();
			tscInfo.removeAllChannels();
		}
		
		// socket channel of users (server)
		Iterator<CMUser> iterUser = interInfo.getLoginUsers().getAllMembers().iterator();
		while(iterUser.hasNext())
		{
			CMUser tUser = iterUser.next();
			CMChannelInfo chInfo = tUser.getSocketChannelInfo();
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
				CMChannelInfo mcInfo = tGroup.getMulticastChannelInfo();
				mcInfo.removeAllChannels();
			}
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMCommManager.terminate(), close and remove all channels.");
	}
	
	public static String getLocalIP()
	{
		String strIP = null;
		try{

			/// enumerate IP addresses bound to the local host
			Enumeration<NetworkInterface> nienum = NetworkInterface.getNetworkInterfaces();
			while (nienum.hasMoreElements())
			{
				NetworkInterface ni = nienum.nextElement();
				Enumeration<InetAddress> enumIA = ni.getInetAddresses();
				while (enumIA.hasMoreElements())
				{
					InetAddress inetAddress = enumIA.nextElement();
					if(CMInfo._CM_DEBUG)
						System.out.println("detected inetAddress: " + inetAddress.getHostAddress());
					if (!inetAddress.isLoopbackAddress() && !inetAddress.isLinkLocalAddress() && 
					inetAddress.isSiteLocalAddress())
					{
						 strIP = inetAddress.getHostAddress().toString();
					}
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		if(strIP == null)
		{
			System.err.println("CMCommManager.getLocalIP(), cannot find local IP");
		}

		return strIP;
	}
	
	public static SelectableChannel openChannel(int channelType, String address, int port, CMInfo cmInfo) throws IOException
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
			commInfo.setServerSocketChannel(ssc);
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
				System.out.println("CMCommManager.openChannel(), MULTICAST failed!");
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
			System.out.println("CMCommManager.openChannel(), unknown channel type: "+channelType);
			return null;
		}
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMCommManager.openChannel(), Ok, type("+channelType+"), address("
					+address+"), port("+port+") hashcode("+ch.hashCode()+").");
			System.out.println("# registered selection keys: "+sel.keys().size());
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
		CMBlockingEventQueue queue = cmInfo.getCommInfo().getBlockingEventQueue();
		CMByteReceiver byteReceiver = new CMByteReceiver(sel, queue);
		byteReceiver.start();
		cmInfo.getCommInfo().setByteReceiver(byteReceiver);
		
		return byteReceiver;
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
