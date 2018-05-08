/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.transformation.learner;

import java.util.HashMap;
import java.util.Map;
import de.uni_leipzig.simba.transformation.dictionary.Dictionary;

/**
 *
 * @author ngonga
 */
public class Controller {

    public static void main(String args[]) {
        long start = System.currentTimeMillis();
        Map<String, String> input = new HashMap<String, String>();
        input.put("60460 Highway 50 (Street)", "60460 Hwy 50");
        input.put("60461 Highway 45 (PO Box 2239)", "Olathe 60461 Hwy 45");
        input.put("599 N E 83rd St Redmond WA (Street)", "599 Northeast 83rd St Redmond");
        input.put("543 N E 82nd St Redmond WA (Street)", "543 Northeast 82nd St Redmond");
        input.put("1932 Univ Ave Madison WI (Street)", "1932 University Ave Madison WI");
        Dictionary dict = new Dictionary();
        Dictionary revDict = new Dictionary();
        for(String key: input.keySet())
        {
            dict.addRules(key, input.get(key));
            revDict.addRules(input.get(key), key);
        }
        long end = System.currentTimeMillis();
        
        System.out.println(dict);
//        System.out.println(dict.getMostFrequentRules());
        System.out.println(dict.getRulesWithMaxCoverage());
        System.out.println(revDict.getRulesWithMaxCoverage());
        
//        System.out.println(end-start);
    }
}
