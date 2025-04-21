package org.regpattern2vec;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author @Sainath_Talakanti
 */
public class RegexToDfa {

    private Set<Integer>[] followPos;
    public String finalRegex;
    private Nodes root;
    public List<State> DStates;

    public HashMap<Integer,HashMap<String,Integer>> transitions;

    private  Set<String> input; //set of inputs is used in input regex
    public List<State> stateNames = new ArrayList<>();

    private static HashMap<Integer, String> symbolNum;

    /**
     * This method is to convert the Regular Expression to DFA trasitions
     * @param TypeList List if Relation types
     * @param regex Regular Expression
     */

    public RegexToDfa(List<String> TypeList, String regex) {

        System.out.println("Input Regex: " + regex);

        //allocating
        DStates = new ArrayList<>();
        input = new HashSet<String>();
        transitions = new HashMap<>();

        //Check if regex has incorrect relationShip types
        String splitter = "(?:\\{\\d+,\\})|(?:[^()\\[\\]\\*\\+\\.\\^]+)|(?:[()\\[\\]\\*\\+\\.\\^])";

        Pattern pattern = Pattern.compile(splitter);
        Matcher matcher = pattern.matcher(regex);

        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        //System.out.println("Tokens: " + tokens);

        for (String token : tokens) {
            //System.out.println(token);
            if(!token.equals("(") && !token.equals(")") && !token.equals("{") && !token.equals("}")
                    && !token.equals("[") && !token.equals("]") && !token.equals("*") && !token.equals("+")
                        && !token.equals(".") && !token.equals("|") && !token.equals("^") && !token.matches("\\{\\d+,\\}")  && !TypeList.contains(token)) {
                System.out.println("Incorrect Relationship Type in Given Regular Expression: "+token);
                return;

            }
        }

        /// giving the regex to SyntaxTree class constructor and creating the
        /// syntax tree of the regular expression in it
        SyntaxTree st = new SyntaxTree(regex+".#", TypeList);
        finalRegex = st.getFinalRegex();
        System.out.println("Final Regex: " + finalRegex);
        System.out.println("Number of Leaves: "+ st.getFollowPos().length);
        getSymbols(finalRegex);
        System.out.println("Input:" + input);
        root = st.getRoot(); //root of the syntax tree
        followPos = st.getFollowPos(); //the followPos of the syntax tree

        /// creating the DFA using the syntax tree were created upside and
        /// returning the start state of the resulted DFA
        State q0 = createDFA();

        System.out.println("All transitions: ");
        System.out.println("DStates: " + DStates.size());

        transformStateNames();

        for(State state: DStates) {
            if(!DStates.isEmpty()) {
                if(state.getTransformedName() == 1){
                    state.setIsFirstState();
                }
                if(state.getTransformedName() == DStates.size()-1){
                    state.setIsLastState();
                }
                System.out.println("Transformed State Name: " + state.getTransformedName());
                //System.out.println("State Name: " + state.getName());
                for (Map.Entry<String, State> entry : state.getAllMoves().entrySet()) {
                    if (!entry.getKey().equals("#")) {
                        transitions.computeIfAbsent(state.getTransformedName(), k -> new HashMap<>())
                                .put(entry.getKey(), entry.getValue().getTransformedName());
                        System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue().getTransformedName());
                        //System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue().getName());
                        //System.out.println(state.getAllMoves().entrySet());
                    }
                }
                if(state.isFirstState){
                    System.out.println("First State: " + state.getTransformedName());
                }
                if(state.isLastState){
                    System.out.println("Last State: " + state.getTransformedName());
                }
            }
        }

        for(Map.Entry<Integer,HashMap<String,Integer>> entry: transitions.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
    }

    private void getSymbols(String regex) {
        /**
         * op is a set of characters have operational meaning for example '*'
         * could be a closure operator
         */
        Set<String> op = new HashSet<>(Arrays.asList("(", ")", "*", "|", ".", "[", "]", "+", "^", "{", "}", ","));

        input = new HashSet<>();
        symbolNum = new HashMap<>();
        int num = 1;
        //Check if regex has incorrect relationShip types
        String splitter = "(?:\\{\\d+,\\})|(?:[^()\\[\\]\\*\\+\\.\\^]+)|(?:[()\\[\\]\\*\\+\\.\\^])";

        Pattern pattern = Pattern.compile(splitter);
        Matcher matcher = pattern.matcher(finalRegex);

        List<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        for (String token : tokens) {
            if (!op.contains(token)) {
                input.add(token);
                symbolNum.put(num++, token);
            }
        }
    }

    /**
     *
     * @return DFA states
     */

    private State createDFA() {
        int id = 0;
        Set<Integer> firstpos_n0 = root.getFirstPos();
        State q0 = new State(id++);
        q0.addAllToName(firstpos_n0);
        if (q0.getName().contains(followPos.length)) {
            q0.setAccept();
        }
        DStates.clear();
        DStates.add(q0);
        //System.out.println("Q0: " + q0.getName());
        stateNames.add(q0);

        while (true) {
            boolean exit = true;
            State s = null;
            for (State state : DStates) {
                if (!state.getIsMarked()) {
                    exit = false;
                    s = state;
                }
            }
            if (exit) {
                break;
            }

            if (s.getIsMarked()) {
                continue;
            }
            s.setIsMarked(true); //mark the state
            Set<Integer> name = s.getName();
            for (String a : input) {
                Set<Integer> U = new HashSet<>();
                for (int p : name) {
                    if (symbolNum.get(p).equals(a)) {
                        U.addAll(followPos[p - 1]);
                    }
                }
                boolean flag = false;
                State tmp = null;
                for (State state : DStates) {
                    if (state.getName().equals(U)) {
                        tmp = state;
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
                    State q = new State(id++);
                    q.addAllToName(U);
                    if (U.contains(followPos.length)) {
                        q.setAccept();
                    }
                    DStates.add(q);
                    if(!q.getName().isEmpty()){stateNames.add(q);}
                    tmp = q;
                }
                s.addMove(a, tmp);
            }
        }

        return q0;
    }

    public HashMap<Set<Integer>,Map.Entry<String, State>> getAllTransitions() {

        HashMap<Set<Integer>,Map.Entry<String, State>> allTransitions = new HashMap<>();

        for(State state: DStates) {
            for(Map.Entry<String, State> entry: state.getAllMoves().entrySet()) {
                allTransitions.put(state.getName(),entry);
            }
        }
        return allTransitions;
    }

    public List<Set<Integer>> getAllStateNames() {
        List<Set<Integer>> allStateNames = new ArrayList<>();
        for(State state: stateNames){
            allStateNames.add(state.getName());
        }
        return allStateNames;
    }

    /**
     * get the modified state names
     */

    public void transformStateNames() {
        int counter = 0;
        for (State state : DStates) {
            if(!state.getName().isEmpty()) {
                state.setTransformedName(++counter);
            }
        }
    }

    public List<Integer> getTransformedStateNames() {
        List<Integer> allStateNames = new ArrayList<>();
        for(State state: DStates){
            allStateNames.add(state.getTransformedName());
        }
        return allStateNames;
    }
}
