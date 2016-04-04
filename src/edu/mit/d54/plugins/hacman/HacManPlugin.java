package edu.mit.d54.plugins.hacman;

import java.io.IOException;
import edu.mit.d54.ArcadeController;
import edu.mit.d54.ArcadeListener;

import edu.mit.d54.Display2D;
import edu.mit.d54.DisplayPlugin;

/**
 * 
 */
public class HacManPlugin extends DisplayPlugin implements ArcadeListener {

	private static final float kPlayerSpeed = 2.0f;

	private ArcadeController controller;
	private Player player;

	private java.util.ArrayList<Transform> physicsTransfoms;
	private long timeSinceLastUpdate;

	public HacManPlugin(Display2D display, double framerate) throws IOException {

		super(display, framerate);

		controller = ArcadeController.getInstance();
		player = new Player();

		physicsTransfoms = new java.util.ArrayList<Transform>();
		physicsTransfoms.add(player.transform);

		timeSinceLastUpdate = System.nanoTime();

		MovePlayer(1,0);
	}

	@Override
	protected void onStart()
	{
		controller.setListener(this);
	}

	@Override
	public void arcadeButton(byte b) {

		switch (b) {
			case 'L':
				MovePlayer(-1,0);
				break;
			case 'R':
				MovePlayer(1,0);
				break;
			case 'U':
				MovePlayer(0,-1);
				break;
			case 'D':
				MovePlayer(0,1);
				break;
			default:
				break;
		}

	}

	@Override
	protected void loop() {
		
		// PHYSICS!
		UpdatePhysicsTransforms();

		// DRAW!
		Display2D d = getDisplay();

		// draw map
		// draw pellets
		// draw ghosts
		// draw player
		d.setPixelHSB((int)player.transform.x,(int)player.transform.y,0.15f,1,1);
		// draw fruit(?)
		
		// for (int x=0; x<d.getWidth(); x++)
		// {
		// 	for (int y=0; y<d.getHeight(); y++)
		// 	{
		// 		if (x<5 && y<7)
		// 		{
		// 			if ((x+y)%2==0)
		// 				d.setPixelHSB(x,y,0.666f,1,1);
		// 			else
		// 				d.setPixelHSB(x,y,0,0,1);
		// 		}
		// 		else
		// 		{
		// 			if (x%2==0)
		// 				d.setPixelHSB(x,y,0,1,1);
		// 			else
		// 				d.setPixelHSB(x,y,0,0,1);
		// 		}
		// 	}
		// }
	}

	private void MovePlayer(float dx, float dy) {
		player.transform.setVelocity(dx * kPlayerSpeed, dy * kPlayerSpeed);
	}

	private void UpdatePhysicsTransforms() {

		long currentTime = System.nanoTime();
		float dt = (float)(currentTime - timeSinceLastUpdate) / 1000000000.0f;
		timeSinceLastUpdate = currentTime;

		for (Transform transform : physicsTransfoms) {

			// newton's probably good enough; this stuff doesn't move fast
			float dx = transform.vx * dt;
			float dy = transform.vy * dt;

			// see where it will be
			float xNew = transform.x + dx;
			float yNew = transform.y + dy;

			// check collisions to see if it can move

			// wrap around the bo
			Display2D d=getDisplay();
			float width = d.getWidth();
			float height = d.getHeight();
			if (xNew >= width) {xNew -= width;}
			if (xNew < 0) {xNew += width;}
			if (yNew >= height) {yNew -= height;}
			if (yNew < 0) {yNew += height;}

			// do move
			transform.x = xNew;
			transform.y = yNew;
		}
	}

}
