package lsh;

import com.bbn.openmap.util.quadtree.*;
import com.bbn.openmap.util.quadtree.MoreMath;

import utility.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

public class HybridQuadLshNode implements Serializable {
	
	static final long serialVersionUID = -6111633198469889444L;

    public final static int NORTHWEST = 0;
    public final static int NORTHEAST = 1;
    public final static int SOUTHEAST = 2;
    public final static int SOUTHWEST = 3;  //此四个参数分别表示四叉树的四个子块
    
    public final static float NO_MIN_SIZE = -1;
    public final static float DEFAULT_MIN_SIZE = 5;//这两个参数设置矩形区域的物理边界

    public Vector<HybridQuadLshLeaf> items;  //叶子节点存储具体的数据，此值非空，则此时children为空
    public HybridQuadLshNode[] children;  //保存孩子节点
    protected int maxItems;   //每个node最多能有的items，超过这个限度此node就会分裂
    protected float minSize;
    public QuadTreeRect bounds;
    
    //--------------------------------------------------------------
    
    
    //--------------------------------------------
    //if children is null,则将items建立lsh索引
    public ArrayList<HashMap<String,ArrayList<IDTrjPoint>>> lshInx;
    
    //--------------------------------------------
    
    
    /**
     * Added to avoid problems when a node is completely filled with a single
     * point value.
     */
    protected boolean allTheSamePoint;
    protected float firstLat;
    protected float firstLon;

    /**
     * Constructor to use if you are going to store the objects in lat/lon
     * space, and there is really no smallest node size.
     * 
     * @param north northern border of node coverage.
     * @param west western border of node coverage.
     * @param south southern border of node coverage.
     * @param east eastern border of node coverage.
     * @param maximumItems number of items to hold in a node before splitting
     *        itself into four children and redispensing the items into them.
     */
    public HybridQuadLshNode(float north, float west, float south, float east, int maximumItems, int hashTableCount) {
        this(north, west, south, east, maximumItems, NO_MIN_SIZE, hashTableCount);
    }

    /**
     * Constructor to use if you are going to store the objects in x/y space,
     * and there is a smallest node size because you don't want the nodes to be
     * smaller than a group of pixels.
     * 
     * @param north northern border of node coverage.
     * @param west western border of node coverage.
     * @param south southern border of node coverage.
     * @param east eastern border of node coverage.
     * @param maximumItems number of items to hold in a node before splitting
     *        itself into four children and redispensing the items into them.
     * @param minimumSize the minimum difference between the boundaries of the
     *        node.
     */
    public HybridQuadLshNode(float north, float west, float south, float east, int maximumItems, float minimumSize,int hashTableCount) {
        bounds = new QuadTreeRect(north, west, south, east);
        maxItems = maximumItems;
        minSize = minimumSize;
        
        items = new Vector<HybridQuadLshLeaf>();
        lshInx = new ArrayList<HashMap<String,ArrayList<IDTrjPoint>>>();
        
        for(int l = 0; l < hashTableCount; l++) {
			
			lshInx.add(new HashMap<String,ArrayList<IDTrjPoint>>());  //每个hash函数对应一个hash表
		}
    }

    /** Return true if the node has children. */
    public boolean hasChildren() {
        return (children != null);
    }

    /**
     * This method splits the node into four children, and disperses the items
     * into the children. The split only happens if the boundary size of the
     * node is larger than the minimum size (if we care). The items in this node
     * are cleared after they are put into the children.
     */
    protected void split(ArrayList<ArrayList<double[]>> a, double b, int hashTableCount, int hashFamilyCount, double w, ArrayList<float[]> coords, ArrayList<float[]> topics) {
        // Make sure we're bigger than the minimum, if we care,
        if (minSize != NO_MIN_SIZE) {
            if (MoreMath.approximately_equal(bounds.north, bounds.south, minSize)
                && MoreMath.approximately_equal(bounds.east, bounds.west, minSize))
                return;
        }

        float nsHalf = (float) (bounds.north - (bounds.north - bounds.south) / 2.0);
        float ewHalf = (float) (bounds.east - (bounds.east - bounds.west) / 2.0);
        children = new HybridQuadLshNode[4];

        children[NORTHWEST] = new HybridQuadLshNode(bounds.north, bounds.west, nsHalf, ewHalf, maxItems, hashTableCount);
        children[NORTHEAST] = new HybridQuadLshNode(bounds.north, ewHalf, nsHalf, bounds.east, maxItems, hashTableCount);
        children[SOUTHEAST] = new HybridQuadLshNode(nsHalf, ewHalf, bounds.south, bounds.east, maxItems, hashTableCount);
        children[SOUTHWEST] = new HybridQuadLshNode(nsHalf, bounds.west, bounds.south, ewHalf, maxItems, hashTableCount);
        
        Vector<HybridQuadLshLeaf> temp = (Vector<HybridQuadLshLeaf>) items.clone();
        items.removeAllElements();
        lshInx.clear();
         
        Enumeration<HybridQuadLshLeaf> things = temp.elements();
        while (things.hasMoreElements()) {
            put(things.nextElement(), a, b, hashTableCount, hashFamilyCount, w, coords, topics);
        }
    }

