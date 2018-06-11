package kr.ac.konkuk.ccslab.cm.info;
import java.io.IOException;
import java.nio.channels.*;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.thread.CMByteReceiver;
import kr.ac.konkuk.ccslab.cm.thread.CMByteSender;

public class CMCommInfo {
	private ServerSocketChannel m_nonBlockServerSocketChannel; // nonblocking server socket channel
	private ServerSocketChannel m_blockServerSocketChannel; // blocking server socket channel
	private CMChannelInfo<Integer> m_nonBlockDCInfo;	// nonblocking datagram channel list
	private CMChannelInfo<Integer> m_blockDCInfo;		// blocking datagram channel list
	//private Vector<SocketChannel> m_scList;
	//private Vector<DatagramChannel> m_dcList;
	//private Vector<MulticastChannel> m_mcList;
	private Selector m_selector;
	private CMBlockingEventQueue m_recvQueue;
	private CMBlockingEventQueue m_sendQueue;
	private CMByteReceiver m_byteReceiver;
	private CMByteSender m_byteSender;
	
	//private Vector<SelectableChannel> m_toBeDeletedChannelList; 
	//for datagram
	//private int m_nDatagramID;
	//private Vector<CMDatagramPacket> m_datagramPacketList;
	// for delay
	private long m_lStart;
	private long m_lEnd;
	private long m_lPDelay;
	// for service rate
	private long m_lRecvCount;
	private long m_lTotalByte;
	
