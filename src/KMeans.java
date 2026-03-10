//Kody Graham
//02/25/2026
//Phase 3
//For: Dr. Emre Celebi's Data Clustering Online Class - 4372


//Coding practices resource I have decided to keep primarily using: https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

/*
Bonus 1 & 2: I am fairly certain my min max normalization is right just because the formula was so simple. As well i think
my random partition method is right but since I was expecting better results from random partition than random selection but my results
show the opposite, I dont want to take the chance of also implementing something like trying to add the more complex Z-Score Normalization
or the maximin implementation methods since I am not 100% certain that my current code is fully correct. After submission I might give them
both a try.

Bonus 3: How does the random partition method compare against the random selection method in theory?
In theory the two initialization methods are both effective at placing points in initial clusters however for random selection,
it is just that, the initial clusters are determined by choosing k random points uniformly from our set of points and then
using those point as the centroid points for our clusters.  While random partition assigns all points to random clusters and
then takes the mean coordinates for all of the points assigned to a given cluster to determine the staring location of the centroid. So
one of the biggest difference is that in theory the random selection centroids are tied to specific a specific data point at initialization
while random partition centroids do not start at any actual data point location it tarts at the mean data location of all the points that
were randomly assigned to it.
*/

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;


public class KMeans {
    public static void main(String[] args) {

        int centroid_start_method = 0;

        while (centroid_start_method != 2) {

            //Parse / Validate our required arguments into an object of the class Parameter
            Parameters parameters = parseUserArguments(args);

            //Read our data set file
            Dataset dataset = readFromDataset(parameters.filename);

            //Normalize my data set by column before k means runs using the min max formula
            dataset = minMaxNorm(dataset);

            //Print the updated normalized data set for correctness checks
            String base = new File(parameters.filename).getName();
            String outFileName = "normalized_output_" + base;
            printNormDataset(dataset, outFileName);

            //Also print to my output files
            //Had to build my file name outside of main since filename is static here
            String outputFilename = makeOutfileName(parameters.filename);

            //Track my best run and may implement a function for tracking the avg total
            //of all runs and comparing it to the best run
            RunResults bestRun = null;
            //RunResults allRuns = null;

            //append existing file and now that we are doing a comparison
            try (PrintStream outFile = new PrintStream(new FileOutputStream(outputFilename, true))) {

                Random random = new Random();

                int runIndex = 1;
                double bestInitialSSE = Double.POSITIVE_INFINITY;
                int bestInitialIndex = -100;
                double bestFinalSSE = Double.POSITIVE_INFINITY;
                int bestFinalRunIndex = -100;
                int bestIterations = Integer.MAX_VALUE;
                int bestIterationsIndex = -100;

                //Each run uses different real random centers just like my original phase 1
                for (runIndex = 1; runIndex <= parameters.numOfRuns; runIndex++) {

                    //Run my whole K Means function
                    RunResults results = runKMeans(dataset, parameters, random, outFile, runIndex, centroid_start_method);

                    //if runKmeans returned with a 0 that means we had an empty cluster re-initialize for this run by
                    //decrementing my runIndex which in turn repeats the run from the beginning using a new initialization
                    if (results.iterations== 0){
                        runIndex--;
                        continue;
                    }

                    if(results.initialSSE < bestInitialSSE){
                        bestInitialSSE = results.initialSSE;
                        bestInitialIndex = results.runNumber;
                    }

                    if(results.finalSSE < bestFinalSSE){
                        bestFinalSSE = results.finalSSE;
                        bestFinalRunIndex = results.runNumber;

                    }

                    if(results.iterations < bestIterations){
                        bestIterations = results.iterations;
                        bestIterationsIndex = results.runNumber;
                    }

                    String methodName = " ";
                    if(centroid_start_method == 0){
                        methodName = "RandomSelection";
                    } else{
                        methodName = "RandomPartition";

                    }

                    String csvName = "comparison.csv";
                    String openFile = parameters.filename;

                    appendComparisonsCSV(csvName, openFile, methodName, results.runNumber, results.initialSSE, results.finalSSE, results.iterations);

                    //If first run or smaller SSE update
                    if (bestRun == null || results.finalSSE < bestRun.finalSSE) {
                        bestRun = results;
                    }
                }

                System.out.println("Best Run: " + bestRun.runNumber + ": SSE = " + bestRun.finalSSE);
                outFile.println("Best Run: " + bestRun.runNumber + ": SSE = " + bestRun.finalSSE);

                //Run data for each method of initial centroids
                System.out.println(" ");
                System.out.println("##################################################");
                outFile.println(" ");
                outFile.println("##################################################");

                if(centroid_start_method==0){
                    System.out.println("Method: Initial Centroids by Random Selection");
                    outFile.println("Method: Initial Centroids by Random Selection");
                } else{
                    System.out.println("Method: Centroids by Random Partition");
                    outFile.println("Method: Centroids by Random Partition");
                }
                System.out.println("##################################################");

                outFile.println("##################################################");

                System.out.println();
                System.out.println("Initial SSE: " + bestInitialSSE + " on Run #: " +  bestInitialIndex);
                System.out.println("Final SSE: " + bestFinalSSE +  " on Run #: " +  bestFinalRunIndex);
                System.out.println("Number of Iterations: " + bestIterations + " on Run #: " +  bestIterationsIndex);
                System.out.println();
                System.out.println("##################################################");


                outFile.println("Initial SSE: " + bestInitialSSE + " on Run #: " +  bestInitialIndex);
                outFile.println("Final SSE: " + bestFinalSSE +  " on Run #: " +  bestFinalRunIndex);
                outFile.println("Number of Iterations: " + bestIterations + " on Run #: " +  bestIterationsIndex);
                outFile.println();
                outFile.println("##################################################");





            } catch (FileNotFoundException e) {
                System.err.println("Error writing to output file: " + outputFilename);
                System.exit(1);
            }
            centroid_start_method += 1;
        }
    }

