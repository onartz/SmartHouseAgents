package meteo.onto;

import jade.content.Concept;
import jade.content.Predicate;
import jade.content.onto.annotations.Slot;


public class Alert implements Predicate{
	
	private WindAlert windAlert;
	private TemperatureAlert tempAlert;
	
	private static final long serialVersionUID = 1L;
	
	public class WindAlert{
		String status;
		float value;
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public float getValue() {
			return value;
		}
		public void setValue(float value) {
			this.value = value;
		}
	
		public WindAlert(){
			status = null;
			value = 0;
		}
	}
	
	public class TemperatureAlert{
		String status;
		String type;
		float value;
		public String getStatus() {
			return status;
		}
		public void setStatus(String status) {
			this.status = status;
		}
		public String getType() {
			return type;
		}
		public TemperatureAlert(String status, String type, float value) {
			super();
			this.status = status;
			this.type = type;
			this.value = value;
		}
		public void setType(String type) {
			this.type = type;
		}
		public float getValue() {
			return value;
		}
		public void setValue(float value) {
			this.value = value;
		}
		public TemperatureAlert(){
			status = null;
			type = null;
			value = 0;
		}
		
	}

	
	public WindAlert getWindAlert() {
		return windAlert;
	}

	public void setWindAlert(WindAlert windAlert) {
		this.windAlert = windAlert;
	}

	public TemperatureAlert getTempAlert() {
		return tempAlert;
	}

	public void setTempAlert(TemperatureAlert tempAlert) {
		this.tempAlert = tempAlert;
	}


	
	public Alert() {
		/*windAlert = null;
		tempAlert = null;*/
		windAlert = new WindAlert();
		tempAlert= new TemperatureAlert();
				
	}
	
}
