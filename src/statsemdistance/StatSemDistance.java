package statsemdistance;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        String filename = "aroundBerlin_30";
        int kNearestNeighbors = 5;
        
        Map imagesTags = DistancesMT.imageTagsFromFile(filename + ".csv");
        DistancesMT.printMap(imagesTags);
        
        if (DistancesMT.PRINT == 1)
            System.out.println("ImageTags size : " + imagesTags.size());

        Map<Integer,Double> scores = new TreeMap<>();
        
        for(int tagPercentage = 1; tagPercentage < 50; tagPercentage++){
            
             /* Get representative tags */
            //ArrayList representativeTags = DistancesMT.getMostFrequentTags(imagesTags, tagPercentage);
            //ArrayList representativeTags = DistancesMT.getMostFrequentTagsWithShifting(imagesTags, tagPercentage, 10);
            ArrayList representativeTags = LaplacianScore.getRepresentativeTagsByLaplacianScore(imagesTags, tagPercentage);
            System.out.println("Most representative Tags of input: " + representativeTags);

            Map<Integer,String> tagsWithIds = LaplacianScore.allTagsWithIds(imagesTags);
            //System.out.println("Found " + tagsWithIds.size() + " unique tags");

            /* Statistical Distance Matrix */
            //DistancesMT.printDistanceMatrix(DistancesMT.calculcateDistanceMatrix(imagesTags, representativeTags));
            double[][] statisticalDistances = DistancesMT.calculcateDistanceMatrix(tagsWithIds, imagesTags, representativeTags, filename);

            /* Semantic distance matrix */
            List<Integer> commonTags = new ArrayList<>();
            double[][] semanticDistances = DistancesMT.getSemanticDistancesFromFile(filename + "_semDistances.csv", tagsWithIds, commonTags);

            //double averageJaccardDistance = DistancesMT.averageJaccardDistance(kNearestNeighbors, statisticalDistances, semanticDistances, commonTags);
            double sumOfListsDifferenceDistances = DistancesMT.sumOfKNearestTermsDistances(kNearestNeighbors, statisticalDistances, semanticDistances, commonTags, tagsWithIds);
            //System.out.println("Average Jaccard distance : " + averageJaccardDistance + " (" + commonTags.size() + " tags)");
            //System.out.println("Normalized distance between semantic and statistical methods : " + sumOfListsDifferenceDistances + " (" + commonTags.size() + " tags)");

            scores.put(tagPercentage, sumOfListsDifferenceDistances);
            //DistancesMT.writeDistanceMatrixIntoFile(semanticDistances, "semDistances.csv");
            //DistancesMT.writeDistanceMatrixIntoFile(statisticalDistances, "statDistances.csv");
        }
        
        System.out.println("Percentage of representative tags - Score");
        for(Integer percentage : scores.keySet()){
            System.out.println(percentage + " - " + scores.get(percentage));
        }
        
       
 
    }
    
}
