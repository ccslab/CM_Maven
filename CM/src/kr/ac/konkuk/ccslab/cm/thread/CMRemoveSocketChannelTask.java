package kr.ac.konkuk.ccslab.cm.thread;

import java.util.concurrent.Callable;

import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;

public class CMRemoveSocketChannelTask implements Callable<Boolean> {
	private CMChannelInfo<Integer> m_chInfo;
	private int m_nChKey;
	
	public CMRemoveSocketChannelTask(CMChannelInfo<Integer> chInfo, int nChKey)
	{
		m_chInfo = chInfo;
		m_nChKey = nChKey;
	}
	
	@Override
	public Boolean call()
	{
		Boolean bRet = false;
		bRet = m_chInfo.removeChannel(m_nChKey);
		return bRet;
	}
	
	// set/get methods
	public void setChannelInfo(CMChannelInfo<Integer> chInfo)
	{
		m_chInfo = chInfo;
	}
	
	public CMChannelInfo<Integer> getChannelInfo()
	{
		return m_chInfo;
	}
	
	public void setChannelKey(int nChKey)
	{
		m_nChKey = nChKey;
	}
	
	public int getChannelKey()
	{
		return m_nChKey;
	}

}
