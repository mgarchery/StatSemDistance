package statsemdistance;

import Jama.Matrix;

/**
 * This class is used to compute the cosine similarity between the cooccurrence vectors of two tags using multithreading.
 * Uses Jama Matrix.
 * @author mgarchery
 */
public class CosineSimilarityCalculatorThread extends Thread{
 
    
    private Matrix v1;
    private Matrix v2;
    private int indice1;
    private int indice2;
    private Matrix similarities;
    
    public CosineSimilarityCalculatorThread(Matrix v1 , Matrix v2 , int indice1, int indice2, Matrix similarities){
        this.v1 = v1;
        this.v2 = v2;
        this.indice1 = indice1;
        this.indice2 = indice2;
        this.similarities = similarities;
    }
    
    
    @Override
    public void run(){
        if(indice1 == indice2){
            similarities.set(indice1, indice2, 0);
        }else{         
            double cosSim = cosineSimilarity(v1, v2);
            similarities.set(indice1, indice2, cosSim);
            similarities.set(indice2, indice1, cosSim);
        }
    }
    
    
    /**
    * Computes the cosine similarity measure between two row vectors with same size
    * @param v1 first row vector as Jama Matrix
    * @param v2 second row vector as Jama Matrix
    * @return cosine similarity measure if both vectors are correct, else 0
    */
   private double cosineSimilarity(Matrix v1, Matrix v2)
   {
        if (v1.getRowDimension() != 1 || v2.getRowDimension() != 1){
           System.out.println("Cosine similarity can be computed on row vectors only");
           return 0;
        }
        if(v1.getColumnDimension() != v2.getColumnDimension()){
           System.out.println("Both vectors should have the same size to compute cosine similarity");
           return 0;
        }
        
        Matrix dotProduct = v1.arrayTimes(v2);
        int size = dotProduct.getColumnDimension();
        double product = 0;
        
        for(int i = 0; i < size; i++){
            product += dotProduct.get(0, i);
        }
        
        product /= v1.norm2();
        product /= v2.norm2();
        
        return product;
    }
    
    
}
