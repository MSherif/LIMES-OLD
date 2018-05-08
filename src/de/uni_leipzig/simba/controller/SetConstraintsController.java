/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.controller;

import de.uni_leipzig.simba.data.Mapping;

/**
 *
 * @author ngonga
 */
public class SetConstraintsController {

    public Mapping getLinks(String expression, double threshold) {
        Mapping map = new Mapping();
        Parser p = new Parser(expression, threshold);
        if (p.isAtomic()) {
            //this is where business happens
            //if metric is string metric then call PPJoin
            //else call LIMES
        } else {
            Mapping map1 = getLinks(p.term1, p.threshold1);
            Mapping map2 = getLinks(p.term2, p.threshold1);
            //special as we need to do some testing
            if (p.op.equals("add")) {
                map = mergeMaps(p.op, map1, map2, expression, threshold);
            } else {
                map = mergeMaps(p.op, map1, map2, null, threshold);
            }
        }
        return map;
    }

    public Mapping mergeMaps(String operation, Mapping map1, Mapping map2, String condition, double threshold) {
        Mapping map = new Mapping();
        if (operation.equals("max") || operation.equals("or")) {
            map = union(map1, map2);
        }
        else if (operation.equals("and") || operation.equals("min")) {
            map = intersection(map1, map2);
        }
        else if (operation.equals("minus")) {
            map = difference(map1, map2);
        }
        else if (operation.equals("xor"))
        {
            map = union(difference(map1, map2), difference(map2, map1));
        }
        else if (operation.equals("add")) {
            map = union(map1, map2);
            //map = filter(map, condition, threshold);
            //need a condition that still needs to be tested on the union of sets
        }
        return map;
    }

    /** Computes the difference of two mappings.
     * @param map1 First mapping
     * @param map2 Second mapping
     * @return map1 \ map2
     */
    public static Mapping difference(Mapping map1, Mapping map2) {
        Mapping map = new Mapping();

        //go through all the keys in map1
        for (String key : map1.map.keySet()) {
            //if the first term (key) can also be found in map2
            if (map2.map.containsKey(key)) {
                //then go through the second terms and checks whether they can
                //be found in map2 as well
                for (String value : map1.map.get(key).keySet()) {
                    //if yes, take the highest similarity
                    if(!map2.map.get(key).containsKey(value))
                    {
                      map.add(key, value, map1.map.get(key).get(value));
                    }
                }
            }
            else
            {
                map.add(key, map1.map.get(key));
            }
        }
        return map;
    }

    /** Computes the intersection of two mappings. In case an entry exists in both mappings
     * the minimal similarity is taken
     * @param map1 First mapping
     * @param map2 Second mapping
     * @return Intersection of map1 and map2
     */
    public static Mapping intersection(Mapping map1, Mapping map2) {
        Mapping map = new Mapping();

        //go through all the keys in map1
        for (String key : map1.map.keySet()) {
            //if the first term (key) can also be found in map2
            if (map2.map.containsKey(key)) {
                //then go through the second terms and checks whether they can
                //be found in map2 as well
                for (String value : map1.map.get(key).keySet()) {
                    //if yes, take the highest similarity
                    if(map2.map.get(key).containsKey(value))
                    {
                        if(map1.map.get(key).get(value) <= map2.map.get(key).get(value))
                            map.add(key, value, map1.map.get(key).get(value));
                        else
                            map.add(key, value, map2.map.get(key).get(value));
                    }
                }
            }
        }
        return map;
    }
    /** Computes the union of two mappings. In case an entry exists in both mappings
     * the maximal similarity is taken
     * @param map1 First mapping
     * @param map2 Second mapping
     * @return Union of map1 and map2
     */
    public static Mapping union(Mapping map1, Mapping map2) {
        Mapping map = new Mapping();

        //go through all the keys in map1
        for (String key : map1.map.keySet()) {
            //if the first term (key) can also be found in map2
            if (map2.map.containsKey(key)) {
                //then go through the second terms and checks whether they can
                //be found in map2 as well
                for (String value : map1.map.get(key).keySet()) {
                    //if yes, take the highest similarity
                    if (map2.map.get(key).keySet().contains(value)) {
                        if (map1.map.get(key).get(value) >= map2.map.get(key).get(value)) {
                            map.add(key, value, map1.map.get(key).get(value));
                        } else {
                            map.add(key, value, map2.map.get(key).get(value));
                        }
                    } //else write as is in map1
                    else {
                        map.add(key, value, map1.map.get(key).get(value));
                    }
                }

            } //else write as is in map1
            else {
                map.add(key, map1.map.get(key));
            }
        }

        //now for map2
        //go through the keys of map2
        for (String key : map2.map.keySet()) {

            for (String value : map2.map.get(key).keySet()) {
                //if key in map then then check all the values to see if
                // they are already in map. If yes, then they were tested
                // before if no, then they are not in map1 and can be added
                if (map.map.keySet().contains(key)) {
                    if (!map.map.get(key).containsKey(value)) {
                        map.add(key, value, map2.map.get(key).get(value));
                    }
                } //if key not in map then key is in map2\map1
                //simply add
                else {
                    map.add(key, map2.map.get(key));
                }
            }
        }
        return map;
    }

    /** Computes the union of two mappings. In case an entry exists in both mappings
     * the minimal similarity is taken so as to reflect the min operator
     * @param map1 First mapping
     * @param map2 Second mapping
     * @return Union of map1 and map2
     */
    public static Mapping min(Mapping map1, Mapping map2) {
        Mapping map = new Mapping();

        //go through all the keys in map1
        for (String key : map1.map.keySet()) {
            //if the first term (key) can also be found in map2
            if (map2.map.containsKey(key)) {
                //then go through the second terms and checks whether they can
                //be found in map2 as well
                for (String value : map1.map.get(key).keySet()) {
                    //if yes, take the highest similarity
                    if (map2.map.get(key).keySet().contains(value)) {
                        if (map1.map.get(key).get(value) <= map2.map.get(key).get(value)) {
                            map.add(key, value, map1.map.get(key).get(value));
                        } else {
                            map.add(key, value, map2.map.get(key).get(value));
                        }
                    } //else write as is in map1
                    else {
                        map.add(key, value, map1.map.get(key).get(value));
                    }
                }

            } //else write as is in map1
            else {
                map.add(key, map1.map.get(key));
            }
        }

        //now for map2
        //go through the keys of map2
        for (String key : map2.map.keySet()) {

            for (String value : map2.map.get(key).keySet()) {
                //if key in map then then check all the values to see if
                // they are already in map. If yes, then they were tested
                // before if no, then they are not in map1 and can be added
                if (map.map.keySet().contains(key)) {
                    if (!map.map.get(key).containsKey(value)) {
                        map.add(key, value, map2.map.get(key).get(value));
                    }
                } //if key not in map then key is in map2\map1
                //simply add
                else {
                    map.add(key, map2.map.get(key));
                }
            }
        }
        return map;
    }

    public static void main(String args[])
    {
        Mapping m1 = new Mapping();
        m1.add("a", "b", 0.5f);
        m1.add("a", "c", 0.4f);
        m1.add("b", "d", 0.4f);

        Mapping m2 = new Mapping();
        m2.add("a", "b", 0.6f);
        m2.add("f", "g", 0.5f);
        m2.add("b", "e", 0.5f);

        Mapping m = union(difference(m1, m2), difference(m2, m1));
        for(String key: m.map.keySet())
        {
            System.out.println(key +" -> "+m.map.get(key));
        }
    }
}
