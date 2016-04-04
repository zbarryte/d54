package edu.mit.d54.plugins.hacman;

public class Transform extends Object {

	public float x;
	public float y;

	public float vx;
	public float vy;

	public Transform() {

	}

	public void setVelocity(float _vx, float _vy) {
		vx = _vx;
		vy = _vy;
	}
}