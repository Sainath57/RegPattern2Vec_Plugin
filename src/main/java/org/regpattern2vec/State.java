package org.regpattern2vec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author @Sainath_Talakanti
 */
public class State {
    
    private int ID;
    private Set<Integer> name;
    private HashMap<String, State> move;
    
    private boolean IsAcceptable;
    private boolean IsMarked;
    public int transformedName;
    public boolean isFirstState;
    public boolean isLastState;
    
    public State(int ID){
        this.ID = ID;
        move = new HashMap<>();
        name = new HashSet<>();
        IsAcceptable = false;
        IsMarked = false;
        transformedName = 0;
        isFirstState = false;
        isLastState = false;
    }
    
    public void addMove(String symbol, State s){
        move.put(symbol, s);
    }
    
    public void addToName(int number){
        name.add(number);
    }
    public void addAllToName(Set<Integer> number){
        name.addAll(number);
    }
    
    public void setIsMarked(boolean bool){
        IsMarked = bool;
    }
    
    public boolean getIsMarked(){
        return IsMarked;
    }
    
    public Set<Integer> getName(){
        return name;
    }

    public void setAccept() {
        IsAcceptable = true;
    }
    
    public boolean getIsAcceptable(){
        return  IsAcceptable;
    }
    
    public State getNextStateBySymbol(String str){
        return this.move.get(str);
    }
    
    public HashMap<String, State> getAllMoves(){
        return move;
    }

    public int getID(){
        return ID;
    }

    public void setTransformedName(int number){
        this.transformedName = number;
    }

    public int getTransformedName(){
        return transformedName;
    }

    public void setIsFirstState(){
        isFirstState = true;
    }

    public void setIsLastState(){
        isLastState = true;
    }

    public boolean getIsFirstState(){
        return isFirstState;
    }

    public boolean getIsLastState(){
        return isLastState;
    }

}
