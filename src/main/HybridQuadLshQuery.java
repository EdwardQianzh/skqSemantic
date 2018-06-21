package main;

import java.io.*;
import java.util.*;

import utility.*;
import lsh.*;

public class HybridQuadLshQuery {
	
	int pCount = 0;//记录读入轨迹点的个数
	
	//------------lsh参数设置--------------------
	private ArrayList<ArrayList<double[]>> a = new ArrayList<ArrayList<double[]>>();  //LSH的a
	private double b;   //LSH的b，为0~w之间的随机数
	
	
	//---------------------------------------------------

	public static void main(String args[]) {
		
		int L = Integer.parseInt(args[3]);  //hash表的数量,L
		int M = Integer.parseInt(args[4]);  //每个hash表对应的hash函数的数量, M
		double w = Double.parseDouble(args[5]);   //直线上分段的段长，即LSH的w 
		
		HybridQuadLshQuery myQuery = new HybridQuadLshQuery();
		
		//建立QuadTree and LSH hybrid index
		HybridQuadLsh hybridTree;
		hybridTree = myQuery.createIndex(args[0],L,M,w);
		System.out.println("Index end!");
		
		ArrayList<QueryPoint> queryPoints = new ArrayList<QueryPoint>();
		Config.readQueryPoints(queryPoints,args[1]);  //读取索引点
		
		//---------query----------- x 
		long current = System.currentTimeMillis();
		System.out.println();
		ResultPoint[] result = myQuery.searchTopkTrjs(queryPoints, hybridTree, Config.numK,L,M,w,args[0]);
		double duration = System.currentTimeMillis()-current;
		System.out.println();
		System.out.println("Search end!");
		System.out.println();
		System.out.println("Search time: " + duration);
        
        Config.writeResult(args[2], result, duration); //向文件输出结果
		
		hybridTree.clear();
	}

	//===============================create index==============================================================
	//建立索引
	public HybridQuadLsh createIndex(String file,int L,int M,double w) {
		
		ArrayList<IDTrjPoint> allTrjPoints = new ArrayList<IDTrjPoint>();  //保存所有轨迹点
		ArrayList<float[]> coords = new ArrayList<float[]>(); //保存所有点的坐标
		ArrayList<float[]> topics = new ArrayList<float[]>();  //保存所有点的topic distribution
		
		//读取所有的轨迹点到allTrjPoints中
		readAllTrjs(file,allTrjPoints,coords,topics);
		
		setVarA(L,M);
		Random ran = new Random(); //随机数b
	    b = ran.nextDouble()*w;
		
		HybridQuadLsh hybridTree;
		hybridTree = new HybridQuadLsh(Config.north,Config.west,Config.south,Config.east,Config.hybridMaxItems,Config.minSize,L,M,w,a,b,coords,topics);
		
		for(IDTrjPoint tmp : allTrjPoints) {
			hybridTree.put(coords.get(tmp.m_pCountID)[1], coords.get(tmp.m_pCountID)[0], tmp);
		}
		
		return hybridTree;
	}
	
	private void setVarA(int L,int M) {//set the variable a of hash function       
		Random normalRandom = new Random();
		double tmp = 0;
					
		for(int l = 0; l < L; l++) {//L个hash表
			a.add(new ArrayList<double[]>());
						
			for(int m = 0; m < M; m++) {//M个hash函数
				a.get(l).add(new double[Config.dimention]);
							
				for(int k = 0; k < Config.dimention; k++) {//维度
								
					tmp = normalRandom.nextGaussian();
					while(tmp < 0) {
						tmp = normalRandom.nextGaussian();   
					}
					a.get(l).get(m)[k] = tmp;
				}
			}	
		}	
	}
	
