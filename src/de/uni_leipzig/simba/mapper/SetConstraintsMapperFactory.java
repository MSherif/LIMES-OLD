/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.mapper;

import de.uni_leipzig.simba.cache.Cache;
import de.uni_leipzig.simba.filter.Filter;
import de.uni_leipzig.simba.io.KBInfo;

/**
 * Needs renaming to mapperfactory
 * @author ngonga
 */
public class SetConstraintsMapperFactory {

    public static SetConstraintsMapper getMapper(String name, KBInfo sInfo, KBInfo tInfo, Cache s, Cache t, Filter f, int granularity) {
        if (name.equalsIgnoreCase("filterbased")) {
            return new FilterBasedSetConstraintsMapper(sInfo, tInfo, s, t, f);
        } else {
            return new SimpleSetConstraintsMapper(sInfo, tInfo, s, t, f, granularity);
        }
    }
}
