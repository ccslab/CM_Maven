package kr.ac.konkuk.ccslab.cm.manager;
import java.util.*;

import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMPosition;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMDataEvent;
import kr.ac.konkuk.ccslab.cm.event.CMDummyEvent;
import kr.ac.konkuk.ccslab.cm.event.CMEvent;
import kr.ac.konkuk.ccslab.cm.event.CMInterestEvent;
import kr.ac.konkuk.ccslab.cm.event.CMMultiServerEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEvent;
import kr.ac.konkuk.ccslab.cm.event.CMUserEventField;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.enums.CMFileSyncMode;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

public class CMGroupManager {

	public static void init() // for server
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		if(!confInfo.getCommArch().equals("CM_PS"))
		{
			return;
		}
		
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		Iterator<CMSession> iterSession = interInfo.getSessionList().iterator();
		while(iterSession.hasNext())
		{
			CMSession session = iterSession.next();
			Iterator<CMGroup> iterGroup = session.getGroupList().iterator();
			while(iterGroup.hasNext())
			{
				CMGroup group = iterGroup.next();
				DatagramChannel dc = null;
				try {
					dc = (DatagramChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_MULTICAST_CHANNEL, 
							group.getGroupAddress(), group.getGroupPort());
					if(dc == null)
					{
						System.out.println("CMGroupManager.init(), multicast channel open error!"
								+ ": group("+group.getGroupName()+"), addr("+group.getGroupAddress()
								+"), port("+group.getGroupPort()+")");
						return;
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				
				// store membership key (required for leaving the group with this channel)
				MembershipKey key = CMCommManager.joinMulticastGroup(dc, group.getGroupAddress());
				group.setMembershipKey(key);
				// store multicast channel
				InetSocketAddress sockAddress = new InetSocketAddress(group.getGroupAddress(), group.getGroupPort());
				group.getMulticastChannelInfo().addChannel(sockAddress, dc);	// default channel
			}
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMGroupManager.init(), succeeded.");
	}
	
	public static void init(String strSession, String strGroup)	// for client
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		if(!confInfo.getCommArch().equals("CM_PS"))
		{
			return;
		}
		
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMSession session = interInfo.findSession(strSession);
		if(session == null)
		{
			System.out.println("CMGroupManager.init(), session("+strSession+") not found.");
			return;
		}
		CMGroup group = session.findGroup(strGroup);
		if(group == null)
		{
			System.out.println("CMGroupManager.init(), session("+strSession+") found, group("
					+strGroup+") NOT found.");
			return;
		}
		
		DatagramChannel dc = null;
		try {
			dc = (DatagramChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_MULTICAST_CHANNEL, 
					group.getGroupAddress(), group.getGroupPort());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		// store membership key (required for leaving the group with this channel)
		MembershipKey key = CMCommManager.joinMulticastGroup(dc, group.getGroupAddress());
		group.setMembershipKey(key);
		// store multicast channel
		InetSocketAddress sockAddress = new InetSocketAddress(group.getGroupAddress(), group.getGroupPort());
		group.getMulticastChannelInfo().addChannel(sockAddress, dc);	// default channel
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMGroupManager.init(), session("+strSession+"), group("+strGroup
					+") succeeded.");
		}
		
		return;
	}
	
	public static void terminate(String strSession, String strGroup) // for client
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMSession session = interInfo.findSession(strSession);
		if(session == null)
		{
			System.out.println("CMGroupManager.terminate(), session("+strSession+") not found.");
			return;
		}
		CMGroup group = session.findGroup(strGroup);
		if(group == null)
		{
			System.out.println("CMGroupManager.terminate(), session("+strSession+") found, group("
					+strGroup+") NOT found.");
			return;
		}
		
		// remove region member
		group.getGroupUsers().removeAllMembers();
		// close and remove all multicast channels in CM_PS mode
		if(confInfo.getCommArch().equals("CM_PS"))
		{
			group.getMulticastChannelInfo().removeAllChannels();
			group.setMembershipKey(null);
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMGroupManager.terminate() ends. session("+session.getSessionName()
					+"), group("+group.getGroupName()+").");
		}
		
		return;
	}
	
