package kr.ac.konkuk.ccslab.cm.thread;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.util.UUID;

import kr.ac.konkuk.ccslab.cm.entity.CMMessage;
import kr.ac.konkuk.ccslab.cm.entity.CMSendFileInfo;
import kr.ac.konkuk.ccslab.cm.event.CMBlockingEventQueue;
import kr.ac.konkuk.ccslab.cm.event.CMFileEvent;
import kr.ac.konkuk.ccslab.cm.info.CMCommInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInfo;
import kr.ac.konkuk.ccslab.cm.info.CMInteractionInfo;
//import kr.ac.konkuk.ccslab.cm.manager.CMCommManager;
import kr.ac.konkuk.ccslab.cm.manager.CMEventManager;
import kr.ac.konkuk.ccslab.cm.manager.CMFileTransferManager;

public class CMSendFileTask implements Runnable {

	CMSendFileInfo m_sendFileInfo;
	CMBlockingEventQueue m_sendQueue;

	public CMSendFileTask(CMSendFileInfo sendFileInfo)
	{
		m_sendFileInfo = sendFileInfo;
		m_sendQueue = CMCommInfo.getInstance().getSendBlockingEventQueue();
	}
	
	@Override
	public void run() {
		RandomAccessFile raf = null;
		FileChannel fc = null;
		long lSentSize = m_sendFileInfo.getSentSize();
		long lFileSize = m_sendFileInfo.getFileSize();
		SocketChannel sendSC = m_sendFileInfo.getSendChannel();
		int nReadBytes = -1;
		int nSendBytes = -1;
		int nSendBytesSum = -1;
		ByteBuffer buf = ByteBuffer.allocateDirect(CMInfo.FILE_BLOCK_LEN);
		CMFileEvent fe = null;
		boolean bInterrupted = false;
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();

		// open the file
		try {
			raf = new RandomAccessFile(m_sendFileInfo.getFilePath(), "rw");
			fc = raf.getChannel();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			sendErrorToProcThread();
			return;
		}
		
		// skip file offset by the previously sent size
		if(lSentSize > 0)
		{
			try {
				//raf.seek(lRecvSize);
				fc.position(lSentSize);
			} catch (IOException e) {
				e.printStackTrace();
				closeRandomAccessFile(raf);
				sendErrorToProcThread();
				return;
			}
		}

		// main loop for receiving and writing file blocks
		nSendBytes = 0;
		while( lSentSize < lFileSize && !bInterrupted)
		{
			// check for interrupt by other thread
			if(Thread.currentThread().isInterrupted())
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMSendFileTask.run(); interrupted at the outer loop! file name("
							+m_sendFileInfo.getFileName()+"), file size("+lFileSize+"), sent size("+lSentSize+").");
				}

				bInterrupted = true;
				continue;
			}
			
			// initialize the ByteBuffer
			buf.clear();
			
			// read a file block
			try {
				nReadBytes = fc.read(buf);
			} catch (IOException e1) {
				e1.printStackTrace();
				closeRandomAccessFile(raf);
				sendErrorToProcThread();
				return;
			}
			
			// send a file block
			buf.flip();
			nSendBytesSum = 0;
			while(nSendBytesSum < nReadBytes && !bInterrupted)
			{
				if(Thread.currentThread().isInterrupted())
				{
					if(CMInfo._CM_DEBUG)
					{
						System.out.println("CMSendFileTask.run(); interrupted at the inner loop! file name("
								+m_sendFileInfo.getFileName()+"), file size("+lFileSize+"), sent size("+lSentSize+").");
					}
					bInterrupted = true;
					continue;
				}
				
				try {
					nSendBytes = sendSC.write(buf);
					
					// update the size of read and sent file blocks
					lSentSize += nSendBytes;
					m_sendFileInfo.setSentSize(lSentSize);
					nSendBytesSum += nSendBytes;
				} catch (IOException e) {
					e.printStackTrace();
					closeRandomAccessFile(raf);
					try {
						sendSC.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
					
					sendErrorToProcThread();
					return;
				}
			} // inner while loop
			
		} // outer while loop

