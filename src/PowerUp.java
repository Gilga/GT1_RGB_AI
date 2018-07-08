import lenz.htw.zpifub.PowerupType;

public class PowerUp {
    
	static PowerupType BOMB = PowerupType.BOMB;
	static PowerupType RAIN = PowerupType.RAIN;
	static PowerupType SLOW = PowerupType.SLOW;
	
	public Position pos = new Position();
	public PowerupType type = null;
	public PowerUp(Position pos, PowerupType type) { this.pos=pos; this.type=type; };
	
	public String toString() {
		return ""+type+pos;
	}
}
