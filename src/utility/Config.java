package utility;

import java.io.*;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class Config {
	/*总共的数量*/
	public static final int sumTrjs = 350446;
	
	/*一条轨迹的轨迹点数量*/
/*	public static final int sumPointsOfOneTrj = 15;*/
	
	/*总共的轨迹点数量*/
	
/*	public static final int sumPoints = sumTrjs*sumPointsOfOneTrj;*/
	
	/*主题数量*/
	public static final int topics = 20;
	public static final int top_k = 6;  //>0

	//设置聚类最大迭代次数
	public static final int maxIterations = 500;
	public static final int kOfMediods = 6;   //聚类数量，也是B+树的M(阶数)
	
	//设置QuadTree默认属性
	public static final float north = 1;
	public static final float south = 0;
	public static final float west = 0;
	public static final float east = 1;
	public static final int maxItems = 8;//quadtree
	public static final float minSize = 0.0f;
	
	//设置混合索引中每个节点的最大项目数
	public static final int hybridMaxItems = 50000;
	
	
	//设置distance参数
	public static final double k = 0.5;
	
/*	//设置iDistance的index key
	public static final int c = 10;
	*/
	
	//LSH参数设置
	public static final int dimention = Config.topics;  //LSH的维度设置
	
	//设置返回的数量
	public static final int numK = 10;

	//设置的编辑距离阈值
	public static final int tau = 3;
	
	
	//===========================================================================
	//===================================函数========================================
    //得到query点和轨迹点之间的topic distance
    public static double getTopicDis(QueryPoint QP,TrjPoint TP) {
		double cosTop=0, cosDown=1, cosDownLeft=0, cosDownRight=0;//cosine distance between two vectors.
    	for(int i = 0; i < Config.topics; i++) {
			cosTop += TP.m_pTopics[i] * QP.m_pTopics[i];
			cosDownLeft += Math.pow(TP.m_pTopics[i], 2);
			cosDownRight += Math.pow(QP.m_pTopics[i], 2);
    	}
		cosDown = Math.sqrt(cosDownLeft) * Math.sqrt(cosDownRight);
    	double topicDis = cosTop/cosDown;
    	
    	return topicDis;
    }
    //得到query点和轨迹点之间的space distance
    public static double getSpaceDis(QueryPoint QP,TrjPoint TP) {
    	/*double dx = QP.lon - TP.m_pCoordinate[0];
        double dy = QP.lat - TP.m_pCoordinate[1];*/
		double spaceDistance = computeEuclideanDist(QP.lat, QP.lon, TP.m_pCoordinate[1], TP.m_pCoordinate[0]);
        spaceDistance = 2/(1+Math.exp(-1*spaceDistance))-1;
        return spaceDistance;
    }

	//compute the Euclidean distance between two coordinates.
	public static double computeEuclideanDist(double lat1, double lon1, double lat2, double lon2){
		double radLat1 = lat1 * Math.PI / 180;
		double radLat2 = lat2 * Math.PI / 180;
		double a = radLat1 - radLat2;
		double b = lon1 * Math.PI / 180 - lon2 * Math.PI / 180;
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
				+ Math.cos(radLat1) * Math.cos(radLat2)
				* Math.pow(Math.sin(b / 2), 2)));
		s = s * 6378137.0;// 取WGS84标准参考椭球中的地球长半径(单位:m)
		s = Math.round(s * 10000) / 10000;

		return s;
	}

    //得到总距离
    public static double getDis(QueryPoint QP,TrjPoint TP) {
    	
    	
        double spaceDistance = getSpaceDis(QP,TP);;
        
        double topicDistance = getTopicDis(QP,TP);
        
        double distance = Config.k*spaceDistance + (1-Config.k)*topicDistance;
        
        return distance;
    	  
    }
    
    //计算minimum match distance
    public static double computeMMDis(ArrayList<QueryPoint> query, ArrayList<TrjPoint> trj) {
    	double dis=0;
    	for(int i = 0; i < query.size(); i++) {//索引点的个数
    		
    		double minDis = getDis(query.get(i),trj.get(0));
    		
    		for(int j = 1; j < trj.size(); j++) {//轨迹点的个数
    			
    			double tmpDis = getDis(query.get(i),trj.get(j));
    			
    			if(minDis > tmpDis) {
    				minDis = tmpDis;//find minimum point match
    			}
    			
    		}
    		
    		dis += minDis;//compute minimum match distance
    	}
    	return dis;
    }
    
    /*---------------------------------------------------------------------------------------*/
    public static int sort(ResultPoint[] containK,double mmDis,int trjID,int countK,int numK) { //先删除del，再插入tmpDis
    	
    	int count = 0;
    	if(countK < numK) {//containK没有放满
    		while(count < countK && containK[count].dis < mmDis) {
        		count++;
        	}
    		for(int i = countK; i > count; i--) {
    			containK[i].dis = containK[i-1].dis;
    			containK[i].m_trjID = containK[i-1].m_trjID;
				containK[i].m_pCoordinate = containK[i-1].m_pCoordinate;
    		}
    		containK[count].dis = mmDis;
    		containK[count].m_trjID = trjID;
    		countK++;
    		
    	} else {//containK已满
    		
    		while(count < countK && containK[count].dis < mmDis) {
        		count++;
        	}
    		if(count == numK) {
    			return countK;
    		} else {
    			for(int i = numK-1; i > count; i--) {
        			containK[i].dis = containK[i-1].dis;
        			containK[i].m_trjID = containK[i-1].m_trjID;
        		}
    			containK[count].dis = mmDis;
        		containK[count].m_trjID = trjID;
    		}
    		
    	}
    	return countK;
    }

	/*-----------------------------------------------------------------------------------------*/
	//get coordinate from trjID
	public static float[] getCoordBytrjID(int trjID){
		float[] tempCoord = new float[2];
		int ttau = Config.tau + 1;
		try {
			FileReader fr = new FileReader("F:/work/Data_Process/Foursquare/Objects_LA_NYC/object"+trjID+".txt");
			BufferedReader br = new BufferedReader(fr);
			String str = br.readLine();
			String[] s = str.split("	");
			tempCoord[0] = Float.parseFloat(s[2]);
			tempCoord[1] = Float.parseFloat(s[3]);
		}catch(Exception e){
			e.printStackTrace();
		}
		return tempCoord;
	}

    /*-------------------------------------IO--------------------------------------------------*/
    public static ArrayList<TrjPoint> readOneTrj(String filename) {
    	
		//定义输入缓冲流，读取文件中的一行 
		LineNumberReader lr = null;
		try {
			lr = new LineNumberReader(new FileReader(filename));
			
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open trajectory file " + filename + ".");
			System.exit(-1);
		}
		
		ArrayList<TrjPoint> trj = new ArrayList<TrjPoint>();
		try {
			String line = lr.readLine();
			
			int countTopic = 0;
			
			while(line != null) {
				TrjPoint tmp_TrjPoint = new TrjPoint();
				countTopic = 0;
				
				StringTokenizer st = new StringTokenizer(line);
				
				tmp_TrjPoint.m_trjID = new Integer(st.nextToken()).intValue();   //读入轨迹编号
				
				tmp_TrjPoint.m_pointID = new Integer(st.nextToken()).intValue(); //读入轨迹点编号
				
				tmp_TrjPoint.m_pCoordinate[0] = new Float(st.nextToken()).floatValue();  //读入轨迹点的横坐标x
				tmp_TrjPoint.m_pCoordinate[0] = tmp_TrjPoint.m_pCoordinate[0]/1000;
				
				tmp_TrjPoint.m_pCoordinate[1] = new Float(st.nextToken()).floatValue();  //读入轨迹点的纵坐标y
				tmp_TrjPoint.m_pCoordinate[1] = tmp_TrjPoint.m_pCoordinate[1]/1000;
				
				while(st.hasMoreTokens()) {
					tmp_TrjPoint.m_pTopics[countTopic] = new Float(st.nextToken()).floatValue();
					countTopic++;
				}
				
				trj.add(tmp_TrjPoint);
				
				line = lr.readLine();
			}
			lr.close();				
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return trj;
	}
    
  //读取索引点
  	public static void readQueryPoints(ArrayList<QueryPoint> queryPoints,String queryFile) {
  		LineNumberReader lr = null;
  		try {
  			lr = new LineNumberReader(new FileReader(queryFile));
  			
  		} catch (FileNotFoundException e) {
  			System.err.println("Cannot open trajectory file " + queryFile + ".");
  			System.exit(-1);
  		}
  		try {
  			String line = lr.readLine();	
  			while(line != null) {
  				QueryPoint tmp_QueryPoint = new QueryPoint();
  				int countTopic = 0;
  				StringTokenizer st = new StringTokenizer(line);
  				tmp_QueryPoint.lon = new Float(st.nextToken()).floatValue();  //读入轨迹点的横坐标
				/*System.out.println(tmp_QueryPoint.lon);*/
  				tmp_QueryPoint.lon = tmp_QueryPoint.lon/1000;
  				
  				tmp_QueryPoint.lat = new Float(st.nextToken()).floatValue();  //读入轨迹点的纵坐标
				/*System.out.println(tmp_QueryPoint.lat);*/
  				tmp_QueryPoint.lat= tmp_QueryPoint.lat/1000;
  				while(st.hasMoreTokens()) {//读入topic分布
  					tmp_QueryPoint.m_pTopics[countTopic++] = new Float(st.nextToken()).floatValue();
  				}
  				queryPoints.add(tmp_QueryPoint);
  				line = lr.readLine();
  			}
  			lr.close();				
  		} catch (IOException e) {
  			e.printStackTrace();
  		}	
  		
  	}
  	
  //向频幕输出一个轨迹点
  	public static void printTrjPoint(TrjPoint p) {
  		
  		System.out.print(p.m_trjID + " ");
  		System.out.print(p.m_pointID + " ");
  		System.out.print(p.m_pCoordinate[0] + " ");
  		System.out.print(p.m_pCoordinate[1] + " ");
  		
  		for(int j = 0; j < Config.topics; j++) {
  			System.out.print(p.m_pTopics[j] + " ");
  		}
  		
  		System.out.println();
  	}
  	
  	//向频幕输出搜索到的轨迹
  	public static void printResult(ResultPoint[] result) {

  		for(int i = 0; i < result.length; i++) {
  			System.out.print(result[i].dis + " ");
  			System.out.print(result[i].m_trjID + " ");
  			System.out.println();
  		}
  		
  	}
  	
  	public static void writeResult(String filename, ResultPoint[] result, double time) {
  		try {
			File file = new File(filename);
			if(!file.exists()) {
				file.createNewFile();
			}
			FileWriter fw = new FileWriter(file);
			BufferedWriter bw = new BufferedWriter(fw);
			
			for(int i = 0; i < result.length; i++) {
				
				bw.write(result[i].dis + "	");
				bw.write(result[i].m_trjID + "	");
				float[] coord = new float[2];
				coord = getCoordBytrjID(result[i].m_trjID);
				bw.write(String.valueOf(coord[1]));
				bw.write("	");
				bw.write(String.valueOf(coord[0]));
				if(i != (result.length - 1)) {
					bw.newLine(); 
				} 
			}
			
			bw.flush();  
            bw.close();  
            fw.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
  	}
  	
}


