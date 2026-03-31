import java.io.File;
import java.io.IOException;


// -------------------------------------------------------------------------
/**
 *  This file is the main that first sorts the files from keys into 
 *  min -> max order then after will output in a new file the sorted with a
 *  merge algorithm
 * 
 *  @author kece05
 *  @version Mar 25, 2026
 */
public class ExternalSort {

    /**
     * The working memory available to the program: 50,000 bytes
     */
    private static final int MEMBYTES = 50000;

    /**
     * Sorts the target file using a two-phase external sort.
     * @param theFileName The name of the file to be sorted
     *
     * @throws IOException if one of the phases fails
     */
    public static void sort(String theFileName)
        throws IOException {
        // Allocating our single 50,000-byte memory pool for the entire program
        byte[] workingMem = new byte[MEMBYTES];

        // Creating a temporary file to hold our initial sorted runs
        String runFileName = theFileName + ".runs"; 

        try {
            // Read the unsorted file and build Min-Heap
            // then write to the temp file as sorted runs
            Generator.generateRuns(theFileName, runFileName, workingMem);

            // Multiway merge the runs from the temp file back into 
            // the original file
            MultiMerge.mergeRuns(runFileName, theFileName, workingMem);
            
        } 
        finally {
            // Deleting temp file
            File tempFile = new File(runFileName);
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }
}