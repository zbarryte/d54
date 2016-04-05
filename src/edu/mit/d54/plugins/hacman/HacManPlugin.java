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

	private ArcadeController controller;

	private ScenePlay scenePlay;

	public HacManPlugin(Display2D display, double framerate) throws IOException {

		super(display, framerate);

		// we need to do this so we can set the listener later
		controller = ArcadeController.getInstance();

		ResetGame();
	}

	private void ResetGame() {
		scenePlay = new ScenePlay(getDisplay());
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
		if (scenePlay.state == ScenePlay.State.Won) {ResetGame();}

		// TODO: increment the map on a win
		if (scenePlay.state == ScenePlay.State.Lost) {ResetGame();}

		// update the game
		scenePlay.update();
	}

}
