package org.regpattern2vec;

import java.util.HashSet;
import java.util.Set;

/**
 * @author @Sainath_Talakanti
 */

public class Nodes {

    private String symbol;
    private Nodes parent;
    private Nodes left;
    private Nodes right;

    private Set<Integer> firstPos;
    private Set<Integer> lastPos;
    private boolean nullable;

    public Nodes(String symbol) {
        this.symbol = symbol;
        parent = null;
        left = null;
        right = null;

        firstPos = new HashSet<>();
        lastPos = new HashSet<>();
        nullable = false;
    }

    /**
     * @return the symbol
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    /**
     * @return the parent
     */
    public Nodes getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(Nodes parent) {
        this.parent = parent;
    }

    /**
     * @return the left
     */
    public Nodes getLeft() {
        return left;
    }

    /**
     * @param left the left to set
     */
    public void setLeft(Nodes left) {
        this.left = left;
    }

    /**
     * @return the right
     */
    public Nodes getRight() {
        return right;
    }

    /**
     * @param right the right to set
     */
    public void setRight(Nodes right) {
        this.right = right;
    }


    public void addToFirstPos(int number) {
        firstPos.add(number);
    }
    public void addAllToFirstPos(Set set) {
        firstPos.addAll(set);
    }

    public void addToLastPos(int number) {
        lastPos.add(number);
    }
    public void addAllToLastPos(Set set) {
        lastPos.addAll(set);
    }

    /**
     * @return the firstPos
     */
    public Set<Integer> getFirstPos() {
        return firstPos;
    }

    /**
     * @return the lastPos
     */
    public Set<Integer> getLastPos() {
        return lastPos;
    }

    /**
     * @return the nullable
     */
    public boolean isNullable() {
        return nullable;
    }

    /**
     * @param nullable the nullable to set
     */
    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }
}
