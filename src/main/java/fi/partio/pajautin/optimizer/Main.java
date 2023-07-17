package fi.partio.pajautin.optimizer;

import fi.partio.pajautin.optimizer.engine.EagerOptimizer;
import fi.partio.pajautin.optimizer.engine.Optimizer;
import fi.partio.pajautin.optimizer.engine.SanityChecker;
import fi.partio.pajautin.optimizer.member.Problem;



public class Main {
    public static void main(String[] args) {


        if (args[0].equals("optimize")) {

           for (var done = optimize(args); !done ; done = optimize(args)) {
               System.out.println("Optimization failed, trying again");
           }
        } else if (args[0].equals("test-data")) {
            test(args);
        } else if (args[0].equals("csv")) {
            csv(args);
        } else {
            System.out.println("Unknown command: " + args[0]);
        }
    }

    private static void test(String[] args) {
        DataUtil.writeProjectedData(3250);
    }

    private static boolean optimize(String[] args) {
        Problem problem = new Problem(DataUtil.readJsonFileToList(args[1]),DataUtil.readJsonFileToMap(args[2]), DataUtil.readJsonFileToDoubleList(args[3]));
        Optimizer optimizer = new EagerOptimizer(problem);
        optimizer.optimize();

        // Check sanity
        if (SanityChecker.checkSanity(problem)) {
            System.out.println("Sanity check passed");
            ResultExporter.exportProblem(problem);
            return true;
        } else {
            System.out.println("Sanity check failed");
            return false;
        }


    }

    private static void csv(String[] args) {
        DataUtil.writeCsvFile(args[1], args[2] );
    }
}