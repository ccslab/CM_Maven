package kr.ac.konkuk.ccslab.cm.entity;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

public class CMMember extends CMObject{
	private Vector<CMUser> m_memberList;

	public CMMember()
	{
		m_nType = CMInfo.CM_MEMBER;
		m_memberList = new Vector<CMUser>();
	}
	
	// add member information
	public boolean addMember(CMUser user)
	{
		if(isMember(user.getName()))
		{
			System.out.println("CMMember.addMember(), user("+user.getName()+") already exists.");
			return false;
		}
		
		m_memberList.addElement(user);
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMember.addMember(), Ok with user("+user.getName()+").");
		}
		
		return true;
	}
	
	// remove member item (reference)
	public boolean removeMember(CMUser user)
	{
		Iterator<CMUser> iter = m_memberList.iterator();
		int nRemovedNum = 0;
		while(iter.hasNext())
		{
			CMUser tuser = iter.next();
			if(user.getName().equals(tuser.getName()))
			{
				iter.remove();
				nRemovedNum++;
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMMember.removeMember(), user("+user.getName()+") deleted.");
				}
			}
		}
		
		if(nRemovedNum == 0)
		{
			System.out.println("CMMember.removeMember(), user("+user.getName()+") not found.");
			return false;
		}
		
		return true;
	}

	// remove member reference and the member object
	public boolean removeMemberObject(CMUser user)
	{
		Iterator<CMUser> iter = m_memberList.iterator();
		int nRemovedNum = 0;
		boolean bFound = false;
		while(iter.hasNext() && !bFound)
		{
			CMUser tuser = iter.next();
			if(user.getName().equals(tuser.getName()))
			{
				iter.remove();
				tuser = null;
				nRemovedNum++;
				bFound = true;
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMMember.removeMemberObject(), user("+user.getName()+") deleted.");
				}
			}
		}
		
		if(nRemovedNum == 0)
		{
			System.out.println("CMMember.removeMemberObject(), user("+user.getName()+") not found.");
			return false;
		}
		
		return true;
	}

	// remove member item (reference) with user name
	public boolean removeMember(String name)
	{
		Iterator<CMUser> iter = m_memberList.iterator();
		int nRemovedNum = 0;
		boolean bFound = false;
		while(iter.hasNext() && !bFound)
		{
			CMUser tuser = iter.next();
			if(name.equals(tuser.getName()))
			{
				iter.remove();
				nRemovedNum++;
				bFound = true;
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMMember.removeMember(), user("+name+") deleted.");
				}
			}
		}
		
		if(nRemovedNum == 0)
		{
			System.out.println("CMMember.removeMember(), user("+name+") not found.");
			return false;
		}
		
		return true;
	}

	// remove member item (reference) and the member object with user name
	public boolean removeMemberObject(String name)
	{
		Iterator<CMUser> iter = m_memberList.iterator();
		int nRemovedNum = 0;
		boolean bFound = false;
		while(iter.hasNext() && !bFound)
		{
			CMUser tuser = iter.next();
			if(name.equals(tuser.getName()))
			{
				iter.remove();
				tuser = null;
				nRemovedNum++;
				bFound = true;
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMMember.removeMemberObject(), user("+name+") deleted.");
				}
			}
		}
		
		if(nRemovedNum == 0)
		{
			System.out.println("CMMember.removeMemberObject(), user("+name+") not found.");
			return false;
		}
		
		return true;
	}

	// remove all member items (references)
	public boolean removeAllMembers()
	{
		if( m_memberList.isEmpty() )
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMMember.removeAllMembers(), already empty.");
			return false;
		}
		
		m_memberList.removeAllElements();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMember.removeAllMembers(), Ok");
		}
		
		return true;
	}

	// remove all member items (references) and the member objects
	public boolean removeAllMemberObjects()
	{
		if( m_memberList.isEmpty() )
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMMember.removeAllMemberObjects(), already empty.");
			return false;
		}
		
		/*
		Iterator<CMUser> iter = m_memberList.iterator();
		while(iter.hasNext())
		{
			CMUser tuser = iter.next();
			iter.remove();
			tuser = null;	// not clear
		}
		*/
		
		m_memberList.removeAllElements();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMMember.removeAllMemberObjects(), Ok");
		}
		
		return true;
	}

	// check if a given user is a member
	public boolean isMember(CMUser user)
	{
		Iterator<CMUser> iter = m_memberList.iterator();
		while(iter.hasNext())
		{
			CMUser tuser = iter.next();
			if(user.getName().equals(tuser.getName()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	// check if a given user name is that of a member
	public boolean isMember(String name)
	{
		Iterator<CMUser> iter = m_memberList.iterator();
		while(iter.hasNext())
		{
			CMUser tuser = iter.next();
			if(name.equals(tuser.getName()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	// check if the member list is empty
	public boolean isEmpty()
	{
		return m_memberList.isEmpty();
	}
	
	// find member information
	public CMUser findMember(String name)
	{
		CMUser tuser;
		Iterator<CMUser> iter = m_memberList.iterator();
		while(iter.hasNext())
		{
			tuser = iter.next();
			if(name.equals(tuser.getName()))
				return tuser;
		}
		
		return null;
	}
	
	// get number of members
	public int getMemberNum()
	{
		return m_memberList.size();
	}
	
	// get all members (return member vector)
	public Vector<CMUser> getAllMembers()
	{
		return m_memberList;
	}
}
