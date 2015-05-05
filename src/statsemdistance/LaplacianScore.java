package statsemdistance;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * This class selects the most representative tags according to their Laplacian scores.
 * Uses Jama library (http://math.nist.gov/javanumerics/jama/) for matrix operations
 * 
 * @author mgarchery
 */
public class LaplacianScore {
    
    
    /**
     * Puts the most representative tags (i.e. tags with highest Laplacian scores) in a list
     * @param imagesTags all image tags
     * @return list containing the most representative tags according to Laplacian scores
     */
    public static Map getRepresentativeTagsByLaplacianScore(Map imagesTags) {
        
        Map alltags = DistancesMT.countOccurrences(imagesTags);

        if(DistancesMT.PRINT == 1){
            System.out.println("Getting most representative tags with Laplacian score...");
        }
        
        //affect indices to tags
        Map<Integer,String> tagsWithIds = allTagsWithIds(imagesTags);

        if(DistancesMT.PRINT == 1){
            System.out.println("Number of unique tags: " + tagsWithIds.size());
        }

        //compute cooccurrence, cosine similarity, diagonal and finally Laplacian matrixes
        if(DistancesMT.PRINT == 1){
            System.out.println("Computing cooccurrence matrix...");
        }
        Matrix cooccurrence = cooccurrenceMatrix(tagsWithIds,imagesTags);

        if(DistancesMT.PRINT == 1){
            System.out.println("Computing similarity matrix...");
        }
        Matrix similarity = getCosineSimilarityMatrix(cooccurrence);

        if(DistancesMT.PRINT == 1){
            System.out.println("Computing diagonal matrix...");
        }
        Matrix diagonal = getDiagonalMatrix(similarity);

        if(DistancesMT.PRINT == 1){
            System.out.println("Computing laplacian matrix...");
        }
        Matrix laplacian = diagonal.minus(similarity);

        //compute Laplacian score for each tag
        Map tagsWithLS = getLaplacianScores(cooccurrence, diagonal, laplacian, tagsWithIds);


        //sort result map by descending Laplacian scores
        tagsWithLS = sortMapDescendingDouble(tagsWithLS);
            
            
        
        return tagsWithLS;
    }
    
   /**
    * Prints the most representative tags with their Laplacian score
    * @param tagsWithScore map with <key=tag,value=LS score>
    * @param percentageCount percentage of tags to be printed
    */
   public static void printLSScoresForRepresentativeTags(Map<String,Double> tagsWithScore, double percentageCount){
       Iterator iteratorMin = tagsWithScore.entrySet().iterator();
       
            System.out.println("Scores of most representative tags");
            int i = 0;
            while (iteratorMin.hasNext() && i++ < percentageCount) {
                Map.Entry pair = (Map.Entry) iteratorMin.next();
                System.out.println(pair.getKey() + "(" + String.format( "%.4f", pair.getValue() ) + ")");
            }
   }
   
    /**
     * Maps all tags to an indice (representing the tag position in the cooccurrence and similarity matrixes) 
     * @param imagesTags map containing tags for all images
     * @return map with <key=indice,value=tag>
     */
   public static Map<Integer,String> allTagsWithIds(Map imagesTags) {

        if(DistancesMT.PRINT == 1){
            System.out.println("Mapping tags to matrix indices ...\n");
        }
        
        Map<Integer,String> tagsWithIds = new HashMap<>();

        /* Iterate through the map */
        Iterator itMap = imagesTags.entrySet().iterator();
        int indice = 0; //indice to be affected to the tag
        while (itMap.hasNext()) {
            Map.Entry pairFromMap = (Map.Entry) itMap.next();

             /* Select the list of tags in the pair (ArrayList) */
            ArrayList tagList = (ArrayList) pairFromMap.getValue();

            /* Iterate through that list, adding tags only once to the tagpool and with unique indices */
            for (String s : (ArrayList<String>) tagList) {
                /* Include the tag if it does not already exist */
                    if(!tagsWithIds.containsValue(s)){
                        tagsWithIds.put(indice++,s);
                    }
            }
        }
        if(DistancesMT.PRINT == 1){
            System.out.println("Mapped tags to matrix indices from 0 to " + (indice-1) +".\n");
        }
        return tagsWithIds;
    }

   /**
    * Computes the cosine similarity matrix using multithreading
    * @param cooccurences cooccurrence matrix as Jama Matrix
    * @return similarity matrix as Jama Matrix or null if input matri is not square
    */
   private static Matrix getCosineSimilarityMatrix(Matrix cooccurences){
       
       if(cooccurences.getColumnDimension() != cooccurences.getRowDimension()){
           System.out.println("Cooccurrence matrix must be square");
           return null;
       }
       int size = cooccurences.getRowDimension();
       Matrix similarities = new Matrix(size,size);
       
        if(DistancesMT.PRINT == 1){
            System.out.println("Starting multithreaded cosine similarities calculation");
        }
       
        //number of tasks is (n^2 + n)/2, round up to next integer
        int tasksAmount = (size+1)*(size+1)/2;
        int poolSize = tasksAmount;
        
       /* Multithreading */
       ImageSignatureThreadPoolExecutor executor =  Multithreading.initializeQueueAndGetExecutor(poolSize);
       
       for(int i = 0 ; i < size ; i++){
           for(int j = i; j < size ; j++){
               
               Matrix v1 = cooccurences.getMatrix(i, i, 0, size-1);
               Matrix v2 = cooccurences.getMatrix(j, j, 0, size-1);
               executor.execute(new CosineSimilarityCalculatorThread(v1, v2, i, j, similarities));  
           }
       }
       
       Multithreading.waitForExecutionEnd();
       if(DistancesMT.PRINT == 1){
            System.out.println("Multithreaded cosine similarities calculation done!");
       }
       /* Multithreading ends */
       return similarities;
   }
   
  /**
   * Computes the diagonal matrix for the Laplacian score
   * i-th diagonal term is defined as the sum of all elements of the i-th vector in the cosine similarity matrix
   * @param cosineSimilarityMatrix cosine similarity matrix (must be square, as Jama Matrix)
   * @return diagonal matrix as Jama Matrix or square matrix with zero values if the input matrix is not square
   */
   private static Matrix getDiagonalMatrix(Matrix cosineSimilarityMatrix){
       
       int size = cosineSimilarityMatrix.getRowDimension();
       Matrix diagonal = new Matrix(size, size,0.0);
       
       if(cosineSimilarityMatrix.getColumnDimension() != cosineSimilarityMatrix.getRowDimension()){
           System.out.println("Cosine similarity matrix must be square");
       }else{
           for(int i = 0 ; i < size  ; i++){
           double d = 0;
           for(int j = 0 ; j < size; j++){
               d += cosineSimilarityMatrix.get(i, j);
           }
           diagonal.set(i, i, d);
           }
       }       
       return diagonal;
   }
   
   /**
    * Computes Laplacian scores for all tags using multithreading
    * @param cooccurrences cooccurrence matrix
    * @param diagonal diagonal matrix as defined in LS formula
    * @param laplacian Laplacian matrix
    * @param tagsWithIds map containing the tags and their indices in the matrixes
    * @return scores for all input tags as map<string tag, double score>
    */
   private static Map<String,Double> getLaplacianScores(Matrix cooccurrences, Matrix diagonal, Matrix laplacian, Map<Integer,String> tagsWithIds){
       
       if(cooccurrences.getColumnDimension() != cooccurrences.getRowDimension()){
           System.out.println("Cooccurrence matrix must be square");
           return null;
       }
       
       int size = cooccurrences.getRowDimension();
       
       if(size != diagonal.getColumnDimension() || size != diagonal.getRowDimension() ||
          size != laplacian.getColumnDimension() || size != laplacian.getRowDimension()){
           System.out.println("Matrix and  vector sizes must agree to compute Laplacian score");
           return null;
       }
       
       if(DistancesMT.PRINT == 1){
           System.out.println("Starting multithreaded Laplacian scores calculation");
       }
    
        /* Multithreading */
       ImageSignatureThreadPoolExecutor executor =  Multithreading.initializeQueueAndGetExecutor(size);
        
       Matrix ones = new Matrix(size,1,1); //ones column vector
       Matrix onesT = ones.transpose();
       
       Map<String,Double> tagsWithLS = new HashMap<>();
       
       for(Integer i : tagsWithIds.keySet()){
           Matrix feature = cooccurrences.getMatrix(0, size-1, i, i); //feature vector 
           executor.execute(new LaplacianScoreCalculatorThread(tagsWithIds.get(i), feature, laplacian, diagonal, ones, onesT, tagsWithLS));    
       }
       Multithreading.waitForExecutionEnd();
       
       if(DistancesMT.PRINT == 1){
           System.out.println("Multithreaded Laplacian scores calculation done!");
       }
       /* Multithreading ends */
       return tagsWithLS;
       
   }
   
    /**
     * Sorts a map with key => double in descending order (switch o2 and o1 for ascending, if needed)
     * @param sort Map that is to be sorted
     * @return Map<String, Double>
     */
    private static Map<String, Double> sortMapDescendingDouble(Map<String, Double> sort) {

        /* Make a list out of map */
        List<Map.Entry<String, Double>> list = new LinkedList<>(sort.entrySet());

        /* Sort it with a comparator */
        Collections.sort(list, new Comparator<Map.Entry<String, Double>>() {
            public int compare(Map.Entry<String, Double> o1,
                               Map.Entry<String, Double> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        /* Convert it to a map again */
        Map<String, Double> sorted = new LinkedHashMap<>();

        for (Iterator<Map.Entry<String, Double>> it = list.iterator(); it.hasNext();) {
            Map.Entry<String, Double> entry = it.next();
            sorted.put(entry.getKey(), entry.getValue());
        }
        return sorted;
    }
    
    /**
     * Computes the cooccurrence matrix for all tags
     * @param tagsWithIds
     * @param imageTags
     * @return cooccurrence matrix as Jama Matrix
     */
    private static Matrix cooccurrenceMatrix(Map<Integer,String> tagsWithIds, Map imageTags){
       
       int mapSize = tagsWithIds.size();
       Matrix cooccurrences = new Matrix(mapSize,mapSize);
       
       for(int i = 0; i < tagsWithIds.size(); i++){
           for(int j = i ; j < tagsWithIds.size() ; j++){
               if((i == j)){
                   //cooccurrence of a term with itself is 0
                   cooccurrences.set(i, i, 0);
               }else{
                   int cooccurrence = DistancesMT.cooccurrenceBetweenTerms(tagsWithIds.get(i), tagsWithIds.get(j), imageTags);
                   //cooccurrence is symmetrical
                   cooccurrences.set(i, j, cooccurrence);
                   cooccurrences.set(j, i, cooccurrence);
               }
           }
       }
       return cooccurrences;
   }
    
     public static ArrayList selectBestLSTags(int percentage, Map tagsWithLS){
        
        ArrayList representativeTags = new ArrayList<>();
        
        /* Check the percentage, if correct, proceed */
        if (percentage > 100 || percentage < 0) {
            System.out.println("The given percentage has to be between 0 and 100.");
        }
        else {
            /* Calculate number of tags, at least MIN_REFERENCE_TAG_COUNT, otherwise */
            double percentageCount = tagsWithLS.size() * percentage * 0.01;
            double referenceCount = percentageCount;
        
            /* Go through the map and find the number(percentageCount) highest-rated tags, add them to the list */
            Iterator iteratorMin = tagsWithLS.entrySet().iterator();
            while (iteratorMin.hasNext() && representativeTags.size() < referenceCount) {
                Map.Entry pair = (Map.Entry) iteratorMin.next();
                representativeTags.add(pair.getKey());
            }
            
            if(DistancesMT.PRINT == 1){
                printLSScoresForRepresentativeTags(tagsWithLS,referenceCount);
            }
        
        }
        
        return representativeTags;
    }
    
}
