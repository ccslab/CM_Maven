package kr.ac.konkuk.ccslab.cm.manager;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMSession;
import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMGroup;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
import kr.ac.konkuk.ccslab.cm.entity.CMMember;
import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMUser;
import kr.ac.konkuk.ccslab.cm.event.CMEventSynchronizer;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.event.CMSessionEvent;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMConfigurationInfo;
import kr.ac.konkuk.ccslab.cm.info.CMEventInfo;
import kr.ac.konkuk.ccslab.cm.info.CMFileTransferInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
import kr.ac.konkuk.ccslab.cm.info.CMThreadInfo;
import kr.ac.konkuk.ccslab.cm.thread.CMOpenChannelTask;
import kr.ac.konkuk.ccslab.cm.thread.CMRecvFileTask;
import kr.ac.konkuk.ccslab.cm.thread.CMSendFileTask;

public class CMFileTransferManager {

	public static void init()
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		String strPath = confInfo.getTransferedFileHome().toString();
		
		// if the default directory does not exist, create it.
		File defaultPath = new File(strPath);
		if(!defaultPath.exists() || !defaultPath.isDirectory())
		{
			boolean ret = defaultPath.mkdirs();
			if(ret)
			{
				if(CMInfo._CM_DEBUG_2)
					System.out.println("A default path is created!");
			}
			else
			{
				System.out.println("A default path cannot be created!");
				return;
			}
		}
		
		if(CMInfo._CM_DEBUG_2)
			System.out.println("A default path for the file transfer: "+strPath);