		//if(!bInterrupted)
		if(lSentSize >= lFileSize)
		{
			if(lSentSize > lFileSize)
			{
				System.err.println("CMSendFileTask.run(); the receiver("+m_sendFileInfo.getFileReceiver()
						+"), receiver uuid("+m_sendFileInfo.getFileReceiverUuid()+") already has "
						+ "a bigger size file("+m_sendFileInfo.getFileName()+"); sender size("+lFileSize
						+ "), receiver size("+lSentSize+")");
			}
			
			// send END_FILE_TRANSFER_CHAN with the default TCP socket channel
			fe = new CMFileEvent();
			fe.setID(CMFileEvent.END_FILE_TRANSFER_CHAN);			
			fe.setFileSender(m_sendFileInfo.getFileSender());
			fe.setFileSenderUuid(m_sendFileInfo.getFileSenderUuid());
			fe.setFileReceiver(m_sendFileInfo.getFileReceiver());
			fe.setFileReceiverUuid(m_sendFileInfo.getFileReceiverUuid());
			fe.setFileName(m_sendFileInfo.getFileName());
			fe.setFileSize(m_sendFileInfo.getFileSize());
			fe.setContentID(m_sendFileInfo.getContentID());

			if(CMFileTransferManager.isP2PFileTransfer(fe))
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMSendFileTask.run(), isP2PFileTransfer() "
							+ "returns true.");
				}
				// Sender/Receiver is set by unicastEvent if not specified
				// set event sender and receiver
				String strDefServer = interInfo.getDefaultServerInfo().getServerName();
				// set distribution fields
				fe.setDistributionSession("CM_ONE_USER");
				fe.setDistributionGroup(m_sendFileInfo.getFileReceiver());
				fe.setDistributionUuid(m_sendFileInfo.getFileReceiverUuid());

				// Target is the default server
				CMEventManager.unicastEvent(fe, strDefServer);
			}
			else
			{
				if(CMInfo._CM_DEBUG)
				{
					System.out.println("CMSendFileTask.run(), isP2PFileTransfer() "
							+ "returns false.");
				}
				// Sender/Receiver is set by unicastEvent if not specified
				// Use the method that accepts UUID to support multi-device delivery if needed (or specific target)
				CMEventManager.unicastEvent(fe, m_sendFileInfo.getFileReceiver(), m_sendFileInfo.getFileReceiverUuid());
			}
			
			/*CMMessage msg = new CMMessage(CMEventManager.marshallEvent(fe), m_sendFileInfo.getDefaultChannel());
			m_sendQueue.push(msg);*/
		}

		closeRandomAccessFile(raf);
	}
	
	private void closeRandomAccessFile(RandomAccessFile raf)
	{
		try {
			raf.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendErrorToProcThread()
	{
		CMInteractionInfo interInfo = CMInteractionInfo.getInstance();
		String strMyName = interInfo.getMyself().getName();  // added
		UUID myUuid = interInfo.getMyself().getUuid();  // added

		CMFileEvent fe = new CMFileEvent();
		fe.setID(CMFileEvent.ERR_SEND_FILE_CHAN);
		fe.setSender(strMyName);  // added
		fe.setSenderUuid(myUuid);  // added
		fe.setReceiver(strMyName);  // added
		fe.setReceiverUuid(myUuid);  // added
		fe.setFileSender(strMyName);
		fe.setFileSenderUuid(myUuid); // added
		fe.setFileReceiver(m_sendFileInfo.getFileReceiver());
		fe.setFileReceiverUuid(m_sendFileInfo.getFileReceiverUuid()); // added
		fe.setFileName(m_sendFileInfo.getFileName());
		fe.setContentID(m_sendFileInfo.getContentID());
		ByteBuffer byteBuf = CMEventManager.marshallEvent(fe);
		
		CMBlockingEventQueue recvQueue = CMCommInfo.getInstance().getRecvBlockingEventQueue();
		recvQueue.push(new CMMessage(byteBuf, null));
	}

}
