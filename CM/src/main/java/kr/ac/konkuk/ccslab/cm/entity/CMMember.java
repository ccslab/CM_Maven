package kr.ac.konkuk.ccslab.cm.entity;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.info.CMInfo;

/**
 * This class represents a table of users.
 * 
 * @author CCSLab, Konkuk University
 *
 */
public class CMMember extends CMObject{
	private Hashtable<String, List<CMUser>> m_memberTable;

	/**
	 * creates an instance of the CMMember class.
	 */
	public CMMember()
	{
		m_nType = CMInfo.CM_MEMBER;
		m_memberTable = new Hashtable<>();
	}
	
	/**
	 * Adds a user to this member table.
	 * 
	 * @param user - an added user.
	 * @return true if the user is successfully added, or false.
	 */
	public synchronized boolean addMember(CMUser user)
	{
		String userName = user.getName();
		List<CMUser> userList = m_memberTable.get(userName);

		if( userList == null ) {
			userList = new ArrayList<>();
			m_memberTable.put(userName, userList);
		}

		userList.add(user);

		if(CMInfo._CM_DEBUG) {
			System.out.println("CMMember.addMember(), Ok with user("+userName+").");
			System.out.println("# login of this user: "+userList.size());
		}

		return true;
	}
	
	/**
	 * Removes a user from this member table.
	 * 
	 * <p> If the member list has a user object that has the same name as the given user.
	 * 
	 * @param user - a removed user.
	 * @return true if the user is successfully removed, or false.
	 */
	public synchronized CMUser removeMember(CMUser user)
	{
		String userName = user.getName();
		CMUser removedUser = null;
		List<CMUser> userList = m_memberTable.get(userName);

		if( userList == null ) {
			System.err.println("CMMember.removeMember(user), no user list with name ("+userName+")!");
			return null;
		}

		// find and remove the same object
		if( userList.remove(user) ) {
			removedUser = user;
		}

		if( removedUser == null ) {
			System.err.println("CMMember.removeMember(user), no same user object found with name("
					+userName+"), uuid("+user.getUuid()+")!");
			return null;
		}

		if( userList.isEmpty() ) {
			m_memberTable.remove(userName);
		}

		if(CMInfo._CM_DEBUG) {
			System.out.println("CMMember.removeMember(), user("+userName+") deleted.");
		}

		return removedUser;
	}

	/**
	 * Removes a specific user with the given name and UUID.
	 * * @param name - the user name
	 * @param uuid - the user UUID
	 * @return true if successfully removed, false otherwise.
	 */
	public synchronized boolean removeMember(String name, UUID uuid) {
		List<CMUser> userList = m_memberTable.get(name);
		if( userList == null ) {
			System.err.println("CMMember.removeMember(name, uuid), no user list with name ("+name+") found!");
			return false;
		}

		boolean removed = userList.removeIf( user ->
				(user.getUuid() == null && uuid == null) ||
						(user.getUuid() != null && user.getUuid().equals(uuid))
		);

		if( !removed ) {
			System.err.println("CMMember.removeMember(name, uuid), no user found with name("+name+"), uuid("+uuid+")!");
			return false;
		}

		if( userList.isEmpty() ) {
			m_memberTable.remove(name);
		}

		return true;
	}

	/**
	 * Removes all users in this member table.
	 */
	public synchronized void removeAllMembers()
	{
		if( m_memberTable.isEmpty() ) {
			if(CMInfo._CM_DEBUG)
				System.out.println("CMMember.removeAllMembers(), table already empty!");
			return;
		}

		for( List<CMUser> userList : m_memberTable.values() ) {
			userList.clear();
		}
		m_memberTable.clear();

		if(CMInfo._CM_DEBUG) {
			System.out.println("CMMember.removeAllMembers(), Ok");
		}
	}

	/**
	 * Checks if the member table is empty or not.
	 * 
	 * @return true if the member table is empty, or false.
	 */
	public synchronized boolean isEmpty()
	{
		return m_memberTable.isEmpty();
	}

	/**
	 * Finds member list with a given name.
	 * * @param name - a member name
	 * @return the list of users with the name if found; null otherwise.
	 */
	public synchronized List<CMUser> findMemberList(String name)
	{
		return m_memberTable.get(name);
	}

	/**
	 * Finds a member with a given name and uuid.
	 * 
	 * @param userName - a member name
	 * @param uuid - user uuid
	 * @return the user with the name if found; null otherwise.
	 */
	public synchronized CMUser findMember(String userName, UUID uuid)
	{
		List<CMUser> userList = m_memberTable.get(userName);
		if( userList == null ) {
			System.err.println("CMMember.findMember(), user list with name("+userName+") is null!");
			return null;
		}

		CMUser user = userList.stream()
				.filter( u -> (u.getUuid() == null && uuid == null) ||
						(u.getUuid() != null && u.getUuid().equals(uuid)) )
				.findFirst().orElse(null);

		if( user == null ) {
			System.err.println("CMMember.findMember(), user with name("+userName+"), uuid("+uuid+") not found!");
			return null;
		}

		return user;
	}
	
	/**
	 * Returns the number of current user lists in this member table.
	 * 
	 * @return the number of members.
	 */
	public synchronized int getMemberNum()
	{
		return m_memberTable.size();
	}
	
	
	/**
	 * Returns the reference of this member table.
	 * 
	 * @return the reference of this member table.
	 */
	public synchronized Hashtable<String, List<CMUser>> getAllMembers()
	{
		return m_memberTable;
	}

	/**
	 * Returns the reference of the user list.
	 * @param name - user name
	 * @return the reference of the user list.
	 */
	public synchronized List<CMUser> getAllMembers(String name) {
		return m_memberTable.get(name);
	}

	@Override
	public String toString()
	{
		if(m_memberTable.isEmpty())
			return null;

		StringBuffer strBuf = new StringBuffer();
		for(Map.Entry<String, List<CMUser>> entry : m_memberTable.entrySet()) {
			strBuf.append(entry.getKey()+"("+entry.getValue().size()+") ");
		}

		return strBuf.toString();
	}
}
