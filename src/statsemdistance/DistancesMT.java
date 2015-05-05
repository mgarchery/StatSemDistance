package statsemdistance;/* Database connection */
import java.io.*;
import java.sql.*;
import java.sql.DriverManager;
import treegenerator.services.Inflector;

/* Collections */
import java.util.*;

/**
 * Class "Distances" that uses the flickr database or a *.cvs file to deliver a distance matrix for existing tags based on their
 * cooccurrence in the images. 
 * @author Patrick Sebastian John von Freyend
 */
public class DistancesMT {
    /*
       Used to set minimum reference tag count and percentage
    */
    public static int MIN_REFERENCE_TAG_COUNT = 10;
    public static int REFERENCE_TAG_PERCENTAGE = 10;
    public static int PRINT = 0; // Set to 0 to not print any additional information, 1 to print the progress of the programm
    public static int TEST_POOL_SIZE_DIVISION = 1; // Reduce the size of the test set by setting a higher number; 1 indicating actual size
   
    public static Map<Integer, ArrayList> histogramms = new HashMap();
    
    /**
     * Establishes a database connection (to a postgres database) and saves all image ids and their corresponding tags to a map (string => string list).
     * Please add an appropriate postgresql-driver that corresponds to your version of the JVM to the build path.
     *
     * @param url      URL to the database
     * @param user     Username for the database
     * @param password Password for the database
     * @return void
     */
    public static Map mapFromDatabase(String url, String user, String password) {

        Connection c = null;
        Statement stmt = null;
        Map imagesTags = new HashMap();

        try {
            /* Connect to database */
            Class.forName("org.postgresql.Driver");
            c = DriverManager.getConnection(url, user, password);
            c.setAutoCommit(false);

            /* Successful! */
            if(PRINT == 1)
            System.out.println("Database connection successful. Creating map...\n");

            /* Read images (their ids) and their tags from the database and saves them to a map */
            stmt = c.createStatement();
            String theQuery = "SELECT \n" +
                    "  \"imagefiltred\".id, \n" +
                    "  \"imagetagfiltred\".tag \n" +
                    "FROM \n" +
                    "\"Tag\", \n" +
                    "\"imagefiltred\", \n" +
                    "\"imagetagfiltred\"\n" +
                    "WHERE \n" +
                    "  \"imagefiltred\".id = \"imagetagfiltred\".imageid AND\n" +
                    "  \"Tag\".text = \"imagetagfiltred\".tag AND\n" +
                    "  \"Tag\".gettytag = true AND\n" +
                    "  \"imagefiltred\".lat >= 50 AND \n" +
                    "  \"imagefiltred\".lat <= 55 AND \n" +
                    "  \"imagefiltred\".lon >= 5 AND \n" +
                    "  \"imagefiltred\".lon <= 15";
            ResultSet rs = stmt.executeQuery( theQuery );

            /* Write into the Map */
            while (rs.next()) {
                String id = rs.getString("id");
                String tag = rs.getString("tag");

                /* If the Map doesn't already contain the key */
                if (!imagesTags.containsKey(id)) {
                    /* add the key and create internal list */
                    imagesTags.put(id, new ArrayList());

                    /* add the first tag to the internal list */
                    List internal = (ArrayList) imagesTags.get(id);
                    internal.add(tag);
                } else {
                    /* Key already exists for the image; Search for tag in the internal list and add it */
                    List internal = (ArrayList) imagesTags.get(id);
                    if (!(internal.contains(tag))) {
                        internal.add(tag);
                    }
                }
            }

            if(PRINT == 1)
            System.out.println("The map was successfully created.\n");

            /* close reading */
            rs.close();
            stmt.close();

            /* close connection */
            c.close();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName() + " - " + e.getMessage());
            System.exit(0);
        }

