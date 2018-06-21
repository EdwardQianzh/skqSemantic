package lsh;

public class TmpNode {
	
	public int queryID;
	
	public double dis; //记录node或者leaf到query点的距离
	public HybridQuadLshNode tmpNode; //用于临时保存四叉树的非叶子节点

}
