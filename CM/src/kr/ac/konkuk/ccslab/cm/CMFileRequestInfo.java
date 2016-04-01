package kr.ac.konkuk.ccslab.cm;
import java.io.*;

//used by a sender

public class CMFileRequestInfo {
	public String m_strUserName;
	public String m_strFilePath;	// a file name including path information
	public long m_lFileSize;
	public int m_nContentID;
	public Thread m_fileThread;
	public FileInputStream m_fis;
	public boolean m_bStarted;
	public boolean m_bSentAll;
	public boolean m_bReceivedAck;
	
	public CMFileRequestInfo()
	{
		m_strUserName = null;
		m_strFilePath = null;
		m_lFileSize = -1;
		m_nContentID = -1;
		m_fileThread = null;
		m_fis = null;
		m_bStarted = false;
		m_bSentAll = false;
		m_bReceivedAck = false;
	}
}
