package lsh;

import java.io.Serializable;

public class HybridQuadLshLeaf implements Serializable {
	
	static final long serialVersionUID = 7885745536157252519L;

    public float latitude;  //y
    public float longitude;  //x
    public IDTrjPoint point;  

    public HybridQuadLshLeaf(float lat, float lon, IDTrjPoint p) {
        latitude = lat;
        longitude = lon;
        point = p;
    }
    
    public HybridQuadLshLeaf() {
    	latitude = 0;
        longitude = 0;
        point = null;
    }
	
}
