/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_leipzig.simba.metricfactory;



/**
 * We assume that we have to parse things of the forms OP(term1, term2)<threshold
 * @author ngonga
 */
public class Parser {

    String expression;
    public String term1, term2, op;
    public double threshold, threshold1, threshold2;

    public Parser(String input, double theta) {
        expression = input.replaceAll(" ", "");
        expression = expression.toLowerCase();
        threshold = theta;
        getTerms();        
    }

    /** Tests whether an expression is atomic or not
     * 
     * @return True if atomic, else false
     */
    public boolean isAtomic() {
        //tests for operation labels. If they can be found, then our expression
        //is not atomic
        if (!expression.contains("max(") && !expression.contains("min(")
                && !expression.contains("and(") && !expression.contains("or(")
                && !expression.contains("add(") && !expression.contains("xor(")) {
            return true;
        } else {
            return false;
        }
    }

    public String getTerm1() {
        return term1;
    }

    public String getOperation() {
        return op;
    }

    public String getTerm2() {
        return term2;
    }

    public double getCoef1() {
        if (!term1.contains("*")) {
            return 1.0;
        } else {
            String split[] = term1.split("\\*");
            try {
                double coef = Double.parseDouble(split[0]);
                term1 = split[1];
                return coef;
            } catch (Exception e) {
                return 1.0;
            }
        }
    }

    public double getCoef2() {
        if (!term2.contains("*")) {
            return 1.0;
        } else {
            String split[] = term2.split("\\*");
            try {
                double coef = Double.parseDouble(split[0]);
                term2 = split[1];
                return coef;
            } catch (Exception e) {
                return 1.0;
            }
        }
    }
    /** Splits the expression into two terms
     *
     * @return
     */
    public void getTerms() {
        if (!isAtomic()) {
            int counter = 1;
            boolean found = false;
            op = expression.substring(0, expression.indexOf("("));
            String noOpExpression = expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")"));

            //get terms
            //System.out.println(noOpExpression);
            for (int i = 0; i < noOpExpression.length(); i++) {

                if (noOpExpression.charAt(i) == '(') {
                    counter++;
                    found = true;
                } else if (noOpExpression.charAt(i) == ')') {
                    counter--;
                    found = true;

                } else if (counter == 1 && found && noOpExpression.charAt(i) == ',') {
                    term1 = noOpExpression.substring(0, i);
                    term2 = noOpExpression.substring(i + 1);
                }
            }

            //System.out.println("Term 1 = "+term1);
            //System.out.println("Term 2 = "+term2);
            //now compute thresholds based on operations
            //first numeric operations
            if (op.equals("min") || op.equals("max")) {
                threshold1 = threshold;
                threshold2 = threshold;
            } else if (op.equals("add")) {
                op = "add";
                double coef1 = getCoef1();
                double coef2 = getCoef2();
                System.out.println(coef1+" "+coef2);
                threshold1 = (threshold - coef2) / coef1;
                threshold2 = (threshold - coef1) / coef2;
            } //now set constraints. separator for sets and thresholds is |
            //thus one can write
            // AND(sim(a,b)|0.5,sim(b,d)|0.7)
            // and then set global threshold to the minimal value wanted
            else {
            	
                String set1 = term1.split("\\|")[0];
                threshold1 = Double.parseDouble(term1.split("\\|")[1]);
                term1 = set1;
                String set2 = term2.split("\\|")[0];
                threshold2 = Double.parseDouble(term2.split("\\|")[1]);
                term2 = set2;
            }
        } else {
        	 op = expression.substring(0, expression.indexOf("("));
             String noOpExpression = expression.substring(expression.indexOf("(") + 1, expression.lastIndexOf(")"));
             String split[] = noOpExpression.split(",");
             term1 = split[0];
             term2 = split[1];
        }
    }

    public static void testParsing(String s, double threshold) {
        Parser p = new Parser(s, threshold);
        if (p.isAtomic()) {
            System.out.println("-->" + s + " with threshold " + threshold + " will be carried out.");
        } else {
            testParsing(p.term1, p.threshold1);
            testParsing(p.term2, p.threshold2);
            System.out.println("--> <" + p.op + "> will be carried out on " + p.term1 + " and " + p.term2 + " with "
                    + "threshold " + threshold);
        }
    }

    public static void main(String args[]) {
        //Parser p = new Parser("max(add(0.3*sim(a,b),0.3*sim(c,d)), min(sim3(a,b), sim2(c,d)))", 0.9);
        //Parser.testParsing("ADD(0.3*sim(c,d), 0.7*sim(a,b))", 0.9);
//        Parser.testParsing("max(add(0.5*sim(a,b),0.5*sim(c,d)), min(sim3(a,b), sim2(c,d)))", 0.9);
        //Parser.testParsing("AND(sim1(a,b)|0.7, sim2(b,d)|0.9)", 0.9);
        String m = "levenshtein(x.http://www.okkam.org/ontology_person1.owl#surname, y.http://www.okkam.org/ontology_person2.owl#surname)";
    	Parser.testParsing(m, 0.5d);
    	Parser p = new Parser(m, 0.5);
    	System.out.println(p.isAtomic());
    	System.out.println(p.op);
    	System.out.println(p.term1);
    	System.out.println(p.term2);
    }
}
