package edu.mit.d54.plugins.hacman;

import java.util.ArrayList;
import java.util.HashMap;
import java.awt.image.BufferedImage;

import edu.mit.d54.Display2D;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;


public class ScenePlay extends Object {

	private static final int kSpawnWallColor = 0x000080;
	private static final int kSpawnPlayerColor = 0xFFFF00;
	private static final int kSpawnPelletColor = 0x333333;
	private static final int kSpawnPelletPowerColor = 0xFFFFFF;
	private static final int kSpawnGhostInkyColor = 0x00FFFF;
	private static final int kSpawnGhostPinkyColor = 0xFF0080;
	private static final int kSpawnGhostBlinkyColor = 0xFF0000;
	private static final int kSpawnGhostClydeColor = 0xFF8000;

	private static final float wooblinessPeriod = 15.0f;
	private float wobblinessTimer;

	private Display2D d;

	public enum State {Playing, Won, Lost};
	public State state;

	private static final float kPlayerSpeed = 4.0f;
	private static final float kGhostSpeed = 2.0f;

	private Player player;

	private ArrayList<Transform> physicsTransfoms;
	private long timeSinceLastUpdate;

	private HashMap<Transform,ArrayList<Transform>> collisionMap;

	private ArrayList<Transform> walls;
	private ArrayList<Pellet> pellets;
	private ArrayList<Ghost> ghosts;

	public ScenePlay(Display2D display, String levelName) throws IOException {

		d = display;

		// we need to set up our arrays before we populate them
		walls = new ArrayList<Transform>();
		pellets = new ArrayList<Pellet>();
		ghosts = new ArrayList<Ghost>();

		// we set up our characters before reading the map; we're going to position them based off the map though
		//
		// create MS HAC MAN
		player = new Player();

		// read in the current level
		BufferedImage levelImg = ImageIO.read(ScenePlay.class.getResourceAsStream(levelName));

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

				// MS. HAC MAN
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

					// spooooooky fancy math to get the right hue for the rgb value
					float r = (float)((pixelCol >> 16) & 0xFF) / 255.0f;
					float g = (float)((pixelCol >> 8) & 0xFF) / 255.0f;
					float b = (float)((pixelCol >> 0) & 0xFF) / 255.0f;

					float max = Math.max(r,Math.max(g,b));
					float min = Math.min(r,Math.min(g,b));

					float hue = 0.0f;
					if (max == r) {hue = (g - b)/(max - min);}
					else if (max == g) {hue = 2.0f + (b - r)/(max - min);}
					else if (max == b) {hue = 4.0f + (r - g)/(max - min);}

					hue /= 6.0f;
					if (hue < 0.0f) {hue += 1.0f;}

					ghost.hue = hue;

					ghost.transform.setStartPosition(iX,iY);
					ghosts.add(ghost);

				}

				// // meh
				// else {
				// 	System.out.println("undetected pixel color: " + pixelCol);
				// }


			}
		}

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

	public void restartLevel() {

		// if we don't set the state, the game will still think it's losing/winning
		state = State.Playing;

		player.transform.reset();

		for (Ghost ghost : ghosts) {
			ghost.transform.reset();
		}
	}

	public void update() {

		// it's just nice to have a delta... and we need it later
		long currentTime = System.nanoTime();
		float dt = (float)(currentTime - timeSinceLastUpdate) / 1000000000.0f;
		timeSinceLastUpdate = currentTime;

		// Advance to the next stage if all pellets have been eaten! (cycle back to the first if it's the last one)
		if (pellets.size() <= 0) {state = State.Won;}

		// Lose a life if the player gets haunted or whatever by a ghost
		for (Ghost ghost : ghosts) {
			if ((int)player.transform.x == (int)ghost.transform.x && (int)player.transform.y == (int)ghost.transform.y) {
				
				if (ghost.isWoobly) {
					ghost.transform.reset();
					ghost.isWoobly = false;
				} else {
					state = State.Lost;
				}
			}
		}
		// Then reset ghost and player positions; pellets should remain as they were

		// PHYSICS!
		UpdatePhysicsTransforms(dt);

		// UnWobblify Ghosts
		if (wobblinessTimer > 0) {
			wobblinessTimer -= dt;
			if (wobblinessTimer < 0) {
				for (Ghost ghost : ghosts) {
					ghost.isWoobly = false;
				}
			}
		}

		// Eat Pellets
		boolean didEatPowerPellet = false;
		ArrayList<Pellet> pelletsToRemove = new ArrayList<Pellet>();
		for (Pellet pellet : pellets) {
			if ((int)pellet.transform.x == (int)player.transform.x && (int)pellet.transform.y == (int)player.transform.y) {
				if (pellet.isPower) {didEatPowerPellet = true;}
				// System.out.println("om nom");
				pelletsToRemove.add(pellet);
			}
		}
		for (Pellet pellet : pelletsToRemove) {
			pellets.remove(pellet);
		}
		// the ghosts get all woobly if you eat a power pellet
		if (didEatPowerPellet) {
			// System.out.println("POWER!");
			for (Ghost ghost : ghosts) {
				wobblinessTimer = wooblinessPeriod;
				ghost.isWoobly = true;
			}
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
			float hue = ghost.isWoobly ? 0.65f : ghost.hue;
			d.setPixelHSB((int)ghost.transform.x,(int)ghost.transform.y,hue,1.0f,1.0f);
		}
		// draw player
		d.setPixelHSB((int)player.transform.x,(int)player.transform.y,0.15f,1,1);
		// draw fruit(?)

	}

	public void MovePlayer(float dx, float dy) {
		player.transform.setVelocity(dx * kPlayerSpeed, dy * kPlayerSpeed);
	}

	private void UpdatePhysicsTransforms(float dt) {

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