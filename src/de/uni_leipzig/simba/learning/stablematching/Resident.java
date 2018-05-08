/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.stablematching;

import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class Resident {

    HashMap<Integer, Double> hospitalToWeight;
    int currentChoice;
    List<Double> sortedPreferences;
    int ID;
    public String label="";

    public Resident(int idValue, double[] preferences) {
        ID = idValue;
        
        hospitalToWeight = new HashMap<Integer, Double>();
        //sortedPreferences = new ArrayList<Double>();
        for (int i = 0; i < preferences.length; i++) {
            hospitalToWeight.put(i, preferences[i]);
            System.out.println("Adding "+preferences[i]+" at position "+i);
            //sortedPreferences.add(preferences[i]);
        }
        System.out.println("**** H2Weight = "+hospitalToWeight);
        currentChoice = hospitalToWeight.size();
        //Collections.sort(sortedPreferences);
        //System.exit(1);
        //System.out.println("Preferences of resident "+ID+" with label "+label+" are "+hospitalToWeight);
    }

    public int getNextChoice() {
        currentChoice--;
        System.out.println("Current choice  = " +currentChoice);
        return hospitalToWeight.get(currentChoice).intValue();
    }
}
