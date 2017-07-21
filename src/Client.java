import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import lenz.htw.kipifub.ColorChange;
import lenz.htw.kipifub.net.NetworkClient;

public class Client extends Thread {
	
	NetworkClient networkClient =  null;
	String host = null;
    String name = null;
    int myID = 0;
    
    Client(String name, String host) throws IOException {
    	super(name);
        this.name = name;
        this.host = host;
    }
    
    ThreadLocalRandom tlr = null;
    
    int[][] board = new int[1024][1024];
    int color = 0;
    int[] radius = new int[]{0,0,0}; // radius
    float[] ang = new float[]{0,0,0};
    Position[] pos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldPos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldDir = new Position[]{new Position(),new Position(),new Position()}; // direction
   
    int[] wait = new int[]{0,0,0};
    int[] hitWall = new int[]{0,0,0};
    
    Position[] targetPos = new Position[]{new Position(),new Position(),new Position()};

    boolean[] infinity = new boolean[]{false,false,false};
    
    void move(int i) {
    	Position p = pos[i];
    	Position t = targetPos[i];
    	Position d = new Position(t.x-p.x, t.y-p.y);
    	
		if(!Position.equals(oldDir[i], d)){
			oldDir[i]=d;
			networkClient.setMoveDirection(i, d.x, d.y);
    		//System.out.printf("(%d|%d){%d} [%f,%f] -> [%f,%f]\n", myID, i, radius[i], pos[i].x, pos[i].y, d.x, d.y);
		}
    }
     
    
    boolean isValid(Position p) {
    	if(p.x<0||p.y<0||p.x>1023||p.y>1023) return false;
    	int[] v = p.toInt();
		//return board[v[1]][v[0]] != 0x0; // working incorrectly
		return networkClient.isWalkable(v[0], v[1]);
    }

    int getBoard(Position p){
    	if(!isValid(p)) return 0x0;
    	int[] v = p.toInt();
    	//return board[v[1]][v[0]]; // working incorrectly
    	return 0xFF000000 | networkClient.getBoard(v[0], v[1]);
    }
    
    void setBoard(Position p, int color){
    	if(!isValid(p)) return;
    	int[] v = p.toInt();
    	board[v[1]][v[0]] = color;
     }
    
    boolean canMoveTo(int i, Position t, float step) {
    	Position p=pos[i];

    	float dist=Position.distance(p,t);
    	if(dist<=0) return false;
  
    	Position d = Position.angToPosition(ang[i]);
   	
    	step=(radius[i]/2+1);
    	
    	if(!isValid(Position.add(p, Position.mul(d,new Position(step))))) return false;

    	for(float j=0;j<step;j++) //(dist/step)
    	{
    		p=Position.add(p, d);
    		if(!isValid(p)) return false;
    	}

    	return true;
    }
    
    void setPos(int i, Position p) { oldPos[i]=pos[i]; pos[i] = p;  }
    
    float oppositeAng(float ang) {
    	return (ang+180) % 360;
    }
    
    float randomAngle() {
    	return tlr.nextInt(0, 360);
    }
    
    void setInfiniteDirection(int i, float a) {
    	setDirection(i,a, Integer.MAX_VALUE);
    	infinity[i]=true;
    }
    
    void setDirection(int i, float a, float v) {
    	Position t = pos[i].getNext(i, Position.angToPosition(a), v);
    	if(!Position.equals(targetPos[i], t)) {
    		ang[i] = a;
    		targetPos[i]=t;
    		infinity[i]=false;
    	}
    }
    
    void setDirection(int i, Position t) {
    	if(!Position.equals(targetPos[i], t)) {
    		ang[i] = new Float(Position.calcRotationAngleInDegrees(pos[i],t));
    		targetPos[i]=t;
    		infinity[i]=false;
    	}
    }
    
    boolean boardHasWhite() {
    	Position p = new Position();
    	for(p.y=0;p.y<board.length;p.y++){
        	for(p.x=0;p.x<board[0].length;p.x++){
        		if(getBoard(p) == 0xFFFFFFFF) //isWalkable(p) &&
        			return true;
        	}
    	}
    	return false;
    }
    
    boolean colorIsValid(int bot, int rgb) {
		   return (hasWhite && (
					   (bot == 0 && ColorPixel.getBrightness(rgb) >= 1f)
					|| (bot == 1 && rgb != color)
					|| (bot == 2 && ColorPixel.isColorNotDominantOrWhite(rgb,color))
					))
		|| (!hasWhite && !ColorPixel.isColorDominant(rgb,color));
    	//return (hasWhite && ColorPixel.getBrightness(rgb) >= 1f) ||
    	//		(!hasWhite && ColorPixel.isColorDominant(rgb,color));
    }
    
    int z = 0;
    boolean findRGB(int bot, Position t){
        int rgb = getBoard(t);
        
        if(!isValid(t)||ColorPixel.isColorDominant(rgb,color)) return false;
        
        if(colorIsValid(bot,rgb))
		{
            setDirection(bot, t);
			//if(bot == 0) setBoard(t, color);
        	z++;
        	
        	return true;
		}
        
        return false;
    }
    