    //K means setup section: Absolutely no pow like you made clear in your video and set up my steps just like phase 0 Algorithm 7.1 the Basic K-means algorithm
    //Just going to go ahead and redo my selected points instead of looping in main so i can encapsulate all my K means functionality in my new functions.

    //Start: My K Means Algorithm Section
    //Steps straight from our phase 0

    //Euclidean Distance Squared
    private static double squaredEuclideanDistance(double[] point1, double[] point2) {
        double squaredDist = 0.0;
        int dimension = 0;

        //loop over each dimension
        for (dimension = 0; dimension < point1.length; dimension++) {
            double diff = point1[dimension] - point2[dimension];

            //Just like your video said, no POW!
            squaredDist += diff * diff;
        }
        return squaredDist;
    }

    //Step 1: select K points as initial centroids rand // Phase 2 initial centroids
    private static double[][] initialCentroids(Dataset dataset, int numberOfClusters, Random random) {

        //Select the unique point indexes
        int[] centerIndexes = generateKRandomIndexes(dataset.numberOfPoints, numberOfClusters, random);

        double[][] centroids = new double[numberOfClusters][dataset.numOfDimensions];

        //Copy data points to my centroid array
        int i = 0;
        for (i = 0; i < centerIndexes.length; i++) {
            System.arraycopy(dataset.data[centerIndexes[i]], 0, centroids[i], 0, dataset.numOfDimensions);
        }
        return centroids;
    }

    //step 2: repeat steps 3 and 4 until step 5 is met

    //Step 3: Form K clusters by assigning each point to its closest centroid
    //I know the method name is long but it is a key method so I want its function very clear
    private static int[] assignPointsToClosestCentroid(Dataset dataset, double[][] centroids) {
        int[] assignedPoints = new int[dataset.numberOfPoints];

        int i = 0;
        for (i = 0; i < dataset.numberOfPoints; i++) {

            //Keep track best distance
            double bestDistance = Double.MAX_VALUE;
            int bestCenter = 0;

            //Loop over each of the centroids
            int j = 0;
            for (j = 0; j < centroids.length; j++) {
                double distance = squaredEuclideanDistance(dataset.data[i], centroids[j]);

                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestCenter = j;
                }
            }
            //Store the chosen centroid for the i-th point
            assignedPoints[i] = bestCenter;
        }

