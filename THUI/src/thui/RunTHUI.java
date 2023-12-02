package thui;

import java.io.IOException;


public class RunTHUI {
    public static void main(String[] args) throws IOException {


        // CS_Utility_TopK_THUI
        // String input = "./CS_Utility_TopK_THUI.txt";
        String input = "./thui.txt";
        String output = "./output_revised.txt";
        int topN = 20;
        double alpha = 0.5;
        double beta = 0.5;
        boolean eucsPrune = false;
        
        AlgoTHUI topk = new AlgoTHUI(topN, alpha, beta);
        topk.runAlgorithm(input, output, eucsPrune);
        topk.printStats();
    }
}