	public CMCommInfo()
	{
		m_nonBlockServerSocketChannel = null;
		m_blockServerSocketChannel = null;
		m_nonBlockDCInfo = new CMChannelInfo<Integer>();
		m_blockDCInfo = new CMChannelInfo<Integer>();
		m_byteReceiver = null;
		m_byteSender = null;
		//m_scList = new Vector<SocketChannel>();
		//m_dcList = new Vector<DatagramChannel>();
		//m_mcList = new Vector<MulticastChannel>();
		//m_toBeDeletedChannelList = new Vector<SelectableChannel>();
		//m_datagramPacketList = new Vector<CMDatagramPacket>();
		
		//m_nDatagramID = 0;
		m_lStart = 0;
		m_lEnd = 0;
		m_lPDelay = 0;
		m_lRecvCount = 0;
		m_lTotalByte = 0;
		try {
			m_selector = Selector.open();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		m_recvQueue = new CMBlockingEventQueue();
		m_sendQueue = new CMBlockingEventQueue();
	}
	
	// set/get methods
	public CMBlockingEventQueue getRecvBlockingEventQueue()
	{
		return m_recvQueue;
	}
	
	public CMBlockingEventQueue getSendBlockingEventQueue()
	{
		return m_sendQueue;
	}
	
	public void setNonBlockServerSocketChannel(ServerSocketChannel ssc)
	{
		m_nonBlockServerSocketChannel = ssc;
	}
	
	public ServerSocketChannel getNonBlockServerSocketChannel()
	{
		return m_nonBlockServerSocketChannel;
	}
	
	public void setBlockServerSocketChannel(ServerSocketChannel ssc)
	{
		m_blockServerSocketChannel = ssc;
	}
	
	public ServerSocketChannel getBlockServerSocketChannel()
	{
		return m_blockServerSocketChannel;
	}
	
	public void setByteReceiver(CMByteReceiver receiver)
	{
		m_byteReceiver = receiver;
	}
	
	public CMByteReceiver getByteReceiver()
	{
		return m_byteReceiver;
	}
	
	public void setByteSender(CMByteSender sender)
	{
		m_byteSender = sender;
	}
	
	public CMByteSender getByteSender()
	{
		return m_byteSender;
	}
	
	/*
	public void setDatagramID(int id)
	{
		m_nDatagramID = id;
	}
	
	public int getDatagramID()
	{
		return m_nDatagramID;
	}
	*/
	
	public void setStartTime(long start)
	{
		m_lStart = start;
	}
	
	public long getStartTime()
	{
		return m_lStart;
	}
	
	public void setEndTime(long end)
	{
		m_lEnd = end;
	}
	
	public long getEndTime()
	{
		return m_lEnd;
	}
	
	public void setPDelay(long delay)
	{
		m_lPDelay = delay;
	}
	
	public long getPDelay()
	{
		return m_lPDelay;
	}
	
	public void setRecvCount(long count)
	{
		m_lRecvCount = count;
	}
	
	public long getRecvCount()
	{
		return m_lRecvCount;
	}
	
	public void setTotalByte(long num)
	{
		m_lTotalByte = num;
	}
	
	public long getTotalByte()
	{
		return m_lTotalByte;
	}
	
	public Selector getSelector()
	{
		return m_selector;
	}
	
	public CMChannelInfo<Integer> getNonBlockDatagramChannelInfo()
	{
		return m_nonBlockDCInfo;
	}
	
	public CMChannelInfo<Integer> getBlockDatagramChannelInfo()
	{
		return m_blockDCInfo;
	}
		
	/*
	public Vector<SocketChannel> getSocketChannelList()
	{
		return m_scList;
	}
	
	public Vector<DatagramChannel> getDatagramChannelList()
	{
		return m_dcList;
	}
	
	public Vector<MulticastChannel> getMulticastChannelList()
	{
		return m_mcList;
	}

	public Vector<SelectableChannel> getToBeDeletedChannelList()
	{
		return m_toBeDeletedChannelList;
	}
	
	public Vector<CMDatagramPacket> getDatagramPacketList()
	{
		return m_datagramPacketList;
	}
	*/
	
	/*
	// sc,dc list management (mc list not included)
	public SelectableChannel findChannel(SelectableChannel ch)
	{
		if(ch instanceof SocketChannel)
			return findSocketChannel((SocketChannel)ch);
		else if(ch instanceof DatagramChannel)
			return findDatagramChannel((DatagramChannel)ch);
		
		System.out.println("CMCommInfo.findChannel(), not found");
		return null;
	}
	
	public boolean addChannel(SelectableChannel ch)
	{
		if(ch instanceof SocketChannel)
			return addSocketChannel((SocketChannel)ch);
		else if(ch instanceof DatagramChannel)
			return addDatagramChannel((DatagramChannel)ch);
		
		System.out.println("CMCommInfo.addChannel(), not found");
		return false;
	}
	
	public boolean removeChannel(SelectableChannel ch)
	{
		if(ch instanceof SocketChannel)
			return removeSocketChannel((SocketChannel)ch);
		else if(ch instanceof DatagramChannel)
			return removeDatagramChannel((DatagramChannel)ch);
		
		System.out.println("CMCommInfo.removeChannel(), not found");
		return false;
	}
	
	// sc,dc,mc list
	public void removeAllChannel()
	{
		m_scList.removeAllElements();
		m_dcList.removeAllElements();
		m_mcList.removeAllElements();
	}
	
	public SocketChannel findSocketChannel(SocketChannel ch)
	{
		boolean bFound = false;
		SocketChannel tch = null;
		Iterator<SocketChannel> iter = m_scList.iterator();
		while(iter.hasNext() && !bFound){
			tch = iter.next();
			//if(tch.equals(ch))
			if(tch.hashCode() == ch.hashCode())
			{
				bFound = true;
			}
		}
		
		return tch;
	}
	
	public boolean addSocketChannel(SocketChannel ch)
	{
		if( findSocketChannel(ch) != null )
		{
			System.out.println("CMCommInfo.addSocketChannel(), the channel already exists.");
			return false;
		}
		
		m_scList.addElement(ch);
		return  true;
	}
	
	public boolean removeSocketChannel(SocketChannel ch)
	{
		boolean bFound = false;
		SocketChannel tch = null;
		Iterator<SocketChannel> iter = m_scList.iterator();
		while(iter.hasNext() && !bFound){
			tch = iter.next();
			if(tch.equals(ch))
			{
				iter.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public void removeAllSocketChannels()
	{
		m_scList.removeAllElements();
		return;
	}

	public DatagramChannel findDatagramChannel(DatagramChannel ch)
	{
		boolean bFound = false;
		DatagramChannel tch = null;
		Iterator<DatagramChannel> iter = m_dcList.iterator();
		while(iter.hasNext() && !bFound){
			tch = iter.next();
			if(tch.equals(ch))
			{
				bFound = true;
			}
		}
		
		return tch;
	}
	
	public boolean addDatagramChannel(DatagramChannel ch)
	{
		if( findDatagramChannel(ch) != null )
		{
			System.out.println("CMCommInfo.addDatagramChannel(), the channel already exists.");
			return false;
		}
		
		m_dcList.addElement(ch);
		return  true;
	}
	
	public boolean removeDatagramChannel(DatagramChannel ch)
	{
		boolean bFound = false;
		DatagramChannel tch = null;
		Iterator<DatagramChannel> iter = m_dcList.iterator();
		while(iter.hasNext() && !bFound){
			tch = iter.next();
			if(tch.equals(ch))
			{
				iter.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public void removeAllDatagramChannels()
	{
		m_dcList.removeAllElements();
		return;
	}

	public MulticastChannel findMulticastChannel(MulticastChannel ch)
	{
		boolean bFound = false;
		MulticastChannel tch = null;
		Iterator<MulticastChannel> iter = m_mcList.iterator();
		while(iter.hasNext() && !bFound){
			tch = iter.next();
			if(tch.equals(ch))
			{
				bFound = true;
			}
		}
		
		return tch;
	}
	
	public boolean addMulticastChannel(MulticastChannel ch)
	{
		if( findMulticastChannel(ch) != null )
		{
			System.out.println("CMCommInfo.addMulticastChannel(), the channel already exists.");
			return false;
		}
		
		m_mcList.addElement(ch);
		return  true;
	}
	
	public boolean removeMulticastChannel(MulticastChannel ch)
	{
		boolean bFound = false;
		MulticastChannel tch = null;
		Iterator<MulticastChannel> iter = m_mcList.iterator();
		while(iter.hasNext() && !bFound){
			tch = iter.next();
			if(tch.equals(ch))
			{
				iter.remove();
				bFound = true;
			}
		}
		
		return bFound;
	}
	
	public void removeAllMulticastChannels()
	{
		m_mcList.removeAllElements();
		return;
	}
	*/

}
