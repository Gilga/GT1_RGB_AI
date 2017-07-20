package grid;

import lenz.htw.kipifub.net.*;

public class Grid 
{
	private NetworkClient networkClient;
	private static int tileSize = 16; //power of 2 for correct splitting (1024x1024 board area)
	private boolean[][] board; //
	
	public void registerNetworkClient (NetworkClient networkClient)
	{
		this.networkClient = networkClient;
	}
	
	public void initializeBoard () 
	{
		int boardSize = 1024 / tileSize;
		board = new boolean[boardSize][boardSize];
		
		//update whole board
		for (int y = 0; y < boardSize; y++)
		{
			for (int x = 0; x < boardSize; x++)
			{
				board[x][y] = getIsTileWalkable(x, y);
				updateTile(x, y);
			}
		}
	}
	
	public void updateBoard (int xPos, int yPos, int bot) 
	{
		int radius = networkClient.getInfluenceRadiusForBot(bot);
		int xFrom = getIndex(xPos - radius);
		int yFrom = getIndex(yPos - radius);
		int xTo = getIndex(xPos + radius);
		int yTo = getIndex(yPos + radius);
		
		//update nearby tiles in a square 
		for (int y = yFrom; y <= yTo; y++)
		{
			for (int x = xFrom; x <= xTo; x++)
			{
				updateTile(x, y);
			}
		}
	}
	
	private boolean getIsTileWalkable (int xIndex, int yIndex)
	{
		for (int y = yIndex * tileSize; y < (yIndex + 1) * tileSize; y++)
		{
			for (int x = xIndex * tileSize; x < (xIndex + 1) * tileSize; x++)
			{
				if (!networkClient.isWalkable(x, y))
					return false;
			}
		}
		return true;
	}
	
	private void updateTile (int xIndex, int yIndex)
	{
		
		networkClient.getBoard(xIndex, yIndex);
		
	}
	
	private static int getIndex (int position) 
	{
		return position / tileSize; // integer division - no remainder
	}
	
	class Node implements Comparable<Node>
	{
		int heuristicValue; //Distance to target node - ignoring obstacles
		int movementCost; //Cost for moving to next mode
		int totalCost; //heuristicValue + movementCost
		
		Node parent; //tile from which this tile was reached during A*
		public boolean closed;

		
		@Override
		public int compareTo(Node arg0) {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
}