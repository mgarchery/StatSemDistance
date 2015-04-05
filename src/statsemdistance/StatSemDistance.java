package statsemdistance;

import java.util.ArrayList;
import java.util.Map;
import static statsemdistance.Distances.REFERENCE_TAG_PERCENTAGE;
import static statsemdistance.Distances.calculcateDistanceMatrix;
import static statsemdistance.Distances.getRepresentativeTags;
import static statsemdistance.Distances.mapFromDatabase;
import statsemdistance.DistancesMT;

/**
 *
 * @author eegyedzsigmond
 */
public class StatSemDistance {

    public static void printDistanceMatrix(double[][] theMatrix) {

        int size = theMatrix.length;
        for (int j = 0; j < size; j++) {
            System.out.print(j + ", ");
            for (int i = 0; i < size; i++) {
                System.out.print(theMatrix[i][j] + ", ");

            }
            System.out.println("");
        }

    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
//        // TODO code application logic here
//        Map imagesTags = mapFromDatabase("jdbc:postgresql://localhost:5432/2014GettyClean", "postgres", "postgres");
//        if (Distances.PRINT==1) 
//            System.out.println("ImageTags size : "+imagesTags.size());
//        
//        /* Calculate representativeTags */
//        ArrayList representativeTags = getRepresentativeTags(imagesTags, REFERENCE_TAG_PERCENTAGE);
//        System.out.println(representativeTags);
//        /* 
//
//        // Testing histogramm function
//        ArrayList<Integer> histogramm1 = calculateCooccurrences("allemagne", representativeTags, imagesTags);
//        ArrayList<Integer> histogramm2 = calculateCooccurrences("france", representativeTags, imagesTags);
//        System.out.println(divergenceOfHistogrammes(histogramm1, histogramm2, representativeTags));
//        */
//
//        /* Distance Matrix */
//        printDistanceMatrix(calculcateDistanceMatrix(imagesTags, representativeTags));
        //Map imagesTags = DistancesMT.mapFromDatabase("jdbc:postgresql://localhost:5432/2014GettyClean", "postgres", "postgres");

        /* EXAMPLE
        *
        *  1. Read example.cvs in res/
        *  2. Create a Map "imagesTags" according to the specifications in Distances.java
        *  3. Compute Representative Tags
        *  4. Compute a distance matrix by utilising the representative tags
        *
        * */
        Map imagesTags = DistancesMT.imageTagsFromFile("aroundBerlin_30.csv");
        DistancesMT.printMap(imagesTags);
        
        if (DistancesMT.PRINT == 1)
            System.out.println("ImageTags size : " + imagesTags.size());

        /* Calculate representativeTags */
        //ArrayList representativeTags = DistancesMT.getRepresentativeTags(imagesTags, DistancesMT.REFERENCE_TAG_PERCENTAGE);
        ArrayList representativeTags = LaplacianScore.getRepresentativeTagsByLaplacianScore(imagesTags, DistancesMT.REFERENCE_TAG_PERCENTAGE);
        System.out.println("Most representative Tags of input: " + representativeTags);

        /* Distance Matrix */
        //DistancesMT.printDistanceMatrix(DistancesMT.calculcateDistanceMatrix(imagesTags, representativeTags));
        //DistancesMT.calculcateDistanceMatrix(imagesTags, representativeTags);
 
    }
    
}
