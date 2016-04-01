package kr.ac.konkuk.ccslab.cm;

public class CMPosition {
	public CMPoint3f m_p;
	public CMQuat m_q;
	
	public CMPosition()
	{
		m_p = new CMPoint3f();
		m_q = new CMQuat();
	}
}
