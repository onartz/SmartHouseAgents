package meteo;

import jade.content.onto.BeanOntology;
import jade.content.onto.BeanOntologyException;

/**
 * This singleton class automatically generate an ontology from the class in the
 * ambiflux.onto package
 *
 */
public class Ontology extends BeanOntology {

	/**
	 * serial version to make Eclipse stop complaining
	 */
	private static final long serialVersionUID = -577945769459604716L;

	public static final String NAME = "meteo-ontology";

	public static final String DF_METEO_AGENT_TYPE = "meteo";

	private static Ontology theInstance = new Ontology(NAME);

	// public static final String DF_INVENTORY_AGENT_TYPE = "inventory";
	// public static final String DF_PRODUCT_ID_PROPERTY = "productID";

	public static Ontology getInstance() {
		return theInstance;
	}

	public Ontology(String name) {
		super(name);
		try {
			add("meteo.onto");
		} catch (BeanOntologyException e) {
			// can't happen
			e.printStackTrace();
		}
	}

}
