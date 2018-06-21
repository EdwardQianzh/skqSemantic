package lsh;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import utility.*;

import com.bbn.openmap.util.quadtree.MoreMath;
import com.bbn.openmap.util.quadtree.DataOrganizer;

/**
* The QuadTree lets you organize objects in a grid, that redefines itself and
* focuses more grid when more objects appear in a certain area.
*/

public class HybridQuadLsh implements DataOrganizer, Serializable {
	
	static final long serialVersionUID = -7707825592455579873L;
	
	protected HybridQuadLshNode top;  //四叉树的根节点
	
	protected ArrayList<float[]> coords; //保存所有点的坐标
	protected ArrayList<float[]> topics;
	
	//===========LSH参数==================
	
	private ArrayList<ArrayList<double[]>> a;  //LSH的a
	private double b;   //LSH的b，为0~w之间的随机数+
	
	private int hashTableCount;  //hash表的数量,L
	private int hashFamilyCount;  //每个hash表对应的hash函数的数量, M
	private double w;   //直线上分段的段长，即LSH的w
	
	//=================================
	
	 public HybridQuadLsh(float north,float west,float south,float east,int maxItems,float minSize,
			 int L,int M,double W,ArrayList<ArrayList<double[]>> A,double B,ArrayList<float[]> c,ArrayList<float[]> t) {
		 
	     hashTableCount = L;
	     hashFamilyCount = M;
	     w = W;
	     
	     a = A;
	     b = B;
	     
	     coords = c;
		 topics = t;
	     
	     top = new HybridQuadLshNode(north, west, south, east, maxItems, minSize,hashTableCount);
	 }
	 /**
	  * Add a object into the tree at a location.
	  * 
	  * @param lat up-down location in QuadTree Grid (latitude, y)
	  * @param lon left-right location in QuadTree Grid (longitude, x)
	  * @return true if the insertion worked.
	  */
	 public boolean put(float lat, float lon, Object obj) {//此函数用于建立索引
			
	     return top.put(lat, lon, (IDTrjPoint)obj, a, b, hashTableCount, hashFamilyCount, w, coords, topics);
	 }
	 

	 /**
	  * Remove a object out of the tree at a location.
	  * 
	  * @param lat up-down location in QuadTree Grid (latitude, y)
	  * @param lon left-right location in QuadTree Grid (longitude, x)
	  * @return the object removed, null if the object not found.
	  */
	 public Object remove(float lat, float lon, Object obj) {
	     return top.remove(lat, lon, (IDTrjPoint)obj);
	 }

	 /** Clear the tree. */
	 public void clear() {
	     top.clear();
	 }
	 
	 public HybridQuadLshNode getRoot() {
		 return top;
	 }

	 /**
	  * Get an object closest to a lat/lon.
	  * 
	  * @param lat up-down location in QuadTree Grid (latitude, y)
	  * @param lon left-right location in QuadTree Grid (longitude, x)
	  * @return the object that was found.
	  */
	 public IDTrjPoint get(float lat, float lon) {
	     return top.get(lat, lon);
	 }

	 /**
	  * Get an object closest to a lat/lon, within a maximum distance.
	  * 
	  * @param lat up-down location in QuadTree Grid (latitude, y)
	  * @param lon left-right location in QuadTree Grid (longitude, x)
	  * @param withinDistance the maximum distance to get a hit, in decimal
	  *        degrees.
	  * @return the object that was found, null if nothing is within the maximum
	  *         distance.
	  */
	 public IDTrjPoint get(float lat, float lon, double withinDistance) {
	     return top.get(lat, lon, withinDistance);
	 }

	 /**
	  * Get all the objects within a bounding box.
	  * 
	  * @param north top location in QuadTree Grid (latitude, y)
	  * @param west left location in QuadTree Grid (longitude, x)
	  * @param south lower location in QuadTree Grid (latitude, y)
	  * @param east right location in QuadTree Grid (longitude, x)
	  * @return Vector of objects.
	  */
	 public Vector get(float north, float west, float south, float east) {
	     return get(north, west, south, east, new Vector());
	 }

	 /**
	  * Get all the objects within a bounding box, and return the objects within
	  * a given Vector.
	  * 
	  * @param north top location in QuadTree Grid (latitude, y)
	  * @param west left location in QuadTree Grid (longitude, x)
	  * @param south lower location in QuadTree Grid (latitude, y)
	  * @param east right location in QuadTree Grid (longitude, x)
	  * @param vector a vector to add objects to.
	  * @return Vector of objects.
	  */
	 public Vector get(float north, float west, float south, float east, Vector vector) {

	     if (vector == null) {
	         vector = new Vector<Object>();
	     }
	     // crossing the dateline, right?? Or at least containing the
	     // entire earth. Might be trouble for VERY LARGE scales. The
	     // last check is for micro-errors that happen to lon points
	     // where there might be a smudge overlap for very small
	     // scales.
	     if (west > east || MoreMath.approximately_equal(west, east, .001)) {
	         return top.get(north, west, south, 180, top.get(north, -180, south, east, vector));
	         
	     } else
	         return top.get(north, west, south, east, vector);
	 }

}
