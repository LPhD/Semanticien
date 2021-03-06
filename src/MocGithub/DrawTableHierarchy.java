package MocGithub;

import Util.ProcessingText;


import javax.swing.plaf.synth.SynthScrollBarUI;
import java.io.IOException;
import java.util.*;

import static MocGithub.DrawTableHierarchy.Cell.*;

/**
 * Created by shuruiz on 5/16/17.
 */
public class DrawTableHierarchy {
    HashMap<String, Cluster> clusterTree = new HashMap<>();
    ArrayList<Cluster> noPairLeftClusters = new ArrayList<>();
    int array_width;

    public HashMap<String, Cluster> getClusterTree() {
        return clusterTree;
    }

    public String[][] getHierachyStringFromText(String analysisDir, String splitStep) {
        String hierarchyString = "";
        try {
            hierarchyString = new ProcessingText().readResult(analysisDir + splitStep + "_hierachyArray.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }

        String[] hierarchyList = hierarchyString.split("\n");
        int array_hight = hierarchyList.length;
        String[][] hierachyArray = new String[array_hight][];

        for (int i = 0; i < array_hight; i++) {
            String row = hierarchyList[i];
            String[] cell = row.split(",");
            hierachyArray[i] = cell;

        }
        return hierachyArray;
    }

    public enum Cell {
        topLeft, bottomLeft, bottom, left, none;

        @Override
        public String toString() {
            switch (this) {
                case topLeft:
                    return "topLeft";
                case bottomLeft:
                    return "bottomLeft";
                case bottom:
                    return "bottom";
                case left:
                    return "left";
                case none:
                    return "none";
                default:
                    throw new IllegalArgumentException();
            }
        }

    }

    class Cluster {
        String name;
        int start, end;
        int index;
        String leftChildName, rightChildName;

        Cluster(String name, int index) {
            this.name = name;
            this.index = index;
        }

        Cluster(String name) {
            this.name = name;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        public void setLeftChild(String leftChildName) {
            this.leftChildName = leftChildName;
        }

        public void setRightChild(String rightChildName) {
            this.rightChildName = rightChildName;
        }

        public void setStart(int start) {
            this.start = start;
        }

        public void setEnd(int end) {
            this.end = end;
        }
    }


    public int getTreeHight(String splitStep) {
        int colspan = 0;
        String[] split = splitStep.split("--");
        for (String s : split) {
            for (String sub : s.split("~")) {
                int tmp_colspan = sub.split("_").length;
                if (tmp_colspan > colspan) {
                    colspan = tmp_colspan;
                }
            }
        }
        return colspan;
    }

    public Cell[][] calculatingArray(String analysisDir, String splitStep) {

        array_width = getTreeHight(splitStep);
        clusterTree = generateTree(splitStep, array_width);

        String[] clusterArray = splitStep.split("~");

        int array_hight = clusterArray.length * 2;
        Cell[][] array = new Cell[array_hight][array_width];

        for (int j = 0; j < clusterArray.length; j++) {
            String subCluster = clusterArray[j];
            int level = subCluster.split("_").length - 1;
            boolean hasPair = hasPair(splitStep, subCluster);
            Cluster currentCluster = clusterTree.get(subCluster);
            currentCluster.setStart(j * 2);
            currentCluster.setEnd(j * 2 + 1);
            if (hasPair) {
                if (subCluster.endsWith("_1")) {
                    setLeftChildCluster(array, j * 2, subCluster, level, j * 2);
                } else if (subCluster.endsWith("_2")) {
                    setRightChildCluster(array, 2 * j, subCluster, level, hasPair);
                }

            } else {
                for (int index = array_width - 1; index >= 0; index--) {
                    if (index > level) {
                        array[2 * j][index] = bottom;
                        array[2 * j + 1][index] = none;
                    } else if (index == level) {
                        if (subCluster.endsWith("_1")) {
                            setLeftChildCluster(array, j * 2, subCluster, level, j * 2);
                            noPairLeftClusters.add(clusterTree.get(subCluster));
                        } else if (subCluster.endsWith("_2")) {
                            String leftName = subCluster.replaceAll("[2]$", "1");
                            noPairLeftClusters.remove(leftName);
                            Cluster left = clusterTree.get(leftName);
                            setLeftChildCluster(array, left.getStart() + (left.getEnd() - left.getStart()) / 2, leftName, level, left.getStart());
                            setRightChildCluster(array, 2 * j, subCluster, level, hasPair);
                        }
                    }
                }
            }
        }


        for (int height = 0; height < array_hight; height++) {
            for (int width = array_width - 1; width >= 0; width--) {
                if (array[height][width] == null) {
                    if (height == 0 || height == array_hight - 1) {
                        array[height][width] = none;
                    } else {
                        array[height][width] = none;

                    }
                }
            }
        }

        if (analysisDir.length() > 0) {
            StringBuilder sb = new StringBuilder();

            for (int j = 0; j < array_hight; j++) {
                for (int i = 0; i < array_width; i++) {
                    sb.append(array[j][i].toString() + ",");
                }
                sb.append("\n");
            }
            new ProcessingText().rewriteFile(sb.toString(), analysisDir + splitStep + "_hierachyArray.txt");
        }
        return array;
    }

    private void setRightChildCluster(Cell[][] array, int rightStart, String subCluster, int level, boolean hasPair) {

        for (int index = array_width - 1; index >= 0; index--) {
            if (index > level) {
                if (array[rightStart][index] == null) array[rightStart][index] = bottom;
                if (array[rightStart + 1][index] == null) array[rightStart + 1][index] = none;
            } else if (index == level) {
                array[rightStart][level] = bottomLeft;
                if (array[rightStart + 1][level] == null) {
                    array[rightStart + 1][level] = none;
                }
            }
        }


        Cluster parentCluster = clusterTree.get(subCluster.replaceAll("[\\_][2]$", ""));
        parentCluster.setEnd(clusterTree.get(subCluster).end);


        Cluster leftCluster = clusterTree.get(parentCluster.leftChildName);
        int leftMiddle = getMiddleIndex(leftCluster);

        if (!hasPair) {
            for (int diff = leftMiddle + 2; diff <= rightStart - 1; diff++) {
                int targetLevel = parentCluster.leftChildName.split("_").length > subCluster.split("_").length ? subCluster.split("_").length : parentCluster.leftChildName.split("_").length;
                array[diff][targetLevel - 1] = left;
            }
        }


        setParentCluster(array, level, parentCluster);
        if (parentCluster.name.endsWith("_1")) {
            setLeftChildCluster(array, getMiddleIndex(parentCluster), parentCluster.name, level - 1, leftCluster.start);
        }else  if (parentCluster.name.endsWith("_2")) {
            setRightChildCluster(array, getMiddleIndex(parentCluster), parentCluster.name, level - 1, false);
        }

        if (noPairLeftClusters.size() > 0) {
            for (int i = 0; i <noPairLeftClusters.size(); i++) {
                Cluster leftChild = noPairLeftClusters.get(i);
                int cur_level = leftChild.name.split("_").length - 1;
                String rightChildName = leftChild.name.replaceAll("[_][1]$", "\\_2");
                Cluster rightChild = clusterTree.get(rightChildName);
                if ((cur_level + 1 == level) || (cur_level == level)) {
                    if (cur_level + 1 == level) {
                        int middle = getMiddleIndex(rightChild);
                        noPairLeftClusters.remove(leftChild);
                        setRightChildCluster(array, middle, rightChildName, cur_level, false);
                    } else if (cur_level == level) {
                        int middle = getMiddleIndex(rightChild);
                        noPairLeftClusters.remove(leftChild);
                        setRightChildCluster(array, middle, rightChildName, cur_level, false);
                    }
                }
            }
        }

    }


    private int getMiddleIndex(Cluster leftCluster) {
        return leftCluster.start + (leftCluster.end - leftCluster.start) / 2;
    }

    private void setParentCluster(Cell[][] array, int level, Cluster parentCluster) {
        int parent_start = parentCluster.getStart();
        int parent_end = parentCluster.getEnd();
        if (array[parent_start + (parent_end - parent_start) / 2][level - 1] == null) {
            array[parent_start + (parent_end - parent_start) / 2][level - 1] = bottom;
        }
//        if (level - 1 > 1) {
//            Cluster grandParentCluster = clusterTree.get(parentCluster.name.replaceAll("[\\_][2]$", ""));
//            grandParentCluster.setEnd(clusterTree.get(parentCluster.name).end);
//            noPairLeftClusters.add(grandParentCluster);
//        }
    }

    private void setLeftChildCluster(Cell[][] array, int leftStart, String subCluster, int level, int parent_start) {
        for (int index = array_width - 1; index >= 0; index--) {
            if (index > level) {
                if (array[leftStart][index] == null) array[leftStart][index] = bottom;
                if (array[leftStart + 1][index] == null) array[leftStart + 1][index] = none;
            } else if (index == level) {
                if (array[leftStart][level] == null) array[leftStart][level] = none;
                array[leftStart + 1][level] = topLeft;
            }
        }


        Cluster parentCluster = clusterTree.get(subCluster.replaceAll("[\\_][1]$", ""));
        parentCluster.setStart(parent_start);
    }

    private HashMap<String, Cluster> generateTree(String clusters, int max_split) {
        String clusterName = clusters.split("_")[0];
        Stack clusterList = new Stack();
        clusterList.push(clusterName);
        HashMap<String, Cluster> clusterTree = new HashMap<>();


        String currentCname = (String) clusterList.pop();
        int index = currentCname.split("_").length;
        while (index <= max_split || clusterList.size() > 0) {
            Cluster c = new Cluster(currentCname, index);
            String leftChild = currentCname + "_1";
            String rightChild = currentCname + "_2";
            if (index < max_split - 1 || ((index == max_split - 1) && (clusters.contains(leftChild + "~") || clusters.contains("~" + rightChild)))) {
                c.setLeftChild(leftChild);
                c.setRightChild(rightChild);
                clusterList.push(leftChild);
                clusterList.push(rightChild);
            }
            clusterTree.put(currentCname, c);
            if (clusterList.size() > 0) {
                currentCname = (String) clusterList.pop();
                index = currentCname.split("_").length;
            } else {
                break;
            }
        }


        return clusterTree;


    }

    private boolean hasPair(String clusters, String subCluster) {
        List<String> clusterList = Arrays.asList(clusters.split("~"));

        if (subCluster.endsWith("_1")) {
            String pair = subCluster.replaceAll("[1]$", "2");
            if (clusterList.contains(pair)) {
                return true;
            }
        }
        if (subCluster.endsWith("_2")) {
            String pair = subCluster.replaceAll("[2]$", "1");
            if (clusterList.contains(pair)) {
                return true;
            }
        }
        return false;
    }


}
