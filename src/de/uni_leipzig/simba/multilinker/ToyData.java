/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.multilinker;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.cache.MemoryCache;

/**
 *
 * @author ngonga
 */
public class ToyData {
    public static Cache generateToyData()
    {
        Cache c = new MemoryCache();
        
        c.addTriple("1", "rdfs:label", "Jacob Aagaard");
//        c.addTriple("1", "foaf:name", "Jacob Aagaard");
        c.addTriple("2", "rdfs:label", "Manuel Aaron");
//        c.addTriple("2", "foaf:name", "Manuel Aaron");
        c.addTriple("3", "rdfs:label", "Magnus Carlsen");
//        c.addTriple("3", "foaf:name", "Magnus Carlsen");
        c.addTriple("4", "rdfs:label", "Levon Aronian");
//        c.addTriple("4", "foaf:name", "Levon Aronian");
        c.addTriple("5", "rdfs:label", "Kramnik");
//        c.addTriple("5", "foaf:name", "Kramnik");
        c.addTriple("6", "rdfs:label", "Nigel Short");
//        c.addTriple("6", "foaf:name", "Nigel Short");
        c.addTriple("7", "rdfs:label", "Emmanuel Lasker");
//        c.addTriple("7", "foaf:name", "Emmanuel Lasker");
        c.addTriple("8", "rdfs:label", "John Dominguez");
//        c.addTriple("8", "foaf:name", "John Dominguez");
        c.addTriple("9", "rdfs:label", "Daniel King");
//        c.addTriple("9", "foaf:name", "Daniel King");
        c.addTriple("10", "rdfs:label", "Simon Williams");
//        c.addTriple("10", "foaf:name", "Simon Williams");
        
        return c;
    }
	public static Cache generateToyData(int i)
	{
		Cache c = new MemoryCache();
		switch(i){
		case 0:
			c.addTriple("1", "rdfs:label", "Jacob Aagaard");
			c.addTriple("1", "foaf:name", "Jacob Aagaard");
			c.addTriple("2", "rdfs:label", "Manuel Aaron");
			c.addTriple("2", "foaf:name", "Manuel Aaron");
			c.addTriple("3", "rdfs:label", "Magnus Carlsen");
			c.addTriple("3", "foaf:name", "Magnus Carlsen");
			c.addTriple("4", "rdfs:label", "Levon Aronian");
			c.addTriple("4", "foaf:name", "Levon Aronian");
			c.addTriple("5", "rdfs:label", "Kramnik");
			c.addTriple("5", "foaf:name", "Kramnik");
			c.addTriple("6", "rdfs:label", "Nigel Short");
			c.addTriple("6", "foaf:name", "Nigel Short");
			c.addTriple("7", "rdfs:label", "Emmanuel Lasker");
			c.addTriple("7", "foaf:name", "Emmanuel Lasker");
			c.addTriple("8", "rdfs:label", "John Dominguez");
			c.addTriple("8", "foaf:name", "John Dominguez");
			c.addTriple("9", "rdfs:label", "Daniel King");
			c.addTriple("9", "foaf:name", "Daniel King");
			c.addTriple("10", "rdfs:label", "Simon Williams");
			c.addTriple("10", "foaf:name", "Simon Williams");
			c.addTriple("11", "rdfs:label", "Khaled Tawfek");
			c.addTriple("11", "foaf:name", "Khaled Tawfek");
			break;
		case 1:
			c.addTriple("1", "rdfs:label", "J. Aagaard");
			c.addTriple("1", "foaf:name", "J. Aagaard");
			c.addTriple("2", "rdfs:label", "Aaron Manuel ");
			c.addTriple("2", "foaf:name", "Aaron Manuel");
			c.addTriple("3", "rdfs:label", "Mgnus Crlsen");
			c.addTriple("3", "foaf:name", "Mgnus Crlsen");
			c.addTriple("4", "rdfs:label", "Levon Aronian");
			c.addTriple("4", "foaf:name", "Levon Aronian");
			c.addTriple("5", "rdfs:label", "Kramnik");
			c.addTriple("5", "foaf:name", "Kramnik");
			c.addTriple("6", "rdfs:label", "Nigel Short");
			c.addTriple("6", "foaf:name", "Nigel Short");
			c.addTriple("7", "rdfs:label", "Emmanuel Lasker");
			c.addTriple("7", "foaf:name", "Emmanuel Lasker");
			c.addTriple("8", "rdfs:label", "John Dominguez");
			c.addTriple("8", "foaf:name", "John Dominguez");
			c.addTriple("9", "rdfs:label", "Daniel King");
			c.addTriple("9", "foaf:name", "Daniel King");
			c.addTriple("10", "rdfs:label", "Simon Williams");
			c.addTriple("10", "foaf:name", "Simon Williams");
			c.addTriple("11", "rdfs:label", "Simon Williams");
			c.addTriple("11", "foaf:name", "Simon Williams");
			break;
		case 2:
			c.addTriple("1", "rdfs:label", "Jacob Aagaard");
			c.addTriple("1", "foaf:name", "Jacob Aagaard");
			c.addTriple("2", "rdfs:label", "Manuel Aaron");
			c.addTriple("2", "foaf:name", "Manuel Aaron");
			c.addTriple("3", "rdfs:label", "Magnus Carlsen");
			c.addTriple("3", "foaf:name", "Magnus Carlsen");
			c.addTriple("4", "rdfs:label", "A.Levon");
			c.addTriple("4", "foaf:name", "Aronian Levon ");
			c.addTriple("5", "rdfs:label", "Kranmik");
			c.addTriple("5", "foaf:name", "Kramnk");
			c.addTriple("6", "rdfs:label", "N. Short");
			c.addTriple("6", "foaf:name", "Nigel");
			c.addTriple("7", "rdfs:label", "Emmanuel Lasker");
			c.addTriple("7", "foaf:name", "Emmanuel Lasker");
			c.addTriple("8", "rdfs:label", "John Dominguez");
			c.addTriple("8", "foaf:name", "John Dominguez");
			c.addTriple("9", "rdfs:label", "Daniel King");
			c.addTriple("9", "foaf:name", "Daniel King");
			c.addTriple("10", "rdfs:label", "Simon Williams");
			c.addTriple("10", "foaf:name", "Simon Williams");
			c.addTriple("11", "rdfs:label", "Khaled Tawfek");
			c.addTriple("11", "foaf:name", "Khaled Tawfek");
			break;
		case 3:
			c.addTriple("1", "rdfs:label", "Jacob Aagaard");
			c.addTriple("1", "foaf:name", "Jacob Aagaard");
			c.addTriple("2", "rdfs:label", "Manuel Aaron");
			c.addTriple("2", "foaf:name", "Manuel Aaron");
			c.addTriple("3", "rdfs:label", "Magnus Carlsen");
			c.addTriple("3", "foaf:name", "Magnus Carlsen");
			c.addTriple("4", "rdfs:label", "Levon Aronian");
			c.addTriple("4", "foaf:name", "Levon Aronian");
			c.addTriple("5", "rdfs:label", "Kramnik");
			c.addTriple("5", "foaf:name", "Kramnik");
			c.addTriple("6", "rdfs:label", "Nigel Short");
			c.addTriple("6", "foaf:name", "Nigel Short");
			c.addTriple("7", "rdfs:label", "Emanuel Lasker");
			c.addTriple("7", "foaf:name", "Lasker Emmanuel");
			c.addTriple("8", "rdfs:label", "John D.");
			c.addTriple("8", "foaf:name", "J. Dominguez");
			c.addTriple("9", "rdfs:label", "Deniel Keng");
			c.addTriple("9", "foaf:name", "King Daneil");
			c.addTriple("10", "rdfs:label", "Simon Williams");
			c.addTriple("10", "foaf:name", "Simon Williams");
			c.addTriple("11", "rdfs:label", "Khaled Tawfek");
			c.addTriple("11", "foaf:name", "Khaled Tawfek");
			break;
		case 4:
			c.addTriple("1", "rdfs:label", "Jacob A.");
			c.addTriple("1", "foaf:name", "Jacub Aagard");
			c.addTriple("2", "rdfs:label", "Manuel Aaron");
			c.addTriple("2", "foaf:name", "Manuel Aaron");
			c.addTriple("3", "rdfs:label", "Magnus Carlsen");
			c.addTriple("3", "foaf:name", "Magnus Carlsen");
			c.addTriple("4", "rdfs:label", "Levon Aronian");
			c.addTriple("4", "foaf:name", "Levon Aronian");
			c.addTriple("5", "rdfs:label", "Kramnik");
			c.addTriple("5", "foaf:name", "Kramnik");
			c.addTriple("6", "rdfs:label", "Short N.");
			c.addTriple("6", "foaf:name", "Nigel S.");
			c.addTriple("7", "rdfs:label", "Emmanuel Lasker");
			c.addTriple("7", "foaf:name", "Emmanuel Lasker");
			c.addTriple("8", "rdfs:label", "John Dominguez");
			c.addTriple("8", "foaf:name", "John Dominguez");
			c.addTriple("9", "rdfs:label", "Daniel King");
			c.addTriple("9", "foaf:name", "Daniel King");
			c.addTriple("10", "rdfs:label", "Simon Wiliams");
			c.addTriple("10", "foaf:name", "Williams S.");
			c.addTriple("11", "rdfs:label", "Khaled Tawfek");
			c.addTriple("11", "foaf:name", "Khaled Tawfek");
			break;
		default:
			return null;
		}
		return c;
	}
}
