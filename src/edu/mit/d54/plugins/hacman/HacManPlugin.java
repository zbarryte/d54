package edu.mit.d54.plugins.hacman;

import java.io.IOException;
import edu.mit.d54.ArcadeController;
import edu.mit.d54.ArcadeListener;

import edu.mit.d54.Display2D;
import edu.mit.d54.DisplayPlugin;

import java.util.ArrayList;

/**
 * 
 */
public class HacManPlugin extends DisplayPlugin implements ArcadeListener {

	private static final int numLivesStart = 3;
	private int numLivesCurrent;

	private int levelIndexCurrent;

	private ArrayList<String> levelFilePaths;

	private ArcadeController controller;

	private ScenePlay scenePlay;

	public HacManPlugin(Display2D display, double framerate) throws IOException {

		super(display, framerate);

		// we need to do this so we can set the listener later
		controller = ArcadeController.getInstance();

		// we store the full file path for each file
		levelFilePaths = new ArrayList<String>();
		levelFilePaths.add("/images/hacman/hacman_lvl1.png");
		levelFilePaths.add("/images/hacman/hacman_lvl2.png");
		levelFilePaths.add("/images/hacman/hacman_lvl3.png");

		startNewGame();
	}

	private void startNewGame() {
		numLivesCurrent = numLivesStart;
		levelIndexCurrent = 0;
		startLevel();
	}

	private void startLevel() {
		String fileName = levelFilePaths.get(levelIndexCurrent);

		try{
		  scenePlay = new ScenePlay(getDisplay(),fileName);
		}catch(IOException e){
		  e.printStackTrace();
		}
	}

	private void continueCurrentGame() {

		numLivesCurrent--;

		//System.out.println(numLivesCurrent);

		if (numLivesCurrent < 0) {
			startNewGame();
			return;
		}

		scenePlay.restartLevel();
	}

	private void advanceToNextLevel() {
		levelIndexCurrent ++;
		if (levelIndexCurrent >= levelFilePaths.size()) {levelIndexCurrent = 0;}
		startLevel();
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
				scenePlay.MovePlayer(-1,0);
				break;
			case 'R':
				scenePlay.MovePlayer(1,0);
				break;
			case 'U':
				scenePlay.MovePlayer(0,-1);
				break;
			case 'D':
				scenePlay.MovePlayer(0,1);
				break;
			default:
				break;
		}

	}

	@Override
	protected void loop() {
		
		// TODO: go to the main menu on a loss
		if (scenePlay.state == ScenePlay.State.Lost) {
			continueCurrentGame();
		}

		// on a win, the map gets incremented
		if (scenePlay.state == ScenePlay.State.Won) {
			advanceToNextLevel();
		}

		// update the game
		scenePlay.update();

		// display lives as hud
		Display2D d = getDisplay();
		for (int life = 0; life < numLivesCurrent; ++life) {

			int col = life;
			// right now, there's no way to get more lives, but... why not, right?
			if (life > d.getWidth()) {continue;}
			int row = d.getHeight() - 1;

			d.setPixelHSB(col,row,0.15f,1,1);
		}
	}

}
