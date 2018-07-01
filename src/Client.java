import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import javax.imageio.ImageIO;

import lenz.htw.zpifub.*;
import lenz.htw.zpifub.net.*;

public class Client extends Thread {
	
	class PowerUp {
		public Position pos = new Position();
		public PowerupType type = null;
		public PowerUp(Position pos, PowerupType type) { this.pos=pos; this.type=type; };
		
		public String toString() {
			return ""+type+pos;
		}
	}
	
	NetworkClient networkClient =  null;
	String host = null;
    String name = null;
    int myID = 0;
    int latency = 0;
    int generatePictureSec = 10;
    boolean generatePictureEnabled = false;
    
    List<PowerUp> powerupTypes = new ArrayList<>();
    
    Client(String name, String host) throws IOException {
    	super(name);
        this.name = name;
        this.host = host;
    }
    
    ThreadLocalRandom tlr = null;
    
    int[][] board = new int[1024][1024];
    int[][] boardDebug = new int[1024][1024];
    
    int color = 0;
    int[] radius = new int[]{0,0,0}; // radius
    float[] ang = new float[]{0,0,0};
    Position[] pos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldPos = new Position[]{new Position(),new Position(),new Position()};
    Position[] oldDir = new Position[]{new Position(),new Position(),new Position()}; // direction
   
    long[] wait = new long[]{0,0,0};
    int[] hitWall = new int[]{0,0,0};
    
    Position[] targetPos = new Position[]{new Position(),new Position(),new Position()};

    boolean[] infinity = new boolean[]{false,false,false};
    
    void move(int bot) {
    	Position p = pos[bot];
    	Position t = getWayPoint(bot); //getPassablePosition(getWayPoint(bot),radius[bot]/2);
    	Position d = new Position(t.x-p.x, t.y-p.y);

		if(!Position.equals(oldDir[bot], d)){
			oldDir[bot]=d;
			networkClient.setMoveDirection(bot, d.x, d.y);
    		//System.out.printf("(%d|%d){%d} [%f,%f] -> [%f,%f]\n", myID, bot, radius[bot], pos[bot].x, pos[bot].y, d.x, d.y);
		}
    }
     
    
    boolean isValid(Position p) {
    	//if(p.x<0||p.y<0||p.x>1023||p.y>1023) return false;
    	int[] v = p.toInt();
		//return board[v[1]][v[0]] != 0x0;
		return networkClient.isWalkable(v[0], v[1]);
    }

    int getBoard(Position p){
    	if(!isValid(p)) return 0x0;
    	int[] v = p.toInt();
    	//return board[v[1]][v[0]];
    	return 0xFF000000 | networkClient.getBoard(v[0], v[1]);
    }
    
    void setBoard(Position p, int color){
    	if(!isValid(p)) return;
    	int[] v = p.toInt();
    	//board[v[1]][v[0]] = color;
    	boardDebug[v[1]][v[0]] = color;
     }
    
    void clearBoard() {
    	Position p = new Position();
    	for(p.y=0;p.y<board.length;p.y++){
        	for(p.x=0;p.x<board[0].length;p.x++){
        		int rgb = getBoard(p);
        		if(!(rgb == 0x0 || rgb == 0xFFFFFFFF))
        			setBoard(p, 0xFFFFFFFF);
        	}
    	}
    }
    
