import java.util.ArrayList;
import java.util.List;

import lenz.htw.zpifub.net.NetworkClient;

public class Board {
	
	public int Width = 1024;
    public int Height = 1024;
    public int Size = Width*Height;
    public float HWidth = Width/2;
    public float HHeight = Height/2;
    public float HSize = Size/2;
    public List<ArrayList<Position>> allWayPoints = new ArrayList<ArrayList<Position>>();
    private Screenshot screenshotHandler = null;
    
    public int[][] fields = new int[Width][Height];
    
    public void setScreenshotHandler(Screenshot handler) { screenshotHandler=handler; }
    
    Board(){
    	allWayPoints = new ArrayList<ArrayList<Position>>();
    	for(int bot = 0; bot<3; bot++) allWayPoints.add(new ArrayList<Position>());
    }
    
    
    public void resetWayPoints() {
    	for(int bot = 0; bot<3; bot++) allWayPoints.set(bot, new ArrayList<Position>());
    }
    
    public void set(int[][] fields, int x, int y, int color) {
    	if(inBorder(new Position(x,y),false)) fields[y][x] = color;
    }
    
    public void set(Position p, int color) {
    	if(!inBorder(p,true)) return;
    	Position.Int v = p.toInt();
		fields[v.y][v.x] = color;
    }
    
    public int get(Position p) {
    	if(!inBorder(p,true)) return 0x0;
    	Position.Int v = p.toInt();
		return fields[v.y][v.x];
    }
    
    public boolean inBorder(Position p, boolean error) {
    	boolean r = p.x>=0 && p.y>=0 && p.x<Width && p.y<Height;
    	
    	if(error) {
			try {
				if(!r) throw new RuntimeException(p+" is not in Border!");
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
    	}
		
		return r;
    }
    
    boolean hasValidColor(Position p) {
    	if(!inBorder(p,true)) return false;
    	Position.Int v = p.toInt();
		return fields[v.y][v.x] != 0x0;
    }
    
    boolean isValid(Position p) {
    	if(!inBorder(p,false)) return false;
		return hasValidColor(p);
    }
    
    void clearBoard() {
    	for(int y=0;y<Height;y++){
        	for(int x=0;x<Width;x++){
        		int rgb = fields[y][x];
        		if(!(rgb == 0x0 || rgb == 0xFFFFFFFF))
        			fields[y][x] = 0xFFFFFFFF;
        	}
    	}
    }
    
    boolean hasWhite() {
    	for(int y=0;y<Height;y++){
        	for(int x=0;x<Width;x++){
        		if(fields[y][x] == 0xFFFFFFFF)
        			return true;
        	}
    	}
    	return false;
    }
    
	
    public void drawPowerUps(int[][] showBoard, List<PowerUp> powerUps) {
    	for(PowerUp powerUp : powerUps) {
    		Position.Int pos = powerUp.pos.toInt();
    		int color = powerUp.type == PowerUp.BOMB ? 0xFFFF00FF : (powerUp.type == PowerUp.RAIN ? 0xFF00FFFF : 0xFF000000);
    		
    		set(showBoard, pos.x, pos.y, color);
    		
    		for(int i=1;i<10;i++) {
    			set(showBoard, pos.x, pos.y+i, color);
    			set(showBoard, pos.x, pos.y-i, color);
    			set(showBoard, pos.x+i, pos.y, color);
    			set(showBoard, pos.x-i, pos.y, color);
    			set(showBoard, pos.x+i, pos.y+i, color);
    			set(showBoard, pos.x-i, pos.y-i, color);
    			set(showBoard, pos.x+i, pos.y-i, color);
    			set(showBoard, pos.x-i, pos.y+i, color);
    		}
    	}
    }
    
    public void drawWayPoints(int[][] showBoard) {
    	for(ArrayList<Position>  waypoints : allWayPoints) {
	    	for(Position waypoint : waypoints) {
	    		Position.Int pos = waypoint.toInt();
	    		int color = 0xFF000000;
	    		set(showBoard, pos.x, pos.y, color);
	    	}
    	}
    }
    
    public void show(int id, List<PowerUp> powerUps) {
    	int[][] showBoard = fields.clone();
    	
    	drawPowerUps(showBoard, powerUps);
    	drawWayPoints(showBoard);
    	
    	screenshotHandler.generatePicture(id,showBoard);
    }
    
    boolean updateFirstTime = true;
    
    public void update(NetworkClient networkClient) {
    	//resetWayPoints();
    	
    	int rgb = 0;
    	int x = 0;
    	int y = 0;
    	
    	for(int i = 0; i<Size; i++) {
    		if(updateFirstTime ? networkClient.isWalkable(x, y) : fields[y][x] != 0x0) {
    			rgb = 0xFF000000  | networkClient.getBoard(x,y);
    			fields[y][x] = rgb;
    		}
    		x++;
    		if(x>=Width) { x=0; y++; }
    	}
    	
    	if(updateFirstTime) updateFirstTime=false;
    }
}
