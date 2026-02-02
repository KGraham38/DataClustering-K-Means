//Kody Graham
//01/28/2026
//Phase 2
//For: Dr. Emre Celebi's Data Clustering Online Class - 4372

//Coding practices resource I have decided to keep primarily using: https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class KMeans {
    public static void main(String[] args) {

        //Parse / Validate our required arguments into an object of the class Parameter
        Parameters parameters = parseUserArguments(args);

        //Read our data set file
        Dataset dataset = readFromDataset(parameters.filename);

        //Generate my k random indexes, all unique
        int[] randomIndexes= generateKRandomIndexes(dataset.numberOfPoints,parameters.numOfClusters, new Random());

        /*
        Not for phase 2 but ill leave it for testing

        //Print the centers to console
        //printTheCenters(dataset.data, randomIndexes);

        //Also print to my output files
        //Had to build my file name outside of main since filename is static here
        String outputFilename = makeOutfileName(parameters.filename);
        saveCenterOutputsToOutputFiles(dataset.data, randomIndexes,outputFilename);
         */
    }

    //K means setup section: Absolutely no pow like you made clear in your video and set up my steps just like phase 0 Algorithm 7.1 Basic K-means algorithm
    //Just going to go ahead and redo my selected points instead of looping in main so i can encapsulate all my K means functionality.

    //Start: K Means

    //Euclidean Distance Squared
    private static double squaredEuclideanDistance(double[] point1, double[] point2) {
        double sum = 0.0;
        int i = 0;
        for (i = 0; i < point1.length; i++) {
            double diff = point1[i] - point2[i];
            sum += diff * diff;
        }
        return sum;
    }
    //Step 1: select K points as initial centroids rand
    private static double[][] initialCentroids(Dataset dataset, int numberOfClusters, Random random) {
        int[] centerIndexes = generateKRandomIndexes(dataset.numberOfPoints, numberOfClusters, random);

        double[][] centroids = new double[numberOfClusters][dataset.numOfDimensions];

        int i = 0;
        for (i = 0; i < centerIndexes.length; i++) {
            System.arraycopy(dataset.data[centerIndexes[i]], 0, centroids[i], 0, dataset.numOfDimensions);
        }
        return centroids;
    }

    //step 2: repeat
    //Step 3: Form K clusters by assigning each point to its closest centroid
    //I know the method name is long but it is a key method so I want its function very clear
    private static int[] assignPointsToClosestCentroid(Dataset dataset, double[][] centroids) {
        int[] assignedPoints = new int[dataset.numberOfPoints];

        int i = 0;
        for (i = 0; i < dataset.numberOfPoints; i++) {
            double bestDistance = Double.MAX_VALUE;
            int bestCenter = 0;

            int j = 0;
            for (j = 0; j < dataset.numOfDimensions; j++) {
                double distance = squaredEuclideanDistance(centroids[i], centroids[j]);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestCenter = j;
                }
            }
            assignedPoints[i] = bestCenter;
        }

        return assignedPoints;
    }

    //Step 4: Recompute the centroid of each cluster
    private static double[][] recomputeCentroids(Dataset dataset, int[] assignedPoints, int numClusters) {

        int dimensions = dataset.numOfDimensions;

        double[][] newCentroids = new double[numClusters][dimensions];
        int[] pointsPerCluster = new int[numClusters];

        int i = 0;
        for (i = 0; i < dataset.numberOfPoints; i++) {
            int center = assignedPoints[i];
            pointsPerCluster[center]++;

            double[] point = dataset.data[i];
            int j = 0;
            for (j = 0; j < pointsPerCluster.length; j++) {
                newCentroids[i][j] += point[j];
            }
        }
        int cent = 0;
        for ( cent = 0; cent < numClusters; cent++) {
            if (pointsPerCluster[cent]==0){
                continue;
            }

            int dim_index =0;
            for (dim_index = 0; dim_index < dimensions; dim_index++) {
                newCentroids[cent][dim_index] /= pointsPerCluster[cent];

            }
        }
        return newCentroids;
    }

    //step 5: until Centroids do not change
    private static double computeSSE(Dataset dataset, double[][] centers, int[] assignedPoints) {
        double sse = 0.0;

        int i = 0;
        for (i=0; i < dataset.numberOfPoints; i++) {
            int cent = assignedPoints[i];
            sse += squaredEuclideanDistance(dataset.data[i], centers[cent]);
        }
        return sse;
    }

    //Additional step: going to need to check for flatline in improvements
    private static boolean lineHasFlattened(double lastSSE, double curSSE, double threshold){
        if (lastSSE == Double.MAX_VALUE){
            return false;
        }
        double improveCheck = (lastSSE - curSSE)/lastSSE;
        boolean hasImproved = improveCheck > threshold;

        return hasImproved;
    }

    //Save the results
    private static final class RunResults{
        int runNumber;
        int iterations;
        double finalSSE;
        double[][] finalCents;

        private RunResults(int runNumber, int iterations, double finalSSE, double[][] finalCents) {
            this.runNumber = runNumber;
            this.iterations = iterations;
            this.finalSSE = finalSSE;
            this.finalCents = finalCents;
        }
    }

    //Run a full sequence of my k mean steps till convergence
    private static RunResults runKMeans(Dataset dataset, Parameters params, Random rand, PrintStream fileOut, int runNum){

        //Print header in both console and my file
        System.out.println("Run #: "+runNum);
        System.out.println("-----------");

        if (fileOut != null){
            fileOut.println("Run #: "+runNum);
            fileOut.println("-----------");
        }

        //Now call each of my steps
        //Step 1
        double[][] centroids = initialCentroids(dataset, params.numOfClusters, rand);
        double lastSSE = Double.MAX_VALUE;
        double curSSE = Double.MAX_VALUE;

        int iterationsDone = 0;

        //Step 2
        int indexNumClus=0;
        for (indexNumClus = 0; indexNumClus < params.numOfClusters; indexNumClus++) {

            //step 3
            int[] assignPoints = assignPointsToClosestCentroid(dataset, centroids);

            //step 4
            double[][] newCentroids = recomputeCentroids(dataset, assignPoints, params.numOfClusters);

            //SSE
            curSSE = computeSSE(dataset, newCentroids, assignPoints);

            //Output
            System.out.println("Iteration " + indexNumClus + " : SSE = " + curSSE);
            if (fileOut != null){
                fileOut.println("Iteration " + indexNumClus + " : SSE = " + curSSE);
            }
            iterationsDone = indexNumClus;

            //Step 5
            if(lineHasFlattened(lastSSE,curSSE,params.convergenceThreshold)){
                centroids = newCentroids;
                break;
            }
            lastSSE = curSSE;
            centroids = newCentroids;
        }


    }

    //Anymore helpers for k means will go here

    //End: K Means

    //Just the helper method for my output file name
    private static String makeOutfileName(String filename) {

        String baseName= new File(filename).getName();

        //Remove the extension
        int endOfFilename = baseName.lastIndexOf('.');
        if  (endOfFilename > 0) {
            baseName = baseName.substring(0, endOfFilename);
        }

        return baseName + "_output.txt";
        }

    //Write the centers we selected to an output file
    private static void saveCenterOutputsToOutputFiles(double[][] data, int[] randomIndexes, String outputFilename) {

        try (PrintStream fileOut = new PrintStream(new FileOutputStream(outputFilename, true))) {
            int index =0;

            //Loop for each center selected in my randomIndexes
            for (index =0; index< randomIndexes.length; index++) {
                int count = randomIndexes[index];
                int j = 0;

                //Loop for each dimension in selected data point
                for(j=0; j < data[count].length; j++) {
                    //Just add a space between the values printed
                    if (j>0){
                        fileOut.print(" ");
                    }
                    fileOut.print(data[count][j]);

                }
                fileOut.println();
            }
            fileOut.println();

        } catch (IOException e) {
            System.err.println("Error writing to the output file: " + outputFilename);
            System.exit(1);
        }
    }

    //Just the method to print the centers we selected to the console
    private static void printTheCenters(double[][] data, int[] randomIndexes) {
        int index =0;
        for (index =0; index< randomIndexes.length; index++) {
            int count = randomIndexes[index];
            int j = 0;
            for(j=0; j < data[count].length; j++) {
                if (j>0){
                    System.out.print(" ");
                }
                System.out.print(data[count][j]);

            }
            System.out.println();
        }
    }

    //Method that delects the K unique indexes randomly and uniformly
    private static int[] generateKRandomIndexes(int numberOfPoints, int numOfClusters, Random random) {

        if (numOfClusters > numberOfPoints) {
            System.err.println("Number of clusters cant be greater than the number of points.");
            System.exit(1);

        }

        //List that holds all of the possible indexes
        List<Integer> indexArray = new ArrayList<>(numberOfPoints);

        //Fill the list
        int i = 0;
        for (i=0; i< numberOfPoints; i++) {
            indexArray.add(i);
        }

        //Super handy built in std library function to randomize uniformly
        Collections.shuffle(indexArray, random);

        //Store exactly the k number of indexes we selected
        int[] randomized = new int[numOfClusters];
        int idx = 0;

        //Copy the k number of indices from our list to our randomized array
        for(idx=0; idx < numOfClusters; idx++) {
            randomized[idx]=indexArray.get(idx);
        }

        return randomized;
    }

    //This method will try to read data from the file specified by the users first argument and will return a Dataset object
    private static Dataset readFromDataset(String filename) {

        Scanner scanner = null;
        try {
            scanner= new Scanner(new File(filename));

        } catch (FileNotFoundException e) {
            System.err.println("Error: Could not read file: " + filename);
                    System.exit(1);
        }

        if (!scanner.hasNextInt()){
            System.err.println("Missing the N for number of points in the first line of: " + filename);
            System.exit(1);
        }
        int numPoints = scanner.nextInt();

        if (!scanner.hasNextInt()){
            System.err.println("Missing the D for number of dimensions in the first line of: " + filename);
            System.exit(1);
        }
        int dimensions = scanner.nextInt();

        //Set up the matrix for all points, NxD sized array
        double[][] data = new double[numPoints][dimensions];

        int pointsIndex = 0;
        int dimIndex =0;

        //Now we add our data to the matrix
        for (pointsIndex =0; pointsIndex < numPoints; pointsIndex++) {
            for (dimIndex=0; dimIndex < dimensions; dimIndex++){
                if(!scanner.hasNextDouble()){
                    System.err.println("File either ended or was improperly formated: " + filename);
                    System.exit(1);
                }
                data[pointsIndex][dimIndex] = scanner.nextDouble();

            }

        }

        scanner.close();

        //Now, ONLY if were able to fill our matrix properly, we return the built dataset type
        return new Dataset(numPoints, dimensions, data);
    }

    //Method to parse and validate the cmd line arguments
    private static Parameters parseUserArguments(String[] args) {

        //Must take 5 parameters
        if(args.length != 5)
        {
            System.err.println("Incorrect Number of Arguments: Must have exactly 5 arguments");
            System.exit(1);
        }

        int numClusters = 0;
        int maxIteration = 0;
        double convergenceThreshold= 0.00;
        int numRuns = 0;
        String filename= args[0];

        try{
            numClusters= Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("The format for number of clusters must be an integer.");
            System.exit(1);
        }

        try{
            maxIteration= Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("The format for the maximum number of iterations must be an integer.");
            System.exit(1);
        }

        try{
            convergenceThreshold = Double.parseDouble(args[3]);
        }catch (NumberFormatException e) {
            System.err.println("The format for the conversion threshold must be in double format.");
            System.exit(1);
        }

        try{
            numRuns= Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            System.err.println("The format for number of runs must be an integer.");
            System.exit(1);
        }

        //Final parameter check to meet criteria
        if(numClusters <= 1){
            System.err.println("The number of clusters must be a positive integer greater than 1.");
            System.exit(1);
        }
        if(maxIteration<1){
            System.err.println("The maximum number of iterations must be a positive integer.");
            System.exit(1);
        }
        if(convergenceThreshold<0){
            System.err.println("The convergence threshold must be a non negative real number.");
            System.exit(1);
        }
        if(numRuns<1){
            System.err.println("The minimum number of runs is 1.");
            System.exit(1);
        }

        //Only if we pass all the checks, return our parameters
        return new Parameters(filename,numClusters,maxIteration,convergenceThreshold,numRuns);
    }

    //Class to handle and protect my different arguments
    private static final class Parameters {
        final String filename;
        final int numOfClusters;
        final int maxNumOfIterations;
        final double convergenceThreshold;
        final int numOfRuns;

        //Constructor params for Parameters class
        private Parameters(String filename, int numOfClusters, int maxNumOfIterations, double convergenceThreshold, int numOfRuns) {
            this.filename = filename;
            this.numOfClusters = numOfClusters;
            this.maxNumOfIterations = maxNumOfIterations;
            this.convergenceThreshold = convergenceThreshold;
            this.numOfRuns = numOfRuns;
        }
    }

    //This class will hold the number of points, dimensions, and the actual data values
    private static final class Dataset {
        final int numberOfPoints;
        final int numOfDimensions;
        final double[][] data;

        //Constructor parameters for Dataset class
        private Dataset(int numPoints, int numDimensions, double[][] data) {
            this.numberOfPoints = numPoints;
            this.numOfDimensions = numDimensions;
            this.data = data;
        }
    }

}