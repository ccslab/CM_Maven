package kr.ac.konkuk.ccslab.cm;
import java.nio.channels.*;

import kr.ac.konkuk.ccslab.cm.info.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.info.CMGroupInfo;

public class CMGroup extends CMGroupInfo {
	private CMMember m_groupUsers;
	private CMChannelInfo m_mcInfo;
	private MembershipKey m_membershipKey;	// required for leaving a group
	
	public CMGroup()
	{
		super();
		m_groupUsers = new CMMember();
		m_mcInfo = new CMChannelInfo();
		m_membershipKey = null;
	}
	
	public CMGroup(String strGroupName, String strAddress, int nPort)
	{
		super(strGroupName, strAddress, nPort);
		m_groupUsers = new CMMember();
		m_mcInfo = new CMChannelInfo();
		m_membershipKey = null;
	}
	
	// set/get methods
	public CMMember getGroupUsers()
	{
		return m_groupUsers;
	}
	
	public CMChannelInfo getMulticastChannelInfo()
	{
		return m_mcInfo;
	}
	
	public MembershipKey getMembershipKey()
	{
		return m_membershipKey;
	}
	
	public void setMembershipKey(MembershipKey key)
	{
		m_membershipKey = key;
	}
}