	//===============================search==============================================================
	//实现Comparator接口
    Comparator<TmpNode> tnpOrder = new Comparator<TmpNode>() {
    	public int compare(TmpNode n1, TmpNode n2) {
    		double dis1 = n1.dis;
    		double dis2 = n2.dis;
    		if(dis1 > dis2) {
    			return 1;
    		} else if(dis1 < dis2) {
    			return -1;
    		} else {
    			return 0;
    		}
    	}
    };
    
    private int hashFamily(float[] topics, double[] a,double w) {//topis与a都为Config.dimension维度的向量；此函数计算一个hash函数的值
    	int h = 0;
		double tmp = b;
		
		for(int i =  0; i < Config.dimention; i++) {
			tmp += topics[i]*a[i];
		}
		tmp = tmp/w;
		h = (int)tmp;
		return h;
	}

	private int hashFamily_Cosine(float[] topics, double[] a){//采用cosin距离的hash family
		int h=1;
		double tmp=0.0;

		for(int i=0; i<Config.dimention; i++){
			tmp += topics[i]*a[i];
		}
		if(tmp>=0)
			return h;
		else{
			h=0;
			return h;
		}
	}

	private  String getKey(float[] topicDistri,int l,int M,double w) {//l为第l个hash table
		
		String result = "";//储存最后的hash结果
		
		for(int i = 0; i < M; i++) {
			int hashResult = hashFamily(topicDistri,a.get(l).get(i),w);
			result += hashResult;
		}
		return result;
	}

	private String getKey_Cosine(float[] topicDistri, int l, int M){//l为第l个hash table

		String result = "";//存储最后的hash结果

		for(int i=0; i<M; i++){
			int hashResult = hashFamily_Cosine(topicDistri, a.get(l).get(i));
			result += hashResult;
		}
		return result;
	}
    /**
	 *
	 * @param query
	 * @param hybridTree
	 * @param numK
	 * @param L
	 * @param M
	 * @param w
     * @param file
     * @return
     */
    public ResultPoint[] searchTopkTrjs(ArrayList<QueryPoint> query,HybridQuadLsh hybridTree,int numK,int L,int M,double w,String file) {
    	
    	HashSet<Integer> trjsFlag = new HashSet<Integer>(); //keyword：trajectory ID
    	
    	PriorityQueue<TmpNode> tmpNodePQ = new PriorityQueue<TmpNode>(20,tnpOrder);
    	for(int i = 0; i < query.size(); i++) {
    		TmpNode tn = new TmpNode();
    		tn.queryID = i;
    		tn.dis = 0;
    		tn.tmpNode = hybridTree.getRoot();
    		
    		tmpNodePQ.add(tn);
    	}//初始化结点优先队列,每个元素代表一个索引点对应的节点集合 
    	
    	double spaceDis = 0;
    	double sumDis = Double.MAX_VALUE;
    	
    	ResultPoint[] containK = new ResultPoint[numK];
		for(int i = 0; i < numK; i++) {
			containK[i] = new ResultPoint();
		}
    	int countK = 0;
    	
    	int ioCount = 0;  //记录读取的轨迹数目

    	//leafDis:upper bound  nodeDis:lower bound
    	while(true) {
    		
    		TmpNode tmp = tmpNodePQ.poll();  //将最小distance的node或者点取出来
    		if(tmp == null) {
    			break;
    		}
    		
    		spaceDis = query.size()*tmp.dis;
    		//System.out.println(Config.k * spaceDis + " " + sumDis);
    		
    		if(Config.k*spaceDis > sumDis) {//符合terminate条件
    			break;
    		}
    		
    		int idx = tmp.queryID;
    		
    		if(tmp.tmpNode.children == null) {//node don't has children, get point according to the lsh
				
				String key = "";
				HashSet<IDTrjPoint> keySet = new HashSet<IDTrjPoint>();
				for(int l = 0; l < L; l++) {
					
					//key = getKey(query.get(idx).m_pTopics,l,M,w);
					key = getKey_Cosine(query.get(idx).m_pTopics,l,M);

					if(tmp.tmpNode.lshInx.get(l).get(key) == null) {//如果hash表中不存在key对应的项 
						continue;
					}
					else {//如果hash表中存在hash存在key对应的项，则取出
						keySet.addAll(tmp.tmpNode.lshInx.get(l).get(key));
					}
				}
				
				for(IDTrjPoint tmpOTP: keySet) {

	    			if(!trjsFlag.contains(tmpOTP.m_trjID)) {
	    				trjsFlag.add(tmpOTP.m_trjID);

						ioCount++;//io次数
                    	
                    	ArrayList<TrjPoint> tmpTrj = Config.readOneTrj(file + "object" + tmpOTP.m_trjID + ".txt");  //从文件读取轨迹
                    	double mmDis = Config.computeMMDis(query,tmpTrj);
        				countK = Config.sort(containK,mmDis,tmpOTP.m_trjID,countK,numK);
        				sumDis = containK[countK-1].dis;
	    				
	                } 
				}

			} else {//node has children
				for(HybridQuadLshNode child : tmp.tmpNode.children) { //遍历四个孩子节点
					//计算query点到节点的距离
                    double childDistance = child.bounds.borderDistance(query.get(idx).lat, query.get(idx).lon);
                    
                    TmpNode tn = new TmpNode();
            		tn.queryID = idx;
            		tn.dis = childDistance;
            		tn.tmpNode = child;
            		
            		tmpNodePQ.add(tn);
                }
			}		
    	}
    	System.out.println("I/O times: " + ioCount);
    	return containK;
    }
    
