package meteo.onto;

import FIPA.DateTime;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;

/*
 * Is relative to wind
 * Est-ce que le vent est fort?
 * Est-ce que le vent sera fort dans 3h?
 * Le vent est fort
 * Le vent sera fort dans 6h
 * Quand le vent était il fort la dernière fois?
 */
public class IsStrong implements Predicate{

	private Wind wind;
	private DateTime when;

	public DateTime getWhen() {
		
		return when;
	}

	public void setWhen(DateTime when) {
		this.when = when;
	}

	public Wind getWind() {
		return wind;
	}

	public void setWind(Wind wind) {
		this.wind = wind;
	}
	
}
