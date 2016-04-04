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

	private java.util.HashMap<Transform,java.util.ArrayList<Transform>> collisionMap;

	private java.util.ArrayList<Transform> walls;

	public HacManPlugin(Display2D display, double framerate) throws IOException {

		super(display, framerate);

		controller = ArcadeController.getInstance();


		walls = new java.util.ArrayList<Transform>();

		// TODO: generate walls from map
		Transform wall = new Transform();
		wall.x = 6;
		wall.y = 8;
		walls.add(wall);

		// create the player
		player = new Player();

		// TODO: position player based on spawn point of map
		player.transform.x = 4;
		player.transform.y = 8;

		// our collision system requires only moving objects to map to things with which they can collide
		collisionMap = new java.util.HashMap<Transform,java.util.ArrayList<Transform>>();

		java.util.ArrayList<Transform> playerColliders = new java.util.ArrayList<Transform>();
		for (Transform transform : walls) {
			playerColliders.add(transform);
		}
		collisionMap.put(player.transform,playerColliders);

		// all moving objects should be added to the physics list, so they can be moved
		physicsTransfoms = new java.util.ArrayList<Transform>();
		physicsTransfoms.add(player.transform);

		// we need to set up our initial time here, so that the first update doesn't get sad
		timeSinceLastUpdate = System.nanoTime();
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
		for (Transform wall : walls) {
			d.setPixelHSB((int)wall.x,(int)wall.y,0.65f,1,1);
		}
		// draw pellets
		// draw ghosts
		// draw player
		d.setPixelHSB((int)player.transform.x,(int)player.transform.y,0.15f,1,1);
		// draw fruit(?)
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

			// wrap around the board
			Display2D d=getDisplay();
			float width = d.getWidth();
			float height = d.getHeight();
			if (xNew >= width) {xNew -= width;}
			if (xNew < 0) {xNew += width;}
			if (yNew >= height) {yNew -= height;}
			if (yNew < 0) {yNew += height;}

			// we only want to move if we won't collide with something solid
			boolean canMove = true;
			java.util.ArrayList<Transform> colliders = collisionMap.get(transform);
			if (colliders != null) {
				for (Transform collider : colliders) {
					if ((int)xNew == (int)collider.x && (int)yNew == (int)collider.y) {

						canMove = false;
						break;

					}
				}
			}

			if (!canMove) {continue;}

			// do move
			transform.x = xNew;
			transform.y = yNew;
			
		}
	}

}