    /**
     * Get the node that covers a certain lat/lon pair.
     * 
     * @param lat up-down location in QuadTree Grid (latitude, y)
     * @param lon left-right location in QuadTree Grid (longitude, x)
     * @return node if child covers the point, null if the point is out of
     *         range.
     */
    protected HybridQuadLshNode getChild(float lat, float lon) {
        if (bounds.pointWithinBounds(lat, lon)) {
            if (children != null) {
                for (HybridQuadLshNode child : children) {
                    if (child.bounds.pointWithinBounds(lat, lon))
                        return child.getChild(lat, lon);
                }
            } else
                return this; // no children, lat, lon here...
        }
        return null;
    }

    /**
     * Add a object into the tree at a location.
     * 
     * @param lat up-down location in QuadTree Grid (latitude, y)
     * @param lon left-right location in QuadTree Grid (longitude, x)
     * @param obj object to add to the tree.
     * @return true if the pution worked.
     */
    public boolean put(float lat, float lon, IDTrjPoint p, ArrayList<ArrayList<double[]>> a, double b, int hashTableCount, int hashFamilyCount, double w, ArrayList<float[]> coords, ArrayList<float[]> topics) {
        return put(new HybridQuadLshLeaf(lat, lon, p), a, b, hashTableCount, hashFamilyCount, w, coords, topics);
    }

    /**
     * Add a QuadTreeLeaf into the tree at a location.
     * 
     * @param leaf object-location composite0
     * @return true if the pution worked.
     */
    public boolean put(HybridQuadLshLeaf leaf, ArrayList<ArrayList<double[]>> a, double b, int hashTableCount, int hashFamilyCount, double w, ArrayList<float[]> coords, ArrayList<float[]> topics) {
        if (children == null) {
        	
            this.items.addElement(leaf);
            //-----------lsh---------------------
            String key = "";
            for(int l = 0; l < hashTableCount; l++) {
				//key = getKey(topics.get(leaf.point.m_pCountID),l,a,b,hashFamilyCount,w);
				key = getKey_Cosine(topics.get(leaf.point.m_pCountID),a,l,hashFamilyCount);
				
				
				if(this.lshInx.get(l).get(key) == null) {//如果hash表中不存在key对应的项，则增加key，value的项
					ArrayList<IDTrjPoint> tmpArr = new ArrayList<IDTrjPoint>();
					tmpArr.add(leaf.point);
					this.lshInx.get(l).put(key, tmpArr);
				}
				else {//如果hash表中存在hash存在key对应的项，则直接加入轨迹点
					this.lshInx.get(l).get(key).add(leaf.point);
				}
			}
            //-----------------------------------
            
            if (this.items.size() == 1) {
                this.allTheSamePoint = true;
                this.firstLat = leaf.latitude;
                this.firstLon = leaf.longitude;
            } else {
                if (this.firstLat != leaf.latitude || this.firstLon != leaf.longitude) {
                    this.allTheSamePoint = false;
                }
            }

            if (this.items.size() > maxItems && !this.allTheSamePoint)
                split(a, b, hashTableCount, hashFamilyCount, w, coords, topics);
            return true;
        } else {
        	HybridQuadLshNode node = getChild(leaf.latitude, leaf.longitude);
            if (node != null) {
                return node.put(leaf,a, b, hashTableCount, hashFamilyCount, w, coords, topics);
            }
        }
        return false;
    }
    
    //----------------------------------------------------------------
    
