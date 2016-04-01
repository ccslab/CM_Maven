package kr.ac.konkuk.ccslab.cm;

import java.util.*;
import java.util.Map.Entry;
import java.io.IOException;
import java.nio.channels.*;

// Information of map of pair (channel number, SelectableChannel)
// Used by CMUser (user channels), CMCommInfo (datagram channels), CMGroup (multicast channels),
// and CMServer (server channels)
public class CMChannelInfo {
	private HashMap<Integer, SelectableChannel> m_chMap;
	
	public CMChannelInfo()
	{
		m_chMap = new HashMap<Integer, SelectableChannel>();
	}
	
	// management of the HashMap of channels

	public boolean addChannel(SelectableChannel ch, int nNum)
	{
		Integer NNum = new Integer(nNum);
		
		if(ch == null)
		{
			System.out.println("CMChannelInfo.addChannel(), channel is null.");
			return false;
		}
		
		// check if nNum already exists or not
		if(m_chMap.containsKey(NNum))
		{
			System.out.println("CMChannelInfo.addChannel(), channel index("
					+nNum+") already exists.");
			return false;
		}
		
		// check if the same ch already exists or not
		if(m_chMap.containsValue(ch))
		{
			System.out.println("CMChannelInfo.addChanel(), channel already exists.");
			return false;
		}
		
		m_chMap.put(NNum, ch);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMChannelInfo.addChannel(), ch("+ch.hashCode()+"), index("+nNum+")");
		}

		return true;
	}

	public boolean removeChannel(int nNum)
	{
		Integer NNum = new Integer(nNum);
		SelectableChannel ch = null;
		
		if(!m_chMap.containsKey(NNum))
		{
			System.out.println("CMChannelInfo.removeChannel(), channel num("
					+nNum+") does not exists.");
			return false;
		}
		
		ch = m_chMap.get(NNum);
		if(ch != null && ch.isOpen())
		{
			try {
				ch.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		m_chMap.remove(NNum);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMChannelInfo.removeChannel(), ch("+ch.hashCode()+"), index("+nNum+")");
		}

		return true;
	}

	public void removeAllChannels()
	{
		SelectableChannel ch = null;
		Iterator<Entry<Integer, SelectableChannel>> iter = m_chMap.entrySet().iterator();
		
		while( iter.hasNext() )
		{
			Map.Entry<Integer, SelectableChannel> e = (Map.Entry<Integer, SelectableChannel>) iter.next();
			ch = e.getValue();
			if(ch != null && ch.isOpen())
			{
				try {
					ch.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		}

		m_chMap.clear();
	}

	public boolean removeAllAddedChannels()
	{
		SelectableChannel ch = null;
		
		Iterator<Entry<Integer, SelectableChannel>> iter = m_chMap.entrySet().iterator();
		if(m_chMap.isEmpty())
		{
			System.out.println("CMChannelInfo.removeAllAddedChannels(), channel map is empty.");
			return false;
		}
		
		while( iter.hasNext() )
		{
			Map.Entry<Integer, SelectableChannel> e = (Map.Entry<Integer, SelectableChannel>) iter.next();
			if( e.getKey().intValue() != 0 ) // if key is not default key (0)
			{
				ch = e.getValue();
				if(ch != null && ch.isOpen())
				{
					try {
						ch.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
				iter.remove();
			}
		}
		
		return true;
	}

	public SelectableChannel findChannel(int nNum)
	{
		Integer NNum = new Integer(nNum);
		return m_chMap.get(NNum);
	}
	
	public int findChannelIndex(SelectableChannel ch)
	{
		boolean bFound = false;
		int ret = -1;
		SelectableChannel tch = null;
		Iterator<Entry<Integer, SelectableChannel>> iter = m_chMap.entrySet().iterator();
		
		while(iter.hasNext() && !bFound)
		{
			Map.Entry<Integer, SelectableChannel> e = (Map.Entry<Integer, SelectableChannel>) iter.next();
			tch = e.getValue();
			//System.out.println("ch code: "+ch.hashCode()+", tch code: "+tch.hashCode());
			if( tch.equals(ch) )
			{
				ret = e.getKey().intValue();
				bFound = true;
			}
		}
		
		return ret;
	}

}
