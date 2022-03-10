package hr.babo.pricefinder.python;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;

import java.io.IOException;

public class Python {
    public static int pythonCallNoOutput(String pythonScriptPath, String... args) {
        String argsS = " " + String.join(" ", args);
        String line = "python " + pythonScriptPath + argsS;

        CommandLine cmdLine = CommandLine.parse(line);

        DefaultExecutor executor = new DefaultExecutor();
        int exitCode;
        try {
            exitCode = executor.execute(cmdLine);
        } catch (IOException e) {
            return 1;
        }

        return exitCode;
    }
}
