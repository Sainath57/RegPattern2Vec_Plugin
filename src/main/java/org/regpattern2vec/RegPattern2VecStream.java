package org.regpattern2vec;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;
import org.neo4j.procedure.*;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

/**
 * @author Sainath_Talakanti
 * This code is a procedure to generate the stream of embeddings of nodes
 */

public class RegPattern2VecStream {

    public class Output {
        public final Map<String, double[]> embedding;
        public Output(Map<String, double[]> embedding) {
            this.embedding = embedding;
        }
    }

    @Context
    public GraphDatabaseService gdbs;
    @Context
    public Log log;

    static List<List<Node>> regularExpressionRandomWalks = new ArrayList<>();
    static Map<String, double[]> embeddings = new HashMap<>();
    static String walkFileName = "rawFiles/walks.txt";
    static String embeddingFileName = "rawFiles/embeddings.tsv";

    @Procedure(value = "embeddings.regpattern2vec.stream", mode = Mode.READ)
    @Description("Stream the embeddings of RegPattern2Vec")

    /**
     * This is the starting method of this procedure
     * @param regPattern The regular expression
     * @param walkLength Max. Length of each walk
     * @param walkCount Number of walks per node
     * @param windowSize size of the window to be taken for Skip-Gram
     * @param embeddingDimension size of the generated embeddings
     * @param negativeSampleSize size of negative samples for training
     * @param learningRate learning rate of the model
     * @param epochs number of repetitions of training
     *
     * @return Stream of Output i.e. embeddings of each node
     */

