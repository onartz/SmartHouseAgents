package meteo.onto;

import jade.content.Predicate;
/*
 * Predicate : a wind has a level of speed (Strong, Calm....)
 * Example of proposition : actual wind is strong, 
 */

public class HasLevel implements Predicate{
	private Wind wind;
	private WindLevel windLevel;
	
	public Wind getWind() {
		return wind;
	}

	public void setWind(Wind wind) {
		this.wind = wind;
	}

	public WindLevel getWindLevel() {
		return windLevel;
	}

	public void setWindLevel(WindLevel windLevel) {
		this.windLevel = windLevel;
	}
	
	
	

}
