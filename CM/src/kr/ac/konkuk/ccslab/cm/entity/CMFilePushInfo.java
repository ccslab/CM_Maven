package kr.ac.konkuk.ccslab.cm.entity;
import java.io.*;

// used by a receiver

public class CMFilePushInfo {
	public String m_strFileName;
	public long m_lFileSize;
	public long m_lRecvSize;
	public FileOutputStream m_fos;
	public int m_nContentID;
	
	public CMFilePushInfo()
	{
		m_strFileName = null;
		m_lFileSize = -1;
		m_lRecvSize = -1;
		m_fos = null;
		m_nContentID = -1;
	}
}
