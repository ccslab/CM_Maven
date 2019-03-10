package kr.ac.konkuk.ccslab.cm.manager;
import java.io.*;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import kr.ac.konkuk.ccslab.cm.entity.CMRecvFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMServer;
import kr.ac.konkuk.ccslab.cm.entity.CMChannelInfo;
import kr.ac.konkuk.ccslab.cm.entity.CMList;
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
import kr.ac.konkuk.ccslab.cm.thread.CMRecvFileTask;
import kr.ac.konkuk.ccslab.cm.thread.CMSendFileTask;

public class CMFileTransferManager {

	public static void init(CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		String strPath = confInfo.getTransferedFileHome().toString();
		
		// if the default directory does not exist, create it.
		File defaultPath = new File(strPath);
		if(!defaultPath.exists() || !defaultPath.isDirectory())
		{
			boolean ret = defaultPath.mkdirs();
			if(ret)
			{
				if(CMInfo._CM_DEBUG)
					System.out.println("A default path is created!");
			}
			else
			{
				System.out.println("A default path cannot be created!");
				return;
			}
		}
		
		if(CMInfo._CM_DEBUG)
			System.out.println("A default path for the file transfer: "+strPath);
						
		return;
	}
	
	public static void terminate(CMInfo cmInfo)
	{
		// nothing to do
	}
	
	public static boolean requestFile(String strFileName, String strFileOwner, CMInfo cmInfo)
	{
		boolean bReturn = false;
		bReturn = requestFile(strFileName, strFileOwner, CMInfo.FILE_DEFAULT, -1, cmInfo);
		return bReturn;
	}
	
	public static boolean requestFile(String strFileName, String strFileOwner, byte byteFileAppend, CMInfo cmInfo)
	{
		boolean bReturn = false;
		bReturn = requestFile(strFileName, strFileOwner, byteFileAppend, -1, cmInfo);
		return bReturn;		
	}
	
	public static boolean requestFile(String strFileName, String strFileOwner, byte byteFileAppend, 
			int nContentID, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN 
				&& myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMFileTransferManager.requestFile(), Client must log in to the default server.");
			return false;
		}
		
