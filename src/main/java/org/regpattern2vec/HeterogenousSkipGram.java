package org.regpattern2vec;

import org.neo4j.graphdb.*;
import org.neo4j.graphdb.Node;
import org.neo4j.logging.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * @author Sainath_Talakanti
 * This code file is to perform Heterogeneous Skip-Gram model on the walks to generate ebeddings
 */

public class HeterogenousSkipGram {

    // Parameters
    private Long embeddingDim;
    private Long windowSize;
    private Long negativeSampleSize;
    private double learningRate;
    private Long epochs;
    private List<Node> walk;
    private List<Node> nodeList;

    // The embeddings: one for input (target) and one for output (context) per node.
// You could use a Map<Node, double[]> if your nodes are objects.
    public Map<String, double[]> inputEmbeddings;
    private Map<String, double[]> outputEmbeddings;

    // A map from node type to list of node IDs of that type (for type-specific negative sampling)
    private Map<String, List<String>> nodesByType;

    // Reuse one Random instance for efficiency.
    private final Random random = new Random();

    /**
     * This is a constructor
     *
     * @param gdbs Grapg Database Service from neo4j
     * @param log To log the results in log file for debugging purpose
     * @param tx an active transaction to work with database
     * @param windowSize size of the window to be taken for Skip-Gram
     * @param embeddingDim size of the generated embeddings
     * @param negativeSampleSize size of negative samples for training
     * @param learningRate learning rate of the model
     * @param epochs number of repetitions of training
     * @param walk List of nodes
     * @throws IOException
     */

    public HeterogenousSkipGram(GraphDatabaseService gdbs,
                                Log log,
                                Transaction tx,
                                Long embeddingDim,
                                Long windowSize,
                                Long negativeSampleSize,
                                double learningRate,
                                Long epochs,
                                List<Node> walk) throws IOException {
        this.embeddingDim = embeddingDim;
        this.windowSize = windowSize;
        this.negativeSampleSize = negativeSampleSize;
        this.learningRate = learningRate;
        this.epochs = epochs;
        this.inputEmbeddings = new HashMap<>();
        this.outputEmbeddings = new HashMap<>();
        this.nodesByType = new HashMap<>();
        this.walk = walk;

        this.nodeList = tx.getAllNodes().stream().toList();
        initializeEmbeddings(nodeList, log);
        train(walk,log);
    }

    /**
     * Initialize embeddings for all nodes in the graph
     * @param nodes List of all nodes of graph
     * @param log
     * @throws IOException
     */
    public void initializeEmbeddings(List<Node> nodes, Log log) throws IOException {
        String nodeTypesFileName = "rawFiles/nodeTypes.csv";
        createFile(nodeTypesFileName, log);
        for (Node node : nodes) {
            String nodeId = node.getElementId();
            int eDim = Math.toIntExact(embeddingDim);
            double[] inputVec = new double[eDim];
            double[] outputVec = new double[eDim];
            for (int i = 0; i < embeddingDim; i++) {
                // small random numbers
                inputVec[i] = (random.nextDouble() - 0.5) / embeddingDim;
                outputVec[i] = 0.0;
            }
            inputEmbeddings.put(nodeId, inputVec);
            outputEmbeddings.put(nodeId, outputVec);

            // Group nodes by type for type-specific negative sampling
            String type = node.getLabels().toString();
            nodesByType.computeIfAbsent(type, k -> new ArrayList<>()).add(nodeId);
            saveNodeTypes(nodeId, type, log, nodeTypesFileName);

        }
    }

    /**
     * Train on the list of walks (each walk is a List<Node>)
     * @param walk
     * @param log
     */

