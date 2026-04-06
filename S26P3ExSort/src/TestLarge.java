import java.io.*;

// -------------------------------------------------------------------------
/**
 * Tests ExternalSort on larger file sizes with both ascii
 * and binary formats. Verifies correctness after sorting.
 *
 * @author kece05
 * @version Mar 25, 2026
 */
public class TestLarge {

    /**
     * Main method to run the large file tests.
     *
     * @param args command line arguments (unused)
     * @throws Exception if file operations fail
     */
    public static void main(String[] args) throws Exception {
        CheckFile fileChecker = new CheckFile();
        FileGenerator gen = new FileGenerator();

        int[] sizes = {50, 100, 200, 500};
        for (int size : sizes) {
            String na = "tl_" + size + "a.bin";
            String nb = "tl_" + size + "b.bin";
            gen.generateFile(na, size, "a");
            gen.generateFile(nb, size, "b");

            ExternalSort.sort(na);
            ExternalSort.sort(nb);

            boolean okA = fileChecker.checkFileA(na, size);
            boolean okB = fileChecker.checkFile(nb, size);
            System.out.println("Size " + size
                + ": A=" + okA + " B=" + okB);
            new File(na).delete();
            new File(nb).delete();
        }
    }
}
