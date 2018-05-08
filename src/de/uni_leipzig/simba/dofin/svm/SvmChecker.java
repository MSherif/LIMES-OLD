package de.uni_leipzig.simba.dofin.svm;

import de.uni_leipzig.simba.data.Mapping;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.apache.log4j.Logger;

public class SvmChecker {
    
static Logger logger = Logger.getLogger("LIMES");
    public static double getDiscriminativeness(Set<Mapping> data) {
        //initialize the data
        ArrayList<String> uris = new ArrayList<String>(data.iterator().next().map.keySet());
        int size = data.size();
        svm_problem problem = new svm_problem();
        //size
        problem.l = uris.size() * (uris.size() + 1) / 2;
        // # of entities times number of features
        svm_node[][] x = new svm_node[uris.size() * (uris.size() + 1) / 2][data.size()];
        double[] y = new double[uris.size() * (uris.size() + 1) / 2];
        //index 
        int counter = 0;
        int entryNumber;
        for (Mapping m : data) {
            entryNumber = 0;
            for (int index = 0; index < uris.size(); index++) {
                for (int index2 = 0; index2 <= index; index2++) {
                    x[entryNumber][counter] = new svm_node();
                    x[entryNumber][counter].index = counter;
                    x[entryNumber][counter].value = m.getSimilarity(uris.get(index), uris.get(index2));
                    entryNumber++;
                }
            }
            counter++;
        }

        entryNumber = 0;
        for (int index = 0; index < uris.size(); index++) {
            for (int index2 = 0; index2 <= index; index2++) {
                if (index == index2) {
                    y[entryNumber] = +1;
                } else {
                    y[entryNumber] = -1;
                    //System.out.println(uris.get(index) + " " + uris.get(index2) + " -> -1");
                }
                entryNumber++;
            }
        }
        problem.x = x;
        problem.y = y;
        svm_parameter parameter = new svm_parameter();
        parameter.C = 1;
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = svm_parameter.LINEAR;
        parameter.eps = 0.0000001;

        svm_model model;
        double errors = 1;
        int count = 0;

        for (int exponent = -15; exponent < 15; exponent++) {
            parameter.C = Math.pow(2, exponent);
            count++;
            errors = 0;
            model = svm.svm_train(problem, parameter);

            double p = 0;
            //System.out.println("==> Iter = " + (exponent + 15));
            for (int i = 0; i < x.length; i++) {
                p = svm.svm_predict(model, x[i]);
                //System.out.println(x[i][0].index + " " + x[i][0].value);
                //System.out.println(i + " -> " + p);
                if (p * y[i] < 0) {
                    errors++;
                }
            }
            if (errors == 0) {
                break;
            }
        }
        if (errors == 0) {
            System.out.println("Discrimative space found");
        }
        return 1 - errors / (double) y.length;
    }
    
    public static double getDiscriminativeness(Set<Mapping> data, ArrayList<String> uris) {
        //initialize the data        
        int size = data.size();
        svm_problem problem = new svm_problem();
        //size
        problem.l = uris.size() * (uris.size() + 1) / 2;
        // # of entities times number of features
        svm_node[][] x = new svm_node[uris.size() * (uris.size() + 1) / 2][data.size()];
        double[] y = new double[uris.size() * (uris.size() + 1) / 2];
        //index 
        int counter = 0;
        int entryNumber;
        for (Mapping m : data) {
            entryNumber = 0;
            if(m==null)
            {
                System.out.println(m);
                logger.info(m + "=> Mapping is null");
                System.exit(1);
            }
            for (int index = 0; index < uris.size(); index++) {
                for (int index2 = 0; index2 <= index; index2++) {
                    x[entryNumber][counter] = new svm_node();
                    x[entryNumber][counter].index = counter;
                    if(index == index2)
                        x[entryNumber][counter].value = 1.0;
                    else
                        x[entryNumber][counter].value = m.getSimilarity(uris.get(index), uris.get(index2));
                    entryNumber++;
                }
            }
            counter++;
        }

        entryNumber = 0;
        for (int index = 0; index < uris.size(); index++) {
            for (int index2 = 0; index2 <= index; index2++) {
                if (index == index2) {
                    y[entryNumber] = +1;
                } else {
                    y[entryNumber] = -1;
                    //System.out.println(uris.get(index) + " " + uris.get(index2) + " -> -1");
                }
                entryNumber++;
            }
        }
        problem.x = x;
        problem.y = y;
        svm_parameter parameter = new svm_parameter();
        parameter.C = 1;
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = svm_parameter.LINEAR;
        parameter.eps = 0.0000001;

        svm_model model;
        double errors = 1;
        int count = 0;

        for (int exponent = -15; exponent < 15; exponent++) {
            parameter.C = Math.pow(2, exponent);
            count++;
            errors = 0;
            model = svm.svm_train(problem, parameter);

            double p = 0;
            //System.out.println("==> Iter = " + (exponent + 15));
            for (int i = 0; i < x.length; i++) {
                p = svm.svm_predict(model, x[i]);
                //System.out.println(x[i][0].index + " " + x[i][0].value);
                //System.out.println(i + " -> " + p);
                if (p * y[i] < 0) {
                    errors++;
                }
            }
            if (errors == 0) {
                break;
            }
        }
        if (errors == 0) {
            System.out.println("Discrimative space found");
        }
        return 1 - errors / (double) y.length;
    }
    
