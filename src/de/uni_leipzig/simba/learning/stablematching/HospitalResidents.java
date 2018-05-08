/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.learning.stablematching;

import de.uni_leipzig.simba.data.Mapping;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 *
 * @author ngonga
 */
public class HospitalResidents {

    static Logger logger = Logger.getLogger("LIMES");

    HashMap<Integer, String> residentReverseIndex = new HashMap<Integer, String>();
        HashMap<String, Integer> hospitalIndex = new HashMap<String, Integer>();
        HashMap<String, Integer> residentIndex = new HashMap<String, Integer>();
        HashMap<Integer, String> hospitalReverseIndex = new HashMap<Integer, String>();

    /** Implements hospital/residents for a similarity mapping. Can be used for
     * detecting stable matching between properties and classes
     * @param m Input Mapping
     * @return Stable matching including weights
     */
    public Mapping getMatching(Mapping m) {
        residentReverseIndex = new HashMap<Integer, String>();
        hospitalIndex = new HashMap<String, Integer>();
        residentIndex = new HashMap<String, Integer>();
        hospitalReverseIndex = new HashMap<Integer, String>();
        //index residents and hospitals
        int rCounter = 0;
        int hCounter = 0;
        for (String key : m.map.keySet()) {
            residentReverseIndex.put(rCounter, key);
            residentIndex.put(key, rCounter);
            rCounter++;
            for (String value : m.map.get(key).keySet()) {
                if (!hospitalIndex.containsKey(value)) {
                    hospitalIndex.put(value, hCounter);
                    hospitalReverseIndex.put(hCounter, value);
                    hCounter++;
                }
            }
        }
        //System.out.println(hospitalIndex);
        //now create resident and hospital list
        logger.info(hCounter + " hospitals and " + rCounter + " residents");
        ArrayList<Resident> residents = new ArrayList<Resident>();
        ArrayList<Hospital> hospitals = new ArrayList<Hospital>();

        //create resident list
        for (int i = 0; i < rCounter; i++) {
            String r = residentReverseIndex.get(i);
            double preferences[] = new double[hCounter];
            //double sortedPreferences[] = new double[hCounter];
            //init
            for (int j = 0; j < hCounter; j++) {
                preferences[j] = 0;
                //sortedPreferences[j] = 0;
            }

            //create resident preference list
            int index;
            for (String h : m.map.get(r).keySet()) {
                index = hospitalIndex.get(h);
                //System.out.println(r+"\t"+h+"\t"+index+"\t"+m.getSimilarity(r, h));
                preferences[index] = m.getSimilarity(r, h);
            }

            //System.out.print("Preferences===\n"+r+"\t");
            for (int j = 0; j < hCounter; j++) {
                //System.out.print(preferences[j]+"\t");
            }
            preferences = sort(preferences);
            //System.out.print("Sorted preferences===\n"+r+"\t");
            for (int j = 0; j < hCounter; j++) {
                //System.out.print(preferences[j]+"\t");
            }
            residents.add(new Resident(i, preferences));
            residents.get(i).label = r;
            //System.out.println(residents.get(i).label + " has preferences " + residents.get(i).hospitalToWeight);
        }

        //create hospital list
        for (int i = 0; i < hCounter; i++) {
            String h = hospitalReverseIndex.get(i);
            double preferences[] = new double[rCounter];
            //init
            for (int j = 0; j < rCounter; j++) {
                preferences[j] = 0;
            }
            //create resident preference list
            for (String r : m.map.keySet()) {
                int index = residentIndex.get(r);
                preferences[index] = m.getSimilarity(r, h);
            }
            preferences = sort(preferences);
            if(rCounter%hCounter == 0)
            hospitals.add(new Hospital(i, (int)(rCounter / hCounter), preferences));
            else
                hospitals.add(new Hospital(i, (int)(rCounter / hCounter)+1, preferences));
            hospitals.get(i).label = h;
        }

        Mapping result = new Mapping();
        ArrayList<ArrayList<Integer>> match = getMatching(residents, hospitals);
        String uri1, uri2;
        for (int i = 0; i < match.size(); i++) {
            for (int j = 0; j < match.get(i).size(); j++) {
                uri1 = residentReverseIndex.get(match.get(i).get(j));
                uri2 = hospitalReverseIndex.get(i);
                result.add(uri1, uri2, m.getSimilarity(uri1, uri2));
            }
        }
        return result;
    }

