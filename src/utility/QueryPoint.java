package utility;


/**
 * Created by Administrator on 2016/2/25.
 */
public class QueryPoint {
	public int qID;
	public float lat;
	public float lon;
	public float[] m_pTopics;
	/*public QueryPoint(double lat, double lon, double[] topDis){
		this.lat = lat;
		this.lon = lon;
		this.m_pTopics = topDis;
	}*/
	public QueryPoint(){
		this.m_pTopics = new float[Config.dimention];
	}
}
