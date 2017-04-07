package kr.ac.konkuk.ccslab.cm.entity;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMObject {
	protected int m_nType;

	public CMObject()
	{
		m_nType = CMInfo.CM_OBJECT;
	}

	// set type of the class
	public void setType(int t)
	{
		m_nType = t;
	}
	
	// get type of the class
	public int getType()
	{
		return m_nType;
	}
}