    //matrix[i][j] = Value of feature j for entity i
    //classification must contain +1 and -1 only

    public static double test(double[][] matrix, double[] classification) {
        svm_problem problem = new svm_problem();
        //size
        problem.l = classification.length;
        // # of entities times number of features
        svm_node[][] x = new svm_node[matrix.length][matrix[0].length];
        // # known data = # known entities
        double[] y = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[0].length; j++) {
                x[i][j] = new svm_node();
                x[i][j].index = j;
                x[i][j].value = matrix[i][j];
            }
        }
        problem.x = x;
        problem.y = classification;
        svm_parameter parameter = new svm_parameter();
        parameter.C = 1;
        parameter.svm_type = svm_parameter.C_SVC;
        parameter.kernel_type = svm_parameter.LINEAR;
        parameter.eps = 0.0000001;

        svm_model model;
        double errors = 1;
        int count = 0;

        for (int exponent = -15; exponent < 15; exponent++) {
            parameter.C = Math.pow(2, exponent);
            count++;
            errors = 0;
            model = svm.svm_train(problem, parameter);            
            double p = 0;
            //System.out.println("==> Iter = " + (exponent + 15));
            for (int i = 0; i < x.length; i++) {
                p = svm.svm_predict(model, x[i]);
                System.out.println(x[i][0].index + " " + x[i][0].value);
                System.out.println(x[i][1].index + " " + x[i][1].value);
                //System.out.println(i + " -> " + p);
                if (p * classification[i] < 0) {
                    errors++;
                }
            }
            if (errors == 0) {
                break;
            }
        }
        if (errors == 0) {
            System.out.println("Discrimative space found");
        }
        return 1 - errors / (double) y.length;
    }

    public static void main(String args[]) {
        Mapping m = new Mapping();
        m.add("a", "a", 0.7);
        m.add("a", "b", 0.9);
        m.add("a", "c", 0.7);
        m.add("b", "d", 0.8);
        m.add("b", "c", 0.6);
        m.add("b", "b", 1.0);
        m.add("c", "c", 1.0);
        m.add("d", "d", 1.0);
        m.add("e", "e", 1.0);

        Mapping m2 = new Mapping();
        m2.add("a", "a", 1.0);
        m2.add("a", "b", 0.9);
        m2.add("a", "c", 0.7);
        m2.add("b", "d", 0.8);
        m2.add("b", "c", 0.6);
        m2.add("b", "b", 1.0);
        m2.add("c", "c", 1.0);
        m2.add("d", "d", 1.0);
        m2.add("e", "e", 1.0);

        Set<Mapping> mappings = new HashSet<Mapping>();
        mappings.add(m);
        //System.out.println("==>"+getDiscriminativeness(mappings));
        mappings.add(m2);
        System.out.println("==>" + getDiscriminativeness(mappings));

    }

    public void test() {
        double matrix[][] = new double[6][2];
        double y[] = new double[6];
        matrix[0][0] = 0.9;
        matrix[0][1] = 0.7;
        y[0] = 1;

        matrix[1][0] = 1;
        matrix[1][1] = 1;
        y[1] = 1;

        matrix[2][0] = 0.9;
        matrix[2][1] = 0.2;
        y[2] = -1;

        matrix[3][0] = 0.3;
        matrix[3][1] = 0.5;
        y[3] = -1;

        matrix[4][0] = 0.1;
        matrix[4][1] = 0.1;
        y[4] = -1;

        matrix[5][0] = 0.8;
        matrix[5][1] = 0.6;
        y[5] = -1;

        double test = SvmChecker.test(matrix, y);
        System.out.println(test);
    }
}
