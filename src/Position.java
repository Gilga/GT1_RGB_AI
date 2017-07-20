
public class Position {
	public float x = 0;
	public float y = 0;
	
	public Position() {}
	public Position(float v) {this.x=v;this.y=v;}
	public Position(float x, float y) {this.x=x;this.y=y;}

    public Position getNext(int i, Position direction, float r) {
    	return new Position(x+(r*direction.x),y+(r*direction.y));
    }

    Position getNormalizedDirection(Position t) {
    	return normalize(getDirection(t,1));
    }
    
    Position getDirection(Position t, float dist) {
    	Position n = subtract(this,t);
    	return dist!=1 ? new Position(n.x/dist,n.y/dist) : n;
    }
    
    static public Position angToPosition(float ang) {
    	ang=new Float(Math.toRadians(ang));
    	return new Position(new Float(Math.sin(ang)),new Float(Math.cos(ang)));
    }

    static public float magnitude(Position p) {
    	return new Float(Math.sqrt(p.x*p.x + p.y*p.y));
    }
    
    static public Position normalize(Position p) {
    	float length = magnitude(p);
    	return new Position(p.x/length,p.y/length);
    }
    
    static public Position mul(Position p, Position t) {
    	return new Position(p.x*t.x,p.y*t.y);
    }
    
    static public Position div(Position p, Position t) {
    	return new Position(p.x/t.x,p.y/t.y);
    }
    
    static public Position add(Position p, Position t) {
    	return new Position(p.x+t.x,p.y+t.y);
    }
    
    static public Position subtract(Position p,Position t) {
    	return new Position(t.x-p.x,t.y-p.y);
    }
    
    
    static public boolean equals(Position p, Position t) {
    	return p.x == t.x && p.y == t.y;
    }
    
    static public float distance(Position p1, Position p2) {
        return new Float(Math.sqrt(
                Math.pow(p1.x - p2.x,2.0) + 
                Math.pow(p1.y - p2.y,2.0)
            ));
    }
}