    private int hashFamily(float[] topics, double[] a, double b, double w) {//topis与a都为Config.dimension维度的向量；此函数计算一个hash函数的值
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

	private String getKey(float[] topics,int l,ArrayList<ArrayList<double[]>> a, double b, int hashFamilyCount, double w) {//l为第l个hash table
		
		String result = "";//储存第l个hash table的M个hash functions对应的M个hash结果
		
		for(int i = 0; i < hashFamilyCount; i++) {
			int hashResult = hashFamily(topics,a.get(l).get(i),b,w);
			result += hashResult;
		}
		return result; //返回hash结果
	}

    private String getKey_Cosine(float[] topics, ArrayList<ArrayList<double[]>> a, int l, int hashFamilyCount){//l为第l个hash table

        String result = "";//存储最后的hash结果

        for(int i=0; i<hashFamilyCount; i++){
            int hashResult = hashFamily_Cosine(topics, a.get(l).get(i));
            result += hashResult;
        }
        return result;
    }
	//--------------------------------------------------------------------------
	
	//=========================================================================================================================
    
    

    /**
     * Remove a object out of the tree at a location.
     * 
     * @param lat up-down location in QuadTree Grid (latitude, y)
     * @param lon left-right location in QuadTree Grid (longitude, x)
     * @return the object removed, null if the object not found.
     */
    public IDTrjPoint remove(float lat, float lon, IDTrjPoint p) {
        return remove(new HybridQuadLshLeaf(lat, lon, p));
    }

    /**
     * Remove a QuadTreeLeaf out of the tree at a location.
     * 
     * @param leaf object-location composite
     * @return the object removed, null if the object not found.
     */
    public IDTrjPoint remove(HybridQuadLshLeaf leaf) {
        if (children == null) {
            // This must be the node that has it...
            for (int i = 0; i < items.size(); i++) {
            	HybridQuadLshLeaf qtl = (HybridQuadLshLeaf) items.elementAt(i);
                if (leaf.point == qtl.point) {
                    items.removeElementAt(i);
                    return qtl.point;
                }
            }
        } else {
        	HybridQuadLshNode node = getChild(leaf.latitude, leaf.longitude);
            if (node != null) {
                return node.remove(leaf);
            }
        }
        return null;
    }

    /** Clear the tree below this node. */
    public void clear() {
        this.items.removeAllElements();
        if (children != null) {
            for (HybridQuadLshNode child : children) {
                child.clear();
            }
            children = null;
        }
    }

    /**
     * Get an object closest to a lat/lon.
     * 
     * @param lat up-down location in QuadTree Grid (latitude, y)
     * @param lon left-right location in QuadTree Grid (longitude, x)
     * @return the object that matches the best distance, null if no object was
     *         found.
     */
    public IDTrjPoint get(float lat, float lon) {
        return get(lat, lon, Double.POSITIVE_INFINITY);
    }

    /**
     * Get an object closest to a lat/lon. If there are children at this node,
     * then the children are searched. The children are checked first, to see if
     * they are closer than the best distance already found. If a closer object
     * is found, bestDistance will be updated with a new Double object that has
     * the new distance.
     * 
     * @param lat up-down location in QuadTree Grid (latitude, y)
     * @param lon left-right location in QuadTree Grid (longitude, x)
     * @param withinDistance maximum get distance.
     * @return the object that matches the best distance, null if no closer
     *         object was found.
     */
    public IDTrjPoint get(float lat, float lon, double withinDistance) {
        return get(lat, lon, new MutableDistance(withinDistance));
    }

    /**
     * Get an object closest to a lat/lon. If there are children at this node,
     * then the children are searched. The children are checked first, to see if
     * they are closer than the best distance already found. If a closer object
     * is found, bestDistance will be updated with a new Double object that has
     * the new distance.
     * 
     * @param lat up-down location in QuadTree Grid (latitude, y)
     * @param lon left-right location in QuadTree Grid (longitude, x)
     * @param bestDistance the closest distance of the object found so far.
     * @return the object that matches the best distance, null if no closer
     *         object was found.
     */
    public IDTrjPoint get(double lat, double lon, MutableDistance bestDistance) {
    	IDTrjPoint closest = null;
        if (children == null) // This must be the node that has it...
        {
            for (HybridQuadLshLeaf qtl : items) {
                double dx = lon - qtl.longitude;
                double dy = lat - qtl.latitude;
                double distanceSqr = dx * dx + dy * dy;

                if (distanceSqr < bestDistance.value) {
                    bestDistance.value = distanceSqr;
                    closest = qtl.point;
                }
            }
            return closest;
        } else {
            // Check the distance of the bounds of the children,
            // versus the bestDistance. If there is a boundary that
            // is closer, then it is possible that another node has an
            // object that is closer.
            for (HybridQuadLshNode child : children) {
                double childDistance = child.bounds.borderDistanceSqr(lat, lon);
                if (childDistance < bestDistance.value) {
                	IDTrjPoint test = child.get(lat, lon, bestDistance);
                    if (test != null)
                        closest = test;
                }
            }
        }
        return closest;
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
    public Vector<?> get(float north, float west, float south, float east) {
        return get(new QuadTreeRect(north, west, south, east), new Vector<IDTrjPoint>());
    }

    /**
     * Get all the objects within a bounding box.
     * 
     * @param north top location in QuadTree Grid (latitude, y)
     * @param west left location in QuadTree Grid (longitude, x)
     * @param south lower location in QuadTree Grid (latitude, y)
     * @param east right location in QuadTree Grid (longitude, x)
     * @param vector current vector of objects.
     * @return Vector of objects.
     */
    public Vector get(float north, float west, float south, float east, Vector vector) {
        return get(new QuadTreeRect(north, west, south, east), vector);
    }

    /**
     * Get all the objects within a bounding box.
     * 
     * @param rect boundary of area to fill.  
     * @param vector current vector of objects.
     * @return updated Vector of objects.
     */
    public Vector get(QuadTreeRect rect, Vector vector) {
        if (children == null) {
            for (HybridQuadLshLeaf qtl : this.items) {
                if (rect.pointWithinBounds(qtl.latitude, qtl.longitude)) {
                    vector.add(qtl.point);
                }
            }
        } else {
            for (HybridQuadLshNode child : children) {
                if (child.bounds.within(rect)) {
                    child.get(rect, vector);
                }
            }
        }
        return vector;
    }

}
