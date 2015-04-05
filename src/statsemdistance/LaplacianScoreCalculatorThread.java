package statsemdistance;

import Jama.Matrix;
import java.util.Map;

/**
 * This class is used to compute the Laplacian score of one tag/feature using multithreading.
 * Uses Jama Matrix.
 * @author mgarchery
 */
public class LaplacianScoreCalculatorThread extends Thread{
    
    private Matrix feature;
    private Matrix laplacian;
    private Matrix diagonal;
    private Matrix ones;
    private Matrix onesT;
    private Map tagsWithLS;
    private String tag;
    
    public LaplacianScoreCalculatorThread(String tag, Matrix feature, Matrix laplacian, Matrix diagonal, Matrix ones, Matrix onesT, Map<String,Double> tagsWithLS){
        this.tag = tag;
        this.feature = feature;
        this.laplacian = laplacian;
        this.diagonal = diagonal;
        this.ones = ones;
        this.onesT = onesT;
        this.tagsWithLS = tagsWithLS;
    }
    
    @Override
    public void run() {
        
        //Compute Laplacian score
        Matrix featureT = feature.transpose();
        double numerator = featureT.times(diagonal).times(ones).get(0,0);
        double denominator = onesT.times(diagonal).times(ones).get(0,0);
          
        Matrix f_ = feature.minus(ones.times(numerator/denominator));
        Matrix f_T = f_.transpose();
           
        double laplacianNumerator = f_T.times(laplacian).times(f_).get(0,0);
        double laplacianDenominator = f_T.times(diagonal).times(f_).get(0,0);
        double laplacianScore = laplacianNumerator/laplacianDenominator;
        
        //Add score to the map
        tagsWithLS.put(tag, laplacianScore);
    }
    
}