        return imagesTags;

    }

    /**
     * Creates a map vom the information giving in a *.cvs file located in the /res folder (string => string list), using the first cell of a row as
     * identifier. If a file can't be read, make sure that you are using the right path.
     *
     * @param fileName      Input path for *.cvs file
     * @return Map
     */
    public static Map imageTagsFromFile(String fileName) {
        Map imagesTags = new HashMap();

        /* Set path to file */
        String filepath = System.getProperty("user.dir");
        filepath = filepath + "/files/" + fileName;

        BufferedReader br = null;
        String line;
        String split = ",";

        try {

            br = new BufferedReader(new FileReader(filepath));
            while ((line = br.readLine()) != null) {

                /* Create an array with the strings of every line */
                String[] words = line.split(split);

                for(int i = 0; i < words.length; i++) {
                    words[i] = words[i].replace(" \"", "").replace("\"", "");
                    //singularize tags to match later in semantic distance matrix
                    words[i] = Inflector.getInstance().singularize(words[i]);
                }
                
                

                /* add the key and create internal list */
                imagesTags.put(words[0], new ArrayList());

                /* Go through the internal list and add all the tags */
                List internal = (ArrayList) imagesTags.get(words[0]);

                for (int i = 1; i < words.length; i++) {
                    /* Key already exists for the image; Search for tag in the internal list and add it */
                    internal.add(words[i]);
                }
            }

        } catch (FileNotFoundException e) { e.printStackTrace();
        } catch (IOException e) { e.printStackTrace();
        } finally {

            if (br != null) {
                try { br.close();} catch (IOException e) { e.printStackTrace(); }
            }

        }

        System.out.println("Reading done");
        return imagesTags;
    }

    /**
     * Prints the contents of a map to the console.
     *
     * @param map   URL to the database
     */
    public static void printMap(Map map) {
        Iterator iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
        }
    }

    /**
     * Creates a tag pool (ArrayList with INT (int) => string) with all the different tags that exist in a map
     *
     * @param imagesTags   Map with all the images and their corresponding tags
     * @return void
     */
    public static ArrayList<String> createTagPool(Map imagesTags) {

        if(PRINT == 1)
        System.out.println("Creating tagpool...\n");

        ArrayList<String> tagpool = new ArrayList<String>();

        /* Iterate through the map */
        Iterator itMap = imagesTags.entrySet().iterator();

        while (itMap.hasNext()) {
            Map.Entry pairFromMap = (Map.Entry) itMap.next();

             /* Select the list of tags in the pair (ArrayList) */
            ArrayList tagList = (ArrayList) pairFromMap.getValue();

            /* Iterate through that list and add tags */
            for (String s : (ArrayList<String>) tagList) {
                /* If it doesn't already include the tag... */
                if (!(tagpool.contains(s))) {
                    tagpool.add(s);
                }
            }
        }

        if(PRINT == 1)
        System.out.println("Created tagpool.\n");

        return tagpool;
    }

    /**
     * Creates a tagpool and counts all the occurrences of each tag in imagesTags (map (string => string list))
     *
     * @param imagesTags Map with all the images and their corresponding tags
     * @return void
     */
    public static Map countOccurrences(Map imagesTags) {

        if(PRINT == 1)
        System.out.println("Creating tagpool and counting occurrences...\n");

        Map tagpool = new HashMap();

        /* Iterate through the map */
        Iterator itMap = imagesTags.entrySet().iterator();

        while (itMap.hasNext()) {
            Map.Entry pairFromMap = (Map.Entry) itMap.next();

             /* Select the list of tags in the pair (ArrayList) */
            ArrayList tagList = (ArrayList) pairFromMap.getValue();

            /* Iterate through that list and add to the tagpool with value 0 (counts) */
            for (String s : (ArrayList<String>) tagList) {
                /* If it doesn't already include the tag... */
                if (!(tagpool.containsKey(s))) {

                    tagpool.put(s, 0);

                    /* For every tag count through imagesTags, find each list and look for the search-tag, is it there: +1 in alltags */
                    Iterator countTags = imagesTags.entrySet().iterator();

                    while (countTags.hasNext()) {
                        Map.Entry countPair = (Map.Entry) countTags.next();

                        /* Find list in element */
                        ArrayList<String> arrayTags = (ArrayList<String>)countPair.getValue();

                        /* Is search in there? */
                        if(arrayTags.contains(s)) {
                            /* increment value by one in ALLTAGS */
                            tagpool.put(s, (Integer)tagpool.get(s) + 1);
                        }
                    }
                }
            }
        }

        if(PRINT == 1)
        System.out.println("Created tagpool, counting successful.\n");

        return tagpool;
    }

    /**
     * Sorts a map with key => integer in descending order (switch o2 and o1 for ascending, if needed)
     * @param sort Map that is to be sorted
     * @return Map<String, Integer>
     */
    private static Map<String, Integer> sortMapDescendingInteger(Map<String, Integer> sort) {

        /* Make a list out of map */
        List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(sort.entrySet());

        /* Sort it with a comparator */
        Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> o1,
                               Map.Entry<String, Integer> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        /* Convert it to a map again */
        Map<String, Integer> sorted = new LinkedHashMap<String, Integer>();

        for (Iterator<Map.Entry<String, Integer>> it = list.iterator(); it.hasNext();) {
            Map.Entry<String, Integer> entry = it.next();
            sorted.put(entry.getKey(), entry.getValue());
        }

        return sorted;
    }
    
    /**
     * Sorts a map with integer => double in descending order (switch o2 and o1 for ascending, if needed)
     * @param sort Map that is to be sorted
     * @return Map<String, double>
     */
    private static Map<Integer, Double> sortMapAscendingDouble(Map<Integer, Double> sort) {

        /* Make a list out of map */
        List<Map.Entry<Integer, Double>> list = new LinkedList<Map.Entry<Integer, Double>>(sort.entrySet());

        /* Sort it with a comparator */
        Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
            public int compare(Map.Entry<Integer, Double> o1,
                               Map.Entry<Integer, Double> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });

        /* Convert it to a map again */
        Map<Integer, Double> sorted = new LinkedHashMap<Integer, Double>();

        for (Iterator<Map.Entry<Integer, Double>> it = list.iterator(); it.hasNext();) {
            Map.Entry<Integer, Double> entry = it.next();
            sorted.put(entry.getKey(), entry.getValue());
        }

        return sorted;
    }

    /**
     * This functions returns, given a minimum reference count of at least MIN_REFERENCE_TAG_COUNT but of a percentage of REFERENCE_TAG_PERCENTAGE (can be set above)
     * if higher, the set of representative tags as an array list from a map with the imageid => tags (string => string list)
     * @param imagesTags Map with all the images and their corresponding tags
     * @param percentage Minimum percentage, to set in REFERENCE_TAG_PERCENTAGE
     * @return ArrayList
     */
    public static Map getMostFrequentTags(Map imagesTags) {

        ArrayList representativeTags = new ArrayList<String>();

        /*
            1. Create Map "alltags" tag => count (0)
            2. Count all occurrences and add to tag pool
            3. Choose "int count" REFERENCE_TAG_PERCENTAGE, minimum MIN_REFERENCE_TAG_COUNT
        */
        Map alltags = countOccurrences(imagesTags);

        if(PRINT == 1)
        System.out.println("Getting most representative tags...");

        /* Sort the tags in descending order */
        alltags = sortMapDescendingInteger(alltags);

        

        return alltags;
    }
    
     public static Map getMostFrequentTagsWithShifting(Map imagesTags, int shiftingPercentage) {

        Map representativeTags = new HashMap<String,Integer>();
        Map skippedFrequentTags = new HashMap<String,Integer>();

        /*
            1. Create Map "alltags" tag => count (0)
            2. Count all occurrences and add to tag pool
            3. Choose "int count" REFERENCE_TAG_PERCENTAGE, minimum MIN_REFERENCE_TAG_COUNT
        */
        Map alltags = countOccurrences(imagesTags);

        if(PRINT == 1)
            System.out.println("Getting most representative tags...");

        /* Sort the tags in descending order */
        alltags = sortMapDescendingInteger(alltags);

            if (shiftingPercentage > 100 || shiftingPercentage < 0){
                System.out.println("The given shifting percentage has to be between 0 and 100.");
            }else {
                
                double skippedTagsCount = shiftingPercentage*alltags.size()*0.01;
                
                /* Go through the map and find the number(percentageCount) highest-rated tags, add them to the list */
                Iterator iteratorMin = alltags.entrySet().iterator();
                
                while (iteratorMin.hasNext() && skippedFrequentTags.size() < skippedTagsCount) {
                    Map.Entry pair = (Map.Entry) iteratorMin.next();
                    skippedFrequentTags.put(pair.getKey(),pair.getValue());
                }
                while (iteratorMin.hasNext()) {
                    Map.Entry pair = (Map.Entry) iteratorMin.next();
                    representativeTags.put(pair.getKey(),pair.getValue());
                }

            }
        
        return representativeTags;
    }

    /**
     * Counts the coocurrence of term1 and term2 in imagesTags
     * @param term
     * @param term2
     * @param imagesTags Map with the images and their corresponding tags (string => string list)
     * @return int
     */
    public static int cooccurrenceBetweenTerms(String term, String term2, Map imagesTags) {
        /* Count of images that both term1 and term 2 are part of in maps */
        int cooccurrence = 0;

        Iterator iterator = imagesTags.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry pair = (Map.Entry) iterator.next();

            /* For every picture, take the list */
            ArrayList tagList = (ArrayList) pair.getValue();

            /* Increment value by one, when both are tags for the same picture */
            if(tagList.contains(term) && tagList.contains(term2)) cooccurrence++;
        }

        // if(PRINT == 1)
        // System.out.println(cooccurrence + " is the cooccurrence for terms \"" + term +"\" and \"" + term2 + "\".");

        return cooccurrence;
    }

    /**
     * Creates a histogramm in counting, how often a term is used in the same photo in imagesTags with each of the frequent terms
     * (For a histogramm, take both the array frequent terms and the array with the count)
     * @param term Term for which the histogramm is supposed to be calculated
     * @param representativeTags Most frequent terms as calculcated by (String values) @getRepresentativeTags
     * @param imagesTags Map with the images and their corresponding tags (string => string list)
     * @return ArrayList<Integer>
     */
    public static ArrayList<Integer> calculateCooccurrences(String term, ArrayList<String> representativeTags, Map imagesTags) {
        ArrayList<Integer> cooccurrences = new ArrayList();

        /* Go through frequentTerms and get every single term */
        for (String term2 : representativeTags) {
            cooccurrences.add(cooccurrenceBetweenTerms(term, term2, imagesTags));
        }

        /* Print the corresponding counts and terms */
        if(PRINT == 1) {

            System.out.println("The term \"" + term + "\" is used simultaneously as follows:");
            for (int i = 0; i < representativeTags.size(); i++) {
                System.out.println(cooccurrences.get(i) + " times with the term \"" + representativeTags.get(i) + "\"");
            }

        }
        return cooccurrences;
    }

    /**
     * Calculates the divergence of two histogramms created by calculateCooccurences according to the Jenson-Shanon Divergence.
     * @param histogramm1
     * @param histogramm2
     * @return double
     */
    public static double divergenceOfHistogrammes(ArrayList<Integer> histogramm1, ArrayList<Integer> histogramm2) {
        double divergence = 0;

        if(histogramm1.size() != histogramm2.size()) {

          System.out.println("The lists do not have the same amount of entries. Are you sure those are histogramms for the same frequent terms?");

        } else {

            double[] hist1 = new double[histogramm1.size()];
            double[] hist2 = new double[histogramm2.size()];

            for (int i = 0; i < histogramm1.size(); i++) {
                hist1[i] = histogramm1.get(i).doubleValue();
                hist2[i] = histogramm2.get(i).doubleValue();
            }

            divergence = JensonShanonDivergence.jensenShannonDivergence(hist1, hist2);
        }

        return divergence;
    }

    /**
     * Calculates a distance matrix for each tag in imagesTags and its representative tags (with the help of the Janson-Shanon-Divergence).
     * It then writes this matrix in a *.csv-file located in the main folder.
     * @param tagsWithIds
     * @param imagesTags
     * @param representativeTags
     * @return double[][]
     */
    public static double[][] calculcateDistanceMatrix(Map<Integer,String> tagsWithIds, Map imagesTags, ArrayList<String> representativeTags, String filename) {
        
        int tagsCount = tagsWithIds.size();
        double[][] distanceMatrix = new double[tagsCount][tagsCount];
        
        /* Multithreading */
        ImageSignatureThreadPoolExecutor executor = Multithreading.initializeQueueAndGetExecutor(tagsCount);

        if(PRINT == 1)
            System.out.println("Starting multithreaded histogramms calculation...");

        /* Calculate a map with the tags and their corresponding histogramm, see global variable histogramms */
        for (Integer i : tagsWithIds.keySet()) {
            executor.execute(new CoocurrenceHistogramm(i, tagsWithIds.get(i), imagesTags, representativeTags));
        }
        Multithreading.waitForExecutionEnd();
        
        if(PRINT == 1)
            System.out.println("Multithreaded histogramms calculation done!");
        /* Multithreading ends */
        
        //get max for normalization
        double max = 0.0;
        
        /* Go through those histogramms and calculate Jenson-Shanon-Divergence for each pair */
        for (int i = 0; i < tagsCount; i++) {
            for (int j = i ; j < tagsCount; j++) {
                try {
                    /* distance of a tag with itself is always 0*/
                    if(i == j){
                        distanceMatrix[i][i] = 0.0;
                    }else{
                        double distance = divergenceOfHistogrammes(histogramms.get(i), histogramms.get(j));
                        distanceMatrix[i][j] = distance;
                        distanceMatrix[j][i] = distance;
                        if(distance > max){
                            max  = distance;
                        }
                    } 
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println("i:" + i + "; i2:" + j + "; amountTags/TEST_POOL_SIZE_DIVISION :" + tagsCount);
                    System.exit(-1); 
                }

            }
        }

        /*normalize and correct matrix (distance between cannot be 0 if tags are not equal! */
        for(int i = 0; i < tagsCount; i++){
            for(int j = i; j < tagsCount; j++){
                
                //correct matrix
                if(distanceMatrix[i][j] == 0.0 && i!=j){
                    distanceMatrix[i][j] = max;
                    distanceMatrix[j][i] = max;
                }
                
                //normalize
                distanceMatrix[i][j] /= max;
                distanceMatrix[j][i] /= max;   
            }          
        }
        
        System.out.println("");
        //writeDistanceMatrixIntoFile(distanceMatrix, tagsWithIds, filename + "_statDistances.csv");

        return distanceMatrix;

    }

    /**
     * Writes a distance matrix calculated by calculcateDistanceMatrix into a file named "distances.cvs" in the *.cvs-format.
     * @param theMatrix A matrix returned by calculcateDistanceMatrix
     * @return int
     */
    public static int writeDistanceMatrixIntoFile(double[][] theMatrix, Map<Integer,String> tagsWithIds, String filename) {

        try {

            /* Find source */
            String filepath = System.getProperty("user.dir");
            filepath = filepath + "/files/";

            /* Create file and empty if needed */
            PrintWriter writer = new PrintWriter(filepath + filename);
            writer.print(" ;");
            
                int size = theMatrix.length;
                //write header
                for (int i = 0; i < size; i++) {
                    writer.append(tagsWithIds.get(i)+ "; ");
                }
                writer.append("\n");
                
                //write distances
                for (int i = 0; i < size; i++) {
                    writer.append(tagsWithIds.get(i)+ "; ");
                    
                    for (int j = 0; j < size; j++) {
                        writer.append(String.format( "%.6f", theMatrix[i][j]) + "; ");

                    }
                    writer.append("\n");
                }
                
                writer.close();

        } catch(IOException e) {
            e.printStackTrace();
            return -1;
        } 
        
        return 1;
    }

    /**
     * Prints a distance matrix calculated by the calculate distance calculcateDistanceMatrix function.
     * @param theMatrix A matrix returned by calculcateDistanceMatrix
     */
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
     * reads a semantic distance matrix from csv file
     * @param filename path of matrix file
     * @param tagsWithIds tags indexes 
     * @param commonTags empty list expected as input, common tags between file and already known tags as output
     * @return semantic distance matrix
     */
    public static double[][] getSemanticDistancesFromFile(String filename, Map<Integer,String> tagsWithIds, List<Integer> commonTags){

        String filepath = System.getProperty("user.dir");
        filepath = filepath + "/files/" + filename;
        
        BufferedReader br = null;
        String line;
        String split = ",";
        String[] header = null;
        int semanticDistancesCount = 0;
        
        double[][] semanticDistances = new double [tagsWithIds.size()][tagsWithIds.size()];
        Map<String,Integer> tagsToIndices = new HashMap<>();
        
        for(Integer i : tagsWithIds.keySet()){
            tagsToIndices.put(tagsWithIds.get(i), i);
        }
        
        
        try {

            br = new BufferedReader(new FileReader(filepath));
            List<String> ignoredTags = new ArrayList<>();
            if((line = br.readLine()) != null){
                header = line.split(split);
                
                for(String tag : header){
                    tag = tag.trim();
                    if(!tagsWithIds.containsValue(tag)){
                       ignoredTags.add(tag); 
                    }else{
                        commonTags.add(tagsToIndices.get(tag));
                    }
                }
                
                System.out.println("Found " + commonTags.size() + " common tags between statistical and semantic distance matrixes");
                
                //remove empty strings from ignored tags list
                while(ignoredTags.contains("")){
                    ignoredTags.remove("");
                }
                
                //remaining tags in list are unknown (i.e. do not appear in statistical distance matrix) and should be ignored 
                if(!ignoredTags.isEmpty()){
                    System.out.println("Found unknown tags in semantic distance matrix file (will be ignored) :");
                    for(String ignored : ignoredTags){
                        System.out.println(ignored);
                    }
                }
                    
            }
            
            while ((line = br.readLine()) != null) {

                /* Read semantic distance matrix from file, convert indices to those used in statistical distance matrix */
                String[] words = line.split(split);
                String iTag = words[0];
                
                
                if(tagsToIndices.containsKey(iTag)){
                    int iStat = tagsToIndices.get(iTag);
                    
                    for(int jSem = 1; jSem < words.length; jSem++){
                        String jTag = header[jSem].trim();
                        if(!jTag.isEmpty() && tagsToIndices.containsKey(jTag)){
                            int jStat = tagsToIndices.get(jTag);
                            if(jStat == iStat){
                                semanticDistances[iStat][jStat] = 0.0;
                            }else{
                                double similarity = Double.parseDouble(words[jSem]);
                                if(similarity == -1.0)
                                    semanticDistances[iStat][jStat] = 1.0;
                                else
                                    semanticDistances[iStat][jStat] = 1.0 - similarity;
                                semanticDistancesCount++;
                            }
                        }else{
                            if(PRINT == 1){
                                //System.out.println("ignored distance in semantic distance matrix between: " + iTag + " and " + header[jSem]);
                        
                            }
                        }
                    }
                }else{
                    if (PRINT == 1){
                        //System.out.println("ignored row in semantic distance matrix between: " + iTag );
                    }
                }
            }

        } catch (FileNotFoundException e) { e.printStackTrace();
        } catch (IOException e) { e.printStackTrace();
        } finally {

            if (br != null) {
                try { br.close();} catch (IOException e) { e.printStackTrace(); }
            }

        }

        System.out.println("Reading done (" + semanticDistancesCount + " semantic distances read)");
        return semanticDistances;
    }
    
    /**
     * returns a list containing the k nearest terms for a given reference term according to given distance matrix
     * @param k number of nearest neighbors to search for
     * @param i index of reference tag
     * @param distances distance matrix
     * @return a list containing k nearest neighbors (in right order)
     */
    private static List getKNearestTerms(int k , int i, double[][] distances){
 
        List<Integer> nearestTerms = new ArrayList<>();
        Map<Integer,Double> scores = new HashMap<>();
        
        for(int j = 0; j < distances.length; j++){

            if(i != j){ //exclude self from nearest neighbors
                
                if(i < j){
                    scores.put(j, distances[i][j]);
                }else{
                    scores.put(j, distances[j][i]);
                }
            }
 
        }
        
        scores = sortMapAscendingDouble(scores);
        List<Map.Entry<Integer,Double>> scoresList = new ArrayList(scores.entrySet());
        
        //sort in ascending distance order (nearest neighbors)
        Collections.sort(scoresList, (Map.Entry<Integer,Double>  entry1, Map.Entry<Integer,Double>  entry2) -> entry1.getValue().compareTo(entry2.getValue()));
        
        Iterator it = scoresList.iterator();
        while (it.hasNext() && nearestTerms.size() < k) {
            Map.Entry pair = (Map.Entry) it.next();
            nearestTerms.add((Integer)pair.getKey());
        }
        
        return nearestTerms;
        
    }
    
    /**
     * computes the average Jaccard distance between semantic and statistical distances
     * @param k number of nearest neighbors
     * @param statDistances statistical distance matrix
     * @param semDistances semantic distance matrix
     * @param commonTags list containing tags that appear in both distance matrixes
     * @return average Jaccard distance (between 0 and 1)
     */
    public static double averageJaccardDistance(int k , double[][] statDistances, double[][] semDistances, List<Integer> commonTags, Map<Integer,String> tagsWithIds){
        
        double averageJaccardDistance = 0.0;
        
        for(Integer index : commonTags){
          
            List<Integer> semanticNearest = getKNearestTerms(k, index, semDistances);
            List<Integer> statisticalNearest = getKNearestTerms(k, index, statDistances);
            
            Set<Integer> intersection = new HashSet<>();
            intersection.addAll(semanticNearest);
            intersection.retainAll(statisticalNearest);
            
            Set<Integer> union = new HashSet<>();
            union.addAll(semanticNearest);
            union.addAll(statisticalNearest);
            
            double jaccardDistance = 1.0 - (double)intersection.size()/(double)union.size();
            averageJaccardDistance += jaccardDistance;
        }
        
        averageJaccardDistance /= commonTags.size();
        return averageJaccardDistance;
        
    }
    
    /**
     * computes a distance between a statistical and a semantic distance matrix:
     * for each term, find k nearest neighbors according to both distances
     * compare both nearest neighbors lists using orderedListsDifferenceDistance
     * compute normalized average distance (between 0 and 1)
     * @param k number of nearest neighbors to consider
     * @param statDistances statistical distance matrix
     * @param semDistances semantic distance matrix
     * @param commonTags commonTags list containing tags that appear in both distance matrixes
     * @param tagsWithIds map with index->tag
     * @return normalized distance between statistical and semantic distance
     */
    public static double sumOfKNearestTermsDistances(int k, double[][] statDistances, double[][] semDistances, List<Integer> commonTags, Map<Integer,String> tagsWithIds){
        
        double result = 0;
        
        for(Integer index : commonTags){
          
            List<Integer> semanticNearest = getKNearestTerms(k, index, semDistances);
            List<Integer> statisticalNearest = getKNearestTerms(k, index, statDistances);
            
            long dist = orderedListsDifferenceDistance(semanticNearest, statisticalNearest);
            result += dist;
            
            if(PRINT == 1){
                System.out.println("Tag : " + tagsWithIds.get(index) + " (" + index + ")");
                System.out.println("semantic nearest: " );
                for(Integer i : semanticNearest){
                    System.out.print(tagsWithIds.get(i) + " ");
                }
                System.out.println();
                
                System.out.println("statistical nearest: ");
                for(Integer i : statisticalNearest){
                    System.out.print(tagsWithIds.get(i) + " ");
                }
                System.out.println();
                System.out.println("distance is " + dist);
            }
        }
        
        //normalize distance, upper bound is 2 * n * k^2
        result /= 2*commonTags.size()* k*k;
        return result;
        
    }
    
    /**
     * computes a distance between two ordered lists of same size, defined as following:
     * number the elements in each list
     * compute the sum of distances:
     * for each element appearing in at least one of the two lists
     *      if the element appears in both lists, distance = difference between index of element in list1 and index in list2
     *      else distance = size of the lists
     * 
     * lower bound is 0, upper bound is 2* n^2 where n is the size of the lists
     * 
     * @param <E> type of the lists
     * @param l1 first list
     * @param l2 second list, should have the same size as l1
     * @return distance between 0 and 2 * n^2, where n is the size of the lists
     */
    private static <E> int orderedListsDifferenceDistance (List<E> l1, List<E> l2) {
        
        final int n = l1.size();
        int distance = 0;
        
        if(n != l2.size()){
            System.out.println("Error while trying to compute distance between ordered lists : both lists should have the same size");
            return Integer.MAX_VALUE;
        }
        
        for(E element : l1){
            if(l2.contains(element)){
                distance += Math.abs(l1.indexOf(element) - l2.indexOf(element));
            }else{
                distance += n;
            }
        }
        
        for(E element : l2){
            if(! l1.contains(element)){
                distance += n;
            }
        }
        return distance;
    }
    
    public static ArrayList selectMostFrequentTags(int percentage, Map alltagsSorted){
        
        ArrayList<String> representativeTagsSelection  = new ArrayList<>();
         /* Check the percentage, if correct, proceed */
        if (percentage > 100 || percentage < 0) System.out.println("The given percentage has to be between 0 and 100.");
        else {
            /* Calculate number of tags, at least MIN_REFERENCE_TAG_COUNT, otherwise */
            double percentageCount = alltagsSorted.size() * percentage * 0.01;
            
            /* Go through the map and find the number(percentageCount) highest-rated tags, add them to the list */
            Iterator iteratorMin = alltagsSorted.entrySet().iterator();
            while (iteratorMin.hasNext() && representativeTagsSelection.size() < percentageCount) {
                Map.Entry pair = (Map.Entry) iteratorMin.next();
                representativeTagsSelection.add((String)pair.getKey());
            }
            
        }
        
        return  representativeTagsSelection;
    }
    
   
   
   public static int writeScoresIntoFile(Map<String,Double> itemsWithScores, String filename) {

        try {

            /* Find source */
            String filepath = System.getProperty("user.dir");
            filepath = filepath + "/files/";

            /* Create file and empty if needed */
            PrintWriter writer = new PrintWriter(filepath + filename);
            
                
                //write distances
                for (String tag : itemsWithScores.keySet()) {
                    writer.append(tag + "; " + String.format( "%.6f", itemsWithScores.get(tag))+ "; ");
                    
                    writer.append("\n");
                }
                
                writer.close();

        } catch(IOException e) {
            e.printStackTrace();
            return -1;
        } 
        
        return 1;
    }
   
   public static int writeScoresIntoFileInt(Map<Integer,Double> itemsWithScores, String filename) {

        try {

            /* Find source */
            String filepath = System.getProperty("user.dir");
            filepath = filepath + "/files/";

            /* Create file and empty if needed */
            PrintWriter writer = new PrintWriter(filepath + filename);
            
                
                //write distances
                for (Integer tag : itemsWithScores.keySet()) {
                    writer.append(tag + "; " + String.format( "%.6f", itemsWithScores.get(tag))+ "; ");
                    
                    writer.append("\n");
                }
                
                writer.close();

        } catch(IOException e) {
            e.printStackTrace();
            return -1;
        } 
        
        return 1;
    }
  
   
   
   
}