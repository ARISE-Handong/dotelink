# DoTeLink
DoTeLink infers fine-grained traceability links between Javadoc comment and JUnit test code.

## Build DoTeLink
To compile DoTelink, run the command: `mvn compile` 
 

## Run DoTeLink
Before running DoTeLink, download all dependency files as jar in the target project directory (argument to `--project_dir`).  
[JavaSymbolSolver](https://github.com/javaparser/javasymbolsolver) needs all dependencies to resolve the target project's method calls `mvn dependency:copy-dependencies`

After downloading the dependency files as a jar, run the command:  

    mvn exec:java -Dexec.mainClass=org.DoTeLink.Main \ 
                  -Dexec.args="--production_class ./dataset/production-class/jfreechart/Range.java \ 
                               --test_class ./dataset/testcode/jfreechart/RangeTest.java \ 
                               --project_dir ./jfreechart-1.0.19"