    boolean canMoveTo(int bot, Position t, float step) {
    	Position p=pos[bot];

    	float dist=Position.distance(p,t);
    	if(dist<=0) return false;
  
    	Position d = Position.angToPosition(ang[bot]);
   	
    	step=(radius[bot]/2+1);
    	
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
    
    void setRandomDirection(int bot) {
    	if(!infinity[bot]) setInfiniteDirection(bot, randomAngle());
    }
    
    void setInfiniteDirection(int bot, float a) {
    	setDirection(bot, a, Integer.MAX_VALUE);
    	infinity[bot]=true;
    }
    
    void setDirection(int bot, float a, float v) {
    	Position t = pos[bot].getNext(Position.angToPosition(a), v);
    	if(!Position.equals(targetPos[bot], t)) {
    		ang[bot] = a;
    		targetPos[bot]=t;
    		infinity[bot]=false;
    	}
    }
    
    void setDirection(int bot, Position t) {
    	if(!Position.equals(targetPos[bot], t)) {
    		ang[bot] = new Float(Position.calcRotationAngleInDegrees(pos[bot],t));
    		targetPos[bot]=t;
    		infinity[bot]=false;
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
    
    int[] searchState = new int[]{0,0,0};
    boolean findRGB(int bot, Position t){
        int rgb = getBoard(t);
        
        if(!isValid(t)||ColorPixel.isColorDominant(rgb,color)) return false;
        
        if(colorIsValid(bot,rgb))
		{
        	setDirection(bot, t);
            
			//if(bot == 0) setBoard(t, color);
        	
        	return true;
		}
        
        return false;
    }
    
	Position[] ways = new Position[]{
			new Position(+1,+0),
    		new Position(+1,+1),
    		new Position(+0,+1),
    		new Position(-1,+1),
    		new Position(-1,+0),
    		new Position(-1,-1),
    		new Position(+0,-1),
    		new Position(+1,-1)
	};
	
	String[] waysStr = new String[]{
			"r",
    		"ru",
    		"u",
    		"lu",
    		"l",
    		"ld",
    		"d",
    		"rd"
	};
    
	int wayPointIndex[] = new int[]{0,0,0};
	ArrayList<ArrayList<Position>> wayPoints = new ArrayList<ArrayList<Position>>();

	boolean hasWayPoints(int bot)
	{
		return wayPoints.get(bot).size()>0;
	}
	
	Position getWayPoint(int bot)
	{
		return hasWayPoints(bot) && wayPointIndex[bot]<wayPoints.get(bot).size() ? wayPoints.get(bot).get(wayPointIndex[bot]) : targetPos[bot];
	}
	
	void clearWayPoints(int bot)
	{
		wayPointIndex[bot] = 0;
		wayPoints.get(bot).clear();
	}
	
	void updateWayPoints(int bot)
	{
		if(!hasWayPoints(bot)) {
			searchState[bot]=0;
			return;
		}
			
		Position w = getWayPoint(bot);
		setBoard(w, 0xFF000000);
		if(!isValid(w) || Position.distance(pos[bot],w) <= radius[bot] || !colorIsValid(bot,getBoard(getWayPoint(bot)))){
			wayPointIndex[bot]++;
		}

		if(wayPointIndex[bot]>=wayPoints.get(bot).size()){
			clearWayPoints(bot);
			searchState[bot]=0;
			System.out.println("done.");
		}
	}
	
	Position getPassablePosition(Position t, float radius){
		Position x = t;
		Position v = t;

		boolean validRadius = true;
		float rm=8;
		float rr=360/rm;
    	for(int dr = 0; dr<=radius;dr++) {
	    	for(float r=0;r<rm;r++) {
	    		x = t.getNext(Position.angToPosition(rr*r), dr);
	    		if(!isValid(x)){
					validRadius=false;
	    		} else {
	    			float d1 = Position.distance(x, t);
	    			float d2 = Position.distance(v, t);
	    			if(d1<d2)
	    				v=x;
	    		}
	    	}
	    	if(!validRadius) break;
    	}
    	
    	if(!validRadius) return v;
    	return t;
	}

	void DijkstraSearch(int bot, int max)
	{
		ArrayList<Position> list = new ArrayList<>();
    	ArrayList<Position> rlist = new ArrayList<>();
    	ArrayList<Position> filter = new ArrayList<>();
    	ArrayList<Position> wlist = wayPoints.get(bot);
    	
    	Position start = pos[bot];
    	Position end = targetPos[bot];
		Position nextPos = start;
		Position bestPos = start;
		Position current = start;
		int bestIndex = 0;
		
		float dist = Position.distance(start, end);
		float dist_last = -1;
		float dist_best = dist;
		float dist_current = dist;
		int found = 0;
		Position rp;
		
		System.out.print("0 ");
    	for(int s = 0; s<1000;s++){
    		
    		found = 0;
    		dist_last = -1;
    		for(int i=0;i<ways.length;i++) {
    			current=Position.add(nextPos,ways[i]);
    			
	    		setBoard(current,0xFFDDDDDD);
	    		
    			boolean validRadius = true;
    			
    			// beachte den radius
    			/*
    			float rm=8;
    			float rr=360/rm;
    	    	for(int dr = 0; dr<=radius[bot]/2;dr++) {
    		    	for(float r=0;r<rm;r++) {
    		    		rp = current.getNext(Position.angToPosition(rr*r), dr);
    		    		if(!isValid(rp)){
    						validRadius=false;
    						break;
    		    		}
    		    	}
    		    	if(!validRadius) break;
    	    	}
    	    	*/
    	           			
    			if (validRadius && isValid(current) && !list.contains(current) && !filter.contains(current)){
    				if(found<1) {
    					found = 1;
    					bestIndex=i;
    					bestPos=current;
    				}
    				dist_current = Position.distance(current, end);
    				if(dist_last<=0 || dist_current<dist_last) {
    					bestPos=current;
    					bestIndex=i;
    					dist_last=dist_current;
    					if(found<2) found = 2;
    					if(dist_last<dist_best) dist_best=dist_last;
     				}
    			}
    		}
    		
    		if(found>0) {
    			nextPos=bestPos;
    			if(Position.distance(nextPos, end)<=0) break;
    			System.out.print("+"+waysStr[bestIndex]+" ");
    			rlist.add(ways[bestIndex]);
    			list.add(nextPos);
    			setBoard(nextPos,0xFF00FF00);
    		}
    		else {
    			System.out.print("-");
    			setBoard(nextPos,0xFFAAAAAA);
    			if(list.size()<=0) break;
     			int step=list.size()-1;
    			list.remove(step);
    			rlist.remove(step);
    			filter.add(nextPos);
    			step--;
       			nextPos = (step<0) ? start : list.get(step);
       			bestIndex=-1;
       			if(step>=0)  {
       				Position h = rlist.get(step);
       				for(int i=0;i<ways.length;i++) { if(Position.equals(h, ways[i])){ bestIndex=i; break;}; }
       			}
       			System.out.print("("+((bestIndex<0) ? "0" : waysStr[bestIndex])+") ");
    		}
    	}
    	System.out.println();

    	nextPos = start;
    	bestPos = new Position();
    	for(Position w : rlist)
    	{
    		nextPos=Position.add(nextPos,w);
    		if(Position.equals(bestPos, w)) continue;
    		bestPos=w;
    		wlist.add(nextPos);
    	}
    	
		for(Position w : filter) setBoard(w, 0xFFAA88AA);
    	for(Position w : list) setBoard(w, 0xFFCCCC00);
    	setBoard(start, 0xFF00FFFF);
    	setBoard(end, 0xFF0000FF);
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
	    		if(findRGB(bot, p.getNext(d, sz))) return;
	    	}
    	}
	}
    
    boolean hasWhite = false;
    
	PowerupType BOMB = PowerupType.values()[0];
	PowerupType RAIN = PowerupType.values()[1];
	PowerupType SLOW = PowerupType.values()[2];
	
	boolean avoidPowerUps(int bot) {
		float dist = 0;
    	for(PowerUp powerupType : powerupTypes) {
    		if(powerupType.type != SLOW) continue;
    		dist = Position.distance(pos[bot],powerupType.pos);
    		if(dist <= 15) { setDirection(bot,Position.add(pos[bot], Position.subtract(pos[bot], powerupType.pos))); return true; }
    	}
    	return false;
	}
    
    boolean preferPowerUps(int bot) {
    	Position t = null;
    	Position target = null;
    	float dist = 0;
    	float min_dist = -1;

    	for(PowerUp powerupType : powerupTypes) {
    		if(powerupType.type == SLOW) continue;
    		t = powerupType.pos;
    		dist=Position.distance(pos[bot],t);
    		if(min_dist<0||dist<min_dist) { min_dist = dist; target = t; }
    	}
    	if(target!=null) { setDirection(bot,target); return true; }
    	
    	setRandomDirection(bot);
    	
    	return false;
    }
    
    void search(int bot) {
    	avoidPowerUps(bot);
    	preferPowerUps(bot);

    	hasWhite = boardHasWhite();
    	
    	Position t = targetPos[bot];
    	float dist=Position.distance(pos[bot],t);
    	int rgb = getBoard(t);
    	int width = board[0].length;
    	int height = board.length;
    	int size = width*height*3;
    	
    	//if(!(myID == 0 && bot == 0)) return;
    	
    	if(!(infinity[bot] || !colorIsValid(bot,rgb) || dist<=radius[bot])) return;
    	//SpiralSearch(bot, size*3);
    	
    	if(bot==1) SpiralSearch(bot, size);
    	else if(bot==0) RadiusSearch(bot, size);
    	else if(bot==2) SpiralSearch(bot, size);
    	
    	/*
    	if(searchState[bot]==1){
    		clearBoard();
    		//SpiralSearch(bot, size*3);
    		targetPos[bot] = new Position(tlr.nextInt(250, 750),tlr.nextInt(250, 750));
    		searchState[bot]++;
    	//if(name == "SpiralSearch") SpiralSearch(bot, size*3);
    	//else if(name == "CircleSearch") CircleSearch(bot, 200);
    	//else if(name == "RadiusSearch") RadiusSearch(bot, size*3);
    	}
    	
    	if(searchState[bot]==1) {
        	if((myID == 0 && bot == 0)) System.out.println("DijkstraSearch");
        	DijkstraSearch(bot, width*10);
        	searchState[bot]++;
    	}
    	
    	if(searchState[bot]==2) {
    		if((myID == 0 && bot == 0)) System.out.println("updateWayPoints: " + wayPoints.get(bot).size());
    		updateWayPoints(bot);
    		
    		//if(!colorIsValid(bot,rgb) || dist<=radius[bot]) {
    		//	clearWayPoints(bot);
    		//}
    	}
    	*/
    }
    
    void findObstacle(int bot) {
		int ts = Integer.MAX_VALUE;
		int s = 0;
		float a = 0;
		float an = ang[bot];
		float tr = 0;
		
		boolean found = false;
		Position p = pos[bot];
		
		int max = radius[bot];
		for(int r=0;r<360;r++){
			a=an+r;
			for(s=1;s<max;s++) {
				if(!isValid(p.getNext(Position.angToPosition(a), s))) {
					if(s<ts) { found=true; ts = s; tr = a;}
					break;
				}
			}
		}
		
		if(found){
			setDirection(bot, oppositeAng(tr)+tlr.nextInt(-90, 90), radius[bot]*10);
			//setInfiniteDirection(bot,oppositeAng(tr)+tlr.nextInt(-45, 45));
			hitWall[bot]++;
		}
		else hitWall[bot]=0;
    }
    
    Position[] waitPos = new Position[]{new Position(),new Position(),new Position()};
    boolean picture=false;
    
    void setNewTarget(int bot) {
    	if(Position.equals(targetPos[bot], new Position())) targetPos[bot] = pos[bot];
    	
    	search(bot);

    	if((System.currentTimeMillis() - wait[bot]) >= 3) {
    		if(Position.equals(waitPos[bot],pos[bot]))
    		{
    			findObstacle(bot);
    		}
    		else hitWall[bot]=0;
    		
    		waitPos[bot]=pos[bot];
    		wait[bot]=System.currentTimeMillis();
    	}
    }
    
    boolean updateBoardFirstTime = true;

    public long getMedian(long[] numArray) {
    	Arrays.sort(numArray);
    	
    	int middle = numArray.length/2;
    	long medianValue = 0; //declare variable 
    	
    	if (numArray.length%2 == 1) 
    	    medianValue = numArray[middle];
    	else
    	   medianValue = (numArray[middle-1] + numArray[middle]) / 2;
    	
    	return medianValue;
    }
    
    public void updateBoard(boolean updateDebug) {
    	int width = board[0].length;
    	int height = board.length;
    	int size = width*height;
    	
    	int rgb = 0;
    	int x = 0;
    	int y = 0;
    	
    	long[] timeList = new long[size];
    	long t = 0;
    	
    	for(int i = 0; i<size; i++) {
    		if(updateBoardFirstTime || board[y][x] != 0){
    			
    			t = System.currentTimeMillis();
	    		rgb = networkClient.getBoard(x,y);
	    		timeList[i] = System.currentTimeMillis() - t;
	    		
	    		if(!updateBoardFirstTime || rgb!=0) {
	    			board[y][x] = 0xFF000000 | rgb;
	    			if(updateDebug) boardDebug[y][x] = board[y][x];
	    		}
    		}
    		x++;
    		if(x>=width) { x=0; y++; }
    	}
    	
    	if(updateBoardFirstTime) updateBoardFirstTime=false;
    	
    	latency = (int) getMedian(timeList);
    }
    
    long generatePictureLastTime = System.currentTimeMillis();
    
    public void generatePicture()
    {
    	if(!generatePictureEnabled) return;
    	long time = System.currentTimeMillis();
    	if((time - generatePictureLastTime) < generatePictureSec*1000) return;
    	generatePictureLastTime = time;
    	
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
    		//if(x == t1[0] && y == t1[1]) buffer[i] = 0xFF000000;
    		//else if(x == t2[0] && y == t2[1]) buffer[i] = 0xFF000000;
    		//else if(x == t3[0] && y == t3[1]) buffer[i] = 0xFF000000;
    		//else
    		buffer[i] = boardDebug[y][x];
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
    
    void remove(int bot, PowerUp p) {
    	float dist = 0;
    	int i = -1;
    	for(PowerUp q : powerupTypes) {
    		i++;
    		dist = Position.distance(p.pos, q.pos);
    		if(p.type == q.type && dist <= 5)
    		{
    	    	System.out.println(p+" removed.");
    			powerupTypes.remove(i);
    			//if(bot>-1) setRandomDirection(bot);
    			break;
    		}
    	}
    }
    
    public void run() {
    	tlr = ThreadLocalRandom.current();
    	
		networkClient = new NetworkClient(host, name, "Enemy Rage Quit");
		myID = networkClient.getMyPlayerNumber();
		System.out.printf("Player: %d started\n", myID);

		updateBoard(true);

		// radius
		radius[0] = networkClient.getInfluenceRadiusForBot(0);
		radius[1] = networkClient.getInfluenceRadiusForBot(1);
		radius[2] = networkClient.getInfluenceRadiusForBot(2);

		setInfiniteDirection(0,randomAngle());
		setInfiniteDirection(1,randomAngle());
		setInfiniteDirection(2,randomAngle());
		
	    //long score = networkClient.getScore(0); // Punkte von rot
		
		wayPoints.add(new ArrayList<>());
		wayPoints.add(new ArrayList<>());
		wayPoints.add(new ArrayList<>());
		
		boolean foundStartPos = false;
		
		Update update = new Update();
		long time = 0;
		
		while(true){
			time = System.currentTimeMillis();
			while ((update = networkClient.pullNextUpdate()) != null) {
				latency = (int) (System.currentTimeMillis() - time);
				
			    //verarbeiten von colorChange
			    int player = update.player; 
			    int bot = update.bot; 
			    int x = update.x; 
			    int y = update.y;
			    PowerupType type = update.type;
			    Position p = new Position(x,y);
			    
			    if (type != null){
			    	PowerUp powerUp = new PowerUp(p,type);
			    	if(player == -1) {
			    		powerupTypes.add(powerUp); // add
			    		System.out.println(type+" "+x+" "+y);
			    	}
			    	else remove(bot, powerUp); // remove, it was used
			    }
			    
			    if(player == myID){
			    	//if(!Position.equals(pos[bot], p))
			    	//Position dir = Position.normalize(Position.subtract(p,pos[bot]));
			    	//p = Position.add(p, Position.mul(dir, 1+latency));
		    		setPos(bot, p);
			    	
			    	/*
			    	if(myID==0 && bot==1) {
			    		float px = pos[bot].x;
			    		float py = pos[bot].y;
			    		targetPos[bot] = new Position((px > 500f ? -1: 1)*100+500,(px > 500f ? -1: 1)*100+500);
			    	}
			    	else targetPos[bot]= pos[bot];
			    	*/
	    			
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
			
			if(foundStartPos) {
				updateBoard(true);
				
				setNewTarget(0);
				setNewTarget(1);
				setNewTarget(2);
				
				generatePicture();
				
				move(0);
				move(1);
				move(2);
			}
			
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			time = System.currentTimeMillis();
		}
    }
}
