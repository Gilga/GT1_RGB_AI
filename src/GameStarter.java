import lenz.htw.kipifub.ColorChange;
import lenz.htw.kipifub.net.NetworkClient;

public class GameStarter {

	public static void main(String[] args) {
		NetworkClient networkClient = new NetworkClient("ip-adresse", "Teamname");
		int myPlayerNumber = networkClient.getMyPlayerNumber(); // 0 = rot, 1 = grün, 2 = blau

		int rgb = networkClient.getBoard(0, 0); // 0-1023 ->
		int b = rgb & 255;
		int g = (rgb >> 8) & 255;
		int r = (rgb >> 16) & 255;

		int influenceRadius = networkClient.getInfluenceRadiusForBot(0); // -> 40

		long score = networkClient.getScore(0); // Punkte von rot

		boolean isWalkable = networkClient.isWalkable(0, 0); // begehbar oder Hinderniss?
		
		networkClient.setMoveDirection(0, 1, 0); // bot 0 nach rechts
		networkClient.setMoveDirection(1, 0.23f, -0.52f); // bot 1 nach rechts unten
		
		ColorChange colorChange = new ColorChange();
		while ((colorChange = networkClient.pullNextColorChange()) != null) {
		    //verarbeiten von colorChange
		    int player = colorChange.player; 
		    int bot = colorChange.bot; 
		    int x = colorChange.x; 
		    int y = colorChange.y;
		}
	}
}
