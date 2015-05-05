package statsemdistance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;


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
        int nearestNeighbors = 10;
        int shifting = 5;
        
        Map imagesTags = DistancesMT.imageTagsFromFile(filename + ".csv");
        DistancesMT.printMap(imagesTags);
        
        if (DistancesMT.PRINT == 1)
            System.out.println("ImageTags size : " + imagesTags.size());

        Map<Integer,Double> scores = new HashMap<>();
        
        /* Get representative tags - choose between one of the three selection methods*/
        
        //most frequent tags
        //Map representativeTags = DistancesMT.getMostFrequentTags(imagesTags);
        
        //most frequent tags with shifting
        //Map representativeTags = DistancesMT.getMostFrequentTagsWithShifting(imagesTags, shifting);
        
        //LS feature selection
        Map representativeTags = LaplacianScore.getTagsWithLaplacianScores(imagesTags);
        //DistancesMT.writeScoresIntoFile(representativeTags, filename + "_LS_scores.csv");
        
        
        for(int tagPercentage = 1; tagPercentage < 26; tagPercentage++){
            
            //ArrayList tagsSelection = DistancesMT.selectMostFrequentTags(tagPercentage, representativeTags); //for highest frequency selection
            ArrayList tagsSelection = LaplacianScore.selectBestLSTags(tagPercentage, representativeTags);  //for Laplacian score selection
                    
            //System.out.println("Most representative Tags of input: " + tagsSelection);
            System.out.println(tagsSelection.size());
            
            Map<Integer,String> tagsWithIds = LaplacianScore.allTagsWithIds(imagesTags);
            //System.out.println("Found " + tagsWithIds.size() + " unique tags");

            /* Statistical Distance Matrix */
            //DistancesMT.printDistanceMatrix(DistancesMT.calculcateDistanceMatrix(imagesTags, representativeTags));
            double[][] statisticalDistances = DistancesMT.calculcateDistanceMatrix(tagsWithIds, imagesTags, tagsSelection, filename);

            /* Semantic distance matrix */
            List<Integer> commonTags = new ArrayList<>();
            double[][] semanticDistances = DistancesMT.getSemanticDistancesFromFile(filename + "_semDistances.csv", tagsWithIds, commonTags);

            //double averageJaccardDistance = DistancesMT.averageJaccardDistance(kNearestNeighbors, statisticalDistances, semanticDistances, commonTags);
            double sumOfListsDifferenceDistances = DistancesMT.sumOfKNearestTermsDistances(nearestNeighbors, statisticalDistances, semanticDistances, commonTags, tagsWithIds);
            //System.out.println("Average Jaccard distance : " + averageJaccardDistance + " (" + commonTags.size() + " tags)");
            //System.out.println("Normalized distance between semantic and statistical methods : " + sumOfListsDifferenceDistances + " (" + commonTags.size() + " tags)");

            scores.put(tagPercentage, sumOfListsDifferenceDistances);
            
            //write statistical and semantic matrixes to files
            //DistancesMT.writeDistanceMatrixIntoFile(semanticDistances, tagsWithIds, "semDistances_"+ tagPercentage +"_.csv");
            //DistancesMT.writeDistanceMatrixIntoFile(statisticalDistances, tagsWithIds, "statDistances_"+ tagPercentage +".csv");
        }
        
        System.out.println("Percentage of representative tags - Score");
        for(Integer percentage : scores.keySet()){
            System.out.println(percentage  + " - " + scores.get(percentage));
        }
        
        //print scores
        DistancesMT.writeScoresIntoFileInt(scores, filename + "_distances_to_sem_matrix.csv");
        
       
 
    }
    
}
