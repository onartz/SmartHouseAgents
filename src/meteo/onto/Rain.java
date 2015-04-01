package meteo.onto;

public class Rain extends WeatherPhenomenon{
	
	private float speed;
	private float direction;
	//private 
	

	public Rain(float speed, float direction) {
		super();
		this.speed = speed;
		this.direction = direction;
	}

	public Rain() {
		speed = 0;
		direction = 0;
	}

	public float getSpeed() {
		return speed;
	}
	
	public void setSpeed(float speed) {
		this.speed = speed;
	}
	public float getDirection() {
		return direction;
	}
	public void setDirection(float direction) {
		this.direction = direction;
	}
}