    public void train(List<Node> walk, Log log) {
        for (int epoch = 0; epoch < epochs; epoch++) {
                // Iterate through each node in the walk
                for (int i = 0; i < walk.size(); i++) {
                    Node targetNode = walk.get(i);
                    String targetId = targetNode.getElementId();
                    double[] targetVec = inputEmbeddings.get(targetId);
                    if (targetVec == null) continue;  // Safety check

                    // Determine the window boundaries
                    int wSize = Math.toIntExact(windowSize);
                    int start = Math.max(0, i - wSize);
                    int end = Math.min(walk.size(), i + wSize + 1);
                    //System.out.println("start: " + start + " end: " + end);

                    // Loop through context nodes
                    for (int j = start; j < end; j++) {
                        if (j == i) continue;
                        Node contextNode = walk.get(j);
                        String contextId = contextNode.getElementId();
                        //System.out.println("contextId: " + contextNode.getProperty("name"));

                        // Get the type-specific negative sampling candidates (exclude the context type)
                        String contextType = contextNode.getLabels().toString();

                        // Update with positive sample: maximize sigma(input * output)
                        updateParameters(targetId, contextId, true, contextType, log);

                        // Negative sampling: sample negativeSampleSize nodes of the same type as context
                        for (int n = 0; n < negativeSampleSize; n++) {
                            //System.out.println(k++);//102
                            String negativeId = sampleNegative(contextType, contextId, log);
                            if (negativeId == null) {
                                log.warn("Skipping negative update due to null negativeId for contextType: " + contextType);
                                continue;
                            }
                            updateParameters(targetId, negativeId, false, contextType, log);
                        }
                    }
                }
            // Optionally: decay learning rate or shuffle walks
        }
    }

    /**
     * This is to update parameters in embedding vectors
     * @param targetId ElementId of target node
     * @param contextId ElementId of context node
     * @param positive label for positive or negative data
     * @param contextType Node type of context node
     * @param log
     */

    private void updateParameters(String targetId, String contextId, boolean positive, String contextType, Log log) {

        double[] targetVec = inputEmbeddings.get(targetId);
        double[] contextVec = outputEmbeddings.get(contextId);

        // Defensive check: if contextVec is null, log and return
        if (contextVec == null) {
            log.warn("Skipping update: contextVec is null for contextId: " + contextId);
            return;
        }
        if (targetVec == null) {
            log.warn("Skipping update: targetVec is null for contextId: " + contextId);
            return;
        }

        // Compute dot product
        double dot = 0.0;
        for (int i = 0; i < embeddingDim; i++) {
            dot += targetVec[i] * contextVec[i];
        }

        // Calculate gradient: sigma(x) = 1 / (1 + exp(-x))
        double sigmoid = 1.0 / (1.0 + Math.exp(-dot));
        // For positive sample, label = 1; for negative, label = 0
        double error = (positive ? 1 : 0) - sigmoid;

        // Update target and context vectors
        for (int i = 0; i < embeddingDim; i++) {
            double gradTarget = error * contextVec[i];
            double gradContext = error * targetVec[i];
            targetVec[i] += learningRate * gradTarget;
            contextVec[i] += learningRate * gradContext;
        }

        // Write back updates (if using mutable arrays this step may be implicit)
        inputEmbeddings.put(targetId, targetVec);
        outputEmbeddings.put(contextId, contextVec);

    }

    /**
     * This is to generate negative samples of walks
     * @param contextType
     * @param trueContextId
     * @param log
     * @return
     */

    private String sampleNegative(String contextType, String trueContextId, Log log) {

        List<String> candidates = nodesByType.get(contextType);

        // Check if there are enough candidates to sample a negative example
        if (candidates == null || candidates.isEmpty()) {
            log.warn("Skipping negative sampling: No candidates available for type: " + contextType);
            return null; // Skip negative sample
        }

        if (candidates.size() == 1 && candidates.get(0).equals(trueContextId)) {
            log.warn("Skipping negative sampling: Only one node available for type: " + contextType);
            return null; // Skip negative sample
        }

        String sampled;
        do {
            sampled = candidates.get(random.nextInt(candidates.size()));
        } while (sampled.equals(trueContextId));
        return sampled;
    }

    /**
     * Get the embedding vector for a node
     * @param nodeId
     * @return
     */
    public double[] getEmbedding(String nodeId) {
        return inputEmbeddings.get(nodeId);
    }

    public void createFile(String filePath, Log log) throws IOException {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                // Delete the file if it exists
                file.delete();
            }
            // Create a new empty file
            file.createNewFile();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
                writer.write("nodeId,nodeType");
                writer.newLine();
                log.info("Initialized new nodeTypes file at: " + filePath);
            }
        } catch (IOException e) {
            log.error("Error initializing walks.txt: " + e.getMessage());
        }
    }

    /**
     * This is to save the nodes and their types in a csv file(for testing purpose
     * @param nodeId
     * @param nodeType
     * @param log
     * @param nodeTypesFileName file name
     */

    public void saveNodeTypes(String nodeId, String nodeType, Log log, String nodeTypesFileName) {

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(nodeTypesFileName, true))) {

            writer.write(nodeId);
            writer.write(',');
            writer.write(nodeType);
            writer.newLine();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

}
