/*
 * (C) Nhu-Huy Le, nle@hm.edu
 * Oracle Corporation Java 1.8.0
 * Microsoft Windows 7 Professional
 * 6.1.7601 Service Pack 1 Build 7601
 * This program is created while attending the courses
 * at Hochschule Muenchen Fak07, Germany in SS14.

Compilerbau: Praktikum
Excercise 6 - LL(1)-Parsergenerator

 - 16/5/2014
 */

package edu.hm.nle.rdpgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Generator for Recursive Descent Parser.
 * Gets a LL(1) Grammar in Backus-Naur-Form (String form) and generates the complete
 * java source code on the console.
 * @author Nhu-Huy Le, nle@hm.edu
 * @version 1.0
 */
public class RDParserGeneratorEpsilon {

    /** Minimal size of the parser source code. */
    private static final int SRC_SIZE = 1660;

    private static final char EPSILON = '#';
    private static final char ESCAPE = '$';

    /**
     * @param args [0] First char defines seperator, second char defines mapper,
     *                  following the productions.
     */
    public static void main(final String[] args) {

        final RDParserGeneratorEpsilon rdpg = new RDParserGeneratorEpsilon();
        final String[][] grammar = rdpg.parseGrammar(args[0]);
        final String[][] table = rdpg.getFFGroup(grammar);
        System.out.println(rdpg.createParser(table));
    }

    /**
     * Parses the BNF grammar (string version).
     * @param input     The BNF grammar in string version.
     * @return 2d string for the productions.
     */
    private String[][] parseGrammar(final String input) {
        final char mapChar = input.charAt(0);
        final char sepChar = input.charAt(1);
        final String[] productions = input.substring(2).split(String.valueOf(sepChar));
        final String[][] grammar = new String[productions.length][3];
        int row = 0;

        for (final String prod: productions) {
            boolean left = true;
            grammar[row][0] = "";
            grammar[row][1] = "";
            for (final char cha: prod.toCharArray()) {
                if(left) {
                    if(cha == mapChar) {
                        left = false;

                        // epsilon prod
                        if (prod.indexOf(mapChar) == prod.length()-1) {
                            grammar[row][1] += EPSILON;
                        }
                    }
                    else {
                        grammar[row][0] += cha;
                    }
                }
                else {
                    grammar[row][1] += cha;
                }
            }
            row++;
        }

        // TEST
        System.out.println("------------- RULES SET");
        for (final String[] gra: grammar) {
            System.out.println(gra[0] + " -> " + gra[1]);
        }

        return grammar;
    }

    /**
     * Parses the first elements of the grammar.
     * @param grammar   The productions as a 2d string with 2 columns.
     * @return first elements per row.
     */
    private String[][] getFFGroup(final String[][] grammar) {
        // hash: no duplicates because mathematical group

        // firstGroup for each symbols
        final HashMap<Character, HashSet<Character>> symbolFirsts = new HashMap<>();
        // followGroup for each nonterminal in symbol
        final HashMap<Character, HashSet<Character>> symbolFollows = new HashMap<>();

        initiate(grammar, symbolFirsts, symbolFollows);

        final List<HashSet<Character>> prodFirsts = getFirstElements(grammar, symbolFirsts);
        getFollowElements(grammar, symbolFirsts, symbolFollows);
        final List<HashSet<Character>> combinedFF = combineFF(grammar, prodFirsts, symbolFollows);

        System.out.println("FIRST SYMB\t" + symbolFirsts);
        System.out.println("FIRST PROD\t" + prodFirsts);
        System.out.println("FOLLW SYMB\t" + symbolFollows);
        System.out.println("MERG PROD\t" + combinedFF);

        // parse to grammar array
        for (int k = 0; k < grammar.length; k++) {
            final Character[] firstArr = new Character[combinedFF.get(k).size()];
            combinedFF.get(k).toArray(firstArr);
            grammar[k][2] = "";

            for (final Character firstEL: firstArr) {
                grammar[k][2]+= firstEL;
        }}

        return grammar;
    }

