package edu.mit.d54.plugins.hacman;

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.image.BufferedImage;

import edu.mit.d54.Display2D;
import java.io.IOException;
import javax.imageio.ImageIO;


public class ScenePlay extends Object {

	private static final int kSpawnWallColor = 0x000080;
	private static final int kSpawnPlayerColor = 0xFFFF00;
	private static final int kSpawnPelletColor = 0x333333;
	private static final int kSpawnPelletPowerColor = 0xFFFFFF;
	private static final int kSpawnGhostInkyColor = 0x00FFFF;
	private static final int kSpawnGhostPinkyColor = 0xFF0080;
	private static final int kSpawnGhostBlinkyColor = 0xFF0000;
	private static final int kSpawnGhostClydeColor = 0xFF8000;

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

	public ScenePlay(Display2D display) throws IOException {

		d = display;

		// 'Playing' is the default state
		state = State.Playing;

		// we need to set up our arrays before we populate them
		walls = new ArrayList<Transform>();
		pellets = new ArrayList<Pellet>();
		ghosts = new ArrayList<Ghost>();

		// we set up our characters before reading the map; we're going to position them based off the map though
		//
		// create MS HAC MAN
		player = new Player();
		player.numLives = kPlayerLivesStart;

		// read in the current level
		BufferedImage levelImg = ImageIO.read(ScenePlay.class.getResourceAsStream("/images/hacman/hacman_lvl1.png"));

		// we generate walls, ghosts, pellets (regular and power), and the player from the map
		// we do this by color
		for (int iX = 0; iX < levelImg.getWidth(); ++iX) {
			for (int iY = 0; iY < levelImg.getHeight(); ++iY) {

				int pixelCol = levelImg.getRGB(iX,iY) & 0xFFFFFF;

				// WALLS!
				if (pixelCol == kSpawnWallColor) {
					Transform wall = new Transform();
					wall.x = iX;
					wall.y = iY;
					walls.add(wall);
				}

				// MS. HACK MAN
				else if (pixelCol == kSpawnPlayerColor) {
					player.transform.setStartPosition(iX,iY);
				}

				// PELLETS
				//
				// regular
				else if (pixelCol == kSpawnPelletColor) {
					Pellet pellet = new Pellet();
					pellet.transform.x = iX;
					pellet.transform.y = iY;
					pellets.add(pellet);
				}
				// powerrr!!!
				else if (pixelCol == kSpawnPelletPowerColor) {
					Pellet pellet = new Pellet();
					pellet.isPower = true;
					pellet.transform.x = iX;
					pellet.transform.y = iY;
					pellets.add(pellet);
				}

				// G-G-Gh-Ghosts!
				//
				// INKY, PINKY, BLINKY, & CLYDE
				else if (pixelCol == kSpawnGhostInkyColor ||
					pixelCol == kSpawnGhostPinkyColor ||
					pixelCol == kSpawnGhostBlinkyColor ||
					pixelCol == kSpawnGhostClydeColor) {

					Ghost ghost = new Ghost();
					ghost.hue = 0.65f; // for now
					ghost.transform.setStartPosition(iX,iY);
					ghosts.add(ghost);
				}

				// meh
				else {
					System.out.println("undetected pixel color: " + pixelCol);
				}


			}
		}

		// TODO: generate pellets from map
		// pellets = new ArrayList<Pellet>();

		// Pellet pellet = new Pellet();
		// pellet.transform.x = 2;
		// pellet.transform.y = 8;
		// pellets.add(pellet);

		// Pellet pellet2 = new Pellet();
		// pellet2.isPower = true;
		// pellet2.transform.x = 5;
		// pellet2.transform.y = 5;
		// pellets.add(pellet2);

		// create the ghosts
		// ghosts = new ArrayList<Ghost>();

		// create INKY, PINKY, BLINKY, and CLYDE
		// Ghost inky = new Ghost();
		// inky.hue = 0.65f;
		// ghosts.add(inky);

		// TODO: position ghosts based on spawn points of map
		// inky.transform.setStartPosition(4,10);

		// TODO: position player based on spawn point of map
		// player.transform.setStartPosition(4,8);

		// COLLISIONS!
		//
		// our collision system requires only moving objects to map to things with which they can collide
		collisionMap = new HashMap<Transform,ArrayList<Transform>>();
		//
		// collide player with walls
		ArrayList<Transform> playerColliders = new ArrayList<Transform>();
		for (Transform transform : walls) {
			playerColliders.add(transform);
		}
		collisionMap.put(player.transform,playerColliders);
		// collide ghosts with walls
		ArrayList<Transform> ghostColliders = new ArrayList<Transform>();
		for (Transform transform : walls) {
			ghostColliders.add(transform);
		}
		for (Ghost ghost : ghosts) {
			collisionMap.put(ghost.transform,ghostColliders);
		}

		// all moving objects should be added to the physics list, so they can be moved
		physicsTransfoms = new ArrayList<Transform>();
		physicsTransfoms.add(player.transform);
		for (Ghost ghost : ghosts) {
			physicsTransfoms.add(ghost.transform);
		}

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