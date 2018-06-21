package lsh;

public class IDTrjPoint implements Cloneable{

	public int m_trjID;   //trajectory ID
	public int m_pointID;   //point ID
	
	public int m_pCountID;  //trajectory point coordinates and topics in the array list

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + m_pointID;
		result = prime * result + m_trjID;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IDTrjPoint other = (IDTrjPoint) obj;
		if (m_pointID != other.m_pointID)
			return false;
		if (m_trjID != other.m_trjID)
			return false;
		return true;
	}
}