	Position[] ways = new Position[]{
			new Position(+1,0),
    		new Position(-1,0),
    		new Position(0,+1),
    		new Position(0,-1),
    		new Position(+1,+1),
    		new Position(+1,-1),
    		new Position(-1,+1),
    		new Position(-1,-1)
	};
    
	// TODO (still testing...)
    void Dijkstra(int bot)
	{
    	Position p = pos[bot];
    	Position t = targetPos[bot];
    	
    	float d = Position.distance(p, t);
    	
    	//!ColorPixel.isColorDominant(getBoard(t),color)
    	ArrayList<Integer> list = new ArrayList<>();
    	ArrayList<ArrayList<Integer>> doNotChoose = new ArrayList<>();
    	list.add(-1);
    	
		Position n;
		int lastID=-1;
		float d2 = 0;
		boolean found = false;
		
    	while(d>0){
    		if(list.size()>doNotChoose.size()) doNotChoose.add(new ArrayList<>());
    		ArrayList<Integer> filter = doNotChoose.get(list.size()-1);
    		
    		found=false;
    		for(int i=0;i<ways.length;i++) {
    			n = ways[i];
    			if (!filter.contains(i) && isValid(Position.add(p,n))){
    				d2 = Position.distance(p, t);
    				if(d2<d) { lastID=i; d=d2; found = true; };
    			}
    		}
    		if(found) { list.add(lastID); p = ways[lastID]; }
    		else {
    			int index = list.size()-1;
    			list.remove(index);
    			if(list.size()<=0) break; // can not move?
    			index--;
      			doNotChoose.get(index).add(lastID);
    			p = ways[list.get(index)];
    			d = Position.distance(p, t);
    		}
    	}
	}
    
	void SpiralSearch(int bot, float size)
	{
		Position p = pos[bot];
		Position d = Position.subtract(p, new Position(board[0].length/2, board.length/2));
		
		float WH = size/2;
		float HH = size/2;
		int t = 0;
	    int x,y,dx,dy;
	    x = y = dx =0;
	    dy = -1;
	    
	    for(int i = 0; i < size; i++){
	    	//double rr = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2));
	    	
	        if ((-WH <= x) && (x <= WH) && (-HH <= y) && (y <= HH))
	        {
	        	if(findRGB(bot, new Position(p.x+x,p.y+y))) return;
	        }
	        
	        if( (x == y) || ((x < 0) && (x == -y)) || ((x > 0) && (x == 1-y))){
	            t = dx;
	            dx = -dy;
	            dy = t;
	        }

	        x += dx;
	        y += dy;
	    }
	}

	void CircleSearch(int bot, float size)
	{
		Position p = pos[bot];
    	for(float r=1;r<size/2;r++) {
    		float r2 = r*r;
	        // iterate through all x-coordinates
	        for (float i = p.y-r; i <= p.y+r; i++) {
	        	float di2 = (i-p.y)*(i-p.y);
	            // iterate through all y-coordinates
	            for (float j = p.x-r; j <= p.x+r; j++) {
	            	float di1 = (j-p.x)*(j-p.x);
	                // test if in-circle
	                if (di1 + di2 <= r2) {
	                	if(findRGB(bot, new Position(j, i))) return;
	                }
	            }
	        }
    	}
	}
	
	void RadiusSearch(int bot, float size)
	{
		Position p = pos[bot];
     	Position d = null;
    	float old_ang = ang[bot];
    	float a = 0;
     	float z = radius[bot];
     	float sz = 0;
     	float sr = 0;
     	float dd = Position.distance(p,new Position(board[0].length/2, board.length/2));
     	float rr = dd + board[0].length/2 + board.length/2;
     	
    	for(float s=1;s<size;s++) {
         	sr = 0; //randomAngle(); //non smooth option
    		sz = s+z/2;
	    	for(float r=0;r<360;r++) {
	    		d = Position.angToPosition(old_ang + sr + r);

	    		if(findRGB(bot, p.getNext(bot, d, sz))) return;
	    	}
    	}
	}
    
    boolean hasWhite = false;
    void search(int bot) {
    	hasWhite = boardHasWhite();
    	
    	Position t = targetPos[bot];
    	float dist=Position.distance(pos[bot],t);
    	int rgb = getBoard(t);
    	int width = board[0].length;
    	int height = board.length;
    	int size = width*height;

    	if(!(infinity[bot] || !colorIsValid(bot,rgb) || dist<=7.5f)) return;
    	
    	if(name == "SpiralSearch") SpiralSearch(bot, size*3);
    	else if(name == "CircleSearch") CircleSearch(bot, 200);
    	else if(name == "RadiusSearch") RadiusSearch(bot, size*3);
    }
    
