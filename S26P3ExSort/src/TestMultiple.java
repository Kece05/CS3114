import java.io.*;

// -------------------------------------------------------------------------
/**
 * Tests ExternalSort on multiple file sizes with both ascii
 * and binary formats. Verifies correctness after sorting.
 *
 * @author kece05
 * @version Mar 25, 2026
 */
public class TestMultiple {

    /**
     * Main method to run the multiple size tests.
     *
     * @param args command line arguments (unused)
     * @throws Exception if file operations fail
     */
    public static void main(String[] args) throws Exception {
        CheckFile fileChecker = new CheckFile();
        FileGenerator gen = new FileGenerator();

        int[] sizes = {1, 2, 3, 5, 10, 11, 12, 15, 20, 25};
        for (int size : sizes) {
            String namea = "ta_" + size + "a.bin";
            String nameb = "ta_" + size + "b.bin";
            gen.generateFile(namea, size, "a");
            gen.generateFile(nameb, size, "b");

            String outA = "ta_out_" + size + "a.bin";
            String outB = "ta_out_" + size + "b.bin";
            SortUtils.copyFile(namea, outA);
            SortUtils.copyFile(nameb, outB);

            ExternalSort.sort(outA);
            ExternalSort.sort(outB);

            boolean okA = fileChecker.checkFileA(outA, size);
            boolean okB = fileChecker.checkFile(outB, size);
            System.out.println("Size " + size
                + ": A=" + okA + " B=" + okB);

            new File(namea).delete();
            new File(nameb).delete();
            new File(outA).delete();
            new File(outB).delete();
        }
    }
}