		// copy the throughput-test file from resource folder to the transfered file home.
		InputStream is = CMFileTransferManager.class.getClassLoader()
				.getResourceAsStream("throughput-test.jpg");
		Path target = Paths.get(strPath, "throughput-test.jpg");
		try {
			Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	public static void terminate()
	{
		// nothing to do
	}
	
	public static boolean requestPermitForPullFile(String strFileName, String strFileOwner)
	{
		boolean bReturn = false;
		bReturn = requestPermitForPullFile(strFileName, strFileOwner, CMInfo.FILE_DEFAULT, 
				-1);
		return bReturn;
	}
	
	public static boolean requestPermitForPullFile(String strFileName, String strFileOwner, 
			byte byteFileAppend)
	{
		boolean bReturn = false;
		bReturn = requestPermitForPullFile(strFileName, strFileOwner, byteFileAppend, -1
        );
		return bReturn;		
	}

	public static boolean requestPermitForPullFile(String strFileName, String strFileOwner, byte byteFileAppend,
												   int nContentID) {
		// Updated to use Singleton pattern for info objects
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

		List<CMUser> fileOwnerList = null;
		CMUser fileOwner = null;

		// 1. If the file owner is a server, call the method with null UUID
		CMServer server = CMInteractionManager.findServer(strFileOwner);
		if( server != null )
		{
			return requestPermitForPullFile(strFileName, strFileOwner, null, byteFileAppend, nContentID);
		}

		// 2. If the file owner is a client, find the list of active login devices
		// Search based on the local system type
		if( confInfo.getSystemType().equals("SERVER") )
		{
			// If 'myself' is a server, search in the login user list
			fileOwnerList = interInfo.getLoginUsers().findMemberList(strFileOwner);
			if( fileOwnerList == null || fileOwnerList.isEmpty() )
			{
				System.err.println("CMFileTransferManager.requestPermitForPullFile(), "
						+ "list of file owner("+strFileOwner+") not found or empty in the login user list!");
				return false;
			}
		}
		else if( confInfo.getSystemType().equals("CLIENT") )
		{
			// If 'myself' is a client, search in the group member list
			fileOwnerList = CMInteractionManager.findGroupMemberOfClient(strFileOwner);
			if( fileOwnerList == null || fileOwnerList.isEmpty() )
			{
				System.err.println("CMFileTransferManager.requestPermitForPullFile(), "
						+ "list of file owner("+strFileOwner+") not found or empty in the same group member list!");
				return false;
			}
		}
		else
		{
			System.err.println("CMFileTransferManager.requestPermitForPullFile(), "
					+ "my system type unknown!");
			return false;
		}

		// 3. Selection Strategy: Select the first owner node in the list for implementation ease
		fileOwner = fileOwnerList.get(0);

		// 4. Delegate to the 5-parameter version with the selected UUID
		return requestPermitForPullFile(strFileName, strFileOwner, fileOwner.getUuid(),
				byteFileAppend, nContentID);
	}
	
	public static boolean requestPermitForPullFile(String strFileName, String strFileOwner, UUID fileOwnerUuid,
			byte byteFileAppend, int nContentID)
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMCommInfo commInfo = CMCommInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		boolean bReturn = false;
		CMUser myself = CMInteractionInfo.getInstance().getMyself();
		
		fInfo.setStartRequestTime(System.currentTimeMillis());
		
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_PERMIT_PULL_FILE);
		fe.setFileSender(strFileOwner);
		fe.setFileSenderUuid(fileOwnerUuid);
		fe.setFileReceiver(myself.getName());	// requester name
		fe.setFileReceiverUuid(myself.getUuid());
		fe.setFileName(strFileName);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteFileAppend);
		
		if(confInfo.isFileTransferScheme() && isP2PFileTransfer(fe))
		{
			ServerSocketChannel ssc = commInfo.getNonBlockServerSocketChannel();
			if(ssc == null)
			{
				////////// for Android client where network-related methods must be called in a separate thread
				////////// rather than the MainActivity thread
				CMOpenChannelTask task = new CMOpenChannelTask(CMInfo.CM_SERVER_CHANNEL,
						myself.getHost(), 0, false);
				ExecutorService es = CMThreadInfo.getInstance().getExecutorService();
				Future<SelectableChannel> future = es.submit(task);
				try {
					ssc = (ServerSocketChannel) future.get();
					commInfo.setNonBlockServerSocketChannel(ssc);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
				//////////
			}
						
			InetSocketAddress isa = null;
			try {
				isa = (InetSocketAddress)ssc.getLocalAddress();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			int nSSCPort = isa.getPort();
			
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.requestPermitForPullFile(), "
						+"assigned port number of ssc("+nSSCPort+").");
			}
			
			// set the port number of SSC in the request event
			fe.setSSCPort(nSSCPort);
		}
		
		if(isP2PFileTransfer(fe))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.requestPermitForPullFile(), "
						+ "isP2PFileTransfer() returns true.");				
			}
			
			// set distribution session and distribution group
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileOwner);
			fe.setDistributionUuid(fileOwnerUuid);

			// send the event to the default server
			String strDefServer = CMInteractionInfo.getInstance().getDefaultServerInfo().getServerName();
			bReturn = CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.requestPermitForPullFile(), "
						+ "isP2PFileTransfer() returns false.");				
			}

			bReturn = CMEventManager.unicastEvent(fe, strFileOwner, fileOwnerUuid);
		}
		
		return bReturn;
	}
	
	public static boolean isP2PFileTransfer(CMFileEvent fe)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strFileSender = fe.getFileSender();
		UUID fileSenderUuid = fe.getFileSenderUuid();
		String strFileReceiver = fe.getFileReceiver();
		UUID fileReceiverUuid = fe.getFileReceiverUuid();
		CMUser myself = interInfo.getMyself();
		
		if(confInfo.getCommArch().contentEquals("CM_CS") &&
				confInfo.getSystemType().contentEquals("CLIENT"))
		{
			String strSession = myself.getCurrentSession();
			String strGroup = myself.getCurrentGroup();
			CMSession session = interInfo.findSession(strSession);
			if(session == null)
			{
				System.err.println("CMFileTransferManager.isP2PFileTransfer(), session("
						+strSession+") not found!");
				return false;
			}
			CMGroup group = session.findGroup(strGroup);
			if(group == null)
			{
				System.err.println("CMFileTransferManager.isP2PFileTransfer(), group("
						+strGroup+") not found!");
				return false;
			}
			CMMember groupMember = group.getGroupUsers();
			
			if(strFileSender.equals(myself.getName()) &&
					Objects.equals(fileSenderUuid, myself.getUuid()) &&
					groupMember.findMember(strFileReceiver, fileReceiverUuid) != null)
			{
				bReturn = true;
			}
			else if(strFileReceiver.equals(myself.getName()) &&
					Objects.equals(fileReceiverUuid, myself.getUuid()) &&
					groupMember.findMember(strFileSender, fileSenderUuid) != null)
			{
				bReturn = true;
			}
		}

		return bReturn;
	}
	
	public static boolean replyPermitForPullFile(CMFileEvent fe, int nReturnCode)
	{
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bRet = false;
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.REPLY_PERMIT_PULL_FILE);
		feAck.setFileSender(fe.getFileSender());
		feAck.setFileSenderUuid(fe.getFileSenderUuid());
		feAck.setFileReceiver(fe.getFileReceiver());
		feAck.setFileReceiverUuid(fe.getFileReceiverUuid());
		feAck.setFileName(fe.getFileName());
		feAck.setContentID(fe.getContentID());
		feAck.setReturnCode(nReturnCode);
		
		if(isP2PFileTransfer(feAck))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.replyPermitForPullFile(), "
						+ "isP2PFileTransfer() returns true.");
			}
			
			if(nReturnCode == 1 && confInfo.isFileTransferScheme())
			{
				// set ssc port number of the file receiver to the receiver client info
				CMUser fileReceiver = CMInteractionManager.findGroupMemberOfClient(fe.getFileReceiver(),
						fe.getFileReceiverUuid());
				if(fileReceiver == null)
				{
					System.err.println("file receiver("+fe.getFileReceiver()+"), uuid("
							+fe.getFileReceiverUuid()+") not found in session("
							+interInfo.getMyself().getCurrentSession()+") and group("
							+interInfo.getMyself().getCurrentGroup()+")!");
					return false;
				}
				fileReceiver.setSSCPort(fe.getSSCPort());
			}
			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(fe.getFileReceiver());
			feAck.setDistributionUuid(fe.getFileReceiverUuid());

			// send the event to the default server
			String strDefServer = CMInteractionInfo.getInstance().getDefaultServerInfo().getServerName();
			bRet = CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.replyPermitForPullFile(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file receiver
			bRet = CMEventManager.unicastEvent(feAck, fe.getFileReceiver(), fe.getFileReceiverUuid());
		}		
		
		if(bRet && nReturnCode == 1)
		{
			String strFilePath = confInfo.getTransferedFileHome().toString() + 
					File.separator + fe.getFileName();
			bRet = pushFile(strFilePath, fe.getFileReceiver(), fe.getFileReceiverUuid(), fe.getFileAppendFlag());
		}
		
		return bRet;
	}
		
	public static boolean cancelPullFile(String strFileSender)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		if(confInfo.isFileTransferScheme())
			bReturn = cancelPullFileWithSepChannel(strFileSender);
		else
		{
			System.err.println("CMFileTransferManager.cancelRequestFile(); default file transfer does not support!");
		}
		
		return bReturn;		
	}

	// cancel the receiving file task with separate channels and threads
	private static boolean cancelPullFileWithSepChannel(String strFileSender)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();

		if(strFileSender != null)
		{
			bReturn = cancelPullFileWithSepChannelForOneSender(strFileSender);
		}
		else // cancel file transfer to all senders
		{
			Set<String> keySet = fInfo.getRecvFileHashtable().keySet();
			Iterator<String> iterKeys = keySet.iterator();
			while(iterKeys.hasNext())
			{
				String iterSender = iterKeys.next();
				bReturn = cancelPullFileWithSepChannelForOneSender(iterSender);
			}
			// clear the sending file hash table
			bReturn = fInfo.clearRecvFileHashtable();
		}
		
		return bReturn;
	}

	// cancel the receiving file task from one sender with a separate channel and thread
	private static boolean cancelPullFileWithSepChannelForOneSender(String strFileSender)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMList<CMRecvFileInfo> recvList = null;
		CMRecvFileInfo rInfo = null;
		boolean bReturn = false;
		Future<CMRecvFileInfo> recvTask = null;
		CMFileEvent fe = null;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		boolean bP2PFileTransfer = false;
		
		// find the CMRecvFile list of the strSender
		recvList = fInfo.getRecvFileList(strFileSender);
		if(recvList == null)
		{
			System.err.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
					+ "receiving file list not found for the sender("+strFileSender+")!");
			return false;
		}
		
		// find the current receiving file task
		rInfo = fInfo.findRecvFileInfoOngoing(strFileSender);
		if(rInfo == null)
		{
			System.err.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
					+ "ongoing receiving task not found for the sender("+strFileSender+")!");
			bReturn = fInfo.removeRecvFileList(strFileSender);
			return bReturn;
		}
		
		// request for canceling the receiving task
		recvTask = rInfo.getRecvTaskResult();
		recvTask.cancel(true);
		// wait for the thread cancellation
		try {
			recvTask.get(10L, TimeUnit.SECONDS);
		} catch(CancellationException e) {
			System.out.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
					+ "the receiving task cancelled.: "
					+ "file sender("+strFileSender+"), file("+rInfo.getFileName()
					+"), file size("+rInfo.getFileSize()+ "), recv size("
					+rInfo.getRecvSize()+")");
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		
		/////////////////////// management of the closed default blocking socket channel
		
		/*
		// get the default blocking socket channel
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockSCInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
						+ "# blocking socket channel: "	+ blockSCInfo.getSize());
			}
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);	// default blocking channel
				
		}
		else	// server
		{
			CMUser receiver = interInfo.getLoginUsers().findMember(strSender);
			blockSCInfo = receiver.getBlockSocketChannelInfo();
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) receiver.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
						+ "# blocking socket channel: "	+ blockSCInfo.getSize());
			}

		}
		*/

		CMServer targetServer = CMInteractionManager.findServer(strFileSender);
		if(targetServer != null)
		{
			blockSCInfo = targetServer.getBlockSocketChannelInfo();
		}
		else
		{
			CMUser targetUser = null;
			if(confInfo.getSystemType().contentEquals("CLIENT"))
			{
				targetUser = CMInteractionManager.findGroupMemberOfClient(strFileSender
				);
			}
			else
			{
				targetUser = interInfo.getLoginUsers().findMember(strFileSender);
			}
			
			if(targetUser == null)
			{
				System.err.println("CMFileTransferManager.cancelPullFileWithSepChannelForOneReceiver(), "
						+"target("+strFileSender+") not found!");
				return false;
			}
			blockSCInfo = targetUser.getBlockSocketChannelInfo();
		}

		defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);
		if(defaultBlockSC == null)
		{
			System.err.println("CMFileTransferManager.cancelPullFileWithSepChannelForOneReceiver(), "
					+"blocking sc of target("+strFileSender+") is null!");
			return false;
		}

		// close the default blocking socket channel if it is open
		// the channel is actually closed due to the interrupt exception of the receiving thread
		if(defaultBlockSC.isOpen())
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
						+ "the default channel is still open and should be closed for reconnection!");
			}
			
			try {
				defaultBlockSC.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
					+ "the default channel is already closed!");
		}
		
		// remove the default blocking socket channel
		blockSCInfo.removeChannel(0);

		// send the cancel event to the sender
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.CANCEL_FILE_RECV_CHAN);
		fe.setFileSender(strFileSender);
		fe.setFileReceiver(interInfo.getMyself().getName());
		
		bP2PFileTransfer = isP2PFileTransfer(fe);
		
		if(bP2PFileTransfer)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPullFileWithSepChannelForOneSender(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set event sender and receiver
			fe.setSender(interInfo.getMyself().getName());
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			fe.setReceiver(strDefServer);
			
			// set distribution fields
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileSender);
			
			// send the event to the default server
			bReturn = CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPullFileWithSepChannelForOneSender(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// set event sender and receiver
			fe.setSender(interInfo.getMyself().getName());
			fe.setReceiver(strFileSender);
			// send the event to the file sender
			bReturn = CMEventManager.unicastEvent(fe, strFileSender);
		}
		
		if(!bReturn)
		{
			return false;
		}
		
		// remove the receiving file list of the sender
		bReturn = fInfo.removeRecvFileList(strFileSender);

		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT") && !bP2PFileTransfer)
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			if(defaultBlockSC == null)
			{
				System.err.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); recreation of "
						+ "the blocking socket channel failed!: server("+serverInfo.getServerAddress()+"), port("
						+ serverInfo.getServerPort() +")");
				return false;
			}
			bReturn = blockSCInfo.addChannel(0, defaultBlockSC);

			if(bReturn)
			{
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				se.setChannelName(interInfo.getMyself().getName());
				se.setChannelNum(0);
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true);
				se = null;

				if(bReturn)
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
								+ "successfully requested to add the blocking socket channel with the key(0) "
								+ "to the server("+serverInfo.getServerName()+")");
					}
					
				}
			}
		}
		
		//////////////////////////////////

		return bReturn;		
	}

	// [Modified] Wrapper method for multi-device support (Design Doc #08)
	// If the file receiver has multiple logged-in devices, this method calls the core method for each device.
	public static boolean requestPermitForPushFile(String strFilePath,
			String strFileReceiver,	byte byteFileAppend, int nContentID)
	{
		// [Modified] Use singleton pattern for info objects
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bReturn = false;

		List<CMUser> fileReceiverList = null;

		// 1. Find the target users (devices) based on system type
		if (confInfo.getSystemType().equals("SERVER")) {
			// If I am a server, find the receiver in the login user list
			fileReceiverList = interInfo.getLoginUsers().findMemberList(strFileReceiver);
		} else if (confInfo.getSystemType().equals("CLIENT")) {
			// If I am a client, find the receiver in the group member list
			fileReceiverList = CMInteractionManager.findGroupMemberOfClient(strFileReceiver);
		}

		// 2. Dispatch logic
		if (fileReceiverList == null || fileReceiverList.isEmpty()) {
			// If no specific device list is found, try sending with null UUID (legacy or single target)
			bReturn = requestPermitForPushFile(strFilePath, strFileReceiver, null, byteFileAppend,
					nContentID);
		} else {
			// Iterate over all found devices (UUIDs) and request permit for each
			for (CMUser user : fileReceiverList) {
				// Logical OR to ensure success if at least one request succeeds
				boolean bResult = requestPermitForPushFile(strFilePath, strFileReceiver,
						user.getUuid(), byteFileAppend, nContentID);
				bReturn = bReturn || bResult;
			}
		}

		return bReturn;
	}

	// [Modified] Core method with UUID parameter (Design Doc #08)
	// This method handles the actual event generation and transmission for a specific target device.
	public static boolean requestPermitForPushFile(String strFilePath, 
			String strFileReceiver, UUID fileReceiverUuid, byte byteFileAppend, int nContentID)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bReturn = false;
		
		// get file information (size)
		File file = new File(strFilePath);
		if(!file.exists())
		{
			System.err.println("CMFileTransferManager.requestPermitForPushFile(), file("
					+strFilePath+") does not exists.");
			return false;
		}
		long lFileSize = file.length();
		
		fInfo.setStartRequestTime(System.currentTimeMillis());
		
		// get sender (my) name
		CMUser myself = interInfo.getMyself();
		String strMyName = myself.getName();
		UUID myUuid = myself.getUuid();
		
		// make and send a REQUEST_PERMIT_PUSH_FILE event
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_PERMIT_PUSH_FILE);
		fe.setFileSender(strMyName);
		fe.setFileSenderUuid(myUuid);
		fe.setFileReceiver(strFileReceiver);
		fe.setFileReceiverUuid(fileReceiverUuid);
		fe.setFilePath(strFilePath);
		fe.setFileSize(lFileSize);
		fe.setFileAppendFlag(byteFileAppend);
		fe.setContentID(nContentID);
		
		if(isP2PFileTransfer(fe))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.requestPermitForPushFile(), "
						+ "isP2PFileTransfer() returns true.");
			}

			// set distribution fields
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileReceiver);
			fe.setDistributionUuid(fileReceiverUuid);
			
			// send the event to the default server
			bReturn = CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.requestPermitForPushFile(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file receiver
			bReturn = CMEventManager.unicastEvent(fe, strFileReceiver, fileReceiverUuid);
		}
		
		return bReturn;
	}
	
	public static boolean replyPermitForPushFile(CMFileEvent fe, int nReturnCode)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bRet = false;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMCommInfo commInfo = CMCommInfo.getInstance();
				
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.REPLY_PERMIT_PUSH_FILE);
		feAck.setFileSender(fe.getFileSender());
		feAck.setFileSenderUuid(fe.getFileSenderUuid());
		feAck.setFileReceiver(fe.getFileReceiver());
		feAck.setFileReceiverUuid(fe.getFileReceiverUuid());
		feAck.setFilePath(fe.getFilePath());
		feAck.setFileSize(fe.getFileSize());
		feAck.setFileAppendFlag(fe.getFileAppendFlag());
		feAck.setContentID(fe.getContentID());
		feAck.setReturnCode(nReturnCode);

		if(confInfo.isFileTransferScheme() && isP2PFileTransfer(feAck))
		{
			ServerSocketChannel ssc = commInfo.getNonBlockServerSocketChannel();
			if(ssc == null)
			{
				try {
					ssc = (ServerSocketChannel) CMCommManager.openNonBlockChannel(CMInfo.CM_SERVER_CHANNEL, 
							interInfo.getMyself().getHost(), 0);
					commInfo.setNonBlockServerSocketChannel(ssc);
				} catch (IOException e) {
					e.printStackTrace();
					return false;
				} 
			}
			
			InetSocketAddress isa = null;
			try {
				isa = (InetSocketAddress)ssc.getLocalAddress();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			int nSSCPort = isa.getPort();
			
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.replyPermitForPushFile(), "
						+"assigned port number of ssc("+nSSCPort+").");
			}
			
			// set the port number of SSC in the ack event
			feAck.setSSCPort(nSSCPort);
		}

		if(isP2PFileTransfer(feAck))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.replyPermitForPushFile(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(fe.getFileSender());
			feAck.setDistributionUuid(fe.getFileSenderUuid());

			// send event to the default server
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			bRet = CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.replyPermitForPushFile(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file sender
			bRet = CMEventManager.unicastEvent(feAck, fe.getFileSender(), fe.getFileSenderUuid());
		}
		
		return bRet;
	}
	
	public static boolean pushFile(String strFilePath, String strReceiver, UUID receiverUuid)
	{
		boolean bReturn = false;
		bReturn = pushFile(strFilePath, strReceiver, receiverUuid, CMInfo.FILE_DEFAULT, -1);
		return bReturn;
	}

	public static boolean pushFile(String strFilePath, String strReceiver, UUID receiverUuid, byte byteFileAppend)
	{
		boolean bReturn = false;
		bReturn = pushFile(strFilePath, strReceiver, receiverUuid, byteFileAppend, -1);
		return bReturn;
	}

	public static boolean pushFile(String strFilePath, String strReceiver, UUID receiverUuid, byte byteFileAppend,
			int nContentID)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		
		fInfo.setStartSendTime(System.currentTimeMillis());
		
		if(confInfo.isFileTransferScheme())
			bReturn = pushFileWithSepChannel(strFilePath, strReceiver, receiverUuid, byteFileAppend, nContentID);
		else
			bReturn = pushFileWithDefChannel(strFilePath, strReceiver, receiverUuid, byteFileAppend, nContentID);
		return bReturn;
	}

	// strFilePath: absolute or relative path to a target file
	private static boolean pushFileWithDefChannel(String strFilePath, String strFileReceiver, UUID fileReceiverUuid,
			byte byteFileAppend, int nContentID)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		
		// get file information (size)
		File file = new File(strFilePath);
		if(!file.exists())
		{
			System.err.println("CMFileTransferManager.pushFileWithDefChannel(), file("+strFilePath+") does not exists.");
			return false;
		}
		long lFileSize = file.length();

		// get my name
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();

		// add send file information
		// receiver name, file path, size
		CMSendFileInfo sfInfo = new CMSendFileInfo();
		sfInfo.setFileSender(strMyName);
		sfInfo.setFileSenderUuid(myUuid);
		sfInfo.setFileReceiver(strFileReceiver);
		sfInfo.setFileReceiverUuid(fileReceiverUuid);
		sfInfo.setFilePath(strFilePath);
		sfInfo.setFileSize(lFileSize);
		sfInfo.setContentID(nContentID);
		sfInfo.setAppendMode(byteFileAppend);
		//fInfo.addSendFileInfo(strFileReceiver, strFilePath, lFileSize, nContentID);
		bReturn = fInfo.addSendFileInfo(sfInfo);
		if(!bReturn)
		{
			System.err.println("CMFileTransferManager.pushFileWithDefChannel(); "
					+ "error for adding the sending file info: "
					+"receiver("+strFileReceiver+", "+fileReceiverUuid+"), file("+strFilePath+"), size("
					+lFileSize+"), content ID("+nContentID+")!");
			return false;
		}
		// set the cancellation flag
		fInfo.setCancelSend(false);

		// get file name
		String strFileName = getFileNameFromPath(strFilePath);
		//System.out.println("file name: "+strFileName);
		
		// start file transfer process
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.START_FILE_TRANSFER);
		fe.setFileSender(strMyName);
		fe.setFileSenderUuid(myUuid);
		fe.setFileReceiver(strFileReceiver);
		fe.setFileReceiverUuid(fileReceiverUuid);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteFileAppend);
		
		if(isP2PFileTransfer(fe))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.pushFileWithDefChannel(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set distribution fields
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileReceiver);
			fe.setDistributionUuid(fileReceiverUuid);
			
			// send the event to the default server
			bReturn = CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.pushFileWithDefChannel(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file receiver
			bReturn = CMEventManager.unicastEvent(fe, strFileReceiver, fileReceiverUuid);
		}
		
		if(!bReturn)
		{
			// remove send file information
			fInfo.removeSendFileInfo(strFileReceiver, fileReceiverUuid, strFileName, nContentID);
		}

		return bReturn;
	}
	
	// strFilePath: absolute or relative path to a target file
	private static boolean pushFileWithSepChannel(String strFilePath, String strFileReceiver, UUID fileReceiverUuid,
			byte byteFileAppend, int nContentID)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();

		// check the creation of the default blocking TCP socket channel
		CMChannelInfo<Integer> blockChannelList = null;
		CMChannelInfo<Integer> nonBlockChannelList = null;
		SocketChannel sc = null;
		SocketChannel dsc = null;

		CMServer targetServer = CMInteractionManager.findServer(strFileReceiver);
		if(targetServer != null)
		{
			blockChannelList = targetServer.getBlockSocketChannelInfo();
			nonBlockChannelList = targetServer.getNonBlockSocketChannelInfo();
		}
		else
		{
			CMUser targetUser = null;
			if(confInfo.getSystemType().contentEquals("CLIENT"))
			{
				targetUser = CMInteractionManager.findGroupMemberOfClient(strFileReceiver, fileReceiverUuid);
				nonBlockChannelList = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
			}
			else
			{
				targetUser = interInfo.getLoginUsers().findMember(strFileReceiver, fileReceiverUuid);
				nonBlockChannelList = targetUser.getNonBlockSocketChannelInfo();
			}
			if(targetUser == null)
			{
				System.err.println("CMFileTransferManager.pushFileWithSepChannel(), target("
						+strFileReceiver+"), uuid("+fileReceiverUuid+") not found!");
				return false;
			}
			blockChannelList = targetUser.getBlockSocketChannelInfo();
		}

		dsc = (SocketChannel) nonBlockChannelList.findChannel(0);	// key for the default TCP socket channel is 0
		if(dsc == null)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default TCP socket channel not found!");
			return false;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default TCP socket channel closed!");
			return false;
		}

		// get file information (size)
		File file = new File(strFilePath);
		if(!file.exists())
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(), file("+strFilePath+") does not exists.");
			return false;
		}
		long lFileSize = file.length();

		// add send file information
		// sender name, receiver name, file path, size, content ID
		CMSendFileInfo sfInfo = new CMSendFileInfo();
		sfInfo.setFileSender(strMyName);
		sfInfo.setFileSenderUuid(myUuid);
		sfInfo.setFileReceiver(strFileReceiver);
		sfInfo.setFileReceiverUuid(fileReceiverUuid);
		sfInfo.setFilePath(strFilePath);
		sfInfo.setFileSize(lFileSize);
		sfInfo.setContentID(nContentID);
		//sfInfo.setSendChannel(sc);
		sfInfo.setDefaultChannel(dsc);
		sfInfo.setAppendMode(byteFileAppend);
		//boolean bResult = fInfo.addSendFileInfo(strReceiver, strFilePath, lFileSize, nContentID);
		bReturn = fInfo.addSendFileInfo(sfInfo);
		if(!bReturn)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "error for adding the sending file info: "
					+"receiver("+strFileReceiver+"), uuid("+fileReceiverUuid+"), file("+strFilePath+"), size("
					+lFileSize+"), content ID("+nContentID+")!");
			return false;
		}

		sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
		
		if(sc == null)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default blocking TCP socket channel not found!");
			
			// open and add a new blocking socket channel to the file receiver
			sc = CMCommManager.addBlockSocketChannel(0, strFileReceiver, fileReceiverUuid);
			if(sc == null)
			{
				/*
				// remove the sending file info
				fInfo.removeSendFileInfo(sfInfo);
				*/
				// cancel the sending file task
				cancelPushFile(strFileReceiver, fileReceiverUuid);
				return false;				
			}
			
			// The START_FILE_TRANSFER_CHAN event will be sent after this node receives 
			// the ADD_BLOCK_SOCKET_CHANNEL_ACK event at CMInteractionManager.process..() method.
			return true;
		}
		else
		{
			sfInfo.setSendChannel(sc);			
		}
		
		// send the START_FILE_TRANSFER_CHAN event
		bReturn = sendSTART_FILE_TRANSFER_CHAN(sfInfo);
		return bReturn;
	}
	
	public static boolean sendSTART_FILE_TRANSFER_CHAN(CMSendFileInfo sfInfo)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		String strFilePath = sfInfo.getFilePath();
		String strFileReceiver = sfInfo.getFileReceiver();
		UUID fileReceiverUuid = sfInfo.getFileReceiverUuid();
		long lFileSize = sfInfo.getFileSize();
		int nContentID = sfInfo.getContentID();
		byte byteAppendMode = sfInfo.getAppendMode();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		boolean bReturn = false;
		
		
		// get file name
		String strFileName = getFileNameFromPath(strFilePath);

		// start file transfer process
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.START_FILE_TRANSFER_CHAN);
		fe.setFileSender(strMyName);
		fe.setFileSenderUuid(myUuid);
		fe.setFileReceiver(strFileReceiver);
		fe.setFileReceiverUuid(fileReceiverUuid);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteAppendMode);
		
		if(isP2PFileTransfer(fe))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.pushFileWithSepChannel(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set distribution fields
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileReceiver);
			fe.setDistributionUuid(fileReceiverUuid);
			
			// send the event to the default server
			bReturn = CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.pushFileWithSepChannel(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file receiver
			bReturn = CMEventManager.unicastEvent(fe, strFileReceiver, fileReceiverUuid);
		}

		if(!bReturn)
		{
			//fInfo.removeSendFileInfo(strFileReceiver, strFileName, nContentID);
			fInfo.removeSendFileInfo(sfInfo);
		}
		
		return bReturn;
	}

	/**
	 * Cancels all ongoing push file transfers for a target receiver name.
	 * In a multi-device environment, it cancels transfers for all UUIDs associated with the receiver.
	 * @param strFileReceiver The name of the file receiver.
	 * @return true if at least one cancellation request was successful, false otherwise.
	 */
	public static boolean cancelPushFile(String strFileReceiver) {
		// Applied singleton pattern for Info objects
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bReturn = false;

		// Check system type: SERVER
		if( confInfo.getSystemType().equals("SERVER") )
		{
			// Find all login sessions (UUIDs) for the given user name
			List<CMUser> fileReceiverList = interInfo.getLoginUsers().findMemberList(strFileReceiver);

			if( fileReceiverList == null || fileReceiverList.isEmpty() )
			{
				// If not in login list, the receiver might be another server (no UUID)
				bReturn = cancelPushFile(strFileReceiver, null);
			}
			else
			{
				// Cancel transfers for all identified device UUIDs
				for( CMUser user : fileReceiverList )
				{
					// Return true if any of the calls return true (using bitwise OR assignment)
					bReturn |= cancelPushFile(strFileReceiver, user.getUuid());
				}
			}
		}
		// Check system type: CLIENT
		else if( confInfo.getSystemType().equals("CLIENT") )
		{
			// Find group members matching the receiver name
			List<CMUser> fileReceiverList = CMInteractionManager.findGroupMemberOfClient(strFileReceiver);

			if( fileReceiverList == null || fileReceiverList.isEmpty() )
			{
				// If not a group member, the receiver is likely the server (no UUID)
				bReturn = cancelPushFile(strFileReceiver, null);
			}
			else
			{
				// Cancel transfers for all matching member UUIDs in the group
				for( CMUser user : fileReceiverList )
				{
					bReturn |= cancelPushFile(strFileReceiver, user.getUuid());
				}
			}
		}
		else
		{
			// System type error handling
			System.err.println("CMFileTransferManager.cancelPushFile(), Unknown system type: "
					+ confInfo.getSystemType());
		}

		return bReturn;
	}
	
	public static boolean cancelPushFile(String strFileReceiver, UUID fileReceiverUuid)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		if(confInfo.isFileTransferScheme())
			bReturn = cancelPushFileWithSepChannel(strFileReceiver, fileReceiverUuid);
		else
			bReturn = cancelPushFileWithDefChannel(strFileReceiver, fileReceiverUuid);
		
		return bReturn;
	}
	
	private static boolean cancelPushFileWithDefChannel(String strFileReceiver)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMList<CMSendFileInfo> sendList = null;
		Iterator<CMSendFileInfo> iterSendList = null;
		CMSendFileInfo sInfo = null;
		CMFileEvent fe = null;
		String strDefServer = null;

		if(strFileReceiver != null)
		{
			// find the CMSendFile list of the strReceiver
			sendList = fInfo.getSendFileList(strFileReceiver, fileReceiverUuid);
			if(sendList == null)
			{
				System.err.println("CMFileTransferManager.cancelPushFileWithDefChannel(); Sending file list "
						+ "not found for the receiver("+strFileReceiver+"), uuid("+fileReceiverUuid+").!");
				return false;
			}			
		}
		
		// set the flag
		fInfo.setCancelSend(true);
		
		// send the cancellation event to the receiver
		// close the RandomAccessFile and remove the sending file info of the receiver
		if(strFileReceiver != null) // for the target receiver
		{
			fe = new CMFileEvent();
			fe.setID(CMFileEvent.CANCEL_FILE_SEND);
			fe.setFileSender(interInfo.getMyself().getName());
			fe.setFileReceiver(strFileReceiver);
			
			if(isP2PFileTransfer(fe))
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.cancelPushFileWithDefChannel(), "
							+ "isP2PFileTransfer() returns true.");
				}
				// set event sender and receiver
				fe.setSender(interInfo.getMyself().getName());
				strDefServer = interInfo.getDefaultServerInfo().getServerName();
				fe.setReceiver(strDefServer);
				
				// set distribution fields
				fe.setDistributionSession("CM_ONE_USER");
				fe.setDistributionGroup(strFileReceiver);
				
				// send the event to the default server
				CMEventManager.unicastEvent(fe, strDefServer);
			}
			else
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.cancelPushFileWithDefChannel(), "
							+ "isP2PFileTransfer() returns false.");
				}
				// set event sender and receiver
				fe.setSender(interInfo.getMyself().getName());
				fe.setReceiver(strFileReceiver);
				// send the event to the file receiver
				CMEventManager.unicastEvent(fe, strFileReceiver);
			}
			
			// close the RandomAccessFile
			iterSendList = sendList.getList().iterator();
			while(iterSendList.hasNext())
			{
				sInfo = iterSendList.next();
				if(sInfo.getReadFile() != null)
				{
					try {
						sInfo.getReadFile().close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
			
			bReturn = fInfo.removeSendFileList(strFileReceiver, fileReceiverUuid);
		}
		else	// for all receivers
		{
			Set<String> keySet = fInfo.getSendFileHashtable().keySet();
			Iterator<String> iterKeys = keySet.iterator();
			while(iterKeys.hasNext())
			{
				String iterFileReceiver = iterKeys.next();
				fe = new CMFileEvent();
				fe.setID(CMFileEvent.CANCEL_FILE_SEND);
				fe.setFileSender(interInfo.getMyself().getName());
				fe.setFileReceiver(iterFileReceiver);
				
				if(isP2PFileTransfer(fe))
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMFileTransferManager.cancelPushFileWithDefChannel(), "
								+ "isP2PFileTransfer() returns true.");
					}
					// set event sender and receiver
					fe.setSender(interInfo.getMyself().getName());
					strDefServer = interInfo.getDefaultServerInfo().getServerName();
					fe.setReceiver(strDefServer);
					
					// set distribution fields
					fe.setDistributionSession("CM_ONE_USER");
					fe.setDistributionGroup(iterFileReceiver);
					
					// send the event to the default server
					CMEventManager.unicastEvent(fe, strDefServer);
				}
				else
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMFileTransferManager.cancelPushFileWithDefChannel(), "
								+ "isP2PFileTransfer() returns false.");
					}
					// set event sender and receiver
					fe.setSender(interInfo.getMyself().getName());
					fe.setReceiver(iterFileReceiver);
					// send the event to file receiver
					CMEventManager.unicastEvent(fe, iterFileReceiver);
				}
				
				// close the RandomAccessFile
				sendList = fInfo.getSendFileList(iterFileReceiver, fileReceiverUuid);
				iterSendList = sendList.getList().iterator();
				while(iterSendList.hasNext())
				{
					sInfo = iterSendList.next();
					if(sInfo.getReadFile() != null)
					{
						try {
							sInfo.getReadFile().close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			bReturn = fInfo.clearSendFileHashtable();
		}
		
		if(bReturn)
		{
			if(CMInfo._CM_DEBUG)
				System.out.println("CMFileTransferManager.cancelPushFileWithDefChannel(); succeeded for "
						+ "receiver("+strFileReceiver+").");
		}
		else
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithDefChannel(); failed for "
					+ "receiver("+strFileReceiver+")!");
		}
		
		return bReturn;
	}
	
	// cancel the sending file task with separate channels and threads
	private static boolean cancelPushFileWithSepChannel(String strFileReceiver)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();

		if(strFileReceiver != null)
		{
			bReturn = cancelPushFileWithSepChannelForOneReceiver(strFileReceiver);
		}
		else // cancel file transfer to all receivers
		{
			Set<String> keySet = fInfo.getSendFileHashtable().keySet();
			Iterator<String> iterKeys = keySet.iterator();
			while(iterKeys.hasNext())
			{
				String iterReceiver = iterKeys.next();
				bReturn = cancelPushFileWithSepChannelForOneReceiver(iterReceiver);
			}
			// clear the sending file hash table
			bReturn = fInfo.clearSendFileHashtable();
		}

		return bReturn;
	}

	// cancel the sending file task to one receiver with a separate channel and thread
	private static boolean cancelPushFileWithSepChannelForOneReceiver(String strFileReceiver)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMList<CMSendFileInfo> sendList = null;
		CMSendFileInfo sInfo = null;
		boolean bReturn = false;
		Future<CMSendFileInfo> sendTask = null;
		CMFileEvent fe = null;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		boolean bP2PFileTransfer = false;
		
		// find the CMSendFile list of the strReceiver
		sendList = fInfo.getSendFileList(strFileReceiver, fileReceiverUuid);
		if(sendList == null)
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); Sending file list "
					+ "not found for the receiver("+strFileReceiver+")!");
			//return false;
		}
		else
		{
			// find the current sending file task
			sInfo = fInfo.findSendFileInfoOngoing(strFileReceiver, fileReceiverUuid);
			if(sInfo == null)
			{
				System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); ongoing sending task "
						+ "not found for the receiver("+strFileReceiver+")!");
				bReturn = fInfo.removeSendFileList(strFileReceiver, fileReceiverUuid);
				//return bReturn;
			}
			else
			{
				// request for canceling the sending task
				sendTask = sInfo.getSendTaskResult();
				sendTask.cancel(true);
				// wait for the thread cancellation
				try {
					sendTask.get(10L, TimeUnit.SECONDS);
				} catch(CancellationException e) {
					System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
							+ "the sending task cancelled.: "
							+ "receiver("+strFileReceiver+"), file("+sInfo.getFileName()+"), file size("+sInfo.getFileSize()
							+ "), sent size("+sInfo.getSentSize()+")");
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException e) {
					e.printStackTrace();
				}
				
			}

			// remove the sending file list of the receiver
			bReturn = fInfo.removeSendFileList(strFileReceiver, fileReceiverUuid);

		}
		

		/////////////////////// management of the closed default blocking socket channel
		
		/*
		// get the default blocking socket channel
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockSCInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
						+ "# blocking socket channel: "	+ blockSCInfo.getSize());
			}
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);	// default blocking channel
				
		}
		else	// server
		{
			CMUser receiver = interInfo.getLoginUsers().findMember(strFileReceiver);
			blockSCInfo = receiver.getBlockSocketChannelInfo();
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) receiver.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
						+ "# blocking socket channel: "	+ blockSCInfo.getSize());
			}

		}
		*/
		CMServer targetServer = CMInteractionManager.findServer(strFileReceiver);
		if(targetServer != null)
		{
			blockSCInfo = targetServer.getBlockSocketChannelInfo();
		}
		else
		{
			CMUser targetUser = null;
			if(confInfo.getSystemType().contentEquals("CLIENT"))
			{
				targetUser = CMInteractionManager.findGroupMemberOfClient(strFileReceiver
				);
			}
			else
			{
				targetUser = interInfo.getLoginUsers().findMember(strFileReceiver);
			}
			
			if(targetUser == null)
			{
				System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(), "
						+"target("+strFileReceiver+") not found!");
				return false;
			}
			blockSCInfo = targetUser.getBlockSocketChannelInfo();
		}

		defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);
		if(defaultBlockSC == null)
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(), "
					+"blocking sc of target("+strFileReceiver+") is null!");
			//return false;
		}
		else if(defaultBlockSC.isOpen())
		{
			// close the default blocking socket channel if it is open
			// the channel is actually closed due to the interrupt exception of the sending thread

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
						+ "the default channel is still open and should be closed for reconnection!");
			}
			
			try {
				defaultBlockSC.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
					+ "the default channel is already closed!");
		}
		
		// remove the default blocking socket channel
		blockSCInfo.removeChannel(0);

		// send the cancel event to the receiver
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.CANCEL_FILE_SEND_CHAN);
		fe.setFileSender(interInfo.getMyself().getName());
		fe.setFileReceiver(strFileReceiver);
		
		bP2PFileTransfer = isP2PFileTransfer(fe);
		
		if(bP2PFileTransfer)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set event sender and receiver
			fe.setSender(interInfo.getMyself().getName());
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			fe.setReceiver(strDefServer);
			
			// set distribution fields
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileReceiver);
			
			// send the event to the default server
			bReturn = CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// set event sender and receiver
			fe.setSender(interInfo.getMyself().getName());
			fe.setReceiver(strFileReceiver);
			// send the event to the file receiver
			bReturn = CMEventManager.unicastEvent(fe, strFileReceiver);
		}
		
		if(!bReturn)
		{
			return false;
		}
		
		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT") && !bP2PFileTransfer)
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort());
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			
			if(defaultBlockSC == null)
			{
				System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); recreation of "
						+ "the blocking socket channel failed!: server("+serverInfo.getServerAddress()+"), port("
						+ serverInfo.getServerPort() +")");
				return false;
			}
			bReturn = blockSCInfo.addChannel(0, defaultBlockSC);

			if(bReturn)
			{
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				se.setChannelName(interInfo.getMyself().getName());
				se.setChannelNum(0);
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true);
				se = null;

				if(bReturn)
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
								+ "successfully requested to add the blocking socket channel with the key(0) "
								+ "to the server("+serverInfo.getServerName()+")");
					}
					
				}
			}
		}
		
		//////////////////////////////////

		return bReturn;
	}

	// srcFile: reference of RandomAccessFile of source file
	// bos: reference of BufferedOutputStream of split file
	public static void splitFile(RandomAccessFile srcFile, long lOffset, long lSplitSize, String strSplitFile)
	{
		long lRemainBytes = lSplitSize;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		int readBytes;
		BufferedOutputStream bos = null;
		try {
			bos = new BufferedOutputStream(new FileOutputStream(strSplitFile));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		try {
			// set file position
			srcFile.seek(lOffset);

			// read and write
			while( lRemainBytes > 0 )
			{
				if(lRemainBytes >= CMInfo.FILE_BLOCK_LEN)
					readBytes = srcFile.read(fileBlock);
				else
					readBytes = srcFile.read(fileBlock, 0, (int)lRemainBytes);

				if( readBytes >= CMInfo.FILE_BLOCK_LEN )
					bos.write(fileBlock);
				else
					bos.write(fileBlock, 0, readBytes);

				lRemainBytes -= readBytes;
			}
			
			bos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return;
	}
	
	public static long mergeFiles(String[] strSplitFiles, int nSplitNum, String strMergeFile)
	{
		long lMergeSize = -1;
		long lSrcSize = 0;
		FileInputStream srcfis = null;
		BufferedOutputStream bos = null;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		int readBytes = 0;

		if(nSplitNum != strSplitFiles.length)
		{
			System.err.println("CMFileTransferManager.mergeFiles(), the number of members in the "
					+"first parameter is different from the given second parameter!");
			return -1;
		}
		
		// open a target file
		try {
			
			bos = new BufferedOutputStream(new FileOutputStream(strMergeFile));
			
			for(int i = 0; i < nSplitNum; i++)
			{
				// open a source file
				File srcFile = new File(strSplitFiles[i]);
				srcfis = new FileInputStream(srcFile);

				// get source file size
				lSrcSize = srcFile.length();

				// concatenate a source file to a target file
				while( lSrcSize > 0 )
				{
					if( lSrcSize >= CMInfo.FILE_BLOCK_LEN )
					{
						readBytes = srcfis.read(fileBlock);
						bos.write(fileBlock, 0, readBytes);
					}
					else
					{
						readBytes = srcfis.read(fileBlock, 0, (int)lSrcSize);
						bos.write(fileBlock, 0, readBytes);
					}

					lSrcSize -= readBytes;
				}

				// close a source file
				srcfis.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(srcfis != null)
				{
					srcfis.close();
				}
				if(bos != null){
					bos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		File targetFile = new File(strMergeFile);
		lMergeSize = targetFile.length();
		
		return lMergeSize;
	}
	
	public static String getFileNameFromPath(String strPath)
	{
		String strName = null;
		int index;
		String sep = File.separator;

		index = strPath.lastIndexOf(sep);
		if(index == -1)
		{
			index = strPath.lastIndexOf("/");
			if(index == -1)
			{
				index = strPath.lastIndexOf("\\");
				if(index == -1)
					return null;
			}
		}
		strName = strPath.substring(index+1);
		/*
		if(index == -1)
			strName = strPath;
		else
			strName = strPath.substring(index+1);
		*/
		
		
		return strName;
	}
	
	//////////////////////////////////////////////////////////////////
	// process file event
	
	public static boolean processEvent(CMMessage msg)
	{
		boolean bForward = true;
		CMFileEvent fe = new CMFileEvent(msg.m_buf);
		
		switch(fe.getID())
		{
		case CMFileEvent.REQUEST_PERMIT_PULL_FILE:
			bForward = processREQUEST_PERMIT_PULL_FILE(fe);
			break;
		case CMFileEvent.REPLY_PERMIT_PULL_FILE:
			bForward = processREPLY_PERMIT_PULL_FILE(fe);
			break;
		case CMFileEvent.REQUEST_PERMIT_PUSH_FILE:
			bForward = processREQUEST_PERMIT_PUSH_FILE(fe);
			break;
		case CMFileEvent.REPLY_PERMIT_PUSH_FILE:
			bForward = processREPLY_PERMIT_PUSH_FILE(fe);
			break;
		case CMFileEvent.START_FILE_TRANSFER:
			bForward = processSTART_FILE_TRANSFER(fe);
			break;
		case CMFileEvent.START_FILE_TRANSFER_ACK:
			bForward = processSTART_FILE_TRANSFER_ACK(fe);
			break;
		case CMFileEvent.CONTINUE_FILE_TRANSFER:
			bForward = processCONTINUE_FILE_TRANSFER(fe);
			break;
		case CMFileEvent.END_FILE_TRANSFER:
			bForward = processEND_FILE_TRANSFER(fe);
			break;
		case CMFileEvent.END_FILE_TRANSFER_ACK:
			bForward = processEND_FILE_TRANSFER_ACK(fe);
			break;
		case CMFileEvent.REQUEST_DIST_FILE_PROC:
			bForward = processREQUEST_DIST_FILE_PROC(fe);
			break;
		case CMFileEvent.START_FILE_TRANSFER_CHAN:
			bForward = processSTART_FILE_TRANSFER_CHAN(fe);
			break;
		case CMFileEvent.START_FILE_TRANSFER_CHAN_ACK:
			bForward = processSTART_FILE_TRANSFER_CHAN_ACK(fe);
			break;
		case CMFileEvent.END_FILE_TRANSFER_CHAN:
			bForward = processEND_FILE_TRANSFER_CHAN(fe);
			break;
		case CMFileEvent.END_FILE_TRANSFER_CHAN_ACK:
			bForward = processEND_FILE_TRANSFER_CHAN_ACK(fe);
			break;
		case CMFileEvent.CANCEL_FILE_SEND:
			bForward = processCANCEL_FILE_SEND(fe);
			break;
		case CMFileEvent.CANCEL_FILE_SEND_ACK:
			bForward = processCANCEL_FILE_SEND_ACK(fe);
			break;
		case CMFileEvent.CANCEL_FILE_SEND_CHAN:
			bForward = processCANCEL_FILE_SEND_CHAN(fe);
			break;
		case CMFileEvent.CANCEL_FILE_SEND_CHAN_ACK:
			bForward = processCANCEL_FILE_SEND_CHAN_ACK(fe);
			break;
		case CMFileEvent.CANCEL_FILE_RECV_CHAN:
			bForward = processCANCEL_FILE_RECV_CHAN(fe);
			break;
		case CMFileEvent.CANCEL_FILE_RECV_CHAN_ACK:
			bForward = processCANCEL_FILE_RECV_CHAN_ACK(fe);
			break;
		case CMFileEvent.ERR_RECV_FILE_CHAN:
			processERR_RECV_FILE_CHAN(fe);
			break;
		case CMFileEvent.ERR_SEND_FILE_CHAN:
			processERR_SEND_FILE_CHAN(fe);
			break;
		default:
			System.err.println("CMFileTransferManager.processEvent(), unknown event id("+fe.getID()+").");
			fe = null;
			return false;
		}
		
		fe.setFileBlock(null);
		fe = null;
		return bForward;
	}
	
	private static boolean processREQUEST_PERMIT_PULL_FILE(CMFileEvent fe)
	{
		boolean bForward = true;
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_PERMIT_PULL_FILE(), "
					+ "file sender("+fe.getFileSender()+"), senderUuid("+fe.getFileSenderUuid()
					+ "), file receiver(requester)("+fe.getFileReceiver()
					+ "), receiverUuid("+fe.getFileReceiverUuid()
					+ "), file("+fe.getFileName()
					+ "), contentID("+fe.getContentID()+"), append flag("
					+ fe.getFileAppendFlag()+"), ssc port("+fe.getSSCPort()+").");
		}

		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileSender().contentEquals(strMyName) || !Objects.equals(fe.getFileSenderUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node (" + strMyName + ", " + myUuid + ") is not the file sender("
						+ fe.getFileSender() + ", " + fe.getFileSenderUuid() + ").");
			}
			return false;
		}

		fInfo.setStartRequestTime(System.currentTimeMillis());
		
		// get the full path of the requested file
		String strFullPath = confInfo.getTransferedFileHome().toString() + 
				File.separator + fe.getFileName(); 
		// check the file existence
		File file = new File(strFullPath);
		if(!file.exists())
		{
			replyPermitForPullFile(fe, -1);
			bForward = false;
			return bForward;
		}		

		if(confInfo.isPermitFileTransferRequest() || 
				fe.getFileName().contentEquals(CMInfo.THROUGHPUT_TEST_FILE))
		{
			replyPermitForPullFile(fe, 1);
			bForward = false;
		}
		
		return bForward;
	}
	
	private static boolean processREPLY_PERMIT_PULL_FILE(CMFileEvent fe)
	{
		CMEventInfo eInfo = CMEventInfo.getInstance();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMCommInfo commInfo = CMCommInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREPLY_PERMIT_PULL_FILE(), "
					+ "file sender("+fe.getFileSender()+"), file sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+fe.getFileReceiver()+"), file receiver uuid("+fe.getFileReceiverUuid()
					+"), file("+fe.getFileName()+"), return code("+fe.getReturnCode()
					+"), contentID("+fe.getContentID()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileReceiver().contentEquals(strMyName) || !Objects.equals(fe.getFileReceiverUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+"), uuid("+myUuid
						+") is not the file receiver("+fe.getFileReceiver()
						+"), uuid("+fe.getFileReceiverUuid()+").");
			}
			bForward = false;
			return bForward;
		}
				
		if(fe.getReturnCode() != 1)
		{
			// print error message
			if(fe.getReturnCode() == -1)
				System.err.println("The requested file does not exists!");
			else if(fe.getReturnCode() == 0)
				System.err.println("sender("+fe.getFileSender()+"), uuid("+fe.getFileSenderUuid()
						+") rejects to send the file!");

			// close the server socket channel for c2c file transfer
			if(confInfo.isFileTransferScheme() && isP2PFileTransfer(fe))
			{
				ServerSocketChannel ssc = commInfo.getNonBlockServerSocketChannel();
				if(ssc != null && ssc.isOpen())
				{
					try {
						ssc.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					commInfo.setNonBlockServerSocketChannel(null);
				}
			}
			
			// notify an waiting thread
			if(fe.getFileName().equals(CMInfo.THROUGHPUT_TEST_FILE))
			synchronized(eventSync)
			{
				eventSync.setReplyEvent(fe);
				eventSync.notify();
			}
		}
		
		return bForward;
	}
	
	private static boolean processREQUEST_PERMIT_PUSH_FILE(CMFileEvent fe)
	{
		boolean bForward = true;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		// [Added] Get my UUID for multi-login verification
		UUID myUuid = interInfo.getMyself().getUuid();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_PERMIT_PUSH_FILE(), ");
			System.out.println("file sender("+fe.getFileSender()+"), file sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+fe.getFileReceiver()+"), file receiver uuid("+fe.getFileReceiverUuid()
					+"), file("+fe.getFilePath()+"), size("+fe.getFileSize()+"), append mode("+fe.getFileAppendFlag()
					+"), contentID("+fe.getContentID()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileReceiver().equals(strMyName) || !Objects.equals(fe.getFileReceiverUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file receiver("
						+fe.getFileReceiver()+"), uuid("+fe.getFileReceiverUuid()+")!");
			}
			return false;
		}
		
		fInfo.setStartRequestTime(System.currentTimeMillis());
		
		// check PERMIT_FILE_TRANSFER field
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		boolean bPermit = confInfo.isPermitFileTransferRequest();
		String strFileName = getFileNameFromPath(fe.getFilePath());
		if(bPermit || strFileName.equals(CMInfo.THROUGHPUT_TEST_FILE))
		{
			replyPermitForPushFile(fe, 1);
			bForward = false;
		}
		
		return bForward;
	}
	
	private static boolean processREPLY_PERMIT_PUSH_FILE(CMFileEvent fe)
	{
		boolean bForward = true;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();  // added
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREPLY_PERMIT_PUSH_FILE(), ");
			System.out.println("file sender("+fe.getFileSender()+"), sender uuid("
					+fe.getFileSenderUuid()+"), file receiver("+fe.getFileReceiver()
					+"), receiver uuid("+fe.getFileReceiverUuid()+"), file("
					+fe.getFilePath()+"), size("+fe.getFileSize()+"), append mode("
					+fe.getFileAppendFlag()+"), contentID("+fe.getContentID()
					+"), return code("+fe.getReturnCode()+"), ssc port("
					+fe.getSSCPort()+").");
		}
		
		// check whether this CM node is the target node of this event or not
		// modified: added uuid comparison
		if(!fe.getFileSender().equals(strMyName) ||
				!Objects.equals(fe.getFileSenderUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added uuid info
				System.err.println("This node ("+strMyName+", "+myUuid
						+") is not the file sender("+fe.getFileSender()
						+", "+fe.getFileSenderUuid()+")!");
			}
			return false;
		}

		String strFileName = getFileNameFromPath(fe.getFilePath());
		if(strFileName.equals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;
				
		if(fe.getReturnCode() == 1)
		{
			if(confInfo.isFileTransferScheme() && isP2PFileTransfer(fe))
			{
				// set ssc port number of the file receiver to the receiver client info
				CMUser fileReceiver = CMInteractionManager.findGroupMemberOfClient(fe.getFileReceiver(),
						fe.getFileReceiverUuid());
				if(fileReceiver == null)
				{
					System.err.println("file receiver("+fe.getFileReceiver()+", "
							+fe.getFileReceiverUuid()+") not found in session("
							+interInfo.getMyself().getCurrentSession()+") and group("
							+interInfo.getMyself().getCurrentGroup()+")!");
					return false;
				}
				fileReceiver.setSSCPort(fe.getSSCPort());
			}
			
			// call pushFile()
			pushFile(fe.getFilePath(), fe.getFileReceiver(), fe.getFileReceiverUuid(), fe.getFileAppendFlag(),
					fe.getContentID());
		}
		
		return bForward;
	}
	
	private static boolean processSTART_FILE_TRANSFER(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bForward = true;
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER(),");
			System.out.println("file sender("+fe.getFileSender()+"), sender uuid("
					+fe.getFileSenderUuid()+"), file receiver("+fe.getFileReceiver()
					+"), receiver uuid("+fe.getFileReceiverUuid()+"), file("+fe.getFileName()
					+"), size("+fe.getFileSize()+"), contentID("+fe.getContentID()
					+"), appendFlag("+fe.getFileAppendFlag()+").");
		}
		
		// check whether this CM node is the target node of this event or not
		// modified: added uuid comparison
		if(!fe.getFileReceiver().equals(strMyName) ||
				!Objects.equals(fe.getFileReceiverUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added uuid info
				System.err.println("This node ("+strMyName+", "+myUuid
						+") is not the file receiver("+fe.getFileReceiver()
						+", "+fe.getFileReceiverUuid()+")!");
			}
			return false;
		}

		fInfo.setStartRecvTime(System.currentTimeMillis());

		if(fe.getFileName().equals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;
		
		// set file size
		long lFileSize = fe.getFileSize();
		
		// set a path of the received file
		String strFullPath = confInfo.getTransferedFileHome().toString();
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + File.separator + fe.getFileSender();
			File subDir = new File(strFullPath);
			if(!subDir.exists() || !subDir.isDirectory())
			{
				boolean ret = subDir.mkdirs();
				if(ret)
				{
					if(CMInfo._CM_DEBUG)
						System.out.println("A sub-directory is created.");
				}
				else
				{
					System.out.println("A sub-directory cannot be created!");
					return bForward;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return bForward;
		}

		// check the existing file
		// open a file output stream
		File file = new File(strFullPath);
		long lRecvSize = 0;
		RandomAccessFile writeFile;
		try {
			writeFile = new RandomAccessFile(strFullPath, "rw");

			if(file.exists())
			{
				if( (fe.getFileAppendFlag() == CMInfo.FILE_APPEND) || 
						((fe.getFileAppendFlag() == CMInfo.FILE_DEFAULT) && confInfo.isFileAppendScheme()) )
				{
					// init received file size
					lRecvSize = file.length();
				
					if(CMInfo._CM_DEBUG)
						System.out.println("The file ("+strFullPath+") exists with the size("+lRecvSize+" bytes).");
				
					// move the file pointer
					try {
						writeFile.seek(lRecvSize);
					} catch (IOException e) {
						e.printStackTrace();
						try {
							writeFile.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						return bForward;
					}
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return bForward;
		}

		// add the received file info in the push list
		fInfo.addRecvFileInfo(fe.getFileSender(), fe.getFileSenderUuid(), fe.getFileName(), lFileSize,
				fe.getContentID(), lRecvSize, writeFile);
		
		// send ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.START_FILE_TRANSFER_ACK);
		feAck.setFileSender(fe.getFileSender());
		feAck.setFileSenderUuid(fe.getFileSenderUuid());
		feAck.setFileReceiver(fe.getFileReceiver());
		feAck.setFileReceiverUuid(fe.getFileReceiverUuid());
		feAck.setFileName(fe.getFileName());
		feAck.setContentID(fe.getContentID());
		feAck.setReceivedFileSize(lRecvSize);
		
		if(isP2PFileTransfer(feAck))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set distribution fields
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(fe.getFileSender());
			feAck.setDistributionUuid(fe.getFileSenderUuid());
			
			// send the event to the default server
			CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file sender
			CMEventManager.unicastEvent(feAck, fe.getFileSender(), fe.getFileSenderUuid());
		}

		return bForward;
	}
	
	private static boolean processSTART_FILE_TRANSFER_ACK(CMFileEvent recvFileEvent)
	{
		String strFileReceiver = null;
		String strFileName = null;
		String strFullFileName = null;
		long lFileSize = -1;
		int nContentID = -1;
		String strFileSender = null;
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMSendFileInfo sInfo = null;
		long lRecvSize = 0;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		String strDefServer = null;
		boolean bForward = true;
		UUID fileReceiverUuid = recvFileEvent.getFileReceiverUuid();
		UUID fileSenderUuid = recvFileEvent.getFileSenderUuid();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "file sender("+recvFileEvent.getFileSender()+"), "
					+ "file sender uuid("+fileSenderUuid+"), "
					+ "file receiver("+recvFileEvent.getFileReceiver()+"), "
					+ "file receiver uuid("+fileReceiverUuid+"), "
					+ "file name("+recvFileEvent.getFileName()+"), "
					+ "content ID("+recvFileEvent.getContentID()+"), "
					+ "received sized("+recvFileEvent.getReceivedFileSize()+")");
		}
		
		// check whether this CM node is the target node of this event or not
		// modified: added uuid comparison
		if(!recvFileEvent.getFileSender().equals(strMyName) ||
				!Objects.equals(recvFileEvent.getFileSenderUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added sender uuid
				System.err.println("This node ("+strMyName+", "+myUuid
						+") is not the file sender("+recvFileEvent.getFileSender()
						+", "+fileSenderUuid+").");
			}
			return false;
		}

		if(recvFileEvent.getFileName().equals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;
		
		// find the CMSendFileInfo object 
		sInfo = fInfo.findSendFileInfo(recvFileEvent.getFileReceiver(), recvFileEvent.getFileReceiverUuid(),
				recvFileEvent.getFileName(), recvFileEvent.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), sendFileInfo not found! : "
					+"file receiver("+recvFileEvent.getFileReceiver()+"), file receiver uuid("
					+recvFileEvent.getFileReceiverUuid()+"), file("+recvFileEvent.getFileName()
					+"), content ID("+recvFileEvent.getContentID()+")");
			return bForward;
		}
		
		strFileReceiver = sInfo.getFileReceiver();
		strFullFileName = sInfo.getFilePath();
		strFileName = getFileNameFromPath(strFullFileName);
		lFileSize = sInfo.getFileSize();
		nContentID = sInfo.getContentID();
					
		lRecvSize = recvFileEvent.getReceivedFileSize();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "Sending file("+strFileName+") to target("+strFileReceiver+"), uuid("+fileReceiverUuid
					+ ") from the file position("+ lRecvSize +").");

		// open the file
		RandomAccessFile readFile = null;
		try {
			readFile = new RandomAccessFile(strFullFileName, "rw");
			if(lRecvSize > 0 && lRecvSize < lFileSize)	// If the receiver uses the append scheme,
			{
				try {
					readFile.seek(lRecvSize);
				} catch (IOException e) {
					e.printStackTrace();
					try {
						readFile.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					return bForward;
				}
			}
			sInfo.setReadFile(readFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return bForward;
		}
		
		// set sender name
		strFileSender = recvFileEvent.getFileSender();
		
		// send blocks
		//long lRemainBytes = lFileSize;
		long lRemainBytes = lFileSize - lRecvSize;
		int nReadBytes = 0;
		byte[] fileBlock = new byte[CMInfo.FILE_BLOCK_LEN];
		CMFileEvent fe = new CMFileEvent();
		
		while(lRemainBytes > 0 && !fInfo.isCancelSend())
		{
			try {
				nReadBytes = sInfo.getReadFile().read(fileBlock);
			} catch (IOException e) {
				e.printStackTrace();
				continue;
			}
			
			// send file block
			fe = new CMFileEvent();
			fe.setID(CMFileEvent.CONTINUE_FILE_TRANSFER);
			fe.setFileSender(strFileSender);
			fe.setFileSenderUuid(fileSenderUuid);
			fe.setFileReceiver(strFileReceiver);
			fe.setFileReceiverUuid(fileReceiverUuid);
			fe.setFileName(strFileName);
			fe.setFileBlock(fileBlock);
			fe.setBlockSize(nReadBytes);
			fe.setContentID(nContentID);
			
			if(isP2PFileTransfer(fe))
			{
				// set distribution fields
				strDefServer = interInfo.getDefaultServerInfo().getServerName();
				fe.setDistributionSession("CM_ONE_USER");
				fe.setDistributionGroup(strFileReceiver);
				fe.setDistributionUuid(fileReceiverUuid);
				
				// send the event to the default server
				CMEventManager.unicastEvent(fe, strDefServer);
			}
			else
			{
				// send the event to the file receiver
				CMEventManager.unicastEvent(fe, strFileReceiver, fileReceiverUuid);
			}
			
			lRemainBytes -= nReadBytes;
		}
		
		if(lRemainBytes < 0)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER(); "
					+ "the receiver("+strFileReceiver+"), uuid("+fileReceiverUuid+") already has "
					+ "a bigger size file("+strFileName+"); sender size("+lFileSize
					+ "), receiver size("+lRecvSize+").");
		}
		
		// close fis
		try {
			sInfo.getReadFile().close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// reset the flag
		if(fInfo.isCancelSend())
		{
			fInfo.setCancelSend(false);
			fileBlock = null;
			fe = null;
			return bForward;
		}

		if(CMInfo._CM_DEBUG) {
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "Ending transfer of file("+strFileName+") to target("+strFileReceiver
					+", "+fileReceiverUuid+"), size("+lFileSize+") Bytes.");
		}

		// send the end of file transfer
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.END_FILE_TRANSFER);
		fe.setFileSender(strFileSender);
		fe.setFileSenderUuid(fileSenderUuid);
		fe.setFileReceiver(strFileReceiver);
		fe.setFileReceiverUuid(fileReceiverUuid);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		
		if(isP2PFileTransfer(fe))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set distribution fields
			strDefServer = interInfo.getDefaultServerInfo().getServerName();
			fe.setDistributionSession("CM_ONE_USER");
			fe.setDistributionGroup(strFileReceiver);
			fe.setDistributionUuid(fileReceiverUuid);
			
			// send the event to the default server
			CMEventManager.unicastEvent(fe, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file receiver
			CMEventManager.unicastEvent(fe, strFileReceiver, fileReceiverUuid);
		}
		
		return bForward;
	}
	
	private static boolean processCONTINUE_FILE_TRANSFER(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;
		
		// check whether this CM node is the target node of this event or not
		// modified: added uuid comparison
		if(!fe.getFileReceiver().equals(strMyName) || !Objects.equals(fe.getFileReceiverUuid(), myUuid))
		{
			/*
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file receiver("
						+fe.getFileReceiver()+").");
			}
			*/
			return false;
		}
		
		if(fe.getFileName().equals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;

		// find info in the recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getFileSender(), fe.getFileSenderUuid(),
				fe.getFileName(), fe.getContentID());
		if( recvInfo == null )
		{
			System.err.println("CMFileTransferManager.processCONTINUE_FILE_TRANSFER(), "
					+ "recv file info for sender("+fe.getFileSender()+"), uuid("+fe.getFileSenderUuid()
					+"), file("+fe.getFileName()+"), content ID("+fe.getContentID()+") not found.");
			return bForward;
		}

		try {
			recvInfo.getWriteFile().write(fe.getFileBlock(), 0, fe.getBlockSize());
		} catch (IOException e) {
			e.printStackTrace();
			return bForward;
		}
		recvInfo.setRecvSize(recvInfo.getRecvSize()+fe.getBlockSize());

		/*
		if(CMInfo._CM_DEBUG)
			System.out.println("Cumulative written file size: "+pushInfo.m_lRecvSize+" Bytes.");
		*/
		
		return bForward;
	}
	
	private static boolean processEND_FILE_TRANSFER(CMFileEvent fe)
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER(), "
					+ "file sender("+fe.getFileSender()+"), sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+fe.getFileReceiver()+"), receiver uuid("+fe.getFileReceiverUuid()
					+"), file("+fe.getFileName()+"), file size("+fe.getFileSize()
					+"), contentID("+fe.getContentID()+")");
		}

		// check whether this CM node is the target node of this event or not
		// modified: added uuid comparison
		if(!fe.getFileReceiver().equals(strMyName) || !Objects.equals(fe.getFileReceiverUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added file receiver uuid
				System.err.println("This node ("+strMyName+", "+myUuid+") is not the file receiver("
						+fe.getFileReceiver()+", "+fe.getFileReceiverUuid()+").");
			}
			return false;
		}

		if(fe.getFileName().equals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;
		
		fInfo.setEndRecvTime(System.currentTimeMillis());
		long lElapsedTime = fInfo.getEndRecvTime() - fInfo.getStartRecvTime();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("Elapsed receiving time ("+lElapsedTime+" ms).");
		}

		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getFileSender(), fe.getFileSenderUuid(),
				fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER(), recv file info "
					+"for sender("+fe.getFileSender()+"), uuid("+fe.getFileSenderUuid()+"), file("
					+fe.getFileName()+"), content ID("+fe.getContentID()+") not found.");

			return bForward;
		}
		// close received file descriptor
		try {
			recvInfo.getWriteFile().close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("received size("+recvInfo.getRecvSize()+").");
		}

		// remove info from push file list
		fInfo.removeRecvFileInfo(fe.getFileSender(), fe.getFileSenderUuid(), fe.getFileName(), fe.getContentID());
		
		// send ack
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_ACK);
		feAck.setFileSender(fe.getFileSender());
		feAck.setFileSenderUuid(fe.getFileSenderUuid());
		feAck.setFileReceiver(fe.getFileReceiver());
		feAck.setFileReceiverUuid(fe.getFileReceiverUuid());
		feAck.setFileName(fe.getFileName());
		feAck.setFileSize(fe.getFileSize());
		feAck.setReturnCode(1);	// success
		feAck.setContentID(fe.getContentID());
		
		if(isP2PFileTransfer(feAck))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set distribution fields
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(fe.getFileSender());
			feAck.setDistributionUuid(fe.getFileSenderUuid());
			
			// send the even to the default server
			CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file sender
			CMEventManager.unicastEvent(feAck, fe.getFileSender(), fe.getFileSenderUuid());
		}

		CMSNSManager.checkCompleteRecvAttachedFiles(fe);

		// check if the transfer is for sync a new file
		CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
		syncManager.checkNewTransferForSync(fe);
		// check if the transfer is for the file-sync local mode
		syncManager.checkTransferForLocalMode(fe);

		return bForward;
	}
	
	private static boolean processEND_FILE_TRANSFER_ACK(CMFileEvent fe)
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		String strFileReceiver = fe.getFileReceiver();
		UUID fileReceiverUuid = fe.getFileReceiverUuid();
		String strFileName = fe.getFileName();
		long lFileSize = fe.getFileSize();
		int nContentID = fe.getContentID();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), "
					+ "file sender("+fe.getFileSender()+"), sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+strFileReceiver+"), receiver uuid("+fileReceiverUuid
					+"), file("+strFileName+"), size("+lFileSize+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		// check whether this CM node is the target node of this event or not
		// modified: added uuid comparison
		if(!fe.getFileSender().equals(strMyName) || !Objects.equals(fe.getFileSenderUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added file sender uuid
				System.err.println("This node ("+strMyName+", "+myUuid+") is not the file sender("
						+fe.getFileSender()+", "+fe.getFileSenderUuid()+").");
			}
			return false;
		}

		if(fe.getFileName().equals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;

		fInfo.setEndSendTime(System.currentTimeMillis());
		long lElapsedTime = fInfo.getEndSendTime() - fInfo.getStartSendTime();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("Elapsed sending time("+lElapsedTime+" ms).");			
		}

		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strFileReceiver, fileReceiverUuid, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), send info not found");
			System.err.println("file receiver("+strFileReceiver+"), file receiver uuid("+fileReceiverUuid
					+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendFileInfo(strFileReceiver, fileReceiverUuid, strFileName, nContentID);
		}
			
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		CMSNSManager.checkCompleteSendAttachedFiles(fe);

		return bForward;
	}
	
	private static boolean processREQUEST_DIST_FILE_PROC(CMFileEvent fe)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_DIST_FILE_PROC(), "
					+ "file sender(requester)("+fe.getFileSender()
					+ "), file receiver("+fe.getFileReceiver()
					+ "), content ID("+fe.getContentID()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileReceiver().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file receiver("
						+fe.getFileReceiver()+").");
			}
			return false;
		}
		
		return bForward;
	}
			
	private static boolean processSTART_FILE_TRANSFER_CHAN(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN(),");
			System.out.println("file sender("+fe.getFileSender()+"), sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+fe.getFileReceiver()+"), receiver uuid("+fe.getFileReceiverUuid()
					+"), file name("+fe.getFileName()+"), size("
					+fe.getFileSize()+"), contentID("+fe.getContentID()+"), appendFlag("
					+fe.getFileAppendFlag()+").");
		}

		// check whether this CM node is the target node of this event or not
		if( !fe.getFileReceiver().equals(strMyName) || !Objects.equals(fe.getFileReceiverUuid(), myUuid) )
		{
			if(CMInfo._CM_DEBUG)
			{
				// changed: added uuid info to error message
				System.err.println("This node ("+strMyName+", "+myUuid+") is not the file receiver("
						+fe.getFileReceiver()+", "+fe.getFileReceiverUuid()+").");
			}
			return false;
		}

		fInfo.setStartRecvTime(System.currentTimeMillis());

		if(fe.getFileName().contentEquals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;

		// set file size
		long lFileSize = fe.getFileSize();
		
		// set a path of the received file
		String strFullPath = confInfo.getTransferedFileHome().toString();
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else if(confInfo.getSystemType().equals("SERVER"))
		{
			// check the sub-directory and create it if it does not exist
			strFullPath = strFullPath + File.separator + fe.getFileSender();
			File subDir = new File(strFullPath);
			if(!subDir.exists() || !subDir.isDirectory())
			{
				boolean ret = subDir.mkdirs();
				if(ret)
				{
					if(CMInfo._CM_DEBUG)
						System.out.println("A sub-directory is created.");
				}
				else
				{
					System.err.println("A sub-directory cannot be created!");
					return bForward;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return bForward;
		}		
		
		// get the default blocking TCP socket channel
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))	// CLIENT
		{
			CMServer serverInfo = CMInteractionManager.findServer(fe.getFileSender());
			if(serverInfo != null)
			{
				// socket channel to the file receiver (server)
				sc = (SocketChannel) serverInfo.getBlockSocketChannelInfo().findChannel(0);
				// default non-blocking socket channel to the file receiver (server)
				dsc = (SocketChannel) serverInfo.getNonBlockSocketChannelInfo().findChannel(0); 
			}
			else
			{
				CMUser targetUser = CMInteractionManager.findGroupMemberOfClient(fe.getFileSender(),
						fe.getFileSenderUuid());
				if(targetUser == null)
				{
					System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN(), file sender("
							+fe.getFileSender()+"), uuid("+fe.getFileSenderUuid()+") not found!");
					return bForward;
				}
				// socket channel to the file receiver (client)
				sc = (SocketChannel) targetUser.getBlockSocketChannelInfo().findChannel(0);
				// default non-blocking socket channel to the default server
				dsc = (SocketChannel) interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo()
						.findChannel(0);
			}
			
		}
		else	// SERVER
		{
			CMUser user = CMInteractionInfo.getInstance().getLoginUsers().findMember(fe.getFileSender(),
					fe.getFileSenderUuid());
			sc = (SocketChannel) user.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) user.getNonBlockSocketChannelInfo().findChannel(0);
		}
		
		if(sc == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default blocking TCP socket channel not found!");
			return bForward;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default blocking TCP socket channel is closed!");
			return bForward;
		}
		
		if(dsc == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default TCP socket channel not found!");
			return bForward;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default TCP socket channel is closed!");
			return bForward;
		}

		// check the existing file
		File file = new File(strFullPath);
		long lRecvSize = 0;
		if(file.exists())
		{
			if( (fe.getFileAppendFlag() == CMInfo.FILE_APPEND) || 
					((fe.getFileAppendFlag() == CMInfo.FILE_DEFAULT) && confInfo.isFileAppendScheme()) )
			{
				// init received file size
				lRecvSize = file.length();
			}
		}

		// add the received file info
		boolean bResult = false;
		CMRecvFileInfo rfInfo = new CMRecvFileInfo();
		rfInfo.setFileSender(fe.getFileSender());
		rfInfo.setFileSenderUuid(fe.getFileSenderUuid());
		rfInfo.setFileReceiver(fe.getFileReceiver());
		rfInfo.setFileReceiverUuid(fe.getFileReceiverUuid());
		rfInfo.setFileName(fe.getFileName());
		rfInfo.setFilePath(strFullPath);
		rfInfo.setFileSize(lFileSize);
		rfInfo.setContentID(fe.getContentID());
		rfInfo.setRecvSize(lRecvSize);
		//rfInfo.setWriteFile(raf);
		rfInfo.setRecvChannel(sc);
		rfInfo.setDefaultChannel(dsc);
		
		bResult = fInfo.addRecvFileInfo(rfInfo);
		if(!bResult)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN(); failed to add "
					+ "the receiving file info!");
			return bForward;
		}
		
		if(!fInfo.isRecvOngoing(fe.getFileSender(), fe.getFileSenderUuid()))
		{
			sendSTART_FILE_TRANSFER_CHAN_ACK(rfInfo);
		}
				
		return bForward;
	}
	
	private static boolean processSTART_FILE_TRANSFER_CHAN_ACK(CMFileEvent fe)
	{
		long lRecvSize = -1;	// received size by the receiver
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMThreadInfo threadInfo = CMThreadInfo.getInstance();
		CMSendFileInfo sInfo = null;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN_ACK(); "
					+ "file sender("+fe.getFileSender()+"), sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+fe.getFileReceiver()+"), receiver uuid("+fe.getFileReceiverUuid()
					+"), file name("+fe.getFileName()
					+ "), file size("+fe.getFileSize()+"), content ID("
					+fe.getContentID()+"), received file size("+fe.getReceivedFileSize()
					+").");
		}
		
		// check whether this CM node is the target node of this event or not
		// changed: check both name and uuid
		if(!fe.getFileSender().equals(strMyName) || !Objects.equals(fe.getFileSenderUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// changed: added uuid info to error message
				System.err.println("This node ("+strMyName+", "+myUuid+") is not the file sender("
						+fe.getFileSender()+", "+fe.getFileSenderUuid()+").");
			}
			return false;
		}

		if(fe.getFileName().contentEquals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;

		// find the CMSendFileInfo object 
		sInfo = fInfo.findSendFileInfo(fe.getFileReceiver(), fe.getFileReceiverUuid(),
				fe.getFileName(), fe.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN_ACK(), sendFileInfo "
					+ "not found! : file receiver("+fe.getFileReceiver()+"), file receiver uuid("
					+fe.getFileReceiverUuid()+"), file("+fe.getFileName()+"), content ID("+fe.getContentID()+")");
			return bForward;
		}
				
		lRecvSize = fe.getReceivedFileSize();
		if(lRecvSize > 0)
		{
			sInfo.setSentSize(lRecvSize);	// update the sent size
			//sInfo.setAppend(true);			// set the file append scheme
		}
					
		// start a dedicated sending thread
		Future<CMSendFileInfo> future = null;
		CMSendFileTask sendFileTask = new CMSendFileTask(sInfo);
		future = threadInfo.getExecutorService().submit(sendFileTask, sInfo);
		sInfo.setSendTaskResult(future);		

		return bForward;		
	}
	
	private static boolean processEND_FILE_TRANSFER_CHAN(CMFileEvent fe)
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bResult = false;
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), "
					+ "file sender("+fe.getFileSender()+"), file sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+fe.getFileReceiver()+"), file receiver uuid("+fe.getFileReceiverUuid()
					+"), file("+fe.getFileName()+"), file size("+fe.getFileSize()+"), contentID("
					+fe.getContentID()+")");
		}

		// check whether this CM node is the target node of this event or not
		// modified: added uuid check
		if(!fe.getFileReceiver().equals(strMyName) || !Objects.equals(fe.getFileReceiverUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added my uuid, file receiver uuid
				System.err.println("This node ("+strMyName+", "+myUuid+") is not the file receiver("
						+fe.getFileReceiver()+", "+fe.getFileReceiverUuid()+").");
			}
			return false;
		}

		if(fe.getFileName().contentEquals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;

		fInfo.setEndRecvTime(System.currentTimeMillis());
		long lElapsedTime = fInfo.getEndRecvTime() - fInfo.getStartRecvTime();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("Elapsed receiving time ("+lElapsedTime+" ms).");
		}

		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getFileSender(), fe.getFileSenderUuid(),
				fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), recv file info "
					+"for sender("+fe.getFileSender()+"), uuid("+fe.getFileSenderUuid()+"), file("
					+fe.getFileName()+"), content ID("+fe.getContentID()+") not found.");

			return bForward;
		}

		// wait the receiving thread
		if(!recvInfo.getRecvTaskResult().isDone())
		{
			try {
				recvInfo.getRecvTaskResult().get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		
		// make ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_CHAN_ACK);
		feAck.setFileSender(fe.getFileSender());
		feAck.setFileSenderUuid(fe.getFileSenderUuid());
		feAck.setFileReceiver(fe.getFileReceiver());
		feAck.setFileReceiverUuid(fe.getFileReceiverUuid());
		feAck.setFileName(fe.getFileName());
		feAck.setFileSize(fe.getFileSize());
		feAck.setContentID(fe.getContentID());

		// check out whether the file is completely received
		if(recvInfo.getFileSize() <= recvInfo.getRecvSize())
		{
			feAck.setReturnCode(1);	// success
			bResult = true;
		}
		else
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(); incompletely received!");
			feAck.setReturnCode(0); // failure
			bResult = false;
		}
		
		// remove info from push file list
		fInfo.removeRecvFileInfo(fe.getFileSender(), fe.getFileSenderUuid(), fe.getFileName(), fe.getContentID());
		
		// send ack
		if(isP2PFileTransfer(feAck))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), "
						+ "isP2PFileTransfer() returns true.");
			}

			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(fe.getFileSender());

			// send the event to the default server
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// send the event to the file sender
			CMEventManager.unicastEvent(feAck, fe.getFileSender(), fe.getFileSenderUuid());
		}

		if(bResult)
			CMSNSManager.checkCompleteRecvAttachedFiles(fe);

		// check if the transfer is for sync a new file
		CMFileSyncManager syncManager = cmInfo.getServiceManager(CMFileSyncManager.class);
		syncManager.checkNewTransferForSync(fe);
		// check if the transfer is for the file-sync local mode
		syncManager.checkTransferForLocalMode(fe);

		// check whether there is a remaining receiving file info or not
		CMRecvFileInfo nextRecvInfo = fInfo.findRecvFileInfoNotStarted(fe.getFileSender(), fe.getFileSenderUuid());
		if(nextRecvInfo != null)
		{
			sendSTART_FILE_TRANSFER_CHAN_ACK(nextRecvInfo);
		}

		return bForward;
	}
	
	private static boolean processEND_FILE_TRANSFER_CHAN_ACK(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		UUID myUuid = interInfo.getMyself().getUuid();
		boolean bForward = true;
		String strFileReceiver = fe.getFileReceiver();
		UUID fileReceiverUuid = fe.getFileReceiverUuid();
		String strFileName = fe.getFileName();
		long lFileSize = fe.getFileSize();
		int nContentID = fe.getContentID();

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN_ACK(), "
					+ "file sender("+fe.getFileSender()+"), file sender uuid("+fe.getFileSenderUuid()
					+"), file receiver("+strFileReceiver+"), file receiver uuid("+fileReceiverUuid
					+"), file("+strFileName+"), size("+lFileSize+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		// check whether this CM node is the target node of this event or not
		// modified: added uuid check
		if(!fe.getFileSender().equals(strMyName) || !Objects.equals(fe.getFileSenderUuid(), myUuid))
		{
			if(CMInfo._CM_DEBUG)
			{
				// modified: added my uuid, file sender uuid
				System.err.println("This node ("+strMyName+", "+myUuid+") is not the file sender("
						+fe.getFileSender()+", "+fe.getFileSenderUuid()+").");
			}
			return false;
		}

		if(fe.getFileName().contentEquals(CMInfo.THROUGHPUT_TEST_FILE))
			bForward = false;

		fInfo.setEndSendTime(System.currentTimeMillis());
		long lElapsedTime = fInfo.getEndSendTime() - fInfo.getStartSendTime();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("Elapsed sending time("+lElapsedTime+" ms).");			
		}

		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strFileReceiver, fileReceiverUuid, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN_ACK(), send info not found");
			System.err.println("file receiver("+strFileReceiver+"), file receiver uuid("+fileReceiverUuid
					+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendFileInfo(strFileReceiver, fileReceiverUuid, strFileName, nContentID);
		}
	
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		CMSNSManager.checkCompleteSendAttachedFiles(fe);

		return bForward;	
	}
	
	private static void sendSTART_FILE_TRANSFER_CHAN_ACK(CMRecvFileInfo rfInfo)
	{
		CMInfo cmInfo = CMInfo.getInstance();
		CMThreadInfo threadInfo = CMThreadInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

		// start a dedicated thread to receive the file
		Future<CMRecvFileInfo> future = null;
		CMRecvFileTask recvFileTask = new CMRecvFileTask(rfInfo);
		future = threadInfo.getExecutorService().submit(recvFileTask, rfInfo);
		rfInfo.setRecvTaskResult(future);
		
		// send ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.START_FILE_TRANSFER_CHAN_ACK);
		feAck.setFileSender(rfInfo.getFileSender());
		feAck.setFileReceiver(CMInteractionInfo.getInstance().getMyself().getName());
		feAck.setFileName(rfInfo.getFileName());
		feAck.setContentID(rfInfo.getContentID());
		feAck.setReceivedFileSize(rfInfo.getRecvSize());
		
		if(isP2PFileTransfer(feAck))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.sendSTART_FILE_TRANSFER_CHAN_ACK(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set event sender and receiver
			feAck.setSender(interInfo.getMyself().getName());
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			feAck.setReceiver(strDefServer);
			
			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(rfInfo.getFileSender());
			
			// send the event to the default server
			CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.sendSTART_FILE_TRANSFER_CHAN_ACK(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// set event sender and receiver
			feAck.setSender(interInfo.getMyself().getName());
			feAck.setReceiver(rfInfo.getFileSender());
			// send the event to the file sender
			CMEventManager.unicastEvent(feAck, rfInfo.getFileSender());
		}

		feAck = null;
	}
	
	private static boolean processCANCEL_FILE_SEND(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		boolean bForward = true;
		String strFileSender = fe.getFileSender();
		UUID fileSenderUuid = fe.getFileSenderUuid();
		CMList<CMRecvFileInfo> recvList = fInfo.getRecvFileList(strFileSender, fileSenderUuid);
		Iterator<CMRecvFileInfo> iter = null;
		CMRecvFileInfo rInfo = null;
		CMFileEvent feAck = new CMFileEvent();
		boolean bReturn = false;
		boolean bP2PFileTransfer = false;
		
		String strMyName = interInfo.getMyself().getName();
		String strDefServer = null;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(), "
					+"file sender("+fe.getFileSender()+"), file receiver("
					+fe.getFileReceiver()+").");
		}

		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileReceiver().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file receiver("
						+fe.getFileReceiver()+").");
			}
			return false;
		}
		
		// make the ack event
		feAck.setID(CMFileEvent.CANCEL_FILE_SEND_ACK);
		feAck.setFileSender(strFileSender);
		feAck.setFileReceiver(fe.getFileReceiver());
		
		bP2PFileTransfer = isP2PFileTransfer(feAck);
		
		// recv file info list not found
		if(recvList == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND(); recv info list not found "
					+ "for sender("+strFileSender+")!");
			feAck.setReturnCode(0);
			
			if(bP2PFileTransfer)
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(), "
							+ "isP2PFileTransfer() returns true.");
				}
				// set event sender and receiver
				feAck.setSender(strMyName);
				strDefServer = interInfo.getDefaultServerInfo().getServerName();
				feAck.setReceiver(strDefServer);
				
				// set distribution fields
				feAck.setDistributionSession("CM_ONE_USER");
				feAck.setDistributionGroup(strFileSender);
				
				// send the event to the default server
				CMEventManager.unicastEvent(feAck, strDefServer);
			}
			else
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(), "
							+ "isP2PFileTransfer() returns false.");
				}
				// set event sender and receiver
				feAck.setSender(strMyName);
				feAck.setReceiver(strFileSender);
				// send the event to the file sender
				CMEventManager.unicastEvent(feAck, strFileSender);
			}
			return bForward;
		}
		
		// close RandomAccessFile and remove the recv file info list
		iter = recvList.getList().iterator();
		while(iter.hasNext())
		{
			rInfo = iter.next();
			if(rInfo.getWriteFile() != null)
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(); cancelled file("
							+rInfo.getFileName()+"), file size("+rInfo.getFileSize()+"), recv size("
							+rInfo.getRecvSize()+").");
				}
				
				try {
					rInfo.getWriteFile().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		bReturn = fInfo.removeRecvFileList(strFileSender, fileSenderUuid);
		
		if(bReturn)
			feAck.setReturnCode(1);
		else
			feAck.setReturnCode(0);
		
		if(bP2PFileTransfer)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set event sender and receiver
			feAck.setSender(strMyName);
			strDefServer = interInfo.getDefaultServerInfo().getServerName();
			feAck.setReceiver(strDefServer);

			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(strFileSender);
			
			// send the event to the default server
			bReturn = CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// set event sender and receiver
			feAck.setSender(strMyName);
			feAck.setReceiver(strFileSender);
			// send the event to the file sender
			bReturn = CMEventManager.unicastEvent(feAck, strFileSender);
		}
		
		if(bReturn)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(); succeeded. sender("
					+fe.getFileSender()+"), receiver("+fe.getFileReceiver()+").");
		}
		else
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND(); failed! sender("
					+fe.getFileSender()+"), receiver("+fe.getFileReceiver()+").");
		}
		
		return bForward;
	}
	
	private static boolean processCANCEL_FILE_SEND_ACK(CMFileEvent fe)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_ACK(), "
					+ "file sender("+fe.getFileSender()+"), file receiver("
					+fe.getFileReceiver()+"), return code("+fe.getReturnCode()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileSender().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file sender("
						+fe.getFileSender()+").");
			}
			return false;
		}
		
		return bForward;
	}
	
	private static boolean processCANCEL_FILE_SEND_CHAN(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMList<CMRecvFileInfo> recvList = null;
		CMRecvFileInfo rInfo = null;
		Future<CMRecvFileInfo> recvTask = null;
		CMFileEvent feAck = null;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		boolean bReturn = false;
		boolean bP2PFileTransfer = false;
		
		String strFileSender = fe.getFileSender();
		UUID fileSenderUuid = fe.getFileSenderUuid();
		boolean bException = false;
		int nReturnCode = -1;
		String strMyName = interInfo.getMyself().getName();
		boolean bForward = true;

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), "
					+"file sender("+fe.getFileSender()+"), file receiver("
					+fe.getFileReceiver()+").");
		}

		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileReceiver().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file receiver("
						+fe.getFileReceiver()+").");
			}
			return false;
		}

		// find the CMRecvFile list of the strSender
		recvList = fInfo.getRecvFileList(strFileSender, fileSenderUuid);
		if(recvList == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); Receiving file list "
					+ "not found for the sender("+strFileSender+")!");
			//return bForward;
		}
		else
		{
			// find the current receiving file task
			rInfo = fInfo.findRecvFileInfoOngoing(strFileSender, fileSenderUuid);
			if(rInfo == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); ongoing receiving task "
						+ "not found for the sender("+strFileSender+"), uuid("+fileSenderUuid+")!");
				fInfo.removeRecvFileList(strFileSender, fileSenderUuid);
				//return bForward;
			}
			else
			{
				// request for canceling the receiving task
				recvTask = rInfo.getRecvTaskResult();
				recvTask.cancel(true);
				// wait for the thread cancellation
				try {
					recvTask.get(10L, TimeUnit.SECONDS);
				} catch(CancellationException e) {
					System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); the receiving task cancelled.: "
							+ "sender("+strFileSender+"), file("+rInfo.getFileName()+"), file size("+rInfo.getFileSize()
							+ "), recv size("+rInfo.getRecvSize()+")");
				} catch (InterruptedException e) {
					e.printStackTrace();
					bException = true;
				} catch (ExecutionException e) {
					e.printStackTrace();
					bException = true;
				} catch (TimeoutException e) {
					e.printStackTrace();
					bException = true;
				} finally {
					if(bException)
						nReturnCode = 0;
					else
						nReturnCode = 1;
				}

			}
			
			// remove the receiving file list of the sender
			fInfo.removeRecvFileList(strFileSender, fileSenderUuid);
		}
		
		
		// send the cancel ack event to the sender
		feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.CANCEL_FILE_SEND_CHAN_ACK);
		feAck.setFileSender(strFileSender);
		feAck.setFileReceiver(fe.getFileReceiver());
		feAck.setReturnCode(nReturnCode);
		
		bP2PFileTransfer = isP2PFileTransfer(feAck);
		
		if(bP2PFileTransfer)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set event sender and receiver
			feAck.setSender(interInfo.getMyself().getName());
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			feAck.setReceiver(strDefServer);
			
			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(strFileSender);
			
			// send the event to the default server
			CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// set event sender and receiver
			feAck.setSender(interInfo.getMyself().getName());
			feAck.setReceiver(strFileSender);
			// send the event to the file sender
			CMEventManager.unicastEvent(feAck, strFileSender);
		}

		//////////////////// the management of the closed default blocking socket channel
		// get the default blocking socket channel
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			if(bP2PFileTransfer)
			{
				// get the file sender (client)
				CMUser targetUser = CMInteractionManager.findGroupMemberOfClient(
						strFileSender);
				if(targetUser == null)
				{
					System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN()"
							+"client file sender("+strFileSender+") not found!");
					//return bForward;
				}
				else
				{
					blockSCInfo = targetUser.getBlockSocketChannelInfo();					
				}
				
				// close and initialize the server socket channel
				CMCommInfo commInfo = CMCommInfo.getInstance();
				ServerSocketChannel ssc = commInfo.getNonBlockServerSocketChannel();
				if(ssc != null && ssc.isOpen())
				{
					try {
						ssc.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
					commInfo.setNonBlockServerSocketChannel(null);
				}
			}
			else
			{
				// get the file sender (server)
				CMServer server = CMInteractionManager.findServer(strFileSender
				);
				if(server == null)
				{
					System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN()"
							+"server file sender("+strFileSender+") not found!");
					return bForward;
				}
				blockSCInfo = server.getBlockSocketChannelInfo();
				/*
				blockSCInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); # blocking socket channel: "
							+ blockSCInfo.getSize());
				}
				// get the default blocking socket channel
				defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);	// default blocking channel
				*/
			}
				
		}
		else	// server
		{
			// get the file sender (login client)
			CMUser sender = interInfo.getLoginUsers().findMember(strFileSender);
			if(sender == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN()"
						+"client file sender("+strFileSender+") not found!");
				return bForward;
			}
			blockSCInfo = sender.getBlockSocketChannelInfo();

			/*
			defaultBlockSC = (SocketChannel) sender.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); # blocking socket channel: "
						+ blockSCInfo.getSize());
			}
			*/
		}

		if(blockSCInfo == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), "
					+"block socket channel list not found for file sender("
					+strFileSender+")!");
			return bForward;
		}
		defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0); 

		// close the default blocking socket channel if it is open
		// the channel is actually closed due to the interrupt exception of the receiving thread
		if(defaultBlockSC == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), the default blocking "
					+"socket channel is null!");
		}
		else if(defaultBlockSC.isOpen())
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); the default channel is "
						+ "still open and should be closed for reconnection!");
			}
			
			try {
				defaultBlockSC.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); the default channel is "
					+ "already closed!");
		}
		
		// remove the default blocking socket channel
		if(defaultBlockSC != null)
			blockSCInfo.removeChannel(0);

		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT") && !bP2PFileTransfer)
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort());
			} catch (IOException e) {
				e.printStackTrace();
				return bForward;
			}
			
			if(defaultBlockSC == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), recreation of "
						+ "the blocking socket channel failed!: server("+serverInfo.getServerAddress()+"), port("
						+ serverInfo.getServerPort() +")");
				return bForward;
			}
			bReturn = blockSCInfo.addChannel(0, defaultBlockSC);

			if(bReturn)
			{
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				se.setChannelName(interInfo.getMyself().getName());
				se.setChannelNum(0);
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true);
				se = null;

				if(bReturn)
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(),successfully requested "
								+ "to add the blocking socket channel with the key(0) to the server("
								+serverInfo.getServerName()+")");
					}
					
				}
			}
		}
		
		/////////////////////
		
		return bForward;
	}
	
	private static boolean processCANCEL_FILE_SEND_CHAN_ACK(CMFileEvent fe)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN_ACK(), "
					+ "file sender("+fe.getFileSender()+"), receiver("
					+fe.getFileReceiver()+"), return code("+fe.getReturnCode()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileSender().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file sender("
						+fe.getFileSender()+").");
			}
			return false;
		}

		return bForward;
	}
	
	private static boolean processCANCEL_FILE_RECV_CHAN(CMFileEvent fe)
	{
		CMFileTransferInfo fInfo = CMFileTransferInfo.getInstance();
		CMList<CMSendFileInfo> sendList = null;
		CMSendFileInfo sInfo = null;
		Future<CMSendFileInfo> sendTask = null;
		CMFileEvent feAck = null;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		CMConfigurationInfo confInfo = CMConfigurationInfo.getInstance();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		boolean bReturn = false;
		
		String strFileReceiver = fe.getFileReceiver();
		UUID fileReceiverUuid = fe.getFileReceiverUuid();
		boolean bException = false;
		int nReturnCode = -1;
		boolean bP2PFileTransfer = false;
		String strMyName = interInfo.getMyself().getName();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(), "
					+"file sender("+fe.getFileSender()+"), file receiver("
					+fe.getFileReceiver()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileSender().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file sender("
						+fe.getFileSender()+").");
			}
			return false;
		}
		
		// find the CMSendFile list of the strReceiver
		sendList = fInfo.getSendFileList(strFileReceiver, fileReceiverUuid);
		if(sendList == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); sending file list "
					+ "not found for the receiver("+strFileReceiver+"), uuid("+fileReceiverUuid+")!");
			return bForward;
		}
		
		// find the current sending file task
		sInfo = fInfo.findSendFileInfoOngoing(strFileReceiver, fileReceiverUuid);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); ongoing sending task "
					+ "not found for the receiver("+strFileReceiver+"), uuid("+fileReceiverUuid+")!");
			fInfo.removeSendFileList(strFileReceiver, fileReceiverUuid);
			return bForward;
		}
		
		// request for canceling the sending task
		sendTask = sInfo.getSendTaskResult();
		sendTask.cancel(true);
		// wait for the thread cancellation
		try {
			sendTask.get(10L, TimeUnit.SECONDS);
		} catch(CancellationException e) {
			System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); the sending task cancelled.: "
					+ "receiver("+strFileReceiver+"), file("+sInfo.getFileName()+"), file size("+sInfo.getFileSize()
					+ "), sent size("+sInfo.getSentSize()+")");
		} catch (InterruptedException e) {
			e.printStackTrace();
			bException = true;
		} catch (ExecutionException e) {
			e.printStackTrace();
			bException = true;
		} catch (TimeoutException e) {
			e.printStackTrace();
			bException = true;
		} finally {
			if(bException)
				nReturnCode = 0;
			else
				nReturnCode = 1;
		}

		// remove the sending file list of the receiver
		fInfo.removeSendFileList(strFileReceiver, fileReceiverUuid);

		// send the cancel ack event to the receiver
		feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.CANCEL_FILE_RECV_CHAN_ACK);
		feAck.setFileSender(fe.getFileSender());
		feAck.setFileReceiver(strFileReceiver);
		feAck.setReturnCode(nReturnCode);
		
		bP2PFileTransfer = isP2PFileTransfer(feAck);
		
		if(bP2PFileTransfer)
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(), "
						+ "isP2PFileTransfer() returns true.");
			}
			// set the event sender and receiver
			feAck.setSender(interInfo.getMyself().getName());
			String strDefServer = interInfo.getDefaultServerInfo().getServerName();
			feAck.setReceiver(strDefServer);
			
			// set distribution fields
			feAck.setDistributionSession("CM_ONE_USER");
			feAck.setDistributionGroup(strFileReceiver);
			
			// send the event to the default server
			CMEventManager.unicastEvent(feAck, strDefServer);
		}
		else
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(), "
						+ "isP2PFileTransfer() returns false.");
			}
			// set the event sender and receiver
			feAck.setSender(interInfo.getMyself().getName());
			feAck.setReceiver(strFileReceiver);
			// send the event to the file receiver
			CMEventManager.unicastEvent(feAck, strFileReceiver);
		}

		//////////////////// the management of the closed default blocking socket channel
		// get the default blocking socket channel
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			if(bP2PFileTransfer)
			{
				// get the file receiver (client)
				CMUser targetUser = CMInteractionManager.findGroupMemberOfClient(
						strFileReceiver);
				if(targetUser == null)
				{
					System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN()"
							+"client file receiver("+strFileReceiver+") not found!");
					return bForward;
				}
				blockSCInfo = targetUser.getBlockSocketChannelInfo();
			}
			else
			{
				// get the file receiver (server)
				CMServer server = CMInteractionManager.findServer(strFileReceiver);
				if(server == null)
				{
					System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN()"
							+"server file receiver("+strFileReceiver+") not found!");
					return bForward;
				}
				blockSCInfo = server.getBlockSocketChannelInfo();

				/*
				blockSCInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); # blocking socket channel: "
							+ blockSCInfo.getSize());
				}
				// get the default blocking socket channel
				defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);	// default blocking channel
				*/
			}				
		}
		else	// server
		{
			// get the file receiver (login client)
			CMUser receiver = interInfo.getLoginUsers().findMember(strFileReceiver);
			if(receiver == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN()"
						+"client file receiver("+strFileReceiver+") not found!");
				return bForward;
			}
			blockSCInfo = receiver.getBlockSocketChannelInfo();
			
			/*
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) sender.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); # blocking socket channel: "
						+ blockSCInfo.getSize());
			}
			*/
		}

		if(blockSCInfo == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(), "
					+"block socket channel list not found for file receiver("
					+strFileReceiver+")!");
			return bForward;
		}
		defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0); 

		// close the default blocking socket channel if it is open
		// the channel is actually closed due to the interrupt exception of the receiving thread
		if(defaultBlockSC.isOpen())
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); the default channel is "
						+ "still open and should be closed for reconnection!");
			}
			
			try {
				defaultBlockSC.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); the default channel is "
					+ "already closed!");
		}
		
		// remove the default blocking socket channel
		blockSCInfo.removeChannel(0);

		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT") && !bP2PFileTransfer)
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort());
			} catch (IOException e) {
				e.printStackTrace();
				return bForward;
			}
			
			if(defaultBlockSC == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(), recreation of "
						+ "the blocking socket channel failed!: server("+serverInfo.getServerAddress()+"), port("
						+ serverInfo.getServerPort() +")");
				return bForward;
			}
			bReturn = blockSCInfo.addChannel(0, defaultBlockSC);

			if(bReturn)
			{
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				se.setChannelName(interInfo.getMyself().getName());
				se.setChannelNum(0);
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true);
				se = null;

				if(bReturn)
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(),successfully requested "
								+ "to add the blocking socket channel with the key(0) to the server("
								+serverInfo.getServerName()+")");
					}
					
				}
			}
		}
		
		/////////////////////
		
		return bForward;	
	}
	
	private static boolean processCANCEL_FILE_RECV_CHAN_ACK(CMFileEvent fe)
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();
		boolean bForward = true;
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN_ACK(), "
					+ "file sender("+fe.getFileSender()+"), file receiver("
					+fe.getFileReceiver()+"), return code("+fe.getReturnCode()+").");
		}
		
		// check whether this CM node is the target node of this event or not		
		if(!fe.getFileReceiver().contentEquals(strMyName))
		{
			if(CMInfo._CM_DEBUG)
			{
				System.err.println("This node ("+strMyName+") is not the file receiver("
						+fe.getFileReceiver()+").");
			}
			return false;
		}

		return bForward;
	}
	
	private static void processERR_RECV_FILE_CHAN(CMFileEvent fe)
	{
		cancelPullFile(fe.getFileSender());
	}
	
	private static void processERR_SEND_FILE_CHAN(CMFileEvent fe)
	{
		cancelPushFile(fe.getFileReceiver(), fe.getFileReceiverUuid());
	}
}
