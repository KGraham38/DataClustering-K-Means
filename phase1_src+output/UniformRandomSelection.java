//Kody Graham
//01/18/2026
//Phase 1
//For: Dr. Emre Celebi's Data Clustering Online Class - 4372

//Coding practices resource I have decided to primarily use: https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

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


public class UniformRandomSelection {
    public static void main(String[] args) {

        //Parse / Validate our required arguments into an object of the class Parameter
        Parameters parameters = parseUserArguments(args);

        //Read our data set file
        Dataset dataset = readFromDataset(parameters.filename);

        //Generate my k random indexes, all unique
        int[] randomIndexes= generateKRandomIndexes(dataset.numberOfPoints,parameters.numOfClusters, new Random());

        //Print the centers to console
        printTheCenters(dataset.data, randomIndexes);

        //Also print to my output files
        //Had to build my file name outside of main since filename is static here
        String outputFilename = makeOutfileName(parameters.filename);
        saveCenterOutputsToOutputFiles(dataset.data, randomIndexes,outputFilename);

    }

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
        List<Integer> indexArray = new ArrayList<>();

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