	public static void leaveGroup(CMUser user)	// for server
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		
		// find current session and group
		CMSession session = interInfo.findSession(user.getCurrentSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.leaveGroup(), user("+user.getName()
					+"), uuid("+user.getUuid()+")'s current "
					+" session("+user.getCurrentSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(user.getCurrentGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.leaveGroup(), user("+user.getName()
					+"), uuid("+user.getUuid()+")'s current "
					+" session("+user.getCurrentSession()+") found, but current group("
					+user.getCurrentGroup()+") NOT FOUND.");
			return;
		}
		
		// remove the user from the group member
		boolean ret = group.getGroupUsers().removeMember(user.getName(), user.getUuid());
		
		if(ret)
		{
			// notify group members of the removal
			notifyGroupUsersOfRemovedUser(user);
			
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMGroupManager.leaveGroup(), succeeded. user("+user.getName()
						+"), uuid("+user.getUuid()+"), session("+user.getCurrentSession()
						+"), group("+user.getCurrentGroup() +"), # remaining members("
						+group.getGroupUsers().getMemberNum()+").");
			}
			user.setCurrentGroup("");
		}
		else
		{
			System.out.println("CMGroupManager.leaveGroup(), failed for user("+user.getName()
					+"), uuid("+user.getUuid()+"), group("+group.getGroupName()+").");
		}
	}
	
	public static void changeGroup(String gName) // for client
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strDefServer = interInfo.getDefaultServerInfo().getServerName();
		CMUser myself = interInfo.getMyself();
		CMSession curSession = interInfo.findSession(myself.getCurrentSession());
		if(curSession == null)
		{
			System.out.println("CMGroupManager.changeGroup(), session("+myself.getCurrentSession()
					+") not found.");
			return;
		}
		CMGroup curGroup = curSession.findGroup(myself.getCurrentGroup());
		if(curGroup == null)
		{
			System.out.println("CMGroupManager.changeGroup(), session("+myself.getCurrentSession()
					+") found, but group("+myself.getCurrentGroup()+") NOT FOUND.");
			return;
		}
		CMGroup targetGroup = curSession.findGroup(gName);
		if(targetGroup == null)
		{
			System.out.println("CMGroupManager.changeGroup(), session("+myself.getCurrentSession()
					+") and group("+myself.getCurrentGroup()+") found, but target group("
					+gName+") NOT FOUND.");
			return;
		}

		// request to leave current group
		CMInterestEvent ie = new CMInterestEvent();
		ie.setID(CMInterestEvent.USER_LEAVE);
		ie.setHandlerSession(myself.getCurrentSession());
		ie.setHandlerGroup(myself.getCurrentGroup());
		ie.setUserName(myself.getName());
		CMEventManager.unicastEvent(ie, strDefServer);
		ie = null;
		// leave the user from local information of the current group
		curGroup.getGroupUsers().removeMember(myself.getName());
		
		// enter the user to local information of the target group
		myself.setCurrentGroup(gName);
		targetGroup.getGroupUsers().addMember(myself);
		
		// request to enter target group
		ie = new CMInterestEvent();
		ie.setID(CMInterestEvent.USER_ENTER);
		ie.setHandlerSession(myself.getCurrentSession());
		ie.setHandlerGroup(myself.getCurrentGroup());
		ie.setUserName(myself.getName());
		ie.setHostAddress(myself.getHost());
		ie.setUDPPort(myself.getUDPPort());
		ie.setCurrentGroup(myself.getCurrentGroup());
		CMEventManager.unicastEvent(ie, strDefServer);
		
