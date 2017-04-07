package kr.ac.konkuk.ccslab.cm.entity;

public class CMPoint3f {
	public float m_x, m_y, m_z;
	
	public CMPoint3f()
	{
		m_x = 0.0f;
		m_y = 0.0f;
		m_z = 0.0f;
	}
	
	public CMPoint3f(float x, float y, float z)
	{
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	public void setPoint(float x, float y, float z)
	{
		m_x = x;
		m_y = y;
		m_z = z;
	}
	
	public float distance(CMPoint3f p1)
	{
		double sq;
		float rt;
		sq = Math.pow((double)m_x-(double)(p1.m_x), 2) + Math.pow((double)m_y-(double)(p1.m_y), 2) + Math.pow((double)m_z-(double)(p1.m_z), 2);
		rt = (float)(Math.sqrt(sq));
		
		return rt;
	}
	
	public float distanceSquared(CMPoint3f p1)
	{
		double sq;
		sq = Math.pow((double)m_x-(double)(p1.m_x), 2) + Math.pow((double)m_y-(double)(p1.m_y), 2) + Math.pow((double)m_z-(double)(p1.m_z), 2);
		
		return (float)sq;
	}
}