	/*------------------------------------IO---------------------------------------------------*/
	//读入所有的轨迹点数据，并将其放入m_TrjPoint中
	public void readAllTrjs(String file,ArrayList<IDTrjPoint> allTrjPoints,ArrayList<float[]> coords,ArrayList<float[]> topics) {
		for(int i = 1; i <= Config.sumTrjs; i++) {
			String filename = new String(file + "object" + i +".txt");
			readOneTrj(filename,allTrjPoints,coords,topics);
		}	
	}
	
	//读入一条轨迹中的轨迹点数据
	public void readOneTrj(String filename,ArrayList<IDTrjPoint> allTrjPoints,ArrayList<float[]> coords,ArrayList<float[]> topics) {
			//定义输入缓冲流，读取文件中的一行
		LineNumberReader lr = null;
		try {
			lr = new LineNumberReader(new FileReader(filename));
				
		} catch (FileNotFoundException e) {
			System.err.println("Cannot open trajectory file " + filename + ".");
			System.exit(-1);
		}
			
		try {
			String line = lr.readLine();	
			while(line != null) {
				//定义新变量
				IDTrjPoint tmp_OneTrjPoint = new IDTrjPoint();
				float[] tmp_coords = new float[2];
				float[] tmp_topics = new float[Config.topics];
				int countTopic = 0;
					
				StringTokenizer st = new StringTokenizer(line);
					
				tmp_OneTrjPoint.m_trjID = new Integer(st.nextToken()).intValue();   //读入轨迹编号
				tmp_OneTrjPoint.m_pointID = new Integer(st.nextToken()).intValue(); //读入轨迹点编号
				tmp_coords[0] = new Float(st.nextToken()).floatValue();  //读入轨迹点的横坐标
				tmp_coords[0] = tmp_coords[0]/1000;
				tmp_coords[1] = new Float(st.nextToken()).floatValue();  //读入轨迹点的纵坐标
				tmp_coords[1] = tmp_coords[1]/1000;
				while(st.hasMoreTokens()) {
					tmp_topics[countTopic++] = new Float(st.nextToken()).floatValue();
				}
				tmp_OneTrjPoint.m_pCountID = pCount++;
					
				allTrjPoints.add(tmp_OneTrjPoint);
				coords.add(tmp_coords);
				topics.add(tmp_topics);
					
				line = lr.readLine();
			}
			lr.close();				
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
}
