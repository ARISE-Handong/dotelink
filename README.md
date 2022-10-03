# DoTeLink
DoTeLink infers fine-grained traceability links between Javadoc comment and JUnit test code.

DoTeLink consists of 3 modules: 
- __Comment segmentation__ segregates a Javadoc comment into sentences 
- __Test Code segmentation__ divides a test method into multiple test code snippets that compose a single test scenario using static slicing 
- __Traceability link inference__ selects a pair of sentence and test code snippet with a high similarity  

## Build DoTeLink
To compile DoTelink, run the command: `mvn compile` 
 

## Run DoTeLink
Before running DoTeLink, download all dependency files as jar in the target project directory using `mvn dependency:copy-dependencies`.   
[JavaSymbolSolver](https://github.com/javaparser/javasymbolsolver) needs all dependencies to resolve the target project's method calls 

After downloading the dependency files as a jar, run the command:  

    mvn exec:java -Dexec.mainClass=org.DoTeLink.Main \
                  -Dexec.args="--production_class ./jfreechart-1.0.19/source/org/jfree/data/Range.java \
                               --test_class ./jfreechart-1.0.19/tests/org/jfree/data/RangeTest.java \
                               --project_dir ./jfreechart-1.0.19"
