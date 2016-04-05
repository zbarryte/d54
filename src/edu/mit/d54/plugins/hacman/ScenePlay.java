package edu.mit.d54.plugins.hacman;

import java.util.ArrayList;
import java.util.HashMap;

import edu.mit.d54.Display2D;


public class ScenePlay extends Object {

	private Display2D d;

	public enum State {Playing, Won, Lost};
	public State state;

	private static final float kPlayerSpeed = 2.0f;
	private static final int kPlayerLivesStart = 3;

	private Player player;

	private ArrayList<Transform> physicsTransfoms;
	private long timeSinceLastUpdate;

	private HashMap<Transform,ArrayList<Transform>> collisionMap;

	private ArrayList<Transform> walls;
	private ArrayList<Pellet> pellets;
	private ArrayList<Ghost> ghosts;

	public ScenePlay(Display2D display) {

		d = display;

		// we start in the the playing state
		state = State.Playing;

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

		Pellet pellet2 = new Pellet();
		pellet2.isPower = true;
		pellet2.transform.x = 5;
		pellet2.transform.y = 5;
		pellets.add(pellet2);

		// create the ghosts
		ghosts = new ArrayList<Ghost>();

		// create INKY, PINKY, BLINKY, and CLYDE
		Ghost inky = new Ghost();
		inky.hue = 0.65f;
		ghosts.add(inky);

		// TODO: position ghosts based on spawn points of map
		inky.transform.setStartPosition(4,10);

		// create MS HAC MAN
		player = new Player();
		player.numLives = kPlayerLivesStart;

		// TODO: position player based on spawn point of map
		player.transform.setStartPosition(4,8);

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

		// start the level
		restartLevel();
	}

	private void restartLevel() {

		if (player.numLives < 0) {
			state = State.Lost;
			return;
		}

		player.transform.reset();

		for (Ghost ghost : ghosts) {
			ghost.transform.reset();
		}
	}

	public void update() {

		// Advance to the next stage if all pellets have been eaten! (cycle back to the first if it's the last one)
		if (pellets.size() <= 0) {state = State.Won;}

		// Lose a life if the player gets haunted or whatever by a ghost
		for (Ghost ghost : ghosts) {
			if ((int)player.transform.x == (int)ghost.transform.x && (int)player.transform.y == (int)ghost.transform.y) {
				// TODO: handle woobly ghosts

				player.numLives--;

				restartLevel();
			}
		}
		// Then reset ghost and player positions; pellets should remain as they were

		// PHYSICS!
		UpdatePhysicsTransforms();

		// Eat Pellets
		boolean didEatPowerPellet = false;
		ArrayList<Pellet> pelletsToRemove = new ArrayList<Pellet>();
		for (Pellet pellet : pellets) {
			if ((int)pellet.transform.x == (int)player.transform.x && (int)pellet.transform.y == (int)player.transform.y) {
				if (pellet.isPower) {didEatPowerPellet = true;}
				System.out.println("om nom");
				pelletsToRemove.add(pellet);
			}
		}
		for (Pellet pellet : pelletsToRemove) {
			pellets.remove(pellet);
		}
		// TODO: make ghost all woobly for a while if a power pellet was eaten
		if (didEatPowerPellet) {
			System.out.println("POWER!");
		}


		// DRAW!

		// draw map
		for (Transform wall : walls) {
			d.setPixelHSB((int)wall.x,(int)wall.y,0.65f,1,1);
		}
		// draw pellets
		for (Pellet pellet : pellets) {
			float brightness = pellet.isPower ? 1.0f : 0.25f;
			d.setPixelHSB((int)pellet.transform.x,(int)pellet.transform.y,0,0,brightness);
		}
		// draw ghosts
		for (Ghost ghost : ghosts) {
			d.setPixelHSB((int)ghost.transform.x,(int)ghost.transform.y,ghost.hue,0.5f,1.0f);
		}
		// draw player
		d.setPixelHSB((int)player.transform.x,(int)player.transform.y,0.15f,1,1);
		// draw fruit(?)

	}

	public void MovePlayer(float dx, float dy) {
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