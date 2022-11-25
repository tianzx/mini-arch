package net.tianzx.arch.btree;

import java.util.Arrays;
import java.util.Comparator;

public class BPlusTree {
    int m; // int level
    InternalNode root;
    LeafNode firstLeaf;

    /**
     * This method performs a standard binary search on a sorted
     * DictionaryPair[] and returns the index of the dictionary pair
     * with target key t if found. Otherwise, this method returns a negative
     * value.
     *
     * @param dps: list of dictionary pairs sorted by key within leaf node
     * @param t:   target key value of dictionary pair being searched for
     * @return index of the target value if found, else a negative value
     */
    private int binarySearch(DictionaryPair[] dps, int numPairs, int t) {
        Comparator<DictionaryPair> c = (o1, o2) -> {
            Integer a = Integer.valueOf(o1.key);
            Integer b = Integer.valueOf(o2.key);
            return a.compareTo(b);
        };
        return Arrays.binarySearch(dps, 0, numPairs, new DictionaryPair(t, 0), c);
    }

    class Node {
        InternalNode parent;
    }

    /**
     * This method performs a standard linear search on a list of Node[] pointers
     * and returns the index of the first null entry found. Otherwise, this
     * method returns a -1. This method is primarily used in place of
     * binarySearch() when the target t = null.
     *
     * @param pointers: list of Node[] pointers
     * @return index of the target value if found, else -1
     */
    private int linearNullSearch(Node[] pointers) {
        for (int i = 0; i < pointers.length; i++) {
            if (pointers[i] == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * An internal node only holds keys; it
     * does not hold dictionary pairs.
     */
    class InternalNode extends Node {
        int maxDegree;
        int minDegree;
        //childPointers size
        int degree;
        InternalNode leftSibling;
        InternalNode rightSibling;
        Integer[] keys;
        Node[] childPointers;

        private boolean isOverfull() {
            return this.degree == maxDegree + 1;
        }

        /**
         * This method appends 'pointer' to the end of the childPointers
         * instance variable of the InternalNode object. The pointer can point to
         * an InternalNode object or a LeafNode object since the formal
         * parameter specifies a Node object.
         *
         * @param pointer: Node pointer that is to be appended to the
         *                 childPointers list
         */
        private void appendChildPointer(Node pointer) {
            this.childPointers[degree++] = pointer;
        }

        /**
         * Given a pointer to a Node object and an integer index, this method
         * inserts the pointer at the specified index within the childPointers
         * instance variable. As a result of the insert, some pointers may be
         * shifted to the right of the index.
         *
         * @param pointer: the Node pointer to be inserted
         * @param index:   the index at which the insert is to take place
         */
        private void insertChildPointer(Node pointer, int index) {
            for (int i = degree - 1; i >= index; i--) {
                childPointers[i + 1] = childPointers[i];
            }
            this.childPointers[index] = pointer;
            this.degree++;
        }

        /**
         * Given a Node pointer, this method will return the index of where the
         * pointer lies within the childPointers instance variable. If the pointer
         * can't be found, the method returns -1.
         *
         * @param pointer: a Node pointer that may lie within the childPointers
         *                 instance variable
         * @return the index of 'pointer' within childPointers, or -1 if
         * 'pointer' can't be found
         */
        private int findIndexOfPointer(Node pointer) {
            for (int i = 0; i < childPointers.length; i++) {
                if (childPointers[i] == pointer) {
                    return i;
                }
            }
            return -1;
        }


        /**
         * Constructor
         *
         * @param m:    the max degree of the InternalNode
         * @param keys: the list of keys that InternalNode is initialized with
         */
        private InternalNode(int m, Integer[] keys) {
            this.maxDegree = m;
            this.minDegree = (int) Math.ceil(m / 2.0);
            this.degree = 0;
            this.keys = keys;
            this.childPointers = new Node[this.maxDegree + 1];
        }

        /**
         * Constructor
         *
         * @param m:        the max degree of the InternalNode
         * @param keys:     the list of keys that InternalNode is initialized with
         * @param pointers: the list of pointers that InternalNode is initialized with
         */
        private InternalNode(int m, Integer[] keys, Node[] pointers) {
            this.maxDegree = m;
            this.minDegree = (int) Math.ceil(m / 2.0);
            this.degree = linearNullSearch(pointers);
            this.keys = keys;
            this.childPointers = pointers;
        }

        private void removePointer(int index) {
            this.childPointers[index] = null;
            this.degree--;
        }

        private void removePointer(Node pointer) {
            for (int i = 0; i < childPointers.length; i++) {
                if (childPointers[i] == pointer) { this.childPointers[i] = null; }
            }
            this.degree--;
        }

    }
    /**
     * This method modifies a list of Integer-typed objects that represent keys
     * by removing half of the keys and returning them in a separate Integer[].
     * This method is used when splitting an InternalNode object.
     * @param keys: a list of Integer objects
     * @param split: the index where the split is to occur
     * @return Integer[] of removed keys
     */
    private Integer[] splitKeys(Integer[] keys, int split) {

        Integer[] halfKeys = new Integer[this.m];

        // Remove split-indexed value from keys
        keys[split] = null;

        // Copy half of the values into halfKeys while updating original keys
        for (int i = split + 1; i < keys.length; i++) {
            halfKeys[i - split - 1] = keys[i];
            keys[i] = null;
        }
        return halfKeys;
    }
    /**
     * This method modifies the InternalNode 'in' by removing all pointers within
     * the childPointers after the specified split. The method returns the removed
     * pointers in a list of their own to be used when constructing a new
     * InternalNode sibling.
     * @param in: an InternalNode whose childPointers will be split
     * @param split: the index at which the split in the childPointers begins
     * @return a Node[] of the removed pointers
     */
    private Node[] splitChildPointers(InternalNode in, int split) {

        Node[] pointers = in.childPointers;
        Node[] halfPointers = new Node[this.m + 1];

        // Copy half of the values into halfPointers while updating original keys
        for (int i = split + 1; i < pointers.length; i++) {
            halfPointers[i - split - 1] = pointers[i];
            in.removePointer(i);
        }

        return halfPointers;
    }

    /**
     * When an insertion into the B+ tree causes an overfull node, this method
     * is called to remedy the issue, i.e. to split the overfull node. This method
     * calls the sub-methods of splitKeys() and splitChildPointers() in order to
     * split the overfull node.
     * @param in: an overfull InternalNode that is to be split
     */
    private void splitInternalNode(InternalNode in) {
        // Acquire parent
        InternalNode parent = in.parent;
        // Split keys and pointers in half
        int midpoint = getMidpoint();
        int newParentKey = in.keys[midpoint];
        Integer[] halfKeys = splitKeys(in.keys, midpoint);
        Node[] halfPointers = splitChildPointers(in, midpoint);
        // Change degree of original InternalNode in
        in.degree = linearNullSearch(in.childPointers);
        // Create new sibling internal node and add half of keys and pointers
        InternalNode sibling = new InternalNode(this.m, halfKeys, halfPointers);
        for (Node pointer : halfPointers) {
            if (pointer != null) { pointer.parent = sibling; }
        }
        // Make internal nodes siblings of one another
        sibling.rightSibling = in.rightSibling;
        if (sibling.rightSibling != null) {
            sibling.rightSibling.leftSibling = sibling;
        }
        in.rightSibling = sibling;
        sibling.leftSibling = in;
        if (parent == null) {

            // Create new root node and add midpoint key and pointers
            Integer[] keys = new Integer[this.m];
            keys[0] = newParentKey;
            InternalNode newRoot = new InternalNode(this.m, keys);
            newRoot.appendChildPointer(in);
            newRoot.appendChildPointer(sibling);
            this.root = newRoot;

            // Add pointers from children to parent
            in.parent = newRoot;
            sibling.parent = newRoot;

        } else {

            // Add key to parent
            parent.keys[parent.degree - 1] = newParentKey;
            Arrays.sort(parent.keys, 0, parent.degree);

            // Set up pointer to new sibling
            int pointerIndex = parent.findIndexOfPointer(in) + 1;
            parent.insertChildPointer(sibling, pointerIndex);
            sibling.parent = parent;
        }
    }
    private int linearNullSearch(DictionaryPair[] dps) {
        for (int i = 0; i < dps.length; i++) {
            if (dps[i] == null) {
                return i;
            }
        }
        return -1;
    }

    /**
     * hold dictionary pairs
     */
    class LeafNode extends Node {
        int maxNumPairs;
        int minNumPairs;
        //dictionary 数量
        int numPairs;
        //左节点
        LeafNode leftSibling;
        //右节点
        LeafNode rightSibling;
        DictionaryPair[] dictionary;

        public LeafNode(int m, DictionaryPair dp) {
            this.maxNumPairs = m - 1;
            this.minNumPairs = (int) (Math.ceil(m / 2) - 1);
            this.dictionary = new DictionaryPair[m];
            this.numPairs = 0;
            this.insert(dp);
        }

        /**
         * Constructor
         *
         * @param dps:    list of DictionaryPair objects to be immediately inserted
         *                into new LeafNode object
         * @param m:      order of B+ tree that is used to calculate maxNumPairs and
         *                minNumPairs
         * @param parent: parent of newly created child LeafNode
         */
        public LeafNode(int m, DictionaryPair[] dps, InternalNode parent) {
            this.maxNumPairs = m - 1;
            this.minNumPairs = (int) (Math.ceil(m / 2) - 1);
            this.dictionary = dps;
            this.numPairs = linearNullSearch(dps);
            this.parent = parent;
        }

        public boolean insert(DictionaryPair dp) {
            if (this.isFull()) {

                /* Flow of execution goes here when numPairs == maxNumPairs */

                return false;
            } else {

                // Insert dictionary pair, increment numPairs, sort dictionary
                this.dictionary[numPairs] = dp;
                numPairs++;
                Arrays.sort(this.dictionary, 0, numPairs);

                return true;
            }
        }

        public void delete(int index) {

            // Delete dictionary pair from leaf
            this.dictionary[index] = null;

            // Decrement numPairs
            numPairs--;
        }

        public boolean isFull() {
            return numPairs == maxNumPairs;
        }
    }


    public void insert(int key, double value) {
        if (isEmpty()) {
            /* Flow of execution goes here only when first insert takes place */

            // Create leaf node as first node in B plus tree (root is null)
            LeafNode ln = new LeafNode(this.m, new DictionaryPair(key, value));

            // Set as first leaf node (can be used later for in-order leaf traversal)
            this.firstLeaf = ln;
        } else {
            // Find leaf node to insert into
            LeafNode leafNode = (this.root == null) ? this.firstLeaf : findLeafNode(key);
            // Insert into leaf node fails if node becomes overfull
            if (!leafNode.insert(new DictionaryPair(key, value))) {
                // Sort all the dictionary pairs with the included pair to be inserted
                leafNode.dictionary[leafNode.numPairs] = new DictionaryPair(key, value);
                leafNode.numPairs++;
                sortDictionary(leafNode.dictionary);
                // Split the sorted pairs into two halves
                int midpoint = getMidpoint();
                DictionaryPair[] halfDict = splitDictionary(leafNode, midpoint);
                if (leafNode.parent == null) {
                    /* Flow of execution goes here when there is 1 node in tree */
                    // Create internal node to serve as parent, use dictionary midpoint key
                    Integer[] parent_keys = new Integer[this.m];
                    parent_keys[0] = halfDict[0].key;
                    InternalNode parent = new InternalNode(this.m, parent_keys);
                    leafNode.parent = parent;
                    parent.appendChildPointer(leafNode);
                } else {
                    /* Flow of execution goes here when parent exists */

                    // Add new key to parent for proper indexing
                    int newParentKey = halfDict[0].key;
                    leafNode.parent.keys[leafNode.parent.degree - 1] = newParentKey;
                    Arrays.sort(leafNode.parent.keys, 0, leafNode.parent.degree);
                }
                // Create new LeafNode that holds the other half
                LeafNode newLeafNode = new LeafNode(this.m, halfDict, leafNode.parent);

                // Update child pointers of parent node
                int pointerIndex = leafNode.parent.findIndexOfPointer(leafNode) + 1;
                leafNode.parent.insertChildPointer(newLeafNode, pointerIndex);

                // Make leaf nodes siblings of one another
                newLeafNode.rightSibling = leafNode.rightSibling;
                if (newLeafNode.rightSibling != null) {
                    newLeafNode.rightSibling.leftSibling = newLeafNode;
                }
                leafNode.rightSibling = newLeafNode;
                newLeafNode.leftSibling = leafNode;
                if (this.root == null) {
                    // Set the root of B+ tree to be the parent
                    this.root = leafNode.parent;
                } else {
                    /* If parent is overfull, repeat the process up the tree,
			   		   until no deficiencies are found */

                    InternalNode in = leafNode.parent;
                    while (in != null) {
                        if (in.isOverfull()) {
                            splitInternalNode(in);
                        } else {
                            break;
                        }
                        in = in.parent;
                    }
                }

            } else {

            }
        }
    }

    /**
     * This is a simple method that returns the midpoint (or lower bound
     * depending on the context of the method invocation) of the max degree m of
     * the B+ tree.
     *
     * @return (int) midpoint/lower bound
     */
    private int getMidpoint() {
        return (int) Math.ceil((this.m + 1) / 2.0) - 1;
    }

    /**
     * This method splits a single dictionary into two dictionaries where all
     * dictionaries are of equal length, but each of the resulting dictionaries
     * holds half of the original dictionary's non-null values. This method is
     * primarily used when splitting a node within the B+ tree. The dictionary of
     * the specified LeafNode is modified in place. The method returns the
     * remainder of the DictionaryPairs that are no longer within ln's dictionary.
     *
     * @param ln:    list of DictionaryPairs to be split
     * @param split: the index at which the split occurs
     * @return DictionaryPair[] of the two split dictionaries
     */
    private DictionaryPair[] splitDictionary(LeafNode ln, int split) {

        DictionaryPair[] dictionary = ln.dictionary;

		/* Initialize two dictionaries that each hold half of the original
		   dictionary values */
        DictionaryPair[] halfDict = new DictionaryPair[this.m];

        // Copy half of the values into halfDict
        for (int i = split; i < dictionary.length; i++) {
            halfDict[i - split] = dictionary[i];
            ln.delete(i);
        }

        return halfDict;
    }

    private void sortDictionary(DictionaryPair[] dictionary) {
        Arrays.sort(dictionary, new Comparator<DictionaryPair>() {
            @Override
            public int compare(DictionaryPair o1, DictionaryPair o2) {
                if (o1 == null && o2 == null) {
                    return 0;
                }
                if (o1 == null) {
                    return 1;
                }
                if (o2 == null) {
                    return -1;
                }
                return o1.compareTo(o2);
            }
        });
    }

    /**
     * This method starts at the root of the B+ tree and traverses down the
     * tree via key comparisons to the corresponding leaf node that holds 'key'
     * within its dictionary.
     *
     * @param key: the unique key that lies within the dictionary of a LeafNode object
     * @return the LeafNode object that contains the key within its dictionary
     */
    private LeafNode findLeafNode(int key) {
        // Initialize keys and index variable
        Integer[] keys = this.root.keys;
        int i;
        // Find next node on path to appropriate leaf node
        for (i = 0; i < this.root.degree - 1; i++) {
            if (key < keys[i]) {
                break;
            }
        }
        /* Return node if it is a LeafNode object,
		   otherwise repeat the search function a level down */
        Node child = this.root.childPointers[i];
        if (child instanceof LeafNode) {
            return (LeafNode) child;
        } else {
            return findLeafNode((InternalNode) child, key);
        }
    }

    private LeafNode findLeafNode(InternalNode node, int key) {
        // Initialize keys and index variable
        Integer[] keys = node.keys;
        int i;

        // Find next node on path to appropriate leaf node
        for (i = 0; i < node.degree - 1; i++) {
            if (key < keys[i]) {
                break;
            }
        }
        /* Return node if it is a LeafNode object,
		   otherwise repeat the search function a level down */
        Node childNode = node.childPointers[i];
        if (childNode instanceof LeafNode) {
            return (LeafNode) childNode;
        } else {
            return findLeafNode((InternalNode) node.childPointers[i], key);
        }
    }

    /**
     * This is a simple method that determines if the B+ tree is empty or not.
     *
     * @return a boolean indicating if the B+ tree is empty or not
     */
    private boolean isEmpty() {
        return firstLeaf == null;
    }

    public class DictionaryPair implements Comparable<DictionaryPair> {
        int key;
        double value;

        /**
         * Constructor
         *
         * @param key:   the key of the key-value pair
         * @param value: the value of the key-value pair
         */
        public DictionaryPair(int key, double value) {
            this.key = key;
            this.value = value;
        }

        /**
         * This is a method that allows comparisons to take place between
         * DictionaryPair objects in order to sort them later on
         *
         * @param o
         * @return
         */
        @Override
        public int compareTo(DictionaryPair o) {
            if (key == o.key) {
                return 0;
            } else if (key > o.key) {
                return 1;
            } else {
                return -1;
            }
        }
    }

    public BPlusTree(int m) {
        this.m = m;
        this.root = null;
    }

}
