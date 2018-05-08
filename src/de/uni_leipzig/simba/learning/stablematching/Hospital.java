/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.stablematching;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author ngonga
 */
public class Hospital {

    HashMap<Integer, Double> residentToWeight;
    int currentChoice;
    int capacity;
    List<Double> sortedPreferences;
    ArrayList<Integer> acceptedResidents;
    int ID;
    public String label;
    public Hospital(int idValue, int capacityValue, double[] preferences) {
        ID = idValue;
        residentToWeight = new HashMap<Integer, Double>();
        sortedPreferences = new ArrayList<Double>();
        for (int i = 0; i < preferences.length; i++) {
            residentToWeight.put(i, preferences[i]);
            sortedPreferences.add(preferences[i]);
        }
        capacity = capacityValue;
        Collections.sort(sortedPreferences);
        acceptedResidents = new ArrayList<Integer>();
        System.out.println("Preferences of hospital "+ID+" with capacity "+capacity+" is "+residentToWeight);
    }

    /** Processes an application. If queue full, then get student out that has the
     * smallest weight
     * @param resident ID of resident who apply
     * @return ID of resident that was kicked out or -1 in case none was kicked out
     */
    public int grantAdmission(int resident)
    {
        //System.out.println("Capacity of "+ID+" is "+capacity);
        if(acceptedResidents.size()<capacity)
        {
            acceptedResidents.add(resident);
            System.out.println("Admission granted by "+ID+" to "+resident);
            System.out.println("Accepted residents are now "+acceptedResidents.size()+"/"+capacity);
            System.out.println(ID+"->"+acceptedResidents);
            return -1;
        }
        else
        {
            double min = residentToWeight.get(resident);
            int index = -1;
            for(int i=0; i<acceptedResidents.size(); i++)
            {
                if(residentToWeight.get(acceptedResidents.get(i)) < min)
                {
                    min = residentToWeight.get(resident);
                    index = i;
                }
            }
            if(index == -1)
            {
                System.out.println("Rejection of "+resident+ " by "+ID);
                System.out.println(ID+"->"+acceptedResidents);
                return resident;
            }
            else
            {
                System.out.println("Admission granted by "+ID+" to "+resident);
                int reject = acceptedResidents.get(index);
                acceptedResidents.set(index, resident);
                System.out.println("Rejection of "+index+ " by "+ID);
                System.out.println(ID+"->"+acceptedResidents);
                return reject;
            }
        }
        
    }
}
