package coursierapi.test;

import coursierapi.Dependency;
import coursierapi.Fetch;
import coursierapi.FetchResult;
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

        FetchResult result;
        try {
            result = fetch.fetchResult();
        } catch (CoursierError e) {
            throw new RuntimeException(e);
        }

        Set<String> fileNames = new HashSet<>();
        for (File f : result.getFiles()) {
            fileNames.add(f.getName());
        }

        Set<String> expectedFileNames = new HashSet<>(Arrays.asList("shapeless_2.13-2.3.3.jar", "scala-library-2.13.0.jar"));

        List<Dependency> expectedDependencies = new ArrayList<>(Arrays.asList(
                Dependency.of("com.chuusai", "shapeless_2.13", "2.3.3").withConfiguration("default"),
                Dependency.of("org.scala-lang", "scala-library", "2.13.0").withConfiguration("default")
        ));

        assertEquals(expectedFileNames, fileNames);
        assertEquals(expectedDependencies, result.getDependencies());

    }

}
