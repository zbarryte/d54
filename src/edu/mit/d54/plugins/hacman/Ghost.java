package edu.mit.d54.plugins.hacman;

public class Ghost extends Object {

	public float hue;
	public boolean isWoobly;

	public Transform transform;

	public Ghost() {
		isWoobly = false;
		transform = new Transform();
	}
}