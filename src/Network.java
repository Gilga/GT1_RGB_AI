import lenz.htw.zpifub.net.NetworkClient;

public class Network {
	
	NetworkClient networkClient =  null;
	Network(NetworkClient networkClient){ this.networkClient=networkClient; }
	
    int getBoard(Position pos){
    	Position.Int p = pos.toInt();
    	if(!networkClient.isWalkable(p.x, p.y)) return 0x0;
    	return 0xFF000000 | networkClient.getBoard(p.x, p.y);
    }
    
    void setBoard(Board board, Position pos, int color){
    	Position.Int p = pos.toInt();
    	if(!networkClient.isWalkable(p.x, p.y)) return;
    	board.fields[p.y][p.x] = color;
     }
    
    void clearBoard(Board board) {
    	Position p = new Position();
    	for(p.y=0;p.y<board.Height;p.y++){
        	for(p.x=0;p.x<board.Width;p.x++){
        		int rgb = getBoard(p);
        		if(!(rgb == 0x0 || rgb == 0xFFFFFFFF))
        			setBoard(board, p, 0xFFFFFFFF);
        	}
    	}
    }
    
    boolean networkClient_boardHasWhite(Board board) {
    	Position p = new Position();
    	for(p.y=0;p.y<board.Height;p.y++){
        	for(p.x=0;p.x<board.Width;p.x++){
        		if(getBoard(p) == 0xFFFFFFFF)
        			return true;
        	}
    	}
    	return false;
    }
}