    private void initiate(final String[][] grammar, final Map<Character, HashSet<Character>> symbolFirsts,
                                                    final Map<Character, HashSet<Character>> symbolFollows) {
        for (int k = 0; k < grammar.length; k++) {

            // first, follow: wiederhole für alle...
            // leftSide
            for (final char chr : grammar[k][0].toCharArray()) {
                symbolFirsts.put(chr, new HashSet<>());

                if (Character.isUpperCase(chr)) {
                    symbolFollows.put(chr, new HashSet<>());
                }
            }

            // rightSide
            for (final char chr : grammar[k][1].toCharArray()) {

                if (chr != EPSILON) {
                    symbolFirsts.put(chr, new HashSet<>());

                    // first: first(t) <- {t}
                    if (Character.isUpperCase(chr)) {
                        symbolFollows.put(chr, new HashSet<>());
                    } else {
                        symbolFirsts.get(chr).add(chr);
                    }
                }
            }

            // start symbol
            symbolFollows.get(grammar[0][0].charAt(0)).add(ESCAPE);
        }
    }

    private List<HashSet<Character>> combineFF(final String[][] grammar,
                                                final List<HashSet<Character>> prodFirsts,
                                                final Map<Character, HashSet<Character>> symbolFollows) {
        // Combine First and Follow
        final ArrayList<HashSet<Character>> FFUnited = new ArrayList<>(grammar.length);
        for (final String[] gra : grammar) {
            final HashSet<Character> firstFollow = new HashSet<>();
            FFUnited.add(firstFollow);
        }
        for (int a = 0; a < grammar.length; a++) {
            final HashSet firstA = (HashSet) prodFirsts.get(a);
            final HashSet ffA = FFUnited.get(a);

            ffA.addAll(firstA);
            if (firstA.contains(EPSILON)) {
                ffA.remove(EPSILON);
                ffA.addAll(symbolFollows.get(grammar[a][0].charAt(0)));
            }
        }
        return FFUnited;
    }

    private void getFollowElements(final String[][] grammar,
                                    final Map<Character, HashSet<Character>> symbolFirsts,
                                    final Map<Character, HashSet<Character>> symbolFollows) {
        boolean followChanged = true;
        // follow: solange sich eine Followmenge...
        while (followChanged) {
            followChanged = false;

            for (int k = 0; k < grammar.length; k++) {

                int iRightChr = 1;
                for (final char chrN : grammar[k][1].toCharArray()) {

                    // follow: wiederhole fuer alle NT...
                    if (Character.isUpperCase(chrN)) {
                        int iSymbols = 0;
                        final String symbolsA = grammar[k][1].substring(iRightChr);
                        HashSet firstS = symbolsA.equals("") ?
                                new HashSet() :
                                symbolFirsts.get(symbolsA.charAt(iSymbols));
                        boolean hasEpsilon = firstS.contains(EPSILON);
                        boolean noSLeft = iSymbols == symbolsA.length();

                        // follow: wiederhole für alle S...
                        while (hasEpsilon) {
                            firstS.remove(EPSILON);
                            followChanged |= symbolFollows.get(chrN).addAll(firstS);
                            firstS.add(EPSILON);
                            final boolean isLastS = iSymbols == symbolsA.length() - 1;

                            if (isLastS) {
                                hasEpsilon = false;
                                noSLeft = true;
                            } else {
                                firstS = symbolFirsts.get(symbolsA.charAt(++iSymbols));
                                hasEpsilon = firstS.contains(EPSILON);
                            }
                        }

                        // follow: alle rightSide S durchlaufen...
                        if (noSLeft) {
                            followChanged |= symbolFollows.get(chrN).addAll(
                                    symbolFollows.get(grammar[k][0].charAt(0)));
                        } else {
                            followChanged |= symbolFollows.get(chrN).addAll(
                                    firstS);
                        }
                    }
                    iRightChr++;
                }
            }
        }
    }

