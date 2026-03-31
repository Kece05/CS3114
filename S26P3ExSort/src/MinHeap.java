import java.nio.ByteBuffer;

// -------------------------------------------------------------------------
/**
 *  This class creates an implementation and methods for a min heap tree
 * 
 *  @author kece05
 *  @version Mar 23, 2026
 */
public class MinHeap {
    
    /**
     * The buffer pool being looked at
     */
    private ByteBuffer heapBuffer;
    
    /**
     * The number of records currently in the heap
     */
    private int currentSize;
    
    /**
     * The maximum of records the heap can hold
     */
    private int capacity;
    
    /**
     * Bytes per record
     */
    private static final int RECORD_SIZE = 8;
    
    
    /**
     * Initializing the heap for the heap sort
     * 
     * @param memPool the single 50,000 byte array
     * @param offset the starting index for the heap
     * @param maxRecords number of 8 byte records it can hold
     */
    public MinHeap(byte[] memPool, int offset, int maxRecords) {
        // Wrap ONLY the portion of the array dedicated to the heap
        this.heapBuffer = ByteBuffer.wrap(memPool, offset,
            maxRecords * RECORD_SIZE).slice();
        this.capacity = maxRecords;
        this.currentSize = 0;
    }
    
    /**
     * This method inserts a new chunk into the heap
     * 
     * @param key the key to access the data (what will be used in search)
     * @param data the actual data to be stored
     */
    public void insert(int key, int data) {
        if (currentSize >= capacity) {
            throw new IllegalStateException("Heap is full");
        }
        
        // Inserting the new data at the end of heap
        int byteIndex = currentSize * RECORD_SIZE;
        heapBuffer.putInt(byteIndex, key);
        heapBuffer.putInt(byteIndex + 4, data);
        
        currentSize++;
        
        // Fixing the heap to be properly sorted
        siftUp(currentSize - 1);
    }
    
    /**
     * This method removes the min node in the heap tree and outputs
     * it to the file
     * 
     * @param out the output to file the node in
     * 
     */
    public void removeMin(BufferSort out) {
        if (isEmpty()) {
            throw new IllegalStateException("Heap is empty");
        }
        
        // Getting the top node since it is the min
        int minKey = heapBuffer.getInt(0);
        int minData = heapBuffer.getInt(4);
        
        // Writing the byte output file
        out.writeInt(minKey);
        out.writeInt(minData);
        
        currentSize--;
        
        // Updating the tree for the next min node
        if (currentSize > 0) {
            int lastOffset = currentSize * RECORD_SIZE;
            
            heapBuffer.putInt(0, heapBuffer.getInt(lastOffset));
            heapBuffer.putInt(4, heapBuffer.getInt(lastOffset + 4));
            
            siftDown(0);
        }
    }
    
    /**
     * Restores the min heap property by moving a node down the tree
     * 
     * @param position the node to move down
     */
    private void siftDown(int position) {
        int pos = position;
        while (pos < currentSize / 2) {
            // Getting the location of left and right node
            int leftChild = 2 * pos + 1;
            int rightChild = 2 * pos + 2;
            
            // Assuming the left child is the smallest size
            int smallest = leftChild;

            // Updates if assumption is wrong
            if (rightChild < currentSize && 
                heapBuffer.getInt(rightChild * RECORD_SIZE) < 
                heapBuffer.getInt(leftChild * RECORD_SIZE)) {
                smallest = rightChild;
            }
            
            // Now comparing the key of the smallest child against
            // our current node - updates if the true
            if (heapBuffer.getInt(smallest * RECORD_SIZE) <
                heapBuffer.getInt(pos * RECORD_SIZE)) {
                swap(pos, smallest);
                pos = smallest;
            } 
            else {
                break; 
            }
            
        }
    }
    
    /**
     * This method shifts the current node up if the parent node is more
     * than the current node
     * 
     * @param position the node to move up
     */
    private void siftUp(int position) {
        int pos = position;
        while (pos > 0) {
            int parent = (pos - 1) / 2;
            
            // Finding position in array indices to actual byte offsets
            int curByte = pos * RECORD_SIZE;
            int parByte = parent * RECORD_SIZE;
            
            // Seeing if current is less than the parent
            if (heapBuffer.getInt(curByte) < heapBuffer.getInt(parByte)) {
                //Swapping the positions of parent and current
                swap(pos, parent);
                
                // Moving up the tree
                pos = parent;
            } 
            else {
                break;
            }
        }
    }
    
    /**
     * This method swaps two nodes in the heap tree
     * 
     * @param curr the current node to be swapped
     * @param parent the node to be swap with curr
     */
    private void swap(int curr, int parent) {
        // Getting offset for each
        int currOffset = curr * RECORD_SIZE;
        int parOffset = parent * RECORD_SIZE;
        
        // Creating a temp for the key and data for parent
        int tempKey = heapBuffer.getInt(parOffset);
        int tempData = heapBuffer.getInt(parOffset + 4);
        
        // Swapping the parents with the currents
        heapBuffer.putInt(parOffset, heapBuffer.getInt(currOffset));
        heapBuffer.putInt(parOffset + 4, heapBuffer.getInt(currOffset + 4));
        
        // Swapping the temp with the parents
        heapBuffer.putInt(currOffset, tempKey);
        heapBuffer.putInt(currOffset + 4, tempData);
    }
    
    /**
     * @return if the heap is empty
     */
    public boolean isEmpty() {
        return currentSize == 0;
    }
    
    /**
     * @return if buffer pool is full
     */
    public boolean isFull() {
        return currentSize == capacity;
    }

}
