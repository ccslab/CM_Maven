package kr.ac.konkuk.ccslab.cm;

public class CMQuat {
	public	float m_w, m_x, m_y, m_z;

	public CMQuat()
	{
		m_w = 0.0f;
		m_x = 0.0f;
		m_y = 0.0f;
		m_z = 0.0f;
	}

	public CMQuat(float w, float x, float y, float z)
	{
		m_w = w;
		m_x = x;
		m_y = y;
		m_z = z;
	}

	public void setQuat(float w, float x, float y, float z)
	{
		m_w = w;
		m_x = x;
		m_y = y;
		m_z = z;
	}
}
