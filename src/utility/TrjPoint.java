package utility;

/**
 * Created by Administrator on 2016/3/24.
 */
public class TrjPoint {
    public int m_trjID;
    public int m_pointID;
    public float[] m_pCoordinate;
    public float[] m_pTopics;
    /*public TrjPoint(double[] coord, double[] topDis){
        this.m_pCoordinate = coord;
        this.m_pTopics = topDis;
    }*/
    public TrjPoint(){
        this.m_pCoordinate = new float[2];
        this.m_pTopics = new float[Config.dimention];
    }
}
