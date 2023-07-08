package fi.partio.pajautin.optimizer;

import fi.partio.pajautin.optimizer.engine.EagerOptimizer;
import fi.partio.pajautin.optimizer.engine.Optimizer;
import fi.partio.pajautin.optimizer.member.Problem;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.RootLoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;


public class Main {
    public static void main(String[] args) {




        if (args[0].equals("optimize")) {
            optimize(args);
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

    private static void optimize(String[] args) {
        Problem problem = new Problem(DataUtil.readJsonFileToList(args[1]),DataUtil.readJsonFileToMap(args[2]), DataUtil.readJsonFileToDoubleList(args[3]));
        Optimizer optimizer = new EagerOptimizer(problem);
        optimizer.optimize();
    }

    private static void csv(String[] args) {
        DataUtil.writeCsvFile(args[1], args[2] );
    }
}