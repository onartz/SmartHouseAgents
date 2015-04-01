package meteo.onto;

import jade.content.Predicate;

public class ExceedsWindLimit implements Predicate{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	int limit;
	public int getLimit() {
		return limit;
	}
	public void setLimit(int limit) {
		this.limit = limit;
	}
	public float getValue() {
		return value;
	}
	public void setValue(float value) {
		this.value = value;
	}
	float value;


}
