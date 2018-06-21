package main;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;

public class naiveGetTopk {
		public static double lambda = 0.5;
		
	//get the coordinates of all objects.
		public  void getCoords( ArrayList<float[]> coords) throws IOException{
			FileReader fr=null; 
			BufferedReader br=null;
			for(int i=1; i<=144348; i++) {
				fr = new FileReader("F:/work/Data_Process/Foursquare/Objects_LA/object"+(i)+".txt");
				br = new BufferedReader(fr);
				br.readLine();
				String str = br.readLine();
				String[] s = str.split("	");
				float[] tempCoord = new float[2];
				tempCoord[0] = Float.parseFloat(s[0]);
				tempCoord[1] = Float.parseFloat(s[1]);
//				System.out.println(tempCoord[0] +" "+tempCoord[1]+" "+i);
				coords.add(tempCoord);
			}

			for(int i=1; i<=206098; i++) {
				fr = new FileReader("F:/work/Data_Process/Foursquare/Objects_NYC/object"+(i)+".txt");
				br = new BufferedReader(fr);
				br.readLine();
				String str = br.readLine();
				String[] s = str.split("	");
				float[] tempCoord = new float[2];
				tempCoord[0] = Float.parseFloat(s[0]);
				tempCoord[1] = Float.parseFloat(s[1]);
				coords.add(tempCoord);
//				System.out.println(tempCoord[0] +" "+tempCoord[1]+" "+i);
			}
			br.close();
			fr.close();
		}
		
		//get the topic distributions of all objects.
		public  void getTopicDistribution(ArrayList<float[]> topics) throws IOException{
			FileReader fr = new FileReader("F:/work/Data_Process/Foursquare/Topic_Distribution/topicDistribution_LA.txt");
			BufferedReader br = new BufferedReader(fr);
			String str;
//			int count = 0;
			while((str=br.readLine())!=null){
				float[] tempTopics = new float[20];
				String[] s = str.split("	");
				for(int i=0; i<20; i++){
//					System.out.print(s[i]+" ");
					if(s[i].contains("E") || s[i].contains("-")){
						s[i] = s[i].replaceAll("E", "");
						s[i] = s[i].replaceAll("-", "");
					}
					tempTopics[i] = Float.parseFloat(s[i]);
					if(tempTopics[i]>1)
						tempTopics[i] = 0;
//					System.out.print(tempTopics[i]+" ");
				}
//				System.out.println(++count);
//				for(int i=0; i<20; i++)
//					System.out.print(tempTopics[i]+" ");
//				System.out.println();
				topics.add(tempTopics);
//				for(int i=0; i<20; i++)
//					System.out.print(topics.get(count)[i]+" ");
//				System.out.println();
//				count++;
			}
			
			fr = new FileReader("F:/work/Data_Process/Foursquare/Topic_Distribution/topicDistribution_NYC.txt");
			br = new BufferedReader(fr);
//			count  = 0;
			while((str=br.readLine())!=null){
				float[] tempTopics = new float[20];
				String[] s = str.split("	");
				for(int i=0; i<20; i++){
//					System.out.print(s[i]+" ");
					if(s[i].contains("E") || s[i].contains("-")){
						s[i] = s[i].replaceAll("E", "");
						s[i] = s[i].replaceAll("-", "");
					}
					tempTopics[i] = Float.parseFloat(s[i]);
//					System.out.print(tempTopics[i]+" ");
					if(tempTopics[i]>1)
						tempTopics[i] = 0;
				}
//				System.out.println(++count);
//				for(int i=0; i<20; i++)
//					System.out.print(tempTopics[i]+" ");
//				System.out.println();
				topics.add(tempTopics);
			}
			br.close();
			fr.close();
		}
	
		//compute the hybrid distance between query and every object and return all hybrid distance.
		public TreeMap computeHybridDist(float[] queryCoord, float[] queryTopic, ArrayList<float[]> coords, ArrayList<float[]> topics){
			float[] coordDist = new float[coords.size()];
			float[] topicDist = new float[topics.size()];
			TreeMap prioryQueue = new TreeMap<Object, Object>();
			for(int i=0; i<coords.size() && i<topics.size(); i++){
				coordDist[i] = (float) computeEuclideanDist(queryCoord[0], queryCoord[1], coords.get(i)[0], coords.get(i)[1]);
				coordDist[i] = (float) (2/(1+Math.exp(-1*coordDist[i]))-1);
				float cosTop=0, cosDown=1, cosDownLeft=0, cosDownRight=0;//cosine distance between two vectors.
				for(int j=0; j<20; j++){
					cosTop += topics.get(i)[j] * queryTopic[j];
					cosDownLeft += Math.pow(topics.get(i)[j], 2);
					cosDownRight += Math.pow(queryTopic[j], 2);
				}
				cosDown = (float) (Math.sqrt(cosDownLeft) * Math.sqrt(cosDownRight));
				topicDist[i] = cosTop/cosDown;
				float hybridDist = (float) (lambda*coordDist[i]+(1-lambda)*topicDist[i]);
				prioryQueue.put(hybridDist, i);
			}
			return prioryQueue;
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
			s = s * 6378137.0;// 取WGS84标准参考椭球中的地球长半径（单位：m）
			s = Math.round(s * 10000) / 10000;

			return s;
		}
		
		//main function
		public static void main(String[] args)  throws IOException{
			ArrayList<float[]> coords = new ArrayList<float[]>();
			ArrayList<float[]> topics = new ArrayList<float[]>();
			float[] queryCoord;
			float[] queryTopic;
			int knn = 100;
			naiveGetTopk ngt = new naiveGetTopk();
			ngt.getCoords(coords);
		    ngt.getTopicDistribution(topics);
		  /*  System.out.println(coords.size());
		   for(int i=0; i<coords.size(); i++)
		    	System.out.println(coords.get(i)[0]+" "+coords.get(i)[1]+" "+(i+1));
		    System.out.println(topics.size());
			for(int i=0; i<topics.size(); i++){
				for(int j=0; j<20; j++)
					System.out.print(topics.get(i)[j]+" ");
				System.out.println(i+1);
			}*/
		    //===================get knn results==================//
		    long startTime = System.currentTimeMillis();
		    queryCoord = coords.get(0);
		    queryTopic = topics.get(0);
		    System.out.println("Query Coordinate is "+queryCoord[0]+" "+ queryCoord[1]);
			TreeMap results = new TreeMap<Object, Object>();
			results = ngt.computeHybridDist(queryCoord, queryTopic, coords, topics);
		    Iterator it = results.keySet().iterator();
		    int count = 0;
			FileWriter fw = new FileWriter("ideal_Result.txt");
		    System.out.println("Returned Coordinates are :");
		    while(it.hasNext()&&count<knn){
			   int temp = (int)(results.get(it.next()));
			   System.out.println(coords.get(temp)[0]+" "+coords.get(temp)[1]);
				fw.write(String.valueOf(coords.get(temp)[1]));
				fw.write("	");
				fw.write(String.valueOf(coords.get(temp)[0]));
				fw.write("\n");
				fw.flush();
			   count++;
		   }
		   long endTime = System.currentTimeMillis();
		   System.out.println("The whole time of query processing is "+ (endTime - startTime)+" ms.");
	}
}
