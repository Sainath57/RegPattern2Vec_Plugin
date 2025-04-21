package org.regpattern2vec;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.*;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class RegPattern2VecTest {

    private Neo4j embeddedDatabaseServer;
    private Driver driver;

    @BeforeAll
    void initializeNeo4j() {
        // Start an in-process Neo4j instance and register your procedure class (if needed)
        this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withProcedure(RegPattern2VecStream.class)
                .build();

        this.driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());

//        // Load the Cypher file that contains your CREATE statements.
//        try (Session session = driver.session()) {
//            String cypherScript = readCypherFromFile("src/test/resources/movie.cypher");
//            // If your file contains multiple statements separated by semicolons, split them.
//            for (String statement : cypherScript.split(";")) {
//                String trimmed = statement.trim();
//                if (!trimmed.isEmpty()) {
//                    session.writeTransaction(tx -> {
//                        tx.run(trimmed);
//                        return null;
//                    });
//                }
//            }
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to load the cypher file", e);
//        }
    }

    @AfterAll
    void shutdownNeo4j() {
        this.driver.close();
        this.embeddedDatabaseServer.close();
    }

    @AfterEach
    void cleanDb(){
        try(Session session = driver.session()) {
            session.run("MATCH (n) DETACH DELETE n");
        }
    }

    @Test
    void testWalksProcedure() {

        try (Session session = driver.session(); BufferedWriter writer = Files.newBufferedWriter(
                Paths.get("rawFiles/positive_edges.csv"),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            String cypherScript = readCypherFromFile("src/test/resources/movie.cypher");
            //String cypherScript = readCypherFromFile("src/test/resources/example.cypher");
            // If your file contains multiple statements separated by semicolons, split them.
            for (String statement : cypherScript.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    session.executeWrite(tx -> {
                        tx.run(trimmed);
                        return null;
                    });
                }
            }
            // Call your procedure that performs random walks.

            // Adjust parameters ("regex", walkLength, walkCount) as required.
            //Result result = session.run("Call embeddings1.regpattern2vec.stream(\"(ACTED_IN)*[REVIEWED]+((PRODUCED)|(WROTE)).(^FOLLOWS){2,}[DIRECTED]+\",5,5)"); //Evaluation 2
            Result result = session.run("Call embeddings1.regpattern2vec.stream(\"(ACTED_IN)*((DIRECTED)|(WROTE))[PRODUCED]+(REVIEWED)(FOLLOWS)*(^DIRECTED){1,}\")"); //Evaluation 3
            //Result result = session.run("Call embeddings1.regpattern2vec.stream(\"(^R2){2,}[R2]\",5,5)");//Evaluation 1
            //Result result = session.run("Call embeddings1.regpattern2vec.stream(\"(R1)*(R2)\",5,5)");
            //Result result = session.run("Call embeddings1.regpattern2vec(\"(^R2){2,}[R2]\")");

            System.out.println(result.list());
            System.out.println(result.stream());
            System.out.println(result.stream().count());
            System.out.println(result.stream().toList());
            System.out.println(result.stream().toList().size());

            // Verify that we got some results.
            //assertFalse(result.list().isEmpty(), "Procedure should return at least one record.");

            // Iterate over the result and print/check the returned node walk information.
//            for (Record record : result.list()) {
//                // Our procedure returns a record with a field 'node'
//                Object nodeObj = record.get("node").asObject();
//                assertNotNull(nodeObj, "Returned node should not be null.");
//                System.out.println("Returned node record: " + nodeObj);
//            }

            writer.write("u,v");
            writer.newLine();

            // Replace id(u), id(v) with whatever node identifier your walks use
            String exportCypher = "MATCH (u)-[r]->(v) RETURN elementId(u) AS u, elementId(v) AS v";
            session.readTransaction(tx -> {
                Result result1 = tx.run(exportCypher);
                while ( result1.hasNext() ) {
                    Record rec = result1.next();
                    String u = rec.get("u").asString();
                    String v = rec.get("v").asString();
                    try {
                        writer.write(u + "," + v);
                        writer.newLine();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return null;
            });

            writer.flush();
            System.out.println("Exported " + Files.size(Paths.get("rawFiles/positive_edges.csv"))
                    + " bytes to positive_edges.csv");

        }catch (IOException e) {
            throw new RuntimeException("Failed to load the cypher file", e);
        }
    }

    /**
     * Helper method to read a Cypher script from a file.
     *
     * @param filePath the file path to the cypher script
     * @return the file content as a String
     * @throws IOException if the file cannot be read
     */
    private static String readCypherFromFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }
}