    public double[] sort(double[] input) {
        double[] result = new double[input.length];
        double max;
        int index;
        //logger.info(hospitalReverseIndex);
//        for (int k = 0; k < input.length; k++) {
//            logger.info("Input["+k+"] = "+input[k]);
//        }
        for (int k = 0; k < input.length; k++) {
            max = -2;
            index = 0;

            for (int jj = 0; jj < input.length; jj++) {
                if (input[jj] > max) {
                    max = input[jj];
                    index = jj;
                    //logger.info("max = "+max);
                    //logger.info("index = "+index);
                }
            }
            //logger.info("Hospital "+ hospitalReverseIndex.get(index)+" at position "+(input.length - k -1));
            result[input.length - k -1] = index;            
            input[index] = -1.0;
        }
        //System.exit(1);
        //System.out.println("");
        return result;
    }

    public ArrayList<ArrayList<Integer>> getMatching(ArrayList<Resident> r, ArrayList<Hospital> h) {
        ArrayList<Integer> unmatched = new ArrayList<Integer>();
        for (int i = 0; i < r.size(); i++) {
            unmatched.add(i);
        }
        //logger.info(hospitalReverseIndex);
        // implement hospital resident
        int rejected;
        while (unmatched.size() > 0) {
            // get current resident
            logger.info("unmatched = "+unmatched);
            int residentID = unmatched.get(0);
            unmatched.remove(0);
            int bestHospital = r.get(residentID).getNextChoice();
            //logger.info(r.get(residentID).label + " picks "+hospitalReverseIndex.get(bestHospital));
            rejected = h.get(bestHospital).grantAdmission(residentID);
            if (rejected >= 0) {
                System.out.println("Unmatched = "+unmatched+"; Rejected = "+rejected);
                unmatched.add(rejected);
            }
        }

        //builds the final map
        ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();
        for (int i = 0; i < h.size(); i++) {
            result.add(h.get(i).acceptedResidents);
        }
        System.out.println("Results = "+result);
        return result;
    }

    public static void main(String args[]) {
        double[] input = new double[6];
        input[0] = 0;
        input[1] = 0;
        input[2] = 0;
        input[3] = 0;
        input[4] = 1;
        input[5] = 1;

        //double[] result = new (HospitalResidents()).sort(input);
//        for (int i = 0; i < result.length; i++) {
//            //System.out.println(result[i]);
//        }
        //        double preferences[][] = new double[3][3];
//        double hpreferences[][] = new double[3][3];
//
//        preferences[0][0] = 1;
//        preferences[0][1] = 2;
//        preferences[0][2] = 3;
//        preferences[1][0] = 1;
//        preferences[1][1] = 2;
//        preferences[1][2] = 3;
//        preferences[2][0] = 1;
//        preferences[2][1] = 3;
//        preferences[2][2] = 2;
//        ArrayList<Resident> r = new ArrayList<Resident>();
//        r.add(new Resident(0, preferences[0]));
//        r.add(new Resident(1, preferences[1]));
//        r.add(new Resident(2, preferences[2]));
//
//        hpreferences[0][0] = 2;
//        hpreferences[0][1] = 1;
//        hpreferences[0][2] = 3;
//        hpreferences[1][0] = 3;
//        hpreferences[1][1] = 1;
//        hpreferences[1][2] = 2;
//        hpreferences[2][0] = 1;
//        hpreferences[2][1] = 3;
//        hpreferences[2][2] = 2;
//        ArrayList<Hospital> h = new ArrayList<Hospital>();
//        h.add(new Hospital(0, 1, hpreferences[0]));
//        h.add(new Hospital(1, 1, hpreferences[1]));
//        h.add(new Hospital(2, 1, hpreferences[2]));
//
//        //System.out.println(getMatching(r, h));

    }
}
