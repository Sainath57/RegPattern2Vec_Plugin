package org.regpattern2vec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author @Sainath_Talakanti
 */
public class SyntaxTree {

    private String finalRegex;
    private BinaryTree bt;
    private Nodes root; //the head of raw syntax tree
    private int numOfLeafs;
    private Set<Integer> followPos[];

    private List<String> TypeList = new ArrayList<>();


    public SyntaxTree(String regex, List<String> TypeList) {

        this.TypeList = TypeList;
        bt = new BinaryTree();

        /*
          generates the binary tree of the syntax tree
         */
        root = bt.generateTree(regex, TypeList);
        finalRegex = bt.getRegular();
        numOfLeafs = bt.getNumberOfLeafs();
        followPos = new Set[numOfLeafs];
        for (int i = 0; i < numOfLeafs; i++) {
            followPos[i] = new HashSet<>();
        }
        //bt.printInorder(root);
        generateNullable(root);
        generateFirstposLastPos(root);
        generateFollowPos(root);
    }

    private void generateNullable(Nodes node) {
        if (node == null) {
            return;
        }
        if (!(node instanceof LeafNodes)) {
            Nodes left = node.getLeft();
            Nodes right = node.getRight();
            generateNullable(left);
            generateNullable(right);
            switch (node.getSymbol()) {
                case "|":
                    node.setNullable(left.isNullable() | right.isNullable());
                    break;
                case ".":
                    node.setNullable(left.isNullable() & right.isNullable());
                    break;
                case "*":
                    node.setNullable(true);
                    break;
            }
        }
    }

    private void generateFirstposLastPos(Nodes node) {
        if (node == null) {
            return;
        }
        if (node instanceof LeafNodes) {
            LeafNodes lnode = (LeafNodes) node;
            node.addToFirstPos(lnode.getNum());
            node.addToLastPos(lnode.getNum());
        } else {
            Nodes left = node.getLeft();
            Nodes right = node.getRight();
            generateFirstposLastPos(left);
            generateFirstposLastPos(right);
            switch (node.getSymbol()) {
                case "|":
                    node.addAllToFirstPos(left.getFirstPos());
                    node.addAllToFirstPos(right.getFirstPos());
                    //
                    node.addAllToLastPos(left.getLastPos());
                    node.addAllToLastPos(right.getLastPos());
                    break;
                case ".":
                    if (left.isNullable()) {
                        node.addAllToFirstPos(left.getFirstPos());
                        node.addAllToFirstPos(right.getFirstPos());
                    } else {
                        node.addAllToFirstPos(left.getFirstPos());
                    }
                    //
                    if (right.isNullable()) {
                        node.addAllToLastPos(left.getLastPos());
                        node.addAllToLastPos(right.getLastPos());
                    } else {
                        node.addAllToLastPos(right.getLastPos());
                    }
                    break;
                case "*":
                    node.addAllToFirstPos(left.getFirstPos());
                    node.addAllToLastPos(left.getLastPos());
                    break;
            }
        }
    }

    private void generateFollowPos(Nodes node) {
        if (node == null) {
            return;
        }
        Nodes left = node.getLeft();
        Nodes right = node.getRight();
        switch (node.getSymbol()) {
            case ".":
                Object lastpos_c1[] = left.getLastPos().toArray();
                Set<Integer> firstpos_c2 = right.getFirstPos();
                for (int i = 0; i < lastpos_c1.length; i++) {
                    followPos[(Integer) lastpos_c1[i] - 1].addAll(firstpos_c2);
                }
                break;
            case "*":
                Object lastpos_n[] = node.getLastPos().toArray();
                Set<Integer> firstpos_n = node.getFirstPos();
                for (int i = 0; i < lastpos_n.length; i++) {
                    followPos[(Integer) lastpos_n[i] - 1].addAll(firstpos_n);
                }
                break;
        }
        generateFollowPos(node.getLeft());
        generateFollowPos(node.getRight());

    }

    public void show(Nodes node) {
        if (node == null) {
            return;
        }
        show(node.getLeft());
        Object s[] = node.getLastPos().toArray();

        show(node.getRight());
    }

    public void showFollowPos() {
        for (int i = 0; i < followPos.length; i++) {
            Object s[] = followPos[i].toArray();
        }
    }

    public Set<Integer>[] getFollowPos() {
        return followPos;
    }

    public Nodes getRoot() {
        return this.root;
    }

    public String getFinalRegex() {
        return finalRegex;
    }

    public int getNumOfLeafs() {
        return numOfLeafs;
    }
}