        return assignedPoints;
    }

    //Step 4: Recompute the centroid of each cluster
    private static double[][] recomputeCentroids(Dataset dataset, int[] assignedPoints, int numClusters, double[][] lastCentroid) {

        int dimensions = dataset.numOfDimensions;

        //Stores coordinate sum for each of my clusters
        double[][] newCentroids = new double[numClusters][dimensions];
        int[] pointsPerCluster = new int[numClusters];

        //Add the points into their cluster
        int i = 0;
        for (i = 0; i < dataset.numberOfPoints; i++) {
            int cluster = assignedPoints[i];
            pointsPerCluster[cluster]++;

            double[] point = dataset.data[i];

            //Add coord to cluster sum
            int dimIndex = 0;
            for (dimIndex = 0; dimIndex < dimensions; dimIndex++) {
                newCentroids[cluster][dimIndex] += point[dimIndex];
            }
        }

        //Divide the sums by points per cluster to get the mean
        int cent = 0;
        for(cent = 0; cent < numClusters; cent++) {

            /*IntelliJ's IDE recommended arraycopy i was just going to use another for loop to reassign the last centroid
            to the new since in the last iteration it clearly wasnt empty. Using arraycopy is way more efficient code wise for sure.*/
            if (pointsPerCluster[cent] == 0) {
                System.arraycopy(lastCentroid[cent], 0, newCentroids[cent], 0, dimensions);
                continue;
            }

            int dim_index = 0;
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
        for (i = 0; i < dataset.numberOfPoints; i++) {
            //find the assigned cluster for the i-th point
            int cent = assignedPoints[i];
            //Add sd to centroid
            sse += squaredEuclideanDistance(dataset.data[i], centers[cent]);
        }
        return sse;
    }

    //Additional step: going to need to check for flatline in improvements
    private static boolean hasFlattened(double lastSSE, double curSSE, double threshold) {
        if (Double.isInfinite(lastSSE)) {
            return false;
        }

        //If back to back runs have same SSE obviously we have converged
        if(curSSE == lastSSE){
            return true;
        }

        double improveCheck = (lastSSE - curSSE) / lastSSE;

        /*Not really a fan of returning an argument instead of a variable but intelliJ was giving me a warning saying
        declaring a variable just to return it on the next line was redundant and I could not find a mention of how to
        handle this in my code style document so just went with intelliJ's recommendation.*/
        return improveCheck < threshold;
    }

    //Class to save the results for each run
    private static final class RunResults {
        int runNumber;
        int iterations;
        double finalSSE;
        double[][] finalCents;
        double initialSSE;

        private RunResults(int runNumber, int iterations, double finalSSE, double[][] finalCents, double initialSSE) {
            this.runNumber = runNumber;
            this.iterations = iterations;
            this.finalSSE = finalSSE;
            this.initialSSE = initialSSE;
            this.finalCents = finalCents;
        }
    }

    //Run a full sequence of my k mean steps till convergence, basically the main for calling my K Means method
    private static RunResults runKMeans(Dataset dataset, Parameters params, Random rand, PrintStream fileOut, int runNum, int centroid_start_method) {

        //Print header in both console and my file
        if (centroid_start_method == 0) {

            System.out.println("Random Selection - Run #: " + runNum);
        }
        if (centroid_start_method == 1) {
            System.out.println("Random Partition - Run #: " + runNum);
        }
        System.out.println("--------------------------------------");

        if (fileOut != null) {
            if (centroid_start_method == 0) {

                fileOut.println("Random Selection - Run #: " + runNum);
            }
            if (centroid_start_method == 1) {
                fileOut.println("Random Partition - Run #: " + runNum);
            }
            fileOut.println("--------------------------------------");
        }

        //Now call each of my steps
        //Step 1
        double[][] centroids = new double[0][];
        //Phase 2 starting centroids
        if (centroid_start_method == 0) {
            centroids = initialCentroids(dataset, params.numOfClusters, rand);
        }
        else if (centroid_start_method == 1) {

            //Phase 3 starting centroids
            centroids= randomPartitionCentroids(dataset, params.numOfClusters, rand);

        }
        else{
            //Just a safety check
            System.err.println("Error: centroid_start_method has to be either 0 or 1");
        }

        //Save my start sse
        int[] initialAssignments = assignPointsToClosestCentroid(dataset, centroids);
        double initialSSE = computeSSE(dataset, centroids, initialAssignments);

        double lastSSE = Double.POSITIVE_INFINITY;
        double curSSE = Double.POSITIVE_INFINITY;
        int iterationsDone = 0;
        boolean emptyCheck= false;

        //Step 2
        int indexNumClus = 0;
        for (indexNumClus = 1; indexNumClus <= params.maxNumOfIterations; indexNumClus++) {



            //step 3
            int[] assignPoints = assignPointsToClosestCentroid(dataset, centroids);

            //Check for empty clusters in both methods to be safe
            emptyCheck = emptyClusterCheck(assignPoints, params.numOfClusters);
            if (emptyCheck) {
                System.out.println("Empty Cluster found! --> restarting run with new initialization.");
                System.out.println();

                if (fileOut != null) {
                    fileOut.println("Empty Cluster found! --> restarting run with new initialization.");
                    fileOut.println();
                }
                return new RunResults(runNum, 0, curSSE, centroids, initialSSE);
            }

            //step 4
            double[][] newCentroids = recomputeCentroids(dataset, assignPoints, params.numOfClusters, centroids);

            //SSE
            curSSE = computeSSE(dataset, newCentroids, assignPoints);

            //if (iterationsDone==0 && centroid_start_method == 1) {
            //    initialSSE[1] = curSSE;
            //}

            //Output
            System.out.println("Iteration " + indexNumClus + " : SSE = " + curSSE);
            if (fileOut != null) {
                fileOut.println("Iteration " + indexNumClus + " : SSE = " + curSSE);
            }
            iterationsDone = indexNumClus;

            //Step 5
            if (hasFlattened(lastSSE, curSSE, params.convergenceThreshold)) {
                centroids = newCentroids;
                break;
            }
            lastSSE = curSSE;
            centroids = newCentroids;
        }

        //Blank Line between the runs
        System.out.println(" ");
        if (fileOut != null) {
            fileOut.println();
        }

        return new RunResults(runNum, iterationsDone, curSSE, centroids, initialSSE);
    }

    //Anymore helpers for k means will go here

    //I am going to consider all of my phase 3 requirements as "helpers" for my base K mean algorithm so all of phase 3
    //will be here
    //Note for reference during implementation: min max norm formula: x_scaled = x-xmin / (xmax- xmin) and must be between 0 and 1

    // START FOR PART 1 of PHASE 3

    //Fairly straightforward just an empty cluster check that steps through my clusters to make sure they all have more
    //than 0 points in them. If they do have an empty, return true and then I handle it be stepping back a run to recompute the points assigned clusters
    private  static boolean emptyClusterCheck(int[] assignedPoints, int numOfClusters) {
        int[] numClusters = new int[numOfClusters];

        for (int i = 0; i < assignedPoints.length; i++) {
            numClusters[assignedPoints[i]]++;
        }

        for (int i = 0; i < numOfClusters; i++) {
            if (numClusters[i] == 0) {
                return true;
            }
        }
        return false;
    }

    //My function to normalize our datasets based on the formula: x_scaled = x-xmin / (xmax- xmin)
    private static Dataset minMaxNorm(Dataset dataset){

        int numD = dataset.numOfDimensions;
        int numP = dataset.numberOfPoints;

        double[] minsInEach = new double[numD];
        double[] maxsInEach = new double[numD];

        //Assign a high positive to min and low negative to max in every position so they are guaranteed to be overwritten during my loop to collect them
        for (int dim = 0; dim < numD; dim++) {
            minsInEach[dim] = Double.POSITIVE_INFINITY;
            maxsInEach[dim] = Double.NEGATIVE_INFINITY;
        }

        //Loop through each dim and then through each point to find min and max vals
        for (int numPointsInDim = 0; numPointsInDim < numP; numPointsInDim++) {

            for(int dim = 0; dim < numD; dim++) {
                double val = dataset.data[numPointsInDim][dim];
                if(val < minsInEach[dim]) {
                    minsInEach[dim] = val;
                }
                if(val > maxsInEach[dim]) {
                    maxsInEach[dim] = val;
                }

            }
        }

        //Apply the min max norm formula to get my scaled data points
        double[][] x_scaled = new double[numP][numD];
        for(int pointNum = 0; pointNum < numP; pointNum++) {
            for(int dim = 0; dim < numD; dim++) {
                double denominator = maxsInEach[dim] - minsInEach[dim];

                //Check for 0 value, testing a few options for handling it
                if(denominator == 0) {
                    //continue
                    //Max precision for double practically 0 but not?
                    //denominator = .000000000001;

                    x_scaled[pointNum][dim] = 0.0;
                }
                else {
                    if(denominator > 0) {
                        x_scaled[pointNum][dim] = (dataset.data[pointNum][dim] - minsInEach[dim]) / denominator;

                    }
                    else {
                        //Shouldnt happen but just as a debug check
                        System.err.println("ERROR! DENOMINATOR IS LESS THAN 0, PROBLEM WITH NORMALIZATION!");
                        System.exit(1);

                    }

                }
            }
        }

        Dataset newDataset = new Dataset(numP, numD, x_scaled);

        //Return my new dataset with normalized values
        return newDataset;
    }

    //Just a simple function to print my dataset after normalization to help confirm my normalization is working.
    //Rewrites each run
    private static void printNormDataset( Dataset dataset, String outputFilename) {

        try (PrintStream fileOut = new PrintStream(new FileOutputStream(outputFilename))) {

            for(int point = 0; point < dataset.numberOfPoints; point++) {
                for(int dim = 0; dim < dataset.numOfDimensions; dim++) {

                    if (dim > 0) {
                        fileOut.print(" ");
                    }

                    fileOut.print(dataset.data[point][dim]);
                }
                fileOut.println();

            }

        }catch (Exception e) {
            System.err.println("Error writing normalized dataset");
        }
    }

    //END PART 1 of PHASE 3
    //START PART 2 of PHASE 3
    //Random Partition Method
    //Note for self: while implementing think of this method as just random assigning points to clusters and then using
    //the centroids of those randomly assigned points to form new starting centroids

    private static double[][] randomPartitionCentroids(Dataset dataset, int numOfClusters, Random rand) {

        //Assign my points randomly to a cluster in num of clusters
        int[] assignedCentroids = assignPointsRandomly(dataset.numberOfPoints, numOfClusters, rand);

        double[][] totalSum = new double[numOfClusters][dataset.numOfDimensions];
        int[] totalClusters = new int[numOfClusters];

        //Add all the points together for each cluster and the number of points in each cluster
        for (int point = 0; point < dataset.numberOfPoints; point++) {
            int clusterNum = assignedCentroids[point];
            totalClusters[clusterNum]++;

            //and then loop over the number of dimensions to add up all the values per dimension in each of my cluster
            for (int dim = 0; dim < dataset.numOfDimensions; dim++) {
                totalSum[clusterNum][dim] += dataset.data[point][dim];
            }
        }

        //now compute each clusters mean based on assigned points for my new centroid points
        double[][] partitionCentroids = new double[numOfClusters][dataset.numOfDimensions];
        for (int cluster = 0; cluster < numOfClusters; cluster++) {
            for(int dim = 0; dim < dataset.numOfDimensions; dim++) {

                partitionCentroids[cluster][dim] = totalSum[cluster][dim] / totalClusters[cluster];

            }


        }
        //And finally return my new mean based centroids
        return partitionCentroids;
    }

    //Just a helper for my partition function, just loops through the num of points to assign each to one of the clusters at random
    private static int[] assignPointsRandomly(int numPoints, int numOfClusters, Random rand) {

        int[] assignedCentroids = new int[numPoints];
        for (int i = 0; i < numPoints; i++) {
            assignedCentroids[i] = rand.nextInt(numOfClusters);
        }


        return assignedCentroids;

    }


    //Just creates my csv file so i can easily import my data into excel where I can use a pivot table to compare my initialization methods
    private static void appendComparisonsCSV(String csvName, String fileName, String method, int runNum, double initialSSE, double finalSSE, int iterations) {

        File file = new File(csvName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            if(file.length() == 0) {
                writer.write("method,file,runNum,initialSSE,finalSSE,iterations");
                writer.write("\n");
            }


            writer.write(method + "," + fileName + "," + runNum + "," + initialSSE + "," + finalSSE + "," + iterations);
            writer.write("\n");

        } catch (IOException e) {
            System.err.println("Error writing CSV file");
            System.exit(1);
        }

    }
    //END PART 2 OF PHASE 3


    //End: K Means Algorithm Section

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

        //Super handy built in std library function I found to randomize uniformly
        Collections.shuffle(indexArray, random);

        //Store exactly the k number of indexes we selected
        int[] randomized = new int[numOfClusters];
        int idx = 0;

        //Copy the k number of indices from our list to my randomized array
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

        //Now, ONLY if were able to fill our matrix properly, I return the built dataset type
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
        if(convergenceThreshold<0 || convergenceThreshold>=1){
            System.err.println("The convergence threshold must be a non negative real number less than 1.");
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







/*
No longer used but keeping just in case:

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


        //Initial idea for making sure we dont divide by 0, just exit
        //Leaving this check in case copying the array ends up being a wrong way to handle empty clusters.
        Check for 0 points in a cluster now that i think its possible to have an empty cluster because dividing by 0
        points per cluster would cause an error. I know our phase 0 talked about a couple ways of handling empty clusters
        but since you said we can ignore them for now im just using this check to exit cleanly and inform the user of what happened
        int j = 0;
        for(j = 0; j < pointsPerCluster.length; j++) {
            if (pointsPerCluster[j] ==0)
            {
                System.err.println("Empty Cluster Detected: Can not divide by 0! Please try re-running the algorithm, this condition is rare!");
                System.exit(1);
            }
        }

        //Shouldnt happen but just to be extra safe
                if (totalClusters[cluster] == 0) {

                    //System.err.println("ERROR! division by 0 in randomPartitionCentroids!");
                    //System.exit(1);
                }else {
*/