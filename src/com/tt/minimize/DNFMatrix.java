package com.tt.minimize;

import com.google.common.base.Charsets;
import com.google.common.collect.Collections2;
import com.google.common.io.Files;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by tt on 08.11.17.
 */
public class DNFMatrix implements Serializable {

    private int variableNumber;
    private int matrixHeight;
    private List<List<Integer>> matrix;
    private int globalBound = 0;
    private int minCount = 0;
    private String minDnf;
    private String initDnf;

    private DNFMatrix(int variableNumber) {
        this.variableNumber = variableNumber;
        matrixHeight = (int) Math.pow(2, variableNumber);
        matrix = new ArrayList<>();
    }

    public DNFMatrix(DNFMatrix dnfMatrix) {
        this.variableNumber = dnfMatrix.getVariableNumber();
        this.matrixHeight = dnfMatrix.getMatrixHeight();
        this.matrix = dnfMatrix.getMatrix();
    }

    public static void main(String[] args) throws InterruptedException {
        DNFMatrix dnfMatrix = new DNFMatrix(5);

//        dnfMatrix.fillLastDnfMatrix();
        dnfMatrix.fillRandomDnfMatrix();
        System.out.println(dnfMatrix.getMatrix().size());

        dnfMatrix.setInitDnf(dnfMatrix.toString());


        Collection<List<List<Integer>>> permutations = Collections2.permutations(dnfMatrix.matrix);

        for (Iterator iterator = permutations.iterator(); iterator.hasNext(); ) {
            List<List<Integer>> nextMatrix = (List<List<Integer>>) iterator.next();

            List<List<Integer>> copyForPermutations = new ArrayList<>();

            for (List<Integer> innerList : nextMatrix) {
                ArrayList<Integer> copy = new ArrayList<>();
                for (Integer integer : innerList) {
                    copy.add(new Integer(integer));
                }
                copyForPermutations.add(copy);
            }

            dnfMatrix.setMatrix(copyForPermutations);

            dnfMatrix.reduceMatrix();
        }

        File file = new File("test.txt");
        try {
            Files.write("Init DNF: " + dnfMatrix.getInitDnf() +"\n" +
                    "MinDnf" + dnfMatrix.getMinDnf(), file, Charsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void saveDnfMatrix() throws IOException {
        FileOutputStream fos = new FileOutputStream("t.tmp");
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(this);
        oos.close();
    }

    private DNFMatrix loadDnfMatrix() throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream("t.tmp");
        ObjectInputStream ois = new ObjectInputStream(fis);
        DNFMatrix dnfMatrix = (DNFMatrix) ois.readObject();
        ois.close();
        return dnfMatrix;
    }

    private void fillLastDnfMatrix() {
        try {
            DNFMatrix dnfMatrix = loadDnfMatrix();
            this.variableNumber = dnfMatrix.getVariableNumber();
            this.matrixHeight = dnfMatrix.getMatrixHeight();
            this.matrix = dnfMatrix.getMatrix();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void fillRandomDnfMatrix() {
        Random rand = new Random();
        for (int i = 0; i < this.matrixHeight; i++) {
            if (rand.nextInt(2) == 1) {
                ArrayList<Integer> localArr = new ArrayList<>();
                for (int j = this.variableNumber - 1; j >= 0; j--) {
                    localArr.add((i / (int) Math.pow(2, j)) % 2);
                }
                this.matrix.add(localArr);
            }
        }
        try {
            this.saveDnfMatrix();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reduceMatrix() {
//        System.out.println("=========================");
//        System.out.println("Initial matrix: \n" + this);
//        System.out.println("========Gluing==========");
        while (gluing()) ;
        matrix = matrix.stream().distinct().collect(Collectors.toList());

//        System.out.println(this);

        int countAfterGluing = 0;
        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < matrix.get(i).size(); j++) {
                if (matrix.get(i).get(j) != null) countAfterGluing++;
            }
        }


        if (globalBound == 0) {
            globalBound = countAfterGluing;
        }

        if (countAfterGluing > globalBound) {
//            System.out.println("CountAfterGluing: " + countAfterGluing + " > " + "globalBound: " + globalBound);
            return;
        } else {
            globalBound = countAfterGluing;
        }


//        System.out.println("countAfterGluing: " + countAfterGluing);
//
//
//
//        System.out.println("========Absorption==========");
        while (absorption()) ;

        int countAfterAbsorption = 0;
        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < matrix.get(i).size(); j++) {
                if (matrix.get(i).get(j) != null) countAfterAbsorption++;
            }
        }

        if (minCount == 0) {
            minCount = countAfterGluing;
        }
        if (countAfterAbsorption > minCount) {
//            System.out.println("countAfterAbsorption: " + countAfterAbsorption + " >= " + "minCount: " + minCount);
        } else {
            minCount = countAfterAbsorption;
//            System.out.println("minCount: " + minCount);
        }

        if (countAfterAbsorption==minCount){
            setMinDnf(this.toString());
        }

//        System.out.println("countAfterAbsorption: " + countAfterAbsorption);
//
//        System.out.println(this);
//        System.out.println("global: " + globalBound);
    }


    private boolean gluing() {
        boolean isChanged = false;

        for (int i = 0; i < matrix.size(); i++) {
            List<Integer> conjunction = matrix.get(i);

            for (int j = 0; j < conjunction.size(); j++) {
                if (conjunction.get(j) == null) continue;

                Integer reduceVariable = conjunction.get(j);

                for (int z = i+1; z < matrix.size(); z++) {
                    ArrayList<Integer> checkConjunction = new ArrayList<>(matrix.get(z));
                    if (checkConjunction.get(j) == null) break;
                    checkConjunction.set(j, not(checkConjunction.get(j)));


                    if (matrix.get(z).get(j).equals(not(reduceVariable)) &&
                            conjunction.equals(checkConjunction)) {
                        conjunction.set(j, null);
                        isChanged = true;
                        break;
                    }
                }
                if (isChanged) break;
            }
        }

        return isChanged;
    }




    private boolean absorption() {
        matrix = matrix.stream().distinct().collect(Collectors.toList());
        boolean isChanged = false;
        for (int i = 0; i < matrix.size(); i++) {
            List<Integer> conjunction = matrix.get(i);
            for (int z = 0; z < matrix.size(); z++) {
                if (z == i) continue;
                List<Integer> chkConjunction = matrix.get(z);
                int count = 0;
                for (int j = 0; j < conjunction.size(); j++) {
                    if (chkConjunction.get(j) == null && conjunction.get(j) != null) break;
                    if (chkConjunction.get(j) != null && conjunction.get(j) != null &&
                            !Objects.equals(chkConjunction.get(j), conjunction.get(j))) break;
                    if (conjunction.get(j) == null || chkConjunction.get(j) != null) count += 1;
                    if (count == variableNumber) {
                        matrix.set(z, conjunction);
                        isChanged = true;
                        return isChanged;
                    }
                }

            }
        }
        return isChanged;
    }

    private boolean isListOfNulls(List myList) {
        for (Object o : myList)
            if (!(o == null))
                return false;
        return true;
    }

    private static Integer not(Integer integer) {
        return integer.equals(0) ? 1 : 0;
    }


    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < matrix.size(); i++) {
            List<Integer> localArr = matrix.get(i);
            if (localArr.size() != 0) {
                str.append("(");
                for (int j = 0; j < variableNumber; j++) {
                    if (localArr.get(j) == null) {
                        continue;
                    }
                    if (localArr.get(j) == 0) {
                        str.append("/x");
                    } else if (localArr.get(j) == 1) {
                        str.append("x");
                    }
                    str.append(j + 1);
                    if (j != variableNumber - 1) {
                        str.append("\u00B7");
                    }
                }
            }
            str.append(")");
            str.append("\u2228");
        }
        str.deleteCharAt(str.length() - 1);
        str.append("\n");

//        for (int i = 0; i < this.matrix.size(); i++) {
//            List<Integer> localArr = matrix.get(i);
//            for (int j = 0; j < variableNumber; j++) {
//                str.append(localArr.get(j));
//                str.append(" ");
//            }
//            str.append("\n");
//        }


//        String wolfram = new String(str.toString());
//        wolfram = wolfram.replaceAll("/", "!");
//        wolfram = wolfram.replaceAll("∨", "|");
//        wolfram = wolfram.replaceAll("·", "&");
//        wolfram = wolfram.replaceAll("x1", "q");
//        wolfram = wolfram.replaceAll("x2", "w");
//        wolfram = wolfram.replaceAll("x3", "r");
//        wolfram = wolfram.replaceAll("x4", "t");
//        wolfram = wolfram.replaceAll("x5", "u");
//        str.append("\n").append(wolfram);


        return str.toString();
    }

    public int getVariableNumber() {
        return variableNumber;
    }

    public int getMatrixHeight() {
        return matrixHeight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DNFMatrix)) return false;

        DNFMatrix dnfMatrix = (DNFMatrix) o;

        return getMatrix().equals(dnfMatrix.getMatrix());
    }

    @Override
    public int hashCode() {
        return getMatrix().hashCode();
    }

    public void setMatrix(List<List<Integer>> matrix) {
        this.matrix = matrix;
    }

    public void setMinDnf(String minDnf) {
        this.minDnf = minDnf;
    }

    public void setInitDnf(String initDnf) {
        this.initDnf = initDnf;
    }

    public String getMinDnf() {
        return minDnf;
    }

    public String getInitDnf() {
        return initDnf;
    }

    public List<List<Integer>> getMatrix() {
        return matrix;
    }
}
