/* Enca
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.execution;
import org.apache.log4j.Logger;

/**
 * Encapsulates instructions for the execution engine
 * @author ngonga
 */
public class Instruction {
	static Logger logger = Logger.getLogger("LIMES");
    public enum Command {
        RUN, INTERSECTION, UNION, DIFF, RETURN, FILTER, XOR;
    };
    private Command command;
    private String measureExpression;
    private String threshold;
    private int sourceMapping;
    private int targetMapping;
    private int resultIndex;

    /** Constructor
     *
     * @param c Command
     * @param measure Measure expression, for example "trigrams(x.rdfs:label, y.rdfs:label)"
     * @param t Threshold
     * @param source Source mapping
     * @param target  Target mapping
     * 
     */
    public Instruction(Command c, String measure, String threshold, int source, int target, int result) {
        command = c;
        measureExpression = measure;
        this.threshold = threshold;
        sourceMapping = source;
        targetMapping = target;
        resultIndex = result;
    }

    public int getResultIndex() {
        return resultIndex;
    }

    public void setResultIndex(int resultIndex) {
        this.resultIndex = resultIndex;
    }

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public String getMeasureExpression() {
        return measureExpression;
    }

    public void setMeasureExpression(String measureExpression) {
        this.measureExpression = measureExpression;
    }

    public int getSourceMapping() {
        return sourceMapping;
    }

    public void setSourceMapping(int sourceMapping) {
        this.sourceMapping = sourceMapping;
    }

    public int getTargetMapping() {
        return targetMapping;
    }

    public void setTargetMapping(int targetMapping) {
        this.targetMapping = targetMapping;
    }

    public String getThreshold() {
        return threshold;
    }

    public void setThreshold(String threshold) {
        this.threshold = threshold;
    }

    public String toString() {
        String s="";
        if (command.equals(Command.RUN)) {
            s = "RUN\t";
        } else if (command.equals(Command.FILTER)) {
            s = "FILTER\t";
        } else if (command.equals(Command.DIFF)) {
            s = "DIFF\t";
        } else if (command.equals(Command.INTERSECTION)) {
            s = "INTERSECTION\t";
        } else if (command.equals(Command.UNION)) {
            s = "UNION\t";
        } else if (command.equals(Command.XOR)) {
            s = "XOR\t";
        }

        s = s + measureExpression + "\t";
        s = s + threshold + "\t";
        s = s + sourceMapping + "\t";
        s = s + targetMapping + "\t";
        s = s + resultIndex;
        return s;
    }
}