		return;
	}
	
	public static void processEvent(CMMessage msg)
	{
		CMEvent cmEvent = null;
		// unmarshall an event
		cmEvent = CMEventManager.unmarshallEvent(msg.m_buf);
		if(cmEvent == null)
		{
			System.out.println("CMGroupManager.processEvent(), unmarshalled event is null.");
			return ;
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("---- CMGroupManager.processEvent() starts. event(type: "
					+cmEvent.getType()+", id: "+cmEvent.getID()+").");
		}

		int nEventType = cmEvent.getType();
		int nEventID = cmEvent.getID();
		switch(nEventType)
		{
		case CMInfo.CM_INTEREST_EVENT:
			switch(nEventID)
			{
			case CMInterestEvent.USER_ENTER:
				processUSER_ENTER(msg);
				break;
			case CMInterestEvent.USER_LEAVE:
				processUSER_LEAVE(msg);
				break;
			case CMInterestEvent.USER_MOVE:
				processUSER_MOVE(msg);
				break;
			case CMInterestEvent.USER_TALK:
				processUSER_TALK(msg);
				break;
			default:
				System.out.println("CMGroupManager.processEvent(), unknown event(type: "+nEventType+", id: "+nEventID+").");
				cmEvent = null;
				return;
			}
			break;
		case CMInfo.CM_DATA_EVENT:
			switch(nEventID)
			{
			case CMDataEvent.INHABITANT:
				processINHABITANT(msg);
				break;
			case CMDataEvent.NEW_USER:
				processNEW_USER(msg);
				break;
			case CMDataEvent.REMOVE_USER:
				processREMOVE_USER(msg);
				break;
			default:
				System.out.println("CMGroupManager.processEvent(), unknown event(type: "+nEventType+", id: "+nEventID+").");
				cmEvent = null;
				return;
			}
			break;
		case CMInfo.CM_DUMMY_EVENT:
			processDummyEvent(msg);
			break;
		case CMInfo.CM_USER_EVENT:
			processUserEvent(msg);
			break;
		default:
			System.out.println("CMGroupManager.processEvent(), unknown event(type: "+nEventType+").");
			cmEvent = null;
			return;
		}
	
		cmEvent = null;
		return;
	}
	
	///////////////////////////////////////////////////////////////////
	// private methods
	
	private static void processUSER_ENTER(CMMessage msg)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		
		if(!confInfo.getSystemType().equals("SERVER"))
		{
			System.err.println("CMGroupManager.processUSER_ENTER(), system type is not SERVER!");
			return;
		}
		
		CMInterestEvent ie = new CMInterestEvent(msg.m_buf);
		CMSession session = interInfo.findSession(ie.getHandlerSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.processUSER_ENTER(), session("+ie.getHandlerSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(ie.getHandlerGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.processUSER_ENTER(), session("+ie.getHandlerSession()
					+") found, group("+ie.getHandlerGroup()+") NOT found.");
			return;
		}
		// find user in the loginUser table using sender and sender uuid in the event header
		CMUser user = interInfo.getLoginUsers().findMember(ie.getSender(), ie.getSenderUuid());
		if(user == null)
		{
			System.out.println("CMGroupManager.processUSER_ENTER() for session("+ie.getHandlerSession()
					+"), group("+ie.getHandlerGroup()+"), user("+ie.getUserName()
					+"), uuid("+ie.getSenderUuid()+") not found in the login user list.");
			return;
		}
		
		// add the user to the group member list
		user.setCurrentGroup(ie.getHandlerGroup());
		boolean ret = group.getGroupUsers().addMember(user);

		if(!ret)
		{
			System.out.println("CMGroupManager.processUSER_ENTER(), failed for user("
					+user.getName()+"), uuid("+ie.getSenderUuid()+").");
			return;
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMGroupManager.processUSER_ENTER(), add user("+ie.getUserName()
					+"), uuid("+ie.getSenderUuid()+") to group("+group.getGroupName()+"), # group users("
					+group.getGroupUsers().getMemberNum()+").");
		}

		// send the new user existing group member information
		distributeGroupUsers(user);
		
		// send group members the new user information
		notifyGroupUsersOfNewUser(user);
	}
	
	private static void processUSER_LEAVE(CMMessage msg)
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		if(!confInfo.getSystemType().equals("SERVER"))
		{
			return;
		}
		
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMInterestEvent ie = new CMInterestEvent(msg.m_buf);
		CMSession session = interInfo.findSession(ie.getHandlerSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.processUSER_LEAVE(), session("+ie.getHandlerSession()
					+") not found.");
			ie = null;
			return;
		}
		CMGroup group = session.findGroup(ie.getHandlerGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.processUSER_LEAVE(), session("+ie.getHandlerSession()
					+") found, but group("+ie.getHandlerGroup()+") NOT FOUND.");
			ie = null;
			return;
		}
		CMUser user = group.getGroupUsers().findMember(ie.getUserName());
		if(user == null)
		{
			System.out.println("CMGroupManager.processUSER_LEAVE(), session("+ie.getHandlerSession()
					+") group("+ie.getHandlerGroup()+") found, but user("+ie.getUserName()
					+") NOT FOUND.");
			ie = null;
			return;
		}
		
		if(!ie.getHandlerSession().equals(user.getCurrentSession()) || 
				!ie.getHandlerGroup().equals(user.getCurrentGroup()))
		{
			System.out.println("CMGroupManager.processUSER_LEAVE(), user session(or group)"
					+"and requested session(or group) are different!");
			ie = null;
			return;
		}
		
		leaveGroup(user);
		
		ie = null;
		return;
	}
	
	private static void distributeGroupUsers(CMUser targetUser)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		
		CMSession session = interInfo.findSession(targetUser.getCurrentSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.distributeGroupUsers(), session("+targetUser.getCurrentSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(targetUser.getCurrentGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.distributeGroupUsers(), session("+targetUser.getCurrentSession()
					+") found, group("+targetUser.getCurrentGroup()+") NOT found.");
			return;
		}

		// get the socket channel of the target user
		SocketChannel sc = (SocketChannel) targetUser.getNonBlockSocketChannelInfo().findChannel(0);
		if(sc == null)
		{
			System.err.println("CMGroupManager.distributeGroupUsers(), socket channel of user("
					+ targetUser.getName() + "), uuid(" + targetUser.getUuid() + ") not found!");
			return;
		}

		// iterate the group members and send the event
		// CMMember is changed to Hashtable<String, List<CMUser>>
		group.getGroupUsers().getAllMembers().values().stream()
				.flatMap(List::stream)
				.forEach( user -> {
					CMDataEvent de = new CMDataEvent();
					de.setID(CMDataEvent.INHABITANT);
					de.setHandlerSession(session.getSessionName());
					de.setHandlerGroup(group.getGroupName());
					de.setUserName(user.getName());
                	de.setUuid(user.getUuid()); // add UUID of the inhabitant user
					de.setHostAddress(user.getHost());
					de.setUDPPort(user.getUDPPort());
					CMEventManager.unicastEvent(de, sc);
				});

		if(CMInfo._CM_DEBUG)
			System.out.println("CMGroupManager.distributeGroupUsers(), session("+session.getSessionName()
					+"), group("+group.getGroupName()+") info to user("+targetUser.getName()
					+"), uuid("+targetUser.getUuid()+") sent.");
	}
	
	private static void notifyGroupUsersOfNewUser(CMUser newUser)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMSession session = interInfo.findSession(newUser.getCurrentSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.notifyGroupUsersOfNewUser(), session("+newUser.getCurrentSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(newUser.getCurrentGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.notifyGroupUsersOfNewUser(), session("+newUser.getCurrentSession()
					+") found, group("+newUser.getCurrentGroup()+") NOT found.");
			return;
		}
		
		CMDataEvent de = new CMDataEvent();
		de.setID(CMDataEvent.NEW_USER);
		de.setHandlerSession(newUser.getCurrentSession());
		de.setHandlerGroup(newUser.getCurrentGroup());
		de.setUserName(newUser.getName());
		de.setUuid(newUser.getUuid()); // add UUID of the new user
		de.setHostAddress(newUser.getHost());
		de.setUDPPort(newUser.getUDPPort());
		
		CMMember gMember = group.getGroupUsers();
		CMEventManager.castEvent(de, gMember);

		if(CMInfo._CM_DEBUG)
			System.out.println("CMGroupManager.notifyGroupUsersOfNewUser(), session("+session.getSessionName()
					+"), group("+group.getGroupName()+") new user("+newUser.getName()+").");
		
	}
	
	private static void processUSER_MOVE(CMMessage msg)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMInterestEvent ie = new CMInterestEvent(msg.m_buf);
		
		// check if the user is myself or not
		if(interInfo.getMyself().getName().equals(ie.getUserName()))
		{
			System.out.println("CMGroupManager.processUSER_MOVE(), user("+ie.getUserName()
					+") is myself.");
			ie = null;
			return;
		}
		
		CMSession session = interInfo.findSession(ie.getHandlerSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.processUSER_MOVE(), session("+ie.getHandlerSession()
					+") not found.");
			ie = null;
			return;
		}
		CMGroup group = session.findGroup(ie.getHandlerGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.processUSER_MOVE(), session("+ie.getHandlerSession()
					+") found, but group("+ie.getHandlerGroup()+") NOT FOUND.");
			ie = null;
			return;
		}
		
		CMUser user = group.getGroupUsers().findMember(ie.getUserName());
		if(user == null)
		{
			System.out.println("CMGroupManager.processUSER_MOVE(), user("+ie.getUserName()+")"
					+"for session("+session.getSessionName()+") group("+group.getGroupName()
					+") not found.");
			ie = null;
			return;
		}
		
		CMPosition pq = ie.getPosition();
		user.setPosition(pq);
		
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMGroupManager.processUSER_MOVE(), user("+ie.getUserName()+")"
					+"for session("+session.getSessionName()+") group("+group.getGroupName()
					+"), PQ(pos("+pq.m_p.m_x+","+pq.m_p.m_y+","+pq.m_p.m_z+"), quat("+pq.m_q.m_w
					+","+pq.m_q.m_x+","+pq.m_q.m_y+","+pq.m_q.m_z+")).");
		}

		ie = null;
		return;
	}
	
	private static void processUSER_TALK(CMMessage msg)
	{
		if(CMInfo._CM_DEBUG)
		{
			CMInterestEvent ie = new CMInterestEvent(msg.m_buf);
			System.out.println("CMGroupManager.processUSER_TALK(), sent by user("+ie.getUserName()+")");
			System.out.println("session("+ie.getHandlerSession()+"), group("+ie.getHandlerGroup()+")");
			System.out.println("chat: "+ie.getTalk());
			
			ie = null;
		}
		
		return;
	}
	
	private static void processINHABITANT(CMMessage msg)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMDataEvent de = new CMDataEvent(msg.m_buf);

		CMUser myself = interInfo.getMyself();
		if(de.getUserName().equals(myself.getName()) && Objects.equals(de.getUuid(), myself.getUuid()))
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMGroupManager.processINHABITNAT(), the inhabitant is myself.");
			return;
		}
		
		CMSession session = interInfo.findSession(de.getHandlerSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.processINHABITANT(), session("+de.getHandlerSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(de.getHandlerGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.processINHABITANT(), session("+de.getHandlerSession()
					+") found, group("+de.getHandlerGroup()+") NOT found.");
			return;
		}
		
		CMUser tuser = new CMUser();
		tuser.setName(de.getUserName());
		tuser.setUuid(de.getUuid()); // add UUID of the inhabitant user
		tuser.setHost(de.getHostAddress());
		tuser.setUDPPort(de.getUDPPort());
		tuser.setCurrentSession(de.getHandlerSession());
		tuser.setCurrentGroup(de.getHandlerGroup());
		group.getGroupUsers().addMember(tuser);
		// Currently, at the client, login user list and session member list is not maintained..(not clear)
		
		if(CMInfo._CM_DEBUG) {
			System.out.println("CMGroupManager.processINHABITANT(), session("+de.getHandlerSession()
					+"), group("+de.getHandlerGroup()+"), inhabitant("+de.getUserName()
					+"), uuid("+de.getUuid()+") added.");
		}

	}
	
	private static void processNEW_USER(CMMessage msg)
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMDataEvent de = new CMDataEvent(msg.m_buf);

		CMUser myself = interInfo.getMyself();
		if(de.getUserName().equals(myself.getName()) && Objects.equals(de.getUuid(), myself.getUuid()))
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMGroupManager.processNEW_USER(), the new user is myself.");

			// get file-sync mode
			CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
			CMFileSyncMode fileSyncMode = confInfo.getFileSyncMode();
			if(CMInfo._CM_DEBUG) {
				System.out.println("file-sync mode: "+fileSyncMode);
			}
			if(fileSyncMode != CMFileSyncMode.OFF) {
				// get CMFileSyncManager
				CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
				Objects.requireNonNull(syncManager);
				// start file-sync
				syncManager.startFileSync(fileSyncMode);
			}
			return;
		}
		
		CMSession session = interInfo.findSession(de.getHandlerSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.processNEW_USER(), session("+de.getHandlerSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(de.getHandlerGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.processNEW_USER(), session("+de.getHandlerSession()
					+") found, group("+de.getHandlerGroup()+") NOT found.");
			return;
		}
		
		CMUser tuser = new CMUser();
		tuser.setName(de.getUserName());
		tuser.setUuid(de.getUuid()); // add UUID of the new user
		tuser.setHost(de.getHostAddress());
		tuser.setUDPPort(de.getUDPPort());
		tuser.setCurrentSession(de.getHandlerSession());
		tuser.setCurrentGroup(de.getHandlerGroup());
		group.getGroupUsers().addMember(tuser);			
		// Currently, at the client, login user list and session member list is not maintained..(not clear)
		
		if(CMInfo._CM_DEBUG) {
			System.out.println("CMGroupManager.processNEW_USER(), session("+de.getHandlerSession()
					+"), group("+de.getHandlerGroup()+"), new user("+de.getUserName()
					+"), uuid("+de.getUuid()+") added.");
		}

	}
	
	private static void notifyGroupUsersOfRemovedUser(CMUser user)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMSession session = interInfo.findSession(user.getCurrentSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.notifyGroupUsersOfRemovedUser(), session("
					+user.getCurrentSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(user.getCurrentGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.notifyGroupUsersOfRemovedUser(), session("
					+user.getCurrentSession()+") found, but group("+user.getCurrentGroup()
					+") NOT FOUND.");
			return;
		}
		
		CMDataEvent de = new CMDataEvent();
		de.setID(CMDataEvent.REMOVE_USER);
		de.setHandlerSession(session.getSessionName());
		de.setHandlerGroup(group.getGroupName());
		de.setUserName(user.getName());
		de.setUuid(user.getUuid()); // add UUID of the removed user
		CMEventManager.castEvent(de,  group.getGroupUsers());
		
	}
	
	private static void processREMOVE_USER(CMMessage msg)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMDataEvent de = new CMDataEvent(msg.m_buf);
		String strUser = de.getUserName();
		UUID userUuid = de.getUuid();
		String strHandlerSession = de.getHandlerSession();
		String strHandlerGroup = de.getHandlerGroup();
		
		if(strUser.equals(interInfo.getMyself().getName()))
		{
			System.out.println("CMGroupManager.processREMOVE_USER(), the removed user("
					+strUser+") is myself.");
			return;
		}
		
		CMSession session = interInfo.findSession(strHandlerSession);
		if(session == null)
		{
			System.out.println("CMGroupManager.processREMOVE_USER(), session("+strHandlerSession+") not found.");
			return;
		}
		CMGroup group = session.findGroup(strHandlerGroup);
		if(group == null)
		{
			System.out.println("CMGroupManager.processREMOVE_USER(), session("+strHandlerSession
					+") found, group("+strHandlerGroup+") NOT found.");
			return;
		}

		//boolean ret = group.getGroupUsers().removeMember(de.getUserName());
		boolean ret = group.getGroupUsers().removeMember(strUser, userUuid);
		if(ret)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMGroupManager.processREMOVE_USER(), session("+strHandlerSession
						+"), group("+strHandlerGroup+"), user("+strUser+"), uuid("+userUuid+") removed.");
			}
		}
		
		// check whether the p2p file-transfer with the user is ongoing or not
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
//		fInfo.removeSendFileList(strUser, userUuid);	// will be changed after modification with file-transfer related codes
//		fInfo.removeRecvFileList(strUser, userUuid);
		fInfo.removeSendFileList(strUser);
		fInfo.removeRecvFileList(strUser);
	}
	
	private static void processDummyEvent(CMMessage msg)
	{
		if(CMInfo._CM_DEBUG)
		{
			CMDummyEvent due = new CMDummyEvent(msg.m_buf);
			System.out.println("CMGroupManager.processDummyEvent(), session("+due.getHandlerSession()
					+"), group("+due.getHandlerGroup()+")");
			System.out.println("dummy msg: "+due.getDummyInfo());
			
			due = null;
		}
	}
	
	private static void processUserEvent(CMMessage msg)
	{
		if(CMInfo._CM_DEBUG)
		{
			CMUserEvent ue = new CMUserEvent(msg.m_buf);
			System.out.println("CMGroupManager.processUserEvent(), strID("+ue.getStringID()+")");
			System.out.format("%-5s%-20s%-20s%n", "Type", "Field", "Value");
			System.out.println("---------------------------------------------");
			Iterator<CMUserEventField> iter = ue.getAllEventFields().iterator();
			while(iter.hasNext())
			{
				CMUserEventField uef = iter.next();
				System.out.format("%-5d%-20s%-20s%n", uef.nDataType, uef.strFieldName, uef.strFieldValue);
			}
			
			ue = null;
		}
	}
	
	///////////////////////////////////////////////////////////////////////////////
	
	public static void addDistributeGroupUsers(CMUser targetUser)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMSession session = interInfo.findSession(targetUser.getCurrentSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.addDistributeGroupUsers(), session("
					+targetUser.getCurrentSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(targetUser.getCurrentGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.addDistributeGroupUsers(), session("
					+targetUser.getCurrentSession()+") found, but group("
					+targetUser.getCurrentGroup()+") NOT found.");
			return;
		}
		
		Iterator<CMUser> iterUser = group.getGroupUsers().getAllMembers().iterator();
		while(iterUser.hasNext())
		{
			CMUser tUser = iterUser.next();
			CMMultiServerEvent mse = new CMMultiServerEvent();
			mse.setID(CMMultiServerEvent.ADD_GROUP_INHABITANT);
			mse.setServerName(interInfo.getMyself().getName());
			mse.setUserName(tUser.getName());
			mse.setHostAddress(tUser.getHost());
			mse.setUDPPort(tUser.getUDPPort());
			mse.setSessionName(tUser.getCurrentSession());
			mse.setGroupName(tUser.getCurrentGroup());
			CMEventManager.unicastEvent(mse, targetUser.getName());
			mse = null;
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMGroupManager.addDistributeGroupUsers(), session("+session.getSessionName()
					+"), group("+group.getGroupName()+") inhabitants to user("+targetUser.getName()+").");
		return;
	}

	public static void addNotifyGroupUsersOfNewUser(CMUser newUser)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMSession session = interInfo.findSession(newUser.getCurrentSession());
		if(session == null)
		{
			System.out.println("CMGroupManager.addNotifyGroupUsersOfNewUser(), session("
					+newUser.getCurrentSession()+") not found.");
			return;
		}
		CMGroup group = session.findGroup(newUser.getCurrentGroup());
		if(group == null)
		{
			System.out.println("CMGroupManager.addNotifyGroupUsersOfNewUser(), session("
					+newUser.getCurrentSession()+") found, but group("+newUser.getCurrentGroup()
					+") NOT found.");
			return;
		}
		
		CMMultiServerEvent mse = new CMMultiServerEvent();
		mse.setID(CMMultiServerEvent.ADD_NEW_GROUP_USER);
		mse.setServerName(interInfo.getMyself().getName());
		mse.setUserName(newUser.getName());
		mse.setHostAddress(newUser.getHost());
		mse.setUDPPort(newUser.getUDPPort());
		mse.setSessionName(newUser.getCurrentSession());
		mse.setGroupName(newUser.getCurrentGroup());
		
		CMMember gMember = group.getGroupUsers();
		CMEventManager.castEvent(mse, gMember);

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMGroupManager.addNotifyGroupUsersOfNewUser(), session("
					+session.getSessionName()+"), group("+group.getGroupName()+") new user("
					+newUser.getName()+").");
		}
		
		mse = null;
		return;
	}

}
