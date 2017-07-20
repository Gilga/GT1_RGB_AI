package grid;

import java.util.ArrayList;
import java.util.PriorityQueue;

//based on: https://www.youtube.com/watch?v=KNXfSOx4eEE
public class GridGraph 
{
	private PriorityQueue<Node> openNodes;
	private ArrayList<Node> closedNodes;
	
	class Node implements Comparable<Node>
	{
		int heuristicValue; //distance to target node - ignoring obstacles
		int movementCost; //cost for moving to next mode
		int totalCost; //heuristicValue + movementCost
		
		Node parent; //tile from which this tile was reached during A*
		public boolean closed;
		
		@Override
		public int compareTo(Node other) 
		{
			if (this.totalCost < other.totalCost) 
			{
				return -1;
			} 
			else if (this.totalCost > other.totalCost) 
			{
				return 1;
			}
			return 0;
		}
		
	}
}
