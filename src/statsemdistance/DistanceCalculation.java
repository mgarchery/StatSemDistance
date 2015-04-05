package statsemdistance;

import java.util.concurrent.BlockingQueue;

/*
* receives (i, j, coccurrenceMatrix)
* */
public class DistanceCalculation extends Thread {

    private String name = null;
 
    public DistanceCalculation(String name) {
        this.name = name;
    }
     
    @Override
    public void run() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        double somme=1;
//        for (int i=0;i<10000;i++){somme=somme*;};
        System.out.println("Executing : " + name);
        
    }
}