		if(confInfo.isFileTransferScheme())
			bReturn = requestFileWithSepChannel(strFileName, strFileOwner, byteFileAppend, nContentID, cmInfo);
		else
			bReturn = requestFileWithDefChannel(strFileName, strFileOwner, byteFileAppend, nContentID, cmInfo);
		return bReturn;
	}
	
	private static boolean requestFileWithDefChannel(String strFileName, String strFileOwner, byte byteFileAppend, 
			int nContentID, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_FILE_TRANSFER);
		fe.setReceiverName(myself.getName());	// requester name
		fe.setFileName(strFileName);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteFileAppend);
		bReturn = CMEventManager.unicastEvent(fe, strFileOwner, cmInfo);
		
		fe = null;
		return bReturn;
	}
	
	private static boolean requestFileWithSepChannel(String strFileName, String strFileOwner, byte byteFileAppend, 
			int nContentID, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.REQUEST_FILE_TRANSFER_CHAN);
		fe.setReceiverName(myself.getName());	// requester name
		fe.setFileName(strFileName);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteFileAppend);
		bReturn = CMEventManager.unicastEvent(fe, strFileOwner, cmInfo);
		
		fe = null;
		return bReturn;
	}
	
	public static boolean cancelRequestFile(String strSender, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(confInfo.isFileTransferScheme())
			bReturn = cancelRequestFileWithSepChannel(strSender, cmInfo);
		else
		{
			System.err.println("CMFileTransferManager.cancelRequestFile(); default file transfer does not support!");
		}
		
		return bReturn;		
	}

	// cancel the receiving file task with separate channels and threads
	private static boolean cancelRequestFileWithSepChannel(String strSender, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();

		if(strSender != null)
		{
			bReturn = cancelRequestFileWithSepChannelForOneSender(strSender, cmInfo);
		}
		else // cancel file transfer to all senders
		{
			Set<String> keySet = fInfo.getRecvFileHashtable().keySet();
			Iterator<String> iterKeys = keySet.iterator();
			while(iterKeys.hasNext())
			{
				String iterSender = iterKeys.next();
				bReturn = cancelRequestFileWithSepChannelForOneSender(iterSender, cmInfo);
			}
			// clear the sending file hash table
			bReturn = fInfo.clearRecvFileHashtable();
		}
		
		return bReturn;
	}

	// cancel the receiving file task from one sender with a separate channel and thread
	private static boolean cancelRequestFileWithSepChannelForOneSender(String strSender, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMList<CMRecvFileInfo> recvList = null;
		CMRecvFileInfo rInfo = null;
		boolean bReturn = false;
		Future<CMRecvFileInfo> recvTask = null;
		CMFileEvent fe = null;
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		
		// find the CMRecvFile list of the strSender
		recvList = fInfo.getRecvFileList(strSender);
		if(recvList == null)
		{
			System.err.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
					+ "receiving file list not found for the sender("+strSender+")!");
			return false;
		}
		
		// find the current receiving file task
		rInfo = fInfo.findRecvFileInfoOngoing(strSender);
		if(rInfo == null)
		{
			System.err.println("CMFileTransferManager.cancelRequestFileWithSepChannelForOneSender(); "
					+ "ongoing receiving task not found for the sender("+strSender+")!");
			bReturn = fInfo.removeRecvFileList(strSender);
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
					+ "sender("+strSender+"), file("+rInfo.getFileName()+"), file size("+rInfo.getFileSize()
					+ "), recv size("+rInfo.getRecvSize()+")");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/////////////////////// management of the closed default blocking socket channel
		
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
				// TODO Auto-generated catch block
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
		fe.setSenderName(strSender);
		fe.setReceiverName(interInfo.getMyself().getName());
		bReturn = CMEventManager.unicastEvent(fe, strSender, cmInfo);
		if(!bReturn)
		{
			return false;
		}
		
		// remove the receiving file list of the sender
		bReturn = fInfo.removeRecvFileList(strSender);

		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort(), cmInfo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true, cmInfo);
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
	
	public static boolean pushFile(String strFilePath, String strReceiver, CMInfo cmInfo)
	{
		boolean bReturn = false;
		bReturn = pushFile(strFilePath, strReceiver, CMInfo.FILE_DEFAULT, -1, cmInfo);
		return bReturn;
	}

	public static boolean pushFile(String strFilePath, String strReceiver, byte byteFileAppend, CMInfo cmInfo)
	{
		boolean bReturn = false;
		bReturn = pushFile(strFilePath, strReceiver, byteFileAppend, -1, cmInfo);
		return bReturn;
	}

	public static boolean pushFile(String strFilePath, String strReceiver, byte byteFileAppend, 
			int nContentID, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		if(confInfo.getSystemType().equals("CLIENT") && myself.getState() != CMInfo.CM_LOGIN 
				&& myself.getState() != CMInfo.CM_SESSION_JOIN)
		{
			System.err.println("CMFileTransferManager.pushFile(), Client must log in to the default server.");
			return false;
		}
		
		if(confInfo.isFileTransferScheme())
			bReturn = pushFileWithSepChannel(strFilePath, strReceiver, byteFileAppend, nContentID, cmInfo);
		else
			bReturn = pushFileWithDefChannel(strFilePath, strReceiver, byteFileAppend, nContentID, cmInfo);
		return bReturn;
	}

	// strFilePath: absolute or relative path to a target file
	private static boolean pushFileWithDefChannel(String strFilePath, String strReceiver, byte byteFileAppend, 
			int nContentID, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		
		// get file information (size)
		File file = new File(strFilePath);
		if(!file.exists())
		{
			System.err.println("CMFileTransferManager.pushFileWithDefChannel(), file("+strFilePath+") does not exists.");
			return false;
		}
		long lFileSize = file.length();
		
		// add send file information
		// receiver name, file path, size
		fInfo.addSendFileInfo(strReceiver, strFilePath, lFileSize, nContentID);
		
		// set the cancellation flag
		fInfo.setCancelSend(false);

		// get my name
		String strMyName = interInfo.getMyself().getName();

		// get file name
		String strFileName = getFileNameFromPath(strFilePath);
		System.out.println("file name: "+strFileName);
		
		// start file transfer process
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.START_FILE_TRANSFER);
		fe.setSenderName(strMyName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteFileAppend);
		bReturn = CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
		
		if(!bReturn)
		{
			// remove send file information
			fInfo.removeSendFileInfo(strReceiver, strFileName, nContentID);
		}

		file = null;
		fe = null;
		return bReturn;
	}
	
	// strFilePath: absolute or relative path to a target file
	private static boolean pushFileWithSepChannel(String strFilePath, String strReceiver, byte byteFileAppend, 
			int nContentID, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();

		// check the creation of the default blocking TCP socket channel
		CMChannelInfo<Integer> blockChannelList = null;
		CMChannelInfo<Integer> nonBlockChannelList = null;
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockChannelList = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
			nonBlockChannelList = interInfo.getDefaultServerInfo().getNonBlockSocketChannelInfo();
			dsc = (SocketChannel) nonBlockChannelList.findChannel(0); // key for the default TCP socket channel is 0
		}
		else	// SERVER
		{
			CMUser user = interInfo.getLoginUsers().findMember(strReceiver);
			if(user == null)
			{
				System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
						+ "user("+strReceiver+") not found!");
				return false;
			}
			blockChannelList = user.getBlockSocketChannelInfo();
			sc = (SocketChannel) blockChannelList.findChannel(0);	// default key for the blocking channel is 0
			nonBlockChannelList = user.getNonBlockSocketChannelInfo();
			dsc = (SocketChannel) nonBlockChannelList.findChannel(0);	// key for the default TCP socket channel is 0
		}

		if(sc == null)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default blocking TCP socket channel not found!");
			return false;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); "
					+ "default blocking TCP socket channel closed!");
			return false;
		}
		
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
		sfInfo.setSenderName(interInfo.getMyself().getName());
		sfInfo.setReceiverName(strReceiver);
		sfInfo.setFilePath(strFilePath);
		sfInfo.setFileSize(lFileSize);
		sfInfo.setContentID(nContentID);
		sfInfo.setSendChannel(sc);
		sfInfo.setDefaultChannel(dsc);
		//boolean bResult = fInfo.addSendFileInfo(strReceiver, strFilePath, lFileSize, nContentID);
		bReturn = fInfo.addSendFileInfo(sfInfo);
		if(!bReturn)
		{
			System.err.println("CMFileTransferManager.pushFileWithSepChannel(); error for adding the sending file info: "
					+"receiver("+strReceiver+"), file("+strFilePath+"), size("+lFileSize+"), content ID("
					+nContentID+")!");
			return false;
		}

		// get my name
		String strMyName = interInfo.getMyself().getName();

		// get file name
		String strFileName = getFileNameFromPath(strFilePath);
		
		// start file transfer process
		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.START_FILE_TRANSFER_CHAN);
		fe.setSenderName(strMyName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		fe.setFileAppendFlag(byteFileAppend);
		bReturn = CMEventManager.unicastEvent(fe, strReceiver, cmInfo);

		if(!bReturn)
		{
			fInfo.removeSendFileInfo(strReceiver, strFileName, nContentID);
		}
		
		file = null;
		fe = null;
		return bReturn;
	}
	
	public static boolean cancelPushFile(String strReceiver, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(confInfo.isFileTransferScheme())
			bReturn = cancelPushFileWithSepChannel(strReceiver, cmInfo);
		else
			bReturn = cancelPushFileWithDefChannel(strReceiver, cmInfo);
		
		return bReturn;
	}
	
	private static boolean cancelPushFileWithDefChannel(String strReceiver, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMList<CMSendFileInfo> sendList = null;
		Iterator<CMSendFileInfo> iterSendList = null;
		CMSendFileInfo sInfo = null;
		CMFileEvent fe = null;

		if(strReceiver != null)
		{
			// find the CMSendFile list of the strReceiver
			sendList = fInfo.getSendFileList(strReceiver);
			if(sendList == null)
			{
				System.err.println("CMFileTransferManager.cancelPushFileWithDefChannel(); Sending file list "
						+ "not found for the receiver("+strReceiver+")!");
				return false;
			}			
		}
		
		// set the flag
		fInfo.setCancelSend(true);
		
		// send the cancellation event to the receiver
		// close the RandomAccessFile and remove the sending file info of the receiver
		if(strReceiver != null) // for the target receiver
		{
			fe = new CMFileEvent();
			fe.setID(CMFileEvent.CANCEL_FILE_SEND);
			fe.setSenderName(interInfo.getMyself().getName());
			fe.setReceiverName(strReceiver);
			CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
			
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
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			bReturn = fInfo.removeSendFileList(strReceiver);
		}
		else	// for all receivers
		{
			Set<String> keySet = fInfo.getSendFileHashtable().keySet();
			Iterator<String> iterKeys = keySet.iterator();
			while(iterKeys.hasNext())
			{
				String iterReceiver = iterKeys.next();
				fe = new CMFileEvent();
				fe.setID(CMFileEvent.CANCEL_FILE_SEND);
				fe.setSenderName(interInfo.getMyself().getName());
				fe.setReceiverName(iterReceiver);
				CMEventManager.unicastEvent(fe, iterReceiver, cmInfo);
				
				// close the RandomAccessFile
				sendList = fInfo.getSendFileList(iterReceiver);
				iterSendList = sendList.getList().iterator();
				while(iterSendList.hasNext())
				{
					sInfo = iterSendList.next();
					if(sInfo.getReadFile() != null)
					{
						try {
							sInfo.getReadFile().close();
						} catch (IOException e) {
							// TODO Auto-generated catch block
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
						+ "receiver("+strReceiver+").");
		}
		else
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithDefChannel(); failed for "
					+ "receiver("+strReceiver+")!");
		}
		
		return bReturn;
	}
	
	// cancel the sending file task with separate channels and threads
	private static boolean cancelPushFileWithSepChannel(String strReceiver, CMInfo cmInfo)
	{
		boolean bReturn = false;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();

		if(strReceiver != null)
		{
			bReturn = cancelPushFileWithSepChannelForOneReceiver(strReceiver, cmInfo);
		}
		else // cancel file transfer to all receivers
		{
			Set<String> keySet = fInfo.getSendFileHashtable().keySet();
			Iterator<String> iterKeys = keySet.iterator();
			while(iterKeys.hasNext())
			{
				String iterReceiver = iterKeys.next();
				bReturn = cancelPushFileWithSepChannelForOneReceiver(iterReceiver, cmInfo);
			}
			// clear the sending file hash table
			bReturn = fInfo.clearSendFileHashtable();
		}

		return bReturn;
	}

	// cancel the sending file task to one receiver with a separate channel and thread
	private static boolean cancelPushFileWithSepChannelForOneReceiver(String strReceiver, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMList<CMSendFileInfo> sendList = null;
		CMSendFileInfo sInfo = null;
		boolean bReturn = false;
		Future<CMSendFileInfo> sendTask = null;
		CMFileEvent fe = null;
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		
		// find the CMSendFile list of the strReceiver
		sendList = fInfo.getSendFileList(strReceiver);
		if(sendList == null)
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); Sending file list "
					+ "not found for the receiver("+strReceiver+")!");
			return false;
		}
		
		// find the current sending file task
		sInfo = fInfo.findSendFileInfoOngoing(strReceiver);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); ongoing sending task "
					+ "not found for the receiver("+strReceiver+")!");
			bReturn = fInfo.removeSendFileList(strReceiver);
			return bReturn;
		}
		
		// request for canceling the sending task
		sendTask = sInfo.getSendTaskResult();
		sendTask.cancel(true);
		// wait for the thread cancellation
		try {
			sendTask.get(10L, TimeUnit.SECONDS);
		} catch(CancellationException e) {
			System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
					+ "the sending task cancelled.: "
					+ "receiver("+strReceiver+"), file("+sInfo.getFileName()+"), file size("+sInfo.getFileSize()
					+ "), sent size("+sInfo.getSentSize()+")");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/////////////////////// management of the closed default blocking socket channel
		
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
			CMUser receiver = interInfo.getLoginUsers().findMember(strReceiver);
			blockSCInfo = receiver.getBlockSocketChannelInfo();
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) receiver.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
						+ "# blocking socket channel: "	+ blockSCInfo.getSize());
			}

		}

		// close the default blocking socket channel if it is open
		// the channel is actually closed due to the interrupt exception of the sending thread
		if(defaultBlockSC.isOpen())
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.cancelPushFileWithSepChannelForOneReceiver(); "
						+ "the default channel is still open and should be closed for reconnection!");
			}
			
			try {
				defaultBlockSC.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
		fe.setSenderName(interInfo.getMyself().getName());
		fe.setReceiverName(strReceiver);
		bReturn = CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
		if(!bReturn)
		{
			return false;
		}
		
		// remove the sending file list of the receiver
		bReturn = fInfo.removeSendFileList(strReceiver);

		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort(), cmInfo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
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
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true, cmInfo);
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
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
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
				// TODO Auto-generated catch block
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
		/*
		index = strPath.lastIndexOf("/");
		if(index == -1)
		{
			index = strPath.lastIndexOf(sep);
			if(index == -1)
				strName = strPath;
			else
				strName = strPath.substring(index+1);
		}
		else
		{
			strName = strPath.substring(index+1);
		}
		*/
		index = strPath.lastIndexOf(sep);
		if(index == -1)
			strName = strPath;
		else
			strName = strPath.substring(index+1);
		
		return strName;
	}
	
	//////////////////////////////////////////////////////////////////
	// process file event
	
	public static void processEvent(CMMessage msg, CMInfo cmInfo)
	{
		CMFileEvent fe = new CMFileEvent(msg.m_buf);
		
		switch(fe.getID())
		{
		case CMFileEvent.REQUEST_FILE_TRANSFER:
			processREQUEST_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.REPLY_FILE_TRANSFER:
			processREPLY_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER:
			processSTART_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER_ACK:
			processSTART_FILE_TRANSFER_ACK(fe, cmInfo);
			break;
		case CMFileEvent.CONTINUE_FILE_TRANSFER:
			processCONTINUE_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER:
			processEND_FILE_TRANSFER(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER_ACK:
			processEND_FILE_TRANSFER_ACK(fe, cmInfo);
			break;
		case CMFileEvent.REQUEST_DIST_FILE_PROC:
			processREQUEST_DIST_FILE_PROC(fe, cmInfo);
			break;
		case CMFileEvent.REQUEST_FILE_TRANSFER_CHAN:
			processREQUEST_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.REPLY_FILE_TRANSFER_CHAN:
			processREPLY_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER_CHAN:
			processSTART_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.START_FILE_TRANSFER_CHAN_ACK:
			processSTART_FILE_TRANSFER_CHAN_ACK(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER_CHAN:
			processEND_FILE_TRANSFER_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.END_FILE_TRANSFER_CHAN_ACK:
			processEND_FILE_TRANSFER_CHAN_ACK(fe, cmInfo);
			break;
		case CMFileEvent.CANCEL_FILE_SEND:
			processCANCEL_FILE_SEND(fe, cmInfo);
			break;
		case CMFileEvent.CANCEL_FILE_SEND_ACK:
			processCANCEL_FILE_SEND_ACK(fe, cmInfo);
			break;
		case CMFileEvent.CANCEL_FILE_SEND_CHAN:
			processCANCEL_FILE_SEND_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.CANCEL_FILE_SEND_CHAN_ACK:
			processCANCEL_FILE_SEND_CHAN_ACK(fe, cmInfo);
			break;
		case CMFileEvent.CANCEL_FILE_RECV_CHAN:
			processCANCEL_FILE_RECV_CHAN(fe, cmInfo);
			break;
		case CMFileEvent.CANCEL_FILE_RECV_CHAN_ACK:
			processCANCEL_FILE_RECV_CHAN_ACK(fe, cmInfo);
			break;
		default:
			System.err.println("CMFileTransferManager.processEvent(), unknown event id("+fe.getID()+").");
			fe = null;
			return;
		}
		
		fe.setFileBlock(null);
		fe = null;
		return;
	}
	
	private static void processREQUEST_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMUser myself = cmInfo.getInteractionInfo().getMyself();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_FILE_TRANSFER(), requester("
					+fe.getReceiverName()+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()
					+").");
		}

		String strFileName = fe.getFileName();
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.REPLY_FILE_TRANSFER);
		feAck.setFileName(strFileName);

		// get the full path of the requested file
		String strFullPath = confInfo.getTransferedFileHome().toString() + File.separator + strFileName; 
		// check the file existence
		File file = new File(strFullPath);
		if(!file.exists())
		{
			feAck.setReturnCode(0);	// file not found
			CMEventManager.unicastEvent(feAck, fe.getReceiverName(), cmInfo);
			feAck = null;
			return;
		}
		
		feAck.setReturnCode(1);	// file found
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getReceiverName(), cmInfo);
		
		// get the file size
		long lFileSize = file.length();
		
		// add send file information
		// receiver name, file path, size
		fInfo.addSendFileInfo(fe.getReceiverName(), strFullPath, lFileSize, fe.getContentID());

		// start file transfer process
		CMFileEvent feStart = new CMFileEvent();
		feStart.setID(CMFileEvent.START_FILE_TRANSFER);
		feStart.setSenderName(myself.getName());
		feStart.setFileName(fe.getFileName());
		feStart.setFileSize(lFileSize);
		feStart.setContentID(fe.getContentID());
		feStart.setFileAppendFlag(fe.getFileAppendFlag());
		CMEventManager.unicastEvent(feStart, fe.getReceiverName(), cmInfo);

		feAck = null;
		feStart = null;
		file = null;
		return;
	}
	
	private static void processREPLY_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMEventInfo eInfo = cmInfo.getEventInfo();
		CMEventSynchronizer eventSync = eInfo.getEventSynchronizer();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREPLY_FILE_TRANSFER(), file("+fe.getFileName()
					+"), return code("+fe.getReturnCode()+"), contentID("+fe.getContentID()+").");
		}
		
		if(fe.getReturnCode() == 0 && fe.getFileName().equals("throughput-test.jpg"))
		{
			System.err.println("The requested file does not exists!");
			synchronized(eventSync)
			{
				eventSync.setReplyEvent(fe);
				eventSync.notify();
			}
		}
		return;
	}
	
	private static void processSTART_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER(),");
			System.out.println("sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), size("
					+fe.getFileSize()+"), contentID("+fe.getContentID()+"), appendFlag("
					+fe.getFileAppendFlag()+").");
		}
		
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
			strFullPath = strFullPath + File.separator + fe.getSenderName();
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
					return;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
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
						// TODO Auto-generated catch block
						e.printStackTrace();
						try {
							writeFile.close();
						} catch (IOException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						return;
					}
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		
		// add the received file info in the push list
		fInfo.addRecvFileInfo(fe.getSenderName(), fe.getFileName(), lFileSize, fe.getContentID(), 
				lRecvSize, writeFile);
		
		// send ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.START_FILE_TRANSFER_ACK);
		feAck.setReceiverName(cmInfo.getInteractionInfo().getMyself().getName());
		feAck.setFileName(fe.getFileName());
		feAck.setContentID(fe.getContentID());
		feAck.setReceivedFileSize(lRecvSize);
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);

		feAck = null;
		return;
	}
	
	private static void processSTART_FILE_TRANSFER_ACK(CMFileEvent recvFileEvent, CMInfo cmInfo)
	{
		String strReceiver = null;
		String strFileName = null;
		String strFullFileName = null;
		long lFileSize = -1;
		int nContentID = -1;
		String strSenderName = null;
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMSendFileInfo sInfo = null;
		long lRecvSize = 0;
		
		// find the CMSendFileInfo object 
		sInfo = fInfo.findSendFileInfo(recvFileEvent.getReceiverName(), recvFileEvent.getFileName(), 
				recvFileEvent.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), sendFileInfo not found! : "
					+"receiver("+recvFileEvent.getReceiverName()+"), file("+recvFileEvent.getFileName()
					+"), content ID("+recvFileEvent.getContentID()+")");
			return;
		}
		
		strReceiver = sInfo.getReceiverName();
		strFullFileName = sInfo.getFilePath();
		strFileName = getFileNameFromPath(strFullFileName);
		lFileSize = sInfo.getFileSize();
		nContentID = sInfo.getContentID();
					
		lRecvSize = recvFileEvent.getReceivedFileSize();
		
		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "Sending file("+strFileName+") to target("+strReceiver+") from the file position("
					+ lRecvSize +").");

		// open the file
		RandomAccessFile readFile = null;
		try {
			readFile = new RandomAccessFile(strFullFileName, "rw");
			if(lRecvSize > 0 && lRecvSize < lFileSize)	// If the receiver uses the append scheme,
			{
				try {
					readFile.seek(lRecvSize);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					try {
						readFile.close();
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					return;
				}
			}
			sInfo.setReadFile(readFile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		// set sender name
		strSenderName = cmInfo.getInteractionInfo().getMyself().getName();

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
				// TODO Auto-generated catch block
				e.printStackTrace();
				continue;
			}
			
			// send file block
			fe = new CMFileEvent();
			fe.setID(CMFileEvent.CONTINUE_FILE_TRANSFER);
			fe.setSenderName(strSenderName);
			fe.setFileName(strFileName);
			fe.setFileBlock(fileBlock);
			fe.setBlockSize(nReadBytes);
			fe.setContentID(nContentID);
			CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
			
			lRemainBytes -= nReadBytes;
		}
		
		if(lRemainBytes < 0)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER(); "
					+ "the receiver("+strReceiver+") already has "
					+ "a bigger size file("+strFileName+"); sender size("+lFileSize
					+ "), receiver size("+lRecvSize+").");
		}
		
		// close fis
		try {
			sInfo.getReadFile().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// reset the flag
		if(fInfo.isCancelSend())
		{
			fInfo.setCancelSend(false);
			fileBlock = null;
			fe = null;
			return;
		}

		if(CMInfo._CM_DEBUG)
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_ACK(), "
					+ "Ending transfer of file("+strFileName+") to target("+strReceiver
					+"), size("+lFileSize+") Bytes.");

		// send the end of file transfer
		fe = new CMFileEvent();
		fe.setID(CMFileEvent.END_FILE_TRANSFER);
		fe.setSender(strSenderName);
		fe.setReceiver(strReceiver);
		fe.setSenderName(strSenderName);
		fe.setFileName(strFileName);
		fe.setFileSize(lFileSize);
		fe.setContentID(nContentID);
		CMEventManager.unicastEvent(fe, strReceiver, cmInfo);
		
		fileBlock = null;
		fe = null;
		return;
	}
	
	private static void processCONTINUE_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		
		/*
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileManager.processCONTINUE_FILE_TRANSFER(), sender("
					+fe.getSenderName()+"), file("+fe.getFileName()+"), "+fe.getBlockSize()
					+" Bytes, contentID("+fe.getContentID()+").");
		}
		*/

		// find info in the recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		if( recvInfo == null )
		{
			System.err.println("CMFileTransferManager.processCONTINUE_FILE_TRANSFER(), "
					+ "recv file info for sender("+fe.getSenderName()+"), file("+fe.getFileName()
					+"), content ID("+fe.getContentID()+") not found.");
			return;
		}

		try {
			recvInfo.getWriteFile().write(fe.getFileBlock(), 0, fe.getBlockSize());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		recvInfo.setRecvSize(recvInfo.getRecvSize()+fe.getBlockSize());

		/*
		if(CMInfo._CM_DEBUG)
			System.out.println("Cumulative written file size: "+pushInfo.m_lRecvSize+" Bytes.");
		*/
		
		return;
	}
	
	private static void processEND_FILE_TRANSFER(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();

		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER(), recv file info "
					+"for sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), content ID("
					+fe.getContentID()+") not found.");

			return;
		}
		// close received file descriptor
		try {
			recvInfo.getWriteFile().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER(), sender("+fe.getSenderName()
					+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()+"), file size("
					+recvInfo.getFileSize()+"), received size("+recvInfo.getRecvSize()+").");
		}

		// remove info from push file list
		fInfo.removeRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		
		// send ack
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_ACK);
		feAck.setSender(interInfo.getMyself().getName());
		feAck.setReceiver(fe.getSender());
		feAck.setReceiverName(interInfo.getMyself().getName());
		feAck.setFileName(fe.getFileName());
		feAck.setFileSize(fe.getFileSize());
		feAck.setReturnCode(1);	// success
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;
		
		CMSNSManager.checkCompleteRecvAttachedFiles(fe, cmInfo);

		return;
	}
	
	private static void processEND_FILE_TRANSFER_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		String strReceiverName = fe.getReceiverName();
		String strFileName = fe.getFileName();
		long lFileSize = fe.getFileSize();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strReceiverName, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), send info not found");
			System.err.println("receiver("+strReceiverName+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendFileInfo(strReceiverName, strFileName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_ACK(), receiver("
					+strReceiverName+"), file("+strFileName+"), size("+lFileSize+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		CMSNSManager.checkCompleteSendAttachedFiles(fe, cmInfo);

		return;
	}
	
	private static void processREQUEST_DIST_FILE_PROC(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_DIST_FILE_PROC(), user("
						+fe.getReceiverName()+") requests the distributed file processing.");
		}
		return;
	}
	
	private static void processREQUEST_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processREQUEST_FILE_TRANSFER_CHAN(), requester("
					+fe.getReceiverName()+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()
					+").");
		}

		String strFileName = fe.getFileName();
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.REPLY_FILE_TRANSFER_CHAN);
		feAck.setFileName(strFileName);

		// get the full path of the requested file
		String strFullPath = confInfo.getTransferedFileHome().toString() + File.separator + strFileName; 
		// check the file existence
		File file = new File(strFullPath);
		if(!file.exists())
		{
			feAck.setReturnCode(0);	// file not found
			CMEventManager.unicastEvent(feAck, fe.getReceiverName(), cmInfo);
			feAck = null;
			return;
		}
		
		feAck.setReturnCode(1);	// file found
		feAck.setContentID(fe.getContentID());
		CMEventManager.unicastEvent(feAck, fe.getReceiverName(), cmInfo);
		
		pushFileWithSepChannel(strFullPath, fe.getReceiverName(), fe.getFileAppendFlag(), fe.getContentID(), cmInfo);
		return;
	}
	
	private static void processREPLY_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMEventSynchronizer eventSync = cmInfo.getEventInfo().getEventSynchronizer();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileManager.processREPLY_FILE_TRANSFER_CHAN(), file("+fe.getFileName()
					+"), return code("+fe.getReturnCode()+"), contentID("+fe.getContentID()+").");
		}
		
		if(fe.getReturnCode() == 0 && fe.getFileName().equals("throughput-test.jpg"))
		{
			System.err.println("The requested file does not exists!");
			synchronized(eventSync)
			{
				eventSync.setReplyEvent(fe);
				eventSync.notify();
			}
		}

		return;
	}
	
	private static void processSTART_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN(),");
			System.out.println("sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), size("
					+fe.getFileSize()+"), contentID("+fe.getContentID()+"), appendFlag("
					+fe.getFileAppendFlag()+").");
		}
		
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
			strFullPath = strFullPath + File.separator + fe.getSenderName();
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
					return;
				}
			}
			
			strFullPath = strFullPath + File.separator + fe.getFileName();
		}
		else
		{
			System.err.println("Wrong system type!");
			return;
		}		
		
		// get the default blocking TCP socket channel
		SocketChannel sc = null;
		SocketChannel dsc = null;
		if(confInfo.getSystemType().equals("CLIENT"))	// CLIENT
		{
			CMServer serverInfo = cmInfo.getInteractionInfo().getDefaultServerInfo();
			sc = (SocketChannel) serverInfo.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) serverInfo.getNonBlockSocketChannelInfo().findChannel(0);
		}
		else	// SERVER
		{
			CMUser user = cmInfo.getInteractionInfo().getLoginUsers().findMember(fe.getSenderName());
			sc = (SocketChannel) user.getBlockSocketChannelInfo().findChannel(0);
			dsc = (SocketChannel) user.getNonBlockSocketChannelInfo().findChannel(0);
		}
		
		if(sc == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default blocking TCP socket channel not found!");
			return;
		}
		else if(!sc.isOpen())
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default blocking TCP socket channel is closed!");
			return;
		}
		
		if(dsc == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default TCP socket channel not found!");
			return;
		}
		else if(!dsc.isOpen())
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN();"
					+ "the default TCP socket channel is closed!");
			return;
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
		rfInfo.setSenderName(fe.getSenderName());
		rfInfo.setReceiverName(cmInfo.getInteractionInfo().getMyself().getName());
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
			return;
		}
		
		if(!fInfo.isRecvOngoing(fe.getSenderName()))
		{
			sendSTART_FILE_TRANSFER_CHAN_ACK(rfInfo, cmInfo);
			/*
			// start a dedicated thread to receive the file
			Future<CMRecvFileInfo> future = null;
			CMRecvFileTask recvFileTask = new CMRecvFileTask(rfInfo);
			future = fInfo.getExecutorService().submit(recvFileTask, rfInfo);
			rfInfo.setRecvTaskResult(future);
			
			// send ack event
			CMFileEvent feAck = new CMFileEvent();
			feAck.setID(CMFileEvent.START_FILE_TRANSFER_CHAN_ACK);
			feAck.setUserName(cmInfo.getInteractionInfo().getMyself().getName());
			feAck.setFileName(fe.getFileName());
			feAck.setContentID(fe.getContentID());
			feAck.setReceivedFileSize(lRecvSize);
			CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);

			feAck = null;
			*/
		}
				
		return;
	}
	
	private static void processSTART_FILE_TRANSFER_CHAN_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		String strReceiver = null;
		String strFileName = null;
		String strFullFileName = null;
		long lFileSize = -1;	// file size
		int nContentID = -1;
		long lRecvSize = -1;	// received size by the receiver
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMThreadInfo threadInfo = cmInfo.getThreadInfo();
		CMSendFileInfo sInfo = null;
		CMCommInfo commInfo = cmInfo.getCommInfo();
		
		// find the CMSendFileInfo object 
		sInfo = fInfo.findSendFileInfo(fe.getReceiverName(), fe.getFileName(), fe.getContentID());
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN_ACK(), sendFileInfo "
					+ "not found! : receiver("+fe.getReceiverName()+"), file("+fe.getFileName()
					+"), content ID("+fe.getContentID()+")");
			return;
		}
		
		strReceiver = sInfo.getReceiverName();
		strFullFileName = sInfo.getFilePath();
		strFileName = getFileNameFromPath(strFullFileName);
		lFileSize = sInfo.getFileSize();
		nContentID = sInfo.getContentID();
		
		lRecvSize = fe.getReceivedFileSize();
		if(lRecvSize > 0)
		{
			sInfo.setSentSize(lRecvSize);	// update the sent size
			//sInfo.setAppend(true);			// set the file append scheme
		}
					
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processSTART_FILE_TRANSFER_CHAN_ACK(); "
					+ "receiver("+strReceiver+"), file path("+strFullFileName+"), file name("+strFileName
					+ "), file size("+lFileSize+"), content ID("+nContentID+").");
			System.out.println("already received file size by the receiver("+lRecvSize+").");
		}

		// start a dedicated sending thread
		Future<CMSendFileInfo> future = null;
		CMSendFileTask sendFileTask = new CMSendFileTask(sInfo, commInfo.getSendBlockingEventQueue());
		future = threadInfo.getExecutorService().submit(sendFileTask, sInfo);
		sInfo.setSendTaskResult(future);		

		return;		
	}
	
	private static void processEND_FILE_TRANSFER_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		boolean bResult = false;

		// find info from recv file list
		CMRecvFileInfo recvInfo = fInfo.findRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		if(recvInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), recv file info "
					+"for sender("+fe.getSenderName()+"), file("+fe.getFileName()+"), content ID("
					+fe.getContentID()+") not found.");

			return;
		}

		// wait the receiving thread
		if(!recvInfo.getRecvTaskResult().isDone())
		{
			try {
				recvInfo.getRecvTaskResult().get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN(), sender("+fe.getSenderName()
					+"), file("+fe.getFileName()+"), contentID("+fe.getContentID()+"), file size("
					+recvInfo.getFileSize()+"), received size("+recvInfo.getRecvSize()+").");
		}

		// make ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.END_FILE_TRANSFER_CHAN_ACK);
		feAck.setSender(interInfo.getMyself().getName());
		feAck.setReceiver(fe.getSender());
		feAck.setReceiverName(interInfo.getMyself().getName());
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
		fInfo.removeRecvFileInfo(fe.getSenderName(), fe.getFileName(), fe.getContentID());
		
		// send ack
		CMEventManager.unicastEvent(feAck, fe.getSenderName(), cmInfo);		
		feAck = null;

		if(bResult)
			CMSNSManager.checkCompleteRecvAttachedFiles(fe, cmInfo);

		// check whether there is a remaining receiving file info or not
		CMRecvFileInfo nextRecvInfo = fInfo.findRecvFileInfoNotStarted(fe.getSenderName());
		if(nextRecvInfo != null)
		{
			sendSTART_FILE_TRANSFER_CHAN_ACK(nextRecvInfo, cmInfo);
		}
		
		return;
	}
	
	private static void processEND_FILE_TRANSFER_CHAN_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		String strReceiverName = fe.getReceiverName();
		String strFileName = fe.getFileName();
		long lFileSize = fe.getFileSize();
		int nContentID = fe.getContentID();
		
		// find completed send info
		CMSendFileInfo sInfo = fInfo.findSendFileInfo(strReceiverName, strFileName, nContentID);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN_ACK(), send info not found");
			System.err.println("receiver("+strReceiverName+"), file("+strFileName+"), content ID("+nContentID+").");
		}
		else
		{
			// delete corresponding request from the list
			fInfo.removeSendFileInfo(strReceiverName, strFileName, nContentID);
		}
	
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processEND_FILE_TRANSFER_CHAN_ACK(), receiver("
					+strReceiverName+"), file("+strFileName+"), size("+lFileSize+"), return code("+fe.getReturnCode()
					+"), contentID("+nContentID+").");
		}
		
		//////////////////// check the completion of sending attached file of SNS content
		//////////////////// and check the completion of prefetching an attached file of SNS content
		CMSNSManager.checkCompleteSendAttachedFiles(fe, cmInfo);

		return;	
	}
	
	private static void sendSTART_FILE_TRANSFER_CHAN_ACK(CMRecvFileInfo rfInfo, CMInfo cmInfo)
	{
		CMThreadInfo threadInfo = cmInfo.getThreadInfo();

		// start a dedicated thread to receive the file
		Future<CMRecvFileInfo> future = null;
		CMRecvFileTask recvFileTask = new CMRecvFileTask(rfInfo);
		future = threadInfo.getExecutorService().submit(recvFileTask, rfInfo);
		rfInfo.setRecvTaskResult(future);
		
		// send ack event
		CMFileEvent feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.START_FILE_TRANSFER_CHAN_ACK);
		feAck.setReceiverName(cmInfo.getInteractionInfo().getMyself().getName());
		feAck.setFileName(rfInfo.getFileName());
		feAck.setContentID(rfInfo.getContentID());
		feAck.setReceivedFileSize(rfInfo.getRecvSize());
		CMEventManager.unicastEvent(feAck, rfInfo.getSenderName(), cmInfo);

		feAck = null;
	}
	
	private static void processCANCEL_FILE_SEND(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		String strSender = fe.getSenderName();
		CMList<CMRecvFileInfo> recvList = fInfo.getRecvFileList(strSender);
		Iterator<CMRecvFileInfo> iter = null;
		CMRecvFileInfo rInfo = null;
		CMFileEvent feAck = new CMFileEvent();
		boolean bReturn = false;
		
		// make the ack event
		feAck.setID(CMFileEvent.CANCEL_FILE_SEND_ACK);
		feAck.setSenderName(strSender);
		feAck.setReceiverName(interInfo.getMyself().getName());
		
		// recv file info list not found
		if(recvList == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND(); recv info list not found "
					+ "for sender("+strSender+")!");
			feAck.setReturnCode(0);
			CMEventManager.unicastEvent(feAck, strSender, cmInfo);
			return;
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
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		bReturn = fInfo.removeRecvFileList(strSender);
		
		if(bReturn)
			feAck.setReturnCode(1);
		else
			feAck.setReturnCode(0);
		
		bReturn = CMEventManager.unicastEvent(feAck, strSender, cmInfo);
		
		if(bReturn)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND(); succeeded. sender("
					+fe.getSenderName()+"), receiver("+fe.getReceiverName()+").");
		}
		else
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND(); failed! sender("
					+fe.getSenderName()+"), receiver("+fe.getReceiverName()+").");
		}
		
		return;
	}
	
	private static void processCANCEL_FILE_SEND_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_ACK(); sender("+fe.getSenderName()
				+"), receiver("+fe.getReceiverName()+"), return code("+fe.getReturnCode()+").");
		}
		return;
	}
	
	private static void processCANCEL_FILE_SEND_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMList<CMRecvFileInfo> recvList = null;
		CMRecvFileInfo rInfo = null;
		Future<CMRecvFileInfo> recvTask = null;
		CMFileEvent feAck = null;
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		boolean bReturn = false;
		
		String strSender = fe.getSenderName();
		boolean bException = false;
		int nReturnCode = -1;
		
		// find the CMRecvFile list of the strSender
		recvList = fInfo.getRecvFileList(strSender);
		if(recvList == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); Receiving file list "
					+ "not found for the sender("+strSender+")!");
			return;
		}
		
		// find the current receiving file task
		rInfo = fInfo.findRecvFileInfoOngoing(strSender);
		if(rInfo == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); ongoing receiving task "
					+ "not found for the sender("+strSender+")!");
			fInfo.removeRecvFileList(strSender);
			return;
		}
		
		// request for canceling the receiving task
		recvTask = rInfo.getRecvTaskResult();
		recvTask.cancel(true);
		// wait for the thread cancellation
		try {
			recvTask.get(10L, TimeUnit.SECONDS);
		} catch(CancellationException e) {
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); the receiving task cancelled.: "
					+ "sender("+strSender+"), file("+rInfo.getFileName()+"), file size("+rInfo.getFileSize()
					+ "), recv size("+rInfo.getRecvSize()+")");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bException = true;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bException = true;
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bException = true;
		} finally {
			if(bException)
				nReturnCode = 0;
			else
				nReturnCode = 1;
		}

		// remove the receiving file list of the sender
		fInfo.removeRecvFileList(strSender);

		// send the cancel ack event to the sender
		feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.CANCEL_FILE_SEND_CHAN_ACK);
		feAck.setSenderName(strSender);
		feAck.setReceiverName(interInfo.getMyself().getName());
		feAck.setReturnCode(nReturnCode);
		CMEventManager.unicastEvent(feAck, strSender, cmInfo);

		//////////////////// the management of the closed default blocking socket channel
		// get the default blocking socket channel
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockSCInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); # blocking socket channel: "
						+ blockSCInfo.getSize());
			}
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);	// default blocking channel
				
		}
		else	// server
		{
			CMUser sender = interInfo.getLoginUsers().findMember(strSender);
			blockSCInfo = sender.getBlockSocketChannelInfo();
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) sender.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); # blocking socket channel: "
						+ blockSCInfo.getSize());
			}

		}

		// close the default blocking socket channel if it is open
		// the channel is actually closed due to the interrupt exception of the receiving thread
		if(defaultBlockSC.isOpen())
		{
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); the default channel is "
						+ "still open and should be closed for reconnection!");
			}
			
			try {
				defaultBlockSC.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(); the default channel is "
					+ "already closed!");
		}
		
		// remove the default blocking socket channel
		blockSCInfo.removeChannel(0);

		// if the system type is client, it recreates the default blocking socket channel to the default server
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort(), cmInfo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			if(defaultBlockSC == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN(), recreation of "
						+ "the blocking socket channel failed!: server("+serverInfo.getServerAddress()+"), port("
						+ serverInfo.getServerPort() +")");
				return;
			}
			bReturn = blockSCInfo.addChannel(0, defaultBlockSC);

			if(bReturn)
			{
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				se.setChannelName(interInfo.getMyself().getName());
				se.setChannelNum(0);
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true, cmInfo);
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
		
		return;
	}
	
	private static void processCANCEL_FILE_SEND_CHAN_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_SEND_CHAN_ACK(); sender("+fe.getSenderName()
				+"), receiver("+fe.getReceiverName()+"), return code("+fe.getReturnCode()+").");
		}
		return;
	}
	
	private static void processCANCEL_FILE_RECV_CHAN(CMFileEvent fe, CMInfo cmInfo)
	{
		CMFileTransferInfo fInfo = cmInfo.getFileTransferInfo();
		CMList<CMSendFileInfo> sendList = null;
		CMSendFileInfo sInfo = null;
		Future<CMSendFileInfo> sendTask = null;
		CMFileEvent feAck = null;
		CMInteractionInfo interInfo = cmInfo.getInteractionInfo();
		CMConfigurationInfo confInfo = cmInfo.getConfigurationInfo();
		CMChannelInfo<Integer> blockSCInfo = null;
		SocketChannel defaultBlockSC = null;
		boolean bReturn = false;
		
		String strReceiver = fe.getReceiverName();
		boolean bException = false;
		int nReturnCode = -1;
		
		// find the CMSendFile list of the strReceiver
		sendList = fInfo.getSendFileList(strReceiver);
		if(sendList == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); sending file list "
					+ "not found for the receiver("+strReceiver+")!");
			return;
		}
		
		// find the current sending file task
		sInfo = fInfo.findSendFileInfoOngoing(strReceiver);
		if(sInfo == null)
		{
			System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); ongoing sending task "
					+ "not found for the receiver("+strReceiver+")!");
			fInfo.removeSendFileList(strReceiver);
			return;
		}
		
		// request for canceling the sending task
		sendTask = sInfo.getSendTaskResult();
		sendTask.cancel(true);
		// wait for the thread cancellation
		try {
			sendTask.get(10L, TimeUnit.SECONDS);
		} catch(CancellationException e) {
			System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); the sending task cancelled.: "
					+ "receiver("+strReceiver+"), file("+sInfo.getFileName()+"), file size("+sInfo.getFileSize()
					+ "), sent size("+sInfo.getSentSize()+")");
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bException = true;
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bException = true;
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			bException = true;
		} finally {
			if(bException)
				nReturnCode = 0;
			else
				nReturnCode = 1;
		}

		// remove the sending file list of the receiver
		fInfo.removeSendFileList(strReceiver);

		// send the cancel ack event to the receiver
		feAck = new CMFileEvent();
		feAck.setID(CMFileEvent.CANCEL_FILE_RECV_CHAN_ACK);
		feAck.setSenderName(interInfo.getMyself().getName());
		feAck.setReceiverName(strReceiver);
		feAck.setReturnCode(nReturnCode);
		CMEventManager.unicastEvent(feAck, strReceiver, cmInfo);

		//////////////////// the management of the closed default blocking socket channel
		// get the default blocking socket channel
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			blockSCInfo = interInfo.getDefaultServerInfo().getBlockSocketChannelInfo();
			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); # blocking socket channel: "
						+ blockSCInfo.getSize());
			}
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) blockSCInfo.findChannel(0);	// default blocking channel
				
		}
		else	// server
		{
			CMUser sender = interInfo.getLoginUsers().findMember(strReceiver);
			blockSCInfo = sender.getBlockSocketChannelInfo();
			// get the default blocking socket channel
			defaultBlockSC = (SocketChannel) sender.getBlockSocketChannelInfo().findChannel(0);

			if(CMInfo._CM_DEBUG)
			{
				System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(); # blocking socket channel: "
						+ blockSCInfo.getSize());
			}

		}

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
				// TODO Auto-generated catch block
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
		if(confInfo.getSystemType().equals("CLIENT"))
		{
			CMServer serverInfo = interInfo.getDefaultServerInfo();
			try {
				defaultBlockSC = (SocketChannel) CMCommManager.openBlockChannel(CMInfo.CM_SOCKET_CHANNEL, 
						serverInfo.getServerAddress(), serverInfo.getServerPort(), cmInfo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
			
			if(defaultBlockSC == null)
			{
				System.err.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN(), recreation of "
						+ "the blocking socket channel failed!: server("+serverInfo.getServerAddress()+"), port("
						+ serverInfo.getServerPort() +")");
				return;
			}
			bReturn = blockSCInfo.addChannel(0, defaultBlockSC);

			if(bReturn)
			{
				CMSessionEvent se = new CMSessionEvent();
				se.setID(CMSessionEvent.ADD_BLOCK_SOCKET_CHANNEL);
				se.setChannelName(interInfo.getMyself().getName());
				se.setChannelNum(0);
				bReturn = CMEventManager.unicastEvent(se, serverInfo.getServerName(), CMInfo.CM_STREAM, 0, true, cmInfo);
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
		
		return;	
	}
	
	private static void processCANCEL_FILE_RECV_CHAN_ACK(CMFileEvent fe, CMInfo cmInfo)
	{
		if(CMInfo._CM_DEBUG)
		{
			System.out.println("CMFileTransferManager.processCANCEL_FILE_RECV_CHAN_ACK(); sender("+fe.getSenderName()
				+"), receiver("+fe.getReceiverName()+"), return code("+fe.getReturnCode()+").");
		}
		return;
	}
	
}
