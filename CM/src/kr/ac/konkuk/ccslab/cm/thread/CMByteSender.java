package kr.ac.konkuk.ccslab.cm.thread;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;

import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;

public class CMByteSender extends Thread {

	private CMBlockingEventQueue m_sendQueue = null;
	
	public CMByteSender(CMBlockingEventQueue sendQueue)
	{
		m_sendQueue = sendQueue;
	}
	
	public void run()
	{
		CMMessage msg = null;
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMByteSender starts to send messages.");
		while(!Thread.currentThread().isInterrupted())
		{
			msg = m_sendQueue.pop();
			
			if(msg == null)
			{
				if(CMInfo._CM_DEBUG_2)
					System.out.println("CMByteSender.run(), msg is null.");
				break;
			}
			
			if(msg.m_ch instanceof SocketChannel)
			{
				CMCommManager.sendMessage(msg.m_buf, (SocketChannel)msg.m_ch);
			}
			else if(msg.m_ch instanceof DatagramChannel)
			{
				String addr = ((InetSocketAddress)(msg.m_saddr)).getAddress().getHostAddress();
				int port = ((InetSocketAddress)(msg.m_saddr)).getPort();
				CMCommManager.sendMessage(msg.m_buf, (DatagramChannel)msg.m_ch, addr, port);
			}
			
			msg.m_buf = null;	// clear the sent ByteBuffer
			msg = null;			// clear the message
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMByteSender is terminated.");
	}
}