    private List<HashSet<Character>> getFirstElements(final String[][] grammar,
                                                            final Map<Character, HashSet<Character>> symbolFirsts) {
        //firstGroup for each productions
        final ArrayList<HashSet<Character>> prodFirsts = new ArrayList<>(grammar.length);
        boolean firstChanged = true;
        for (final String[] gra : grammar) {
            final HashSet<Character> first = new HashSet<>();
            prodFirsts.add(first);
        }
        while (firstChanged) {
            firstChanged = false;

            // first: fuer alle produktionen...
            for (int k = 0; k < grammar.length; k++) {
                HashSet<Character> noEpsilonS = new HashSet<>();
                boolean noEpsilon = false;

                // first: fuer alle symbole...
                for (final char chr : grammar[k][1].toCharArray()) {

                    if (chr != EPSILON) {
                        final HashSet<Character> firstS = symbolFirsts.get(chr);

                        if (firstS.contains(EPSILON)) {
                            firstS.remove(EPSILON);
                            firstChanged |= prodFirsts.get(k).addAll(firstS);
                            firstChanged |= symbolFirsts.get(grammar[k][0].charAt(0))
                                    .addAll(firstS);
                            firstS.add(EPSILON);
                        } else {
                            noEpsilonS = noEpsilon ? noEpsilonS : firstS;
                            noEpsilon = true;
                        }
                    }
                }

                // first: gibt es ein X mit...
                if (noEpsilon) {
                    firstChanged |= prodFirsts.get(k).addAll(noEpsilonS);
                    firstChanged |= symbolFirsts.get(grammar[k][0].charAt(0))
                            .addAll(noEpsilonS);
                } else {
                    firstChanged |= prodFirsts.get(k).add(EPSILON);
                    firstChanged |= symbolFirsts.get(grammar[k][0].charAt(0))
                            .add(EPSILON);
                }
            }
        }
        return prodFirsts;
    }

    /**
     * Writes the source code of the parser to the console.
     * Most structure strings are predefined.
     * @param table first column: left side of productions, second column: right sde of productions
     *              third column: first group of the row
     * @return Nonterminals Methods for source code of the parser.
     */
    private String createParser(final String[][] table) {
        final StringBuilder srcHead = new StringBuilder(SRC_SIZE);
        final StringBuilder srcBody;
        final String start = table[0][0];

        srcHead.append("import java.util.ArrayList;\nimport java.util.stream.Stream;class RDParser")
        .append(start).append("{\nprivate String input;\nprivate char lookahead;\n");

        createNonTerminals(table, srcHead);

        // METHOD GET_NEXT_TOKEN()
        srcBody = srcHead.append("char getNextToken() {\n\tif(input.length() == 0)\n\t\t")
        .append("return '$';\n\tchar token = input.charAt(0);\n\t")
        .append("input = input.substring(1);\n\treturn token;\n}\n")

        // METHOD PARSE()
        .append("Node parse(String input) throws SyntaxErrorException {\n\t")
        .append("this.input = input;\n\t")
        .append("lookahead = getNextToken();\n\tNode result = ")
        .append(start).append("();\n\tif(lookahead != '$')\n\t\t")
        .append("throw new SyntaxErrorException(\"input remaining: \"+lookahead+")
        .append("this.input);\n\treturn result;\n}\n\n")

        // METHOD TERMINAL()
        .append("Node terminal(char expected) throws SyntaxErrorException {\n\t")
        .append("if(lookahead != expected)\n\t\tthrow new SyntaxErrorException(")
        .append("\"expected \"+expected+\"; found \"+lookahead);\n\t")
        .append("lookahead = getNextToken();\n\t")
        .append("return new Node(Character.toString(expected));\n}\n\n")

        // METHOD MAIN()
        .append("public static void main(String... args) throws SyntaxErrorException {\n\t")
        .append("Node root = new RDParser")
        .append(start).append("().parse(args[0]);\n\t")
        .append("System.out.println(root);\n\troot.prettyPrint();\n}\n")

        // CLASS SYNTAX_ERROR_EXCEPTION
        .append("public static class SyntaxErrorException extends Exception {\n\t")
        .append("public SyntaxErrorException(String message) {\n\t\t")
        .append("super(message);\n\t}\n}\n\n")

        // CLASS NODE
        .append("public class Node extends ArrayList<Node> {\n\t")
        .append("private final String name;\n\t")
        .append("public Node(String name, Node... children){\n\t")
        .append("if(name == null)\n\t\t")
        .append("throw new NullPointerException(\"null node name\");\n\t\t")
        .append("this.name = name;\n\t")
        .append("Stream.of(children).forEach(super::add);\n}\n")
        .append("@Override public String toString() {\n\t")
        .append("return isEmpty()?name: name+super.toString();\n}\n")
        .append("void prettyPrint() {\n\tprettyPrint(1);\n\t}\n")
        .append("void prettyPrint(int depth){\n\t")
        .append("System.out.printf(\"%\"+(depth*4)+\"s%s%n\",\"\",name);\n\t")
        .append("forEach(child -> child.prettyPrint(depth+1));\n\t}\n}\n")

        .append("}");
        return srcBody.toString();
    }

