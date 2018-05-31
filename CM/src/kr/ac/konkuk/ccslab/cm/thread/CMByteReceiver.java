package kr.ac.konkuk.ccslab.cm.thread;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;

import java.net.*;

public class CMByteReceiver extends Thread {
	private Selector m_selector = null;
	private CMBlockingEventQueue m_queue = null;
	
	public CMByteReceiver(Selector sel, CMBlockingEventQueue queue)
	{
		m_selector = sel;
		m_queue = queue;
	}
	
	public void run()
	{
		if(CMInfo._CM_DEBUG)
			System.out.println("CMByteReceiver starts to receive messages.");
		while(!Thread.currentThread().isInterrupted())
		{
			synchronized(Selector.class)
			{
				try {
					//m_selector.select();
					m_selector.select(1);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				Iterator<SelectionKey> iter = m_selector.selectedKeys().iterator();
				while(iter.hasNext())
				{
					SelectionKey key = iter.next();
					if(key.isAcceptable())
						processAccept(key);
					else if(key.isReadable())
						processRead(key);
					
					iter.remove();
				}
				Selector.class.notify();
			}
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMByteReceiver is terminated.");
	}
	
	private void processAccept(SelectionKey key)
	{
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = null;
		try {
			sc = ssc.accept();
			if(sc == null)
			{
				System.err.println("CMByteReceiver.processAccept(), socket channel is null.");
				return;
			}
			sc.configureBlocking(false);
			sc.register(m_selector, SelectionKey.OP_READ);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(CMInfo._CM_DEBUG_2)
		{
			System.out.println("CMByteReceiver.processAccept(), "+sc.toString()+" connected. hashcode: "+sc.hashCode());
			System.out.println("# registered keys in Selector: "+m_selector.keys().size());
			return;
		}
		
		return;
	}
	
	private void processRead(SelectionKey key)
	{
		if( key.channel() instanceof SocketChannel )
			readEventBytes((SocketChannel) key.channel());
		else if( key.channel() instanceof DatagramChannel )
			readDatagramBytes((DatagramChannel) key.channel());
		
		return;
	}
	
	// stream socket channel
	private void readEventBytes(SocketChannel sc)
	{
		ByteBuffer bufByteNum = null;
		ByteBuffer bufEvent = null;
		int ret = -1;
		int nByteNum = -1;
		CMMessage msg = null;
		
		// create ByteBuffer
		bufByteNum = ByteBuffer.allocate(Integer.BYTES);
		bufByteNum.clear();
		ret = readStreamBytes(sc, bufByteNum);
		
		// if a channel is disconnected
		if( ret == 0 || ret == -1 )
		{
			if(CMInfo._CM_DEBUG_2)
			{
				System.out.println("---- CMByteReceiver.readEventBytes(), "+sc.toString()
							+" is disconnected.");
				System.out.println("is open: "+sc.isOpen());
				System.out.println("hash code: "+sc.hashCode());
			}
			if(sc.isOpen())
			{
				try {
					sc.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
		else
		{
			// create a main ByteBuffer
			bufByteNum.flip();
			nByteNum = bufByteNum.getInt();
			if(CMInfo._CM_DEBUG_2)
				System.out.println("#### event byte num: "+nByteNum+" bytes, read byte num: "+ret+" bytes.");
			if(nByteNum > CMInfo.MAX_EVENT_SIZE)
			{
				System.err.println("CMByteReceiver.readEventBytes(): nByteNum("+nByteNum
						+") is greater than the maximum event size("+CMInfo.MAX_EVENT_SIZE+")!");
				return;
			}
			else if(nByteNum < CMInfo.MIN_EVENT_SIZE)
			{
				System.err.println("CMByteReceiver.readEventBytes(): nByteNum("+nByteNum
						+") is less than the minimum event size("+CMInfo.MIN_EVENT_SIZE+")!");
				return;
			}
			//bufEvent = ByteBuffer.allocateDirect(nByteNum);
			bufEvent = ByteBuffer.allocate(nByteNum);
			bufEvent.clear();
			bufEvent.putInt(nByteNum);	// put the first 4 bytes
			// read remaining event bytes
			ret = readStreamBytes(sc, bufEvent);

			if(CMInfo._CM_DEBUG_2)
				System.out.println("---- CMByteReceiver.readEventBytes(), total read bytes: "
								+Integer.BYTES+ret+" bytes.");
		}
		
		// If the channel is disconnected, null message is delivered.
		//if( ret == 0 || ret == -1 )
		if( ret == -1 )
		{
			if(bufEvent != null)
				bufEvent = null;
		}
		else
		{
			bufEvent.flip();
		}

		// send received message and channel to CMEventReceiver.
		msg = new CMMessage(bufEvent, sc);
		m_queue.push(msg);
		
		// clear the temporal ByteBuffer
		if( bufByteNum != null )
			bufByteNum = null;
		
		return;

		// the created ByteBuffer must be cleared after it is processed.
	}

	// read bytes as long as the byte buffer has a remaining empty room
	private int readStreamBytes(SocketChannel sc, ByteBuffer buf)
	{
		int ret = 0;
		int nReceivedByteNum = 0;
		
		while(buf.hasRemaining() && ret != -1)
		{
			try {
				ret = sc.read(buf);
				if(CMInfo._CM_DEBUG_2)
					System.out.println("CMByteReceiver.readStreamBytes(), read "+ret+" bytes.");
				nReceivedByteNum += ret;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return -1;
			}
			
		}
		
		if(ret == -1)
			return -1;
		
		return nReceivedByteNum;
	}

	// datagram or multicast channel
	private void readDatagramBytes(DatagramChannel dc)
	{
		CMMessage msg = null;
		SocketAddress senderAddr = null;	// sender address
		int nByteNum = 0;
		//ByteBuffer bufEvent = ByteBuffer.allocateDirect(CMInfo.SO_RCVBUF_LEN);
		ByteBuffer bufEvent = ByteBuffer.allocate(CMInfo.SO_RCVBUF_LEN);
		bufEvent.clear();	// initialize the ByteBuffer
		
		try {
			senderAddr = dc.receive(bufEvent);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		bufEvent.flip();				// limit: cur position, position: 0
		nByteNum = bufEvent.getInt();	// get the # bytes of the message
		bufEvent.rewind();				// position: 0
		
		// check the completeness of the received message
		if(nByteNum != bufEvent.remaining())
		{
			System.err.println("CMByteReceiver.readDatagramBytes(), receive incomplete message. "
					+ "nByteNum("+nByteNum+" bytes), received byte num ("+bufEvent.remaining()+" bytes).");
			bufEvent = null;
			return;
		}
		
		if(CMInfo._CM_DEBUG_2)
			System.out.println("---- CMByteReceiver.readDatagramBytes(), read "+bufEvent.remaining()+" bytes.");

		// send received message and channel to CMEventReceiver.
		msg = new CMMessage(bufEvent, dc, senderAddr);
		m_queue.push(msg);

		return;
		
		// the created ByteBuffer must be cleared after it is processed.
	}
	
}
