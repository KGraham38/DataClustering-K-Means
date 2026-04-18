//Kody Graham
//04/12/2026
//Phase 5
//For: Dr. Emre Celebi's Data Clustering Online Class - 4372


//Coding practices resource I have decided to keep primarily using: https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

import static java.lang.Math.sqrt;

public class KMeans {
    public static void main(String[] args) {

        //Removed random selection initialization by starting at 1 instead of 0
        int centroid_start_method = 1;

        while (centroid_start_method != 2) {

            //Parse / Validate our required arguments into an object of the class Parameter
            Parameters parameters = parseUserArguments(args);

            //Read our data set file
            Dataset dataset = readFromDataset(parameters.filename);

            //Normalize my data set by column before k means runs using the min max formula
            dataset = minMaxNorm(dataset);

            //K min normally 2 per our instructions
            int kMin=2;
            //K max closest int for sqrt (n/2)
            int kMax= (int) Math.round(sqrt(dataset.numberOfPoints/2.0));

            int maxNumOfEmptyClusters = (int) Math.round(sqrt(dataset.numberOfPoints/2.0));

            //Print the updated normalized data set for correctness checks
            String base = new File(parameters.filename).getName();
            String outFileName = "normalized_output_" + base;
            printNormDataset(dataset, outFileName);

            //Also print to my output files
            //Had to build my file name outside of main since filename is static here
            String outputFilename = makeOutfileName(parameters.filename);

            //Track my best run and may implement a function for tracking the avg total
            //of all runs and comparing it to the best run
            //RunResults bestRun = null;
            //RunResults allRuns = null;

            //append existing file and now that we are doing a comparison
            try (PrintStream outFile = new PrintStream(new FileOutputStream(outputFilename, true))) {

                Random random = new Random();

                int runIndex = 1;

                //Track CH
                double trackBestCHVal = -100;
                int bestCHKIndex = -100;

                //Track SW
                double trackBestSWVal = -100;
                int bestSWKindex = -100;

                //Adding outer loop to loop over our new k range
                for(int k = kMin; k <= kMax; k++) {

                    //Had to move inside the k's loop for obvious reasons
                    RunResults bestRun = null;
                    double bestInitialSSE = Double.POSITIVE_INFINITY;
                    int bestInitialIndex = -100;
                    double bestFinalSSE = Double.POSITIVE_INFINITY;
                    int bestFinalRunIndex = -100;
                    int bestIterations = Integer.MAX_VALUE;
                    int bestIterationsIndex = -100;

                    Parameters paramsPerNumKs = new Parameters(parameters.filename, k, parameters.maxNumOfIterations, parameters.convergenceThreshold,parameters.numOfRuns);

                    int maxRestartCounter= 0;
                    int updatedKMax = 0;



                    //Each run uses different real random centers just like my original phase 1
                    for (runIndex = 1; runIndex <= parameters.numOfRuns; runIndex++) {

                        System.out.println("Test for " + k + " clusters");
                        outFile.println("Test for " + k + " clusters");


                        //Run my whole K Means function
                        RunResults results = runKMeans(dataset, paramsPerNumKs, random, outFile, runIndex, centroid_start_method);

                        //if runKmeans returned with a 0 that means we had an empty cluster re-initialize for this run by
                        //decrementing my runIndex which in turn repeats the run from the beginning using a new initialization
                        if (results.iterations == 0) {
                            runIndex--;
                            maxRestartCounter++;

                            if (maxRestartCounter == maxNumOfEmptyClusters)
                            {
                                int kTemp = k-1;
                                System.out.println("Max empty cluster threshold met kMax set to " + kTemp + " from " + kMax);
                                outFile.println("Max empty cluster threshold met kMax set to " + kTemp + " from " + kMax);
                                kMax = k-1;
                            }

                            continue;
                        }



                        if (results.initialSSE < bestInitialSSE) {
                            bestInitialSSE = results.initialSSE;
                            bestInitialIndex = results.runNumber;
                        }

                        if (results.finalSSE < bestFinalSSE) {
                            bestFinalSSE = results.finalSSE;
                            bestFinalRunIndex = results.runNumber;

                        }

                        if (results.iterations < bestIterations) {
                            bestIterations = results.iterations;
                            bestIterationsIndex = results.runNumber;
                        }

                        String methodName = " ";
                        if (centroid_start_method == 0) {
                            methodName = "RandomSelection";
                        } else {
                            methodName = "RandomPartition";

                        }

                        String csvName = "comparison.csv";
                        String openFile = parameters.filename;

                        appendComparisonsCSV(csvName, openFile, methodName, results.runNumber, results.initialSSE, results.finalSSE, results.iterations);

                        //If first run or smaller SSE update
                        if (bestRun == null || results.finalSSE < bestRun.finalSSE) {
                            bestRun = results;
                        }

                        //Forgot to add this which messed up all of my outputs
                        maxRestartCounter = 0;

                    }

                    //Had to add a check because best run can be null it never finishes a run properly
                    if(bestRun == null){
                        break;
                    }

                    System.out.println("Best Run: " + bestRun.runNumber + ": SSE = " + bestRun.finalSSE);
                    outFile.println("Best Run: " + bestRun.runNumber + ": SSE = " + bestRun.finalSSE);

                    //Had to update to use k from paramsPerK
                    double chIndexVal= computeCHindex(dataset,bestRun, k);
                    System.out.println("CH index(" + k + "): " + chIndexVal);
                    outFile.println("CH index(" + k + "): " + chIndexVal);

                    double swIndexVal= computeTheSWindex(dataset,bestRun, k);
                    System.out.println("SW index(" + k + "): " + swIndexVal);
                    outFile.println("SW index(" + k + "): " + swIndexVal);
                    String csvName = "phase_4_results.csv";
                    appendMyPhase4CSV(csvName, parameters.filename, k, chIndexVal, swIndexVal);


                    if (chIndexVal > trackBestCHVal) {
                        bestCHKIndex = k;
                        trackBestCHVal = chIndexVal;
                    }

                    if (swIndexVal > trackBestSWVal) {
                        bestSWKindex = k;
                        trackBestSWVal = swIndexVal;
                    }


                    System.out.println(" ");
                    outFile.println(" ");
                }

                //Run Data for best k using CH
                System.out.println("##################################################");
                System.out.println("Est Optimal K - CH: " + bestCHKIndex);
                System.out.println("Best CH Val: " + trackBestCHVal);
                System.out.println("##################################################");

                outFile.println("##################################################");
                outFile.println("Est Optimal K - CH: " + bestCHKIndex);
                outFile.println("Best CH Val: " + trackBestCHVal);
                outFile.println("##################################################");

                //Run Data for best k using SW
                System.out.println("##################################################");
                System.out.println("Est Optimal K - SW: " + bestSWKindex);
                System.out.println("Best SW Val: " + trackBestSWVal);
                System.out.println("##################################################");

                outFile.println("##################################################");
                outFile.println("Est Optimal K - SW: " + bestSWKindex);
                outFile.println("Best SW Val: " + trackBestSWVal);
                outFile.println("##################################################");



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

                //Phase 3, not really needed for phase 4
                /*
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
                */




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
        int[] finalAssignedClusterPerPoint;

        private RunResults(int runNumber, int iterations, double finalSSE, double[][] finalCents, double initialSSE, int[] finalAssignedClusterPerPoint) {
            this.runNumber = runNumber;
            this.iterations = iterations;
            this.finalSSE = finalSSE;
            this.initialSSE = initialSSE;
            this.finalCents = finalCents;
            this.finalAssignedClusterPerPoint = finalAssignedClusterPerPoint;
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
                return new RunResults(runNum, 0, curSSE, centroids, initialSSE, null);
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

        //Save the final assignments for each point since my CH implementation will require the final converged centroids
        int [] finalAssignedClusters = assignPointsToClosestCentroid(dataset,centroids);
        return new RunResults(runNum, iterationsDone, curSSE, centroids, initialSSE, finalAssignedClusters);
    }

    //Anymore helpers for k means will go here

    //Phase 5 start (for easy search to get back to my phase 5 logic)

    /*
    Phase 5 IMPORTANT IMPLEMENTATION NOTES: both external validation methods use almost the same variables so should
    be able to create methods to handle them once and call what I need for each

    Likely methods needed: computeTP, computeFN, computeFP, computeN, computeTN

    N = TP+FN+FP+TN #Reread the datamining book section and found this formula for N which can be computed more only
    using just the num of clusters as n for: N= n(n-1)/2


    TP = 1/2((SUM to r from i = 1 SUM to k from j=1 n^2ij)-n)
                                                       ^^ subscripts
    FN = 1/2(SUM to k from j=1 m^2j - SUM to r from i =1 SUM to k from j = 1 n^2ij
                                  ^ subscripts                                  ^^
    FP = 1/2(SUM to r from i = 1 n^2i - SUM to r from i = 1 SUM to k from j=1 n^2ij)
                                    ^                                            ^^
    TN = N-(TP+FN+FP) going to have to implement this one last so I can just call the others in the formula order

    Might be beneficial to have method for SUM to r from i = 1 and SUM to k from j=1 n^2ij since they are used so frequently
    also n^2ij is used a lot in these formulas so a method for it will help too



    */

    //Start the Jaccard external validation method
    /*
        From the resource you recommended:https://dataminingbook.info/book_html/chap17/book.html
        Measures the fraction of true positive point pairs but after ignoring true negatives

        For perfect clustering, the Jaccard Coefficient has value 1 = no false positives or false negatives


        Jaccard= TP/TP+FN+FP

        Steps:
        Compute tp
        compute fn
        compute fp
        then call a computeJaccard function to call the other compute functions and return the results

    */

    //This will be the nij table that will store how many points assigned to i,which is the predicted cluster, but actually in j, the true clust j
    private static int[][] buildClustLabelTable(int[] assignedClusters, int[] trueLabel, int numClusters, int trueNumClusters) {

        int[][] clusterLabelTable = new int[numClusters][trueNumClusters];
        for (int i = 0; i < numClusters; i++) {
            int predictedCluster = assignedClusters[i];
            int trueCluster = trueLabel[i];

            clusterLabelTable[predictedCluster][trueCluster]++;
        }

        return clusterLabelTable;
    }

    //Now i will need the form of SUM n^2ij
    //Just a reminder while implementing n is the total num of points in cluster i
    private static double computeSumOfNijSquared (int[][] clusterLabelTable) {

        double sumSquared = 0;

        for (int i = 0; i < clusterLabelTable.length; i++) {
            for (int j = 0; j < clusterLabelTable.length; j++) {
                double nij = clusterLabelTable[i][j];
                sumSquared += nij * nij;
            }
        }
        return sumSquared;
    }

    //Going to use the more simple formula i found in the validation section of the datamining book reference at the beginning of this phases section
    //N=n(n-1)/2
    private static double computeN(int numPoints){

        //I really dont like these in line returns but intelliJ marks them as a warning if i dont skip the variable declaration
        return (numPoints*(numPoints-1)/2.0);
    }

    //Now TP which counts pairs that are in both the same predicted and true cluster
    private static double computeTP(int[][] clusterLabelTable, int[] trueLabel, int numPoints) {
        double sumNIJSquared = computeSumOfNijSquared(clusterLabelTable);

        return  .5* (sumNIJSquared-numPoints);
    }

    //FN pairs that are in same true cluster but not predicted clust
    private static double computeFN(int[][] clusterLabelTable) {
        double sumNijSquared = computeSumOfNijSquared(clusterLabelTable);
        double simMjSquared = computeSumOfMjSquared(clusterLabelTable);
    }


    //Phase 4 start (for easy search to get back to my phase 4 logic)

    //Phase 4 IMPORTANT IMPLEMENTATION NOTE: Like we talked about in our email I did go ahead and set kmax to theCurrentK-1
    //if there were an equal num of consecutive empty clusters to sqrt(n/2) for a given dataset. That said I let the current
    //k keep running since in my previous experiments I found it even with a lot of consecutive empty clusters, the run was
    //still able to finish eventually so to give me an extra k value for my comparison tables just for completeness I let k finish.
    //Then when it hits the top of the for loop it breaks because the new kmax val is lower than k.

    //Start CH implementation
    /*
    Link for my easy reference of formula: https://en.wikipedia.org/wiki/Calinski%E2%80%93Harabasz_index
    Quick reference while I am implementing:
    CH = (BCSS/(k-1))/(WCSS/(n-k))
    BCSS = i=1, Sum of ni||ci-c||^2
    WCSS = i=1, Sum of (sum of x exists Ci ||x-ci||^2)
    Sums for both above are up to k

    Map of my variables to the formula variables so i can implement it easier
    k = numOfClusters
    n= dataset.numberOfPoints
    WCSS = bestrun.finalSSE
    BCSS = ni is clusterCounts[i]
           ci = bestRun.finalCents[i]
           c bar is the overallMean
           so ||ci-cbar||^2 is the squaredEuclideanDistance(bestRun.finalCents[i], overallMean)

    //Clearer formula explanation here: https://www.graphpad.com/guides/prism/latest/statistics/stat_clustering_calinski_harabasz.htm

    Basic logic: measure how good cluster separation is by computing how far apart the clus centers arw to how
    tight the points are in each cluster.

    */

    //Actually compute the CH index, probably going to be the most complex part of my CH implementation
    private static double computeCHindex(Dataset dataset, RunResults bestRun, int numOfClusters) {
        //Steps:
        //get n and WCSS
        int numPoints = dataset.numberOfPoints;
        double wcss = bestRun.finalSSE;

        //get the total mean
        double bcss = getBcss(dataset, bestRun, numOfClusters);

        //Compute the numerator and denom based on the equation
        double numerator = bcss / (numOfClusters - 1);
        double denom = wcss / (numPoints - numOfClusters);

        //then return
        return numerator /denom;
    }

    //IntelliJ actually recommended to separate this logic into its own method from computeCHindex, I like it so I will
    //do a similar breakdown with SW the actual compute function will just call helpers to keep a better separation of tasks
    private static double getBcss(Dataset dataset, RunResults bestRun, int numOfClusters) {
        double[] overallMean = computeOverallMean(dataset);

        //count clusters
        int[] clusterPointCounts = countPointsInEachCluster(bestRun.finalAssignedClusterPerPoint, numOfClusters);

        //calculate BCSS
        double bcss = 0.0;

        //i = cluster on
        for (int i = 0; i < numOfClusters; i++) {
            double squaredDistOverallMean = squaredEuclideanDistance(bestRun.finalCents[i], overallMean);
            bcss = bcss + clusterPointCounts[i]*squaredDistOverallMean;
        }
        return bcss;
    }

    //Just simply gives me the ni needed for my CH formula
    private static int[] countPointsInEachCluster(int[] assignedPoints, int numOfClusters) {
        int[] countclusters = new int[numOfClusters];
        for (int i = 0; i < assignedPoints.length; i++) {
            countclusters[assignedPoints[i]]++;
        }
        return countclusters;
    }
    private static double [] computeOverallMean(Dataset dataset) {
        double[] overallMean = new double[dataset.numOfDimensions];

        //Sum the points per dimension
        for (int i = 0; i < dataset.numberOfPoints; i++) {
            for (int j = 0; j < dataset.numOfDimensions; j++) {
                overallMean[j] += dataset.data[i][j];
            }
        }
        //And then divide each clusters summed points by the num of points in the cluster to get the mean points for each
        for (int i = 0; i < dataset.numOfDimensions; i++) {
            overallMean[i] /= dataset.numberOfPoints;
        }

        return overallMean;
    }

    //End CH implementation


    //Start SW implementation

    /*
    Formula notes I found online:
    S(i) = (b(i) - a(i) / (max{(a(i), b(i))
    a(i) is average dist between ith object and all other objects in the same cluster
    b(i) average dist of ith object with all objects in the nearest cluster

    Value range [-1,1]
    if s(i) close to 1, assigned to good cluster
    if s(i) val is about 0, sample equal dist from 2 clusters
    if close to -1, bad classification

    IMPORTANT Notes: SW does not use squared dist
                     b(i) is min avg dist to another cluster

    Page 18 explains the formula great: https://cran.r-project.org/web/packages/clusterCrit/vignettes/clusterCrit.pdf

    Basic logic: measure how good each point fits in its cluster by comparing closeness of it in its
    cluster vs how far the nearest other cluster is to it

    */

    //Regular Euclidean Distance
    private static double euclideanDistance(double[] point1, double[] point2) {
        double distance = 0.0;

        for (int i = 0; i < point1.length; i++) {
            double diff = point1[i] - point2[i];
            distance += diff * diff;
        }

        return Math.sqrt(distance);
    }

    //First need a(i)
    private static double computeAForI(Dataset dataset, int[] assignedPoints, int pointIndex) {
        int assignedCluster = assignedPoints[pointIndex];
        int inThisCluster = 0;
        double sumDistances = 0.0;

        for (int i = 0; i < dataset.numberOfPoints; i++) {

            if (i== pointIndex) {
                continue;
            }
            if (assignedPoints[i] == assignedCluster) {
                sumDistances += euclideanDistance(dataset.data[pointIndex], dataset.data[i]);
                inThisCluster++;
            }
        }

        //If there are no other points in the assigned cluster, its a singleton cluster and ill ignore it like
        //recommended in our discussion board. Just return 0 for avg dist between points in this cluster
        if (inThisCluster == 0) {
            return 0.0;
        }

        return sumDistances / inThisCluster;

    }

    //then b(i)
    private static double computeBForI(Dataset dataset, int[] assignedPoints, int pointIndex, int numOfClusters) {
        int assignedCluster = assignedPoints[pointIndex];

        //Should be safe since value should be between -1 and 1
        double bestClusterAvg = 100.0;

        for (int i = 0; i < numOfClusters; i++) {

            //Skip if we are at the point we are comparing distance from
            if (i == assignedCluster) {
                continue;
            }

            double sum = 0.0;
            int count = 0;

            for (int j = 0; j < dataset.numberOfPoints; j++) {
                if (assignedPoints[j] == i) {
                    sum += euclideanDistance(dataset.data[pointIndex], dataset.data[j]);
                    count++;
                }
            }

            if (count > 0){
                double avg = sum / count;
                if (avg < bestClusterAvg) {
                    bestClusterAvg = avg;
                }
            }
        }

        return bestClusterAvg;
    }

    //next going to need the max between a(i) b(i)
    private static double computeSValForPoint(Dataset dataset, int[] assignedPoints, int pointIndex, int numOfClusters) {
        double aValue = computeAForI(dataset, assignedPoints, pointIndex);
        double bValue = computeBForI(dataset, assignedPoints, pointIndex, numOfClusters);

        double max = Math.max(aValue, bValue);

        if(max == 0)
        {
            return 0;
        }

        //silhouette value for the point
        //Was going to have a separate method for computing numerator and denom but all vars are here so makes sense to do it now
        double SV = (bValue - aValue) / max;

        return SV;
    }

    //Finally return my avg Si for all points
    private static double computeTheSWindex(Dataset dataset, RunResults bestRun, int numOfClusters) {
        double totalSW = 0.0;
        for (int i = 0; i < dataset.numberOfPoints; i++) {
            totalSW += computeSValForPoint(dataset,bestRun.finalAssignedClusterPerPoint,i,numOfClusters);
        }

        double finalTotalSW = totalSW / dataset.numberOfPoints;

        return finalTotalSW;
    }

    private static void appendMyPhase4CSV(String csvName, String fileName, int k, double chIndex, double swIndex) {

        File file = new File(csvName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {

            if(file.length() == 0) {
                writer.write("file,k,chIndex,swIndex");
                writer.write("\n");
            }

            writer.write(fileName + "," + k + "," + chIndex + "," + swIndex);
            writer.write("\n");

        } catch (IOException e) {
            System.err.println("Error writing phase 4 CSV file");
            System.exit(1);
        }

    }
    //End SW implementation



    //I am going to consider all of my phase 3 requirements as "helpers" for my base K mean algorithm so all of phase 3
    //will be here
    //Note for reference during implementation: min max norm formula: x_scaled = x-xmin / (xmax- xmin) and must be between 0 and 1

    //START FOR PART 1 of PHASE 3 - just for easier reference of the code added in this phase

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

    //END PART 1 FOR MY PHASE 3
    //START PART 2 FOR MY PHASE 3
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
    //END PART 2 FOR MY PHASE 3


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

        //Must take 4 parameters, removing k user input for phase 4
        if(args.length != 4)
        {
            System.err.println("Incorrect Number of Arguments: Must have exactly 4 arguments");
            System.exit(1);
        }

        int numClusters = 0;
        int maxIteration = 0;
        double convergenceThreshold= 0.00;
        int numRuns = 0;
        String filename= args[0];

        try{
            maxIteration= Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("The format for the maximum number of iterations must be an integer.");
            System.exit(1);
        }

        try{
            convergenceThreshold = Double.parseDouble(args[2]);
        }catch (NumberFormatException e) {
            System.err.println("The format for the conversion threshold must be in double format.");
            System.exit(1);
        }

        try{
            numRuns= Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            System.err.println("The format for number of runs must be an integer.");
            System.exit(1);
        }

        //Final parameter check to meet criteria

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
        return new Parameters(filename,0,maxIteration,convergenceThreshold,numRuns);
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