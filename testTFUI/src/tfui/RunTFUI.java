package tfui;

import java.io.IOException;


public class RunTFUI {
    public static void main(String[] args) throws IOException {


        // CS_Utility_TopK_THUI
        String input = "./CS_Utility_TopK_TFUI_copy_2.txt";
        String output = "./output_revised_2.txt";
        int topN = 10;
        double alpha = 0.5;
        double beta = 0.5;
        boolean eucsPrune = false;
        
        AlgoTHUI topk = new AlgoTHUI(topN, alpha, beta);
        topk.runAlgorithm(input, output, eucsPrune);
        topk.printStats();
    }
}
