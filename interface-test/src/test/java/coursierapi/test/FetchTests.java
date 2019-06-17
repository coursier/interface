package coursierapi.test;

import coursierapi.Dependency;
import coursierapi.Fetch;
import coursierapi.error.CoursierError;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;
import java.util.*;

public class FetchTests {

    @Test
    public void simple() {

        Dependency dep = Dependency.of("com.chuusai", "shapeless_2.13", "2.3.3");

        Fetch fetch = Fetch.create()
                .addDependencies(dep);

        List<File> files;
        try {
            files = fetch.fetch();
        } catch (CoursierError e) {
            throw new RuntimeException(e);
        }

        Set<String> fileNames = new HashSet<>();
        for (File f : files) {
            fileNames.add(f.getName());
        }

        Set<String> expectedFileNames = new HashSet<>(Arrays.asList("shapeless_2.13-2.3.3.jar", "scala-library-2.13.0.jar"));

        assertEquals(expectedFileNames, fileNames);

    }

}
