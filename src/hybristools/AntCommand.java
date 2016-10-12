package hybristools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.core.runtime.IPath;

public class AntCommand implements Predicate<Extension> {

    private final IPath platforPath;
    private final String[] command;

    public AntCommand(IPath platforPath, String... command) {
        this.platforPath = platforPath;
        this.command = command;
    }

    @Override
    public boolean test(Extension ext) {
        System.out.println("run ant classpath gen for " + ext);
        try {
            List<String> commands = new ArrayList<>();
            commands.add(platforPath.makeAbsolute().toOSString() + "/apache-ant-1.9.1/bin/ant.bat");
            commands.addAll(Arrays.asList(command));
            Process p = new ProcessBuilder(commands)
                    .directory(ext.getFolder()).redirectErrorStream(true).inheritIO().start();
            int res = p.waitFor();
            System.out.println(res);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return true;

    }

}