    void findObstacle(int i) {
		
		int ts = Integer.MAX_VALUE;
		int s = 0;
		float a = 0;
		float an = ang[i];
		float tr = 0;
		
		boolean found = false;
		Position p = pos[i];
		
		int max = radius[i];
		for(int r=0;r<360;r++){
			a=an+r;
			for(s=1;s<max;s++) {
				if(!isValid(p.getNext(i, Position.angToPosition(a), s))) {
					if(s<ts) { found=true; ts = s; tr = a;}
					break;
				}
			}
		}
		
		if(found){
			//setDirection(i, oppositeAng(tr)+tlr.nextInt(-45, 45), radius[i]);
			setInfiniteDirection(i,oppositeAng(tr)+tlr.nextInt(-45, 45));
			hitWall[i]++;
		}
		else hitWall[i]=0;
    }
    
    Position[] waitPos = new Position[]{new Position(),new Position(),new Position()};
    
    void setNewTarget(int i) {
    	search(i);

    	if(wait[i]>=3) {
    		if(Position.equals(waitPos[i],pos[i]))
    		{
    			findObstacle(i);
    		}
    		else hitWall[i]=0;
    		
    		waitPos[i]=pos[i];
    		wait[i]=0;
    	}
    	else {
    		wait[i]++;
    	}
    }
    
    void update(int i) {}

    boolean updateBoardFirstTime = true;
    
    public void updateBoard() {
    	int width = board[0].length;
    	int height = board.length;
    	
    	int rgb = 0;
    	int x = 0;
    	int y = 0;
    	for(int i = 0; i<width*height; i++) {
    		if(updateBoardFirstTime || board[y][x] != 0){
	    		rgb = networkClient.getBoard(x,y);
	    		if(!updateBoardFirstTime || rgb!=0) {
	    			board[y][x] = 0xFF000000 | rgb;
	    		}
    		}
    		x++;
    		if(x>=width) { x=0; y++; }
    	}
    	
    	if(updateBoardFirstTime) updateBoardFirstTime=false;
    }
    
    public void generatePicture()
    {
    	int width = board[0].length;
    	int height = board.length;
    	BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    	int[] t1 = targetPos[0].toInt();
    	int[] t2 = targetPos[1].toInt();
    	int[] t3 = targetPos[2].toInt();
    	
    	int x = 0;
    	int y = 0;
    	int[] buffer = new int[width*height];
    	for(int i = 0; i<buffer.length; i++) {
    		if(x == t1[0] && y == t1[1]) buffer[i] = 0xFF000000;
    		else if(x == t2[0] && y == t2[1]) buffer[i] = 0xFF000000;
    		else if(x == t3[0] && y == t3[1]) buffer[i] = 0xFF000000;
    		else buffer[i] =  board[y][x];
    		x++;
    		if(x>=width) { x=0; y++; }
    	}
    	
    	image.setRGB(0,0,width,height,buffer, 0, width);
    	
		try {
			File file = new File(myID + "_board.png");
			ImageIO.write(image, "png", file);
			
		    Desktop dt = Desktop.getDesktop();
		    dt.open(file);
		    
		} catch (Exception e) {
			System.err.println("Fehler: Konnte Boardbild nicht speichern!");
		}
    }
 
    public void run() {
    	tlr = ThreadLocalRandom.current();
    	
		networkClient = new NetworkClient(host, name);
		myID = networkClient.getMyPlayerNumber();
		System.out.printf("Player: %d started\n", myID);

		updateBoard();
		//if(myID == 0) generatePicture();
		
		// radius
		radius[0] = networkClient.getInfluenceRadiusForBot(0);
		radius[1] = networkClient.getInfluenceRadiusForBot(1);
		radius[2] = networkClient.getInfluenceRadiusForBot(2);

		setInfiniteDirection(0,randomAngle());
		setInfiniteDirection(1,randomAngle());
		setInfiniteDirection(2,randomAngle());
		
	    //long score = networkClient.getScore(0); // Punkte von rot
		
		boolean foundStartPos = false;
		ColorChange colorChange = new ColorChange();
		int g = 0;
		while(true){

			while ((colorChange = networkClient.pullNextColorChange()) != null) {
			    //verarbeiten von colorChange
			    int player = colorChange.player; 
			    int bot = colorChange.bot; 
			    int x = colorChange.x; 
			    int y = colorChange.y;
			    Position p = new Position(x,y);
			    
			    if(player == myID){
			    	
			    	if(!Position.equals(pos[bot], p)){
			    		setPos(bot, p);
				    	update(bot);
			    		
			    		if(bot == 0) {

			    			if(!foundStartPos){
			    				//myID == 0 ? 0xFFFF0000 : (myID == 1 ? 0xFF00FF00 : (myID == 2 ? 0xFF0000FF : 0));
			    				color = ColorPixel.getDominantColor(0xFF000000 | networkClient.getBoard(x,y));
				    			System.out.printf("%d: %s color\n", myID, ColorPixel.getColorStr(color));
			    			}
			    			foundStartPos = true;
			    		}
			    	}
			    }
			}
			
			if(foundStartPos) {
				updateBoard();
				
				setNewTarget(0);
				setNewTarget(1);
				setNewTarget(2);
				
				move(0);
				move(1);
				move(2);
				
				if(g==0) {
					//generatePicture();
				}
				g++;
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
    }
}