    public Stream<Output> regpattern2vec(@Name("regPattern") String regPattern,
                                         @Name(value = "walkLength", defaultValue = "10") Long walkLength,
                                         @Name(value = "walkCount", defaultValue = "5") Long walkCount,
                                         @Name(value = "windowSize", defaultValue = "3") Long windowSize,
                                         @Name(value = "embeddingDimension", defaultValue = "128") Long embeddingDimension,
                                         @Name(value = "negativeSampleSize", defaultValue = "5") Long negativeSampleSize,
                                         @Name(value = "learningRate", defaultValue = "0.01") Double learningRate,
                                         @Name(value = "epochs", defaultValue = "1") Long epochs) {

        log.info("RegPattern2Vec plugin is loading...");
        ///Clear any existing walks.
        regularExpressionRandomWalks.clear();
        createFile(walkFileName);
        createFile(embeddingFileName);

        try (Transaction tx = gdbs.beginTx()) {

            Set<String> labelsSet = new HashSet<>();
            Set<String> relTypesSet = new HashSet<>();

            /// Iterate over all nodes to collect labels
//            for (Node node : tx.getAllNodes()) {
//                for (Label label : node.getLabels()) {
//                    labelsSet.add(label.name());
//                }
//            }

            /// Iterate over all relationships to collect relationship types
            for (Relationship rel : tx.getAllRelationships()) {
                RelationshipType type = rel.getType();
                relTypesSet.add(type.name());
            }

            /// Convert sets to lists if needed
            List<String> labelsList = new ArrayList<>(labelsSet);
            List<String> relTypesList = new ArrayList<>(relTypesSet);

            RegexToDfa rd = new RegexToDfa(relTypesList, regPattern);

            for (Node node : tx.getAllNodes()) {
                //System.out.println("Node: " + node.getProperty("name"));
                List<Node> nodeRecords;
                for (int i = 1; i <= walkCount; i++) {

                    nodeRecords = RegularExpressionRandomWalks(gdbs, rd.DStates, node, walkLength);
                    saveWalksInFile(nodeRecords);
                    HeterogenousSkipGram hsg = new HeterogenousSkipGram(gdbs, log, tx, embeddingDimension,windowSize,negativeSampleSize,learningRate,epochs, nodeRecords);
                    embeddings.put(node.getElementId(),hsg.inputEmbeddings.get(node.getElementId()));

                    for(Node nodeRecord: nodeRecords) {
                        //System.out.print(nodeRecord.getLabels().toString()+" ");
                        //System.out.println(nodeRecord.getProperty("name"));
                    }
                    System.out.println();

                }
            }

            System.out.println("Embeddings: " + embeddings);
            saveEmbeddingsInFile(embeddings);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Output Output = new Output(embeddings);
        return Stream.of(Output);
    }

    /**
     * This method if to generate walks from a node
     *
     * @param gd GraphDataBaseService
     * @param transitions List of transitions of DFA of Regular Expression
     * @param node starting node
     * @param walkLength Length of a walk
     *
     * @return walk i.e. list of nodes
     */

    private List<Node> RegularExpressionRandomWalks(GraphDatabaseService gd, List<State> transitions, Node node, Long walkLength){

        List<Node> regularExpressionRandomWalk = new ArrayList<>(List.of(node));
        State currentState = transitions.stream().filter(state -> state.isFirstState).findFirst().orElse(null);
        assert currentState != null;

        Node currentNode = regularExpressionRandomWalk.get(regularExpressionRandomWalk.size() - 1);
        Random random = new Random();

        for(int i = 1; i <= walkLength-1; i++) {

            Iterable<Relationship> allNeighbourRelationships = currentNode.getRelationships();
            List<Relationship> neighbours = new ArrayList<>();
            allNeighbourRelationships.forEach(neighbours::add);
            Collections.shuffle(neighbours,random);
            boolean transitionMade = false;

            for (Relationship rel: neighbours) {

                String relType = rel.getType().toString();
                System.out.println("RelType: " + relType);
                Map<String, State> currentStateTransitions = currentState.getAllMoves();
                System.out.println("Current State: " + currentState.getTransformedName());
                //System.out.println("Current StateTransitions: " + currentStateTransitions.keySet());

                if (currentStateTransitions.containsKey(relType) && !currentStateTransitions.get(relType).getName().isEmpty()) {
                    currentNode = rel.getOtherNode(currentNode);
                    regularExpressionRandomWalk.add(currentNode);
                    currentState = currentState.getNextStateBySymbol(relType);
                    transitionMade = true;
                    break;
                }
            }

                if (!transitionMade) {
                    break;
                }
        }

        return regularExpressionRandomWalk;

    }

    /**
     * This method is to save all the walks generated in a text file
     * @param walk List of nodes of a walk
     */

    private void saveWalksInFile(List<Node> walk){
        try (PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(walkFileName, true)))) { // append = true
            List<String> walkNodeNames = new ArrayList<>();
            for (Node node : walk) {
                String nodeName = node.getElementId();
                walkNodeNames.add(nodeName);
            }
            writer.println(String.join(" ", walkNodeNames));
        } catch (IOException e) {
            log.error("Error writing walks to file: " + e.getMessage());
        }
    }

    /**
     * This method is to save the generated embeddings in a file
     * @param embeddings embeddings of nodes
     */

    private void saveEmbeddingsInFile(Map<String, double[]> embeddings) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(embeddingFileName, false))) {
            for (Map.Entry<String, double[]> entry : embeddings.entrySet()) {
                String nodeId = entry.getKey();
                double[] vec   = entry.getValue();

                // Build one space‑separated string of all coordinates
                String joined = DoubleStream.of(vec)
                        .mapToObj(Double::toString)
                        .collect(Collectors.joining(" "));

                // Write nodeId, one tab, then the joined vector, then newline
                writer.print(nodeId);
                writer.print("\t");
                writer.print(joined);
                writer.print("\n");
            }
            log.info("Wrote " + embeddings.size() + " embeddings to " + embeddingFileName);
        } catch (IOException e) {
            log.error("Error writing embeddings to file: " + e.getMessage(), e);
        }
    }

    /**
     * This method is to create file and checks if already exists
     * @param filePath path of a file
     */

    private void createFile(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // Delete the file if it exists
                file.delete();
            }
            // Create a new empty file
            file.createNewFile();
            log.info("Initialized new walks.txt file at: " + filePath);
        } catch (IOException e) {
            log.error("Error initializing walks.txt: " + e.getMessage());
        }
    }

}