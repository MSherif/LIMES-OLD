/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.transformation.dictionary;

import de.uni_leipzig.simba.transformation.stringops.StringOps;

/**
 *
 * @author ngonga
 */
public class Rule implements Comparable {

    String source;
    String target;
    public static String EPSILON = "";
    boolean fromSourceToTarget = true;

    public boolean isFromSourceToTarget() {
        return fromSourceToTarget;
    }

    public void setFromSourceToTarget(boolean fromSourceToTarget) {
        this.fromSourceToTarget = fromSourceToTarget;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String s) {
        source = s;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String t) {
        target = t;
    }

    /**
     * Constructor
     *
     */
    public Rule(String s, String t) {
        source = s;
        target = t;
    }

    @Override
    public int compareTo(Object o) {
        if (o instanceof Rule) {
            Rule r = (Rule) o;
            return (source + target).compareTo(r.getSource() + r.getTarget());
        } else {
            return 1;
        }
    }

    public String toString() {
        if (fromSourceToTarget) {
            return "<" + source + "> -> <" + target + ">, source";
        } else {
            return "<" + source + "> -> <" + target + ">, target";
        }
    }

    /**
     * Returns the coverage of the rule
     *
     */
    public int getCoverage() {
        String splitSource[] = source.split(StringOps.SEPARATOR);
        String splitTarget[] = target.split(StringOps.SEPARATOR);
        return splitSource.length + splitTarget.length;
    }
}