    /**
     * Depending on the grammar the method(s) creating nodes for nonterminals are written.
     * The methods are altered by the entries of the 2d string array.
     * @param table     first column: left side of productions, second column: right sde of productions
     *                  third column: first group of the row
     * @param src       The source code already initiated.
     * @return Nonterminals Methods for source code of the parser.
     */
    private void createNonTerminals(final String[][] table, final StringBuilder src) {

        for (final String[] row: table) {
            // writes "isDone" in left column, if passed
            if (!row[0].equals("isDone")) {
                final String curNT = row[0];
                // saves all chars for the exception message
                final StringBuilder lookaheads = new StringBuilder();
                src.append("\nNode ").append(curNT).append("() throws SyntaxErrorException {\n");
                // generates nodes per row
                lookaheadBranches(table, curNT, src, lookaheads);
                src.append("\n\tthrow new SyntaxErrorException(\"expected one of ");
                src.append(lookaheads);
                lookaheads.deleteCharAt(lookaheads.length()-1).append("; ");
                src.append("found \" + lookahead);\n}\n");
    }}}

    /**
     * Writes the if-compare structures for each lookahead element of the first group.
     * @param table         first column: left side of productions, second column: right sde of productions
     *                      third column: first group of the row
     * @param curNT         Current Nonterminal to write.
     * @param src           Source code of the parser.
     * @param lookaheads    Merged lookaheads for the exception message.
     */
    private void lookaheadBranches(final String[][] table, final String curNT, final StringBuilder src, final StringBuilder lookaheads) {
        boolean orOp = false;

        for (String[] row: table) {
            if (curNT.equals(row[0])) {
                src.append("\tif (");

                for (final char firstEl: row[2].toCharArray()) {
                    if (orOp) {
                        src.append(" || ");
                    }
                    src.append("lookahead == '").append(firstEl).append('\'');
                    orOp = true;
                    lookaheads.append(firstEl).append(',');
                }
                orOp = false;
                row[0] = "isDone";
                src.append(")\n\t\treturn new Node(\"").append(curNT).append("\",");

                for (final char rightEl: row[1].toCharArray()) {
                    if (Character.isUpperCase(rightEl)) {
                        src.append(rightEl).append("(),");
                    }
                    else {
                        src.append("terminal('").append(rightEl).append("'),");
                }}
                // last trivial comma
                src.deleteCharAt(src.length()-1).append(");");
                src.append("\nelse ");
    }}}
}
