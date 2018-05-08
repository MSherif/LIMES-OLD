package de.uni_leipzig.simba.learning.refinement.evaluation;

import java.util.LinkedList;
import java.util.List;

import de.uni_leipzig.simba.data.Instance;

public class CoraReference {
	public static final String PROP_refKey = "refKey";
	public static final String PROP_author = "author";
	public static final String PROP_title = "title";
	public static final String PROP_year = "year";
	public static final String PROP_JOINEDVENUE = "inBook";
	public static final String PROP_pages = "pages";
	
	
	public int key = -1;
	public String refKey = "";
	public String author = "";
	public String title = "";
	public String year = "";
	public String pages = "";
	
	
	public String journal = "";
	public String volume = "";
	public String booktitle = "";
	
	
	public Instance toCacheInstance() {
		Instance i = new Instance(""+key);
		i.addProperty(PROP_refKey, refKey);
		i.addProperty(PROP_title, title);
		i.addProperty(PROP_year, year);
		i.addProperty(PROP_author, author);
		i.addProperty(PROP_pages, pages);
		if(journal.length()>0) {
			i.addProperty(PROP_JOINEDVENUE, journal+" "+volume);
		} else {
			i.addProperty(PROP_JOINEDVENUE, booktitle);
		}
		return i;
		
	}
	
	
	public static List<String> getProperties() {
		List<String> props = new LinkedList<String>();
		props.add(PROP_refKey);
		props.add(PROP_author);
		props.add(PROP_title);
		props.add(PROP_pages);
		props.add(PROP_year);
		props.add(PROP_JOINEDVENUE);
		return props;
	}
}

