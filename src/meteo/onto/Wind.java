package meteo.onto;

public class Wind extends WeatherPhenomenon{
	
	private float speed;
	private float direction;
	private WindLevel windLevel;
	//private 
	

	public WindLevel getWindLevel() {
		return windLevel;
	}

	public Wind(float speed, float direction) {
		super();
		this.speed = speed;
		this.direction = direction;
		if(speed < 1)
			windLevel = new Calm();
		if(speed>=1 && speed <=15)
			windLevel = new Strong();
		if(speed > 15)
			windLevel = new Storm();
	}

	public Wind() {
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
