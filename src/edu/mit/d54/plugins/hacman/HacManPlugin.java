package edu.mit.d54.plugins.hacman;

import java.io.IOException;
import edu.mit.d54.ArcadeController;
import edu.mit.d54.ArcadeListener;

import edu.mit.d54.Display2D;
import edu.mit.d54.DisplayPlugin;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 */
public class HacManPlugin extends DisplayPlugin implements ArcadeListener {

	private static final float kPlayerSpeed = 2.0f;

	private ArcadeController controller;
	private Player player;

	private ArrayList<Transform> physicsTransfoms;
	private long timeSinceLastUpdate;

	private HashMap<Transform,ArrayList<Transform>> collisionMap;

	private ArrayList<Transform> walls;
	private ArrayList<Pellet> pellets;

	public HacManPlugin(Display2D display, double framerate) throws IOException {

		super(display, framerate);

		// we need to do this so we can set the listener later
		controller = ArcadeController.getInstance();

		ResetGame();
	}

	private void ResetGame() {
		// TODO: generate walls from map
		walls = new ArrayList<Transform>();

		Transform wall = new Transform();
		wall.x = 6;
		wall.y = 8;
		walls.add(wall);

		// TODO: generate pellets from map
		pellets = new ArrayList<Pellet>();

		Pellet pellet = new Pellet();
		pellet.transform.x = 2;
		pellet.transform.y = 8;
		pellets.add(pellet);

		// create the player
		player = new Player();

		// TODO: position player based on spawn point of map
		player.transform.x = 4;
		player.transform.y = 8;

		// our collision system requires only moving objects to map to things with which they can collide
		collisionMap = new HashMap<Transform,ArrayList<Transform>>();

		ArrayList<Transform> playerColliders = new ArrayList<Transform>();
		for (Transform transform : walls) {
			playerColliders.add(transform);
		}
		collisionMap.put(player.transform,playerColliders);

		// all moving objects should be added to the physics list, so they can be moved
		physicsTransfoms = new ArrayList<Transform>();
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
		
		// Advance to the next stage if all pellets have been eaten! (cycle back to the first if it's the last one)
		if (pellets.size() <= 0) {ResetGame();}

		// Lose a life if the player gets haunted or whatever by a ghost

		// PHYSICS!
		UpdatePhysicsTransforms();

		// Eat Pellets
		ArrayList<Pellet> pelletsToRemove = new ArrayList<Pellet>();
		for (Pellet pellet : pellets) {
			if ((int)pellet.transform.x == (int)player.transform.x && (int)pellet.transform.y == (int)player.transform.y) {
				System.out.println("om nom");
				pelletsToRemove.add(pellet);
			}
		}
		for (Pellet pellet : pelletsToRemove) {
			pellets.remove(pellet);
		}


		// DRAW!
		Display2D d = getDisplay();

		// draw map
		for (Transform wall : walls) {
			d.setPixelHSB((int)wall.x,(int)wall.y,0.65f,1,1);
		}
		// draw pellets
		for (Pellet pellet : pellets) {
			// TODO: make power pellets brighter than regular pellets
			d.setPixelHSB((int)pellet.transform.x,(int)pellet.transform.y,0,0,1);
		}
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
			ArrayList<Transform> colliders = collisionMap.get(transform);
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
