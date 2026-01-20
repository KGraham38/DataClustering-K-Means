//Kody Graham
//01/18/2026
//Phase 1
//For: Dr. Emre Celebi's Data Clustering Online Class - 4372

//Coding practices resource I have decided to primarily use: https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

import java.io.*;
import java.util.*;

public class UniformRandomSelection {
    public static void main(String[] args) {

        Parameters parameters = parseUserArguments(args);
        Dataset dataset = readFromDataset(parameters.filename);

        int[] randomIndexes= generateKRandomIndexes(dataset.numberOfPoints,parameters.numOfClusters, new Random());

        //Print to console
        printTheCenters(dataset.data, randomIndexes);

        //Also print to my output files
        String outputFilename = makeOutfileName(parameters.filename);
        saveCenterOutputsToOutputFiles(dataset.data, randomIndexes,outputFilename);

    }

    private static String makeOutfileName(String filename) {
        String baseName= new File(filename).getName();
        int endOfFilename = baseName.lastIndexOf('.');
        if  (endOfFilename > 0) {
            baseName = baseName.substring(0, endOfFilename);
        }
        return baseName + "_output.txt";
        }

    private static void saveCenterOutputsToOutputFiles(double[][] data, int[] randomIndexes, String outputFilename) {
        try (PrintStream fileOut = new PrintStream(new FileOutputStream(outputFilename, true))) {
            int index =0;
            for (index =0; index< randomIndexes.length; index++)
            {
                int count = randomIndexes[index];
                int j = 0;
                for(j=0; j < data[count].length; j++)
                {
                    if (j>0){
                        fileOut.print(" ");
                    }
                    fileOut.print(data[count][j]);

                }
                fileOut.println();
            }
            fileOut.println();

            System.out.println("The file named -> "+ outputFilename + " <- Added To.");

        } catch (IOException e) {
            System.err.println("Error writing to the output file: " + outputFilename);
            System.exit(1);
        }
    }

    private static void printTheCenters(double[][] data, int[] randomIndexes) {
        int index =0;
        for (index =0; index< randomIndexes.length; index++)
        {
            int count = randomIndexes[index];
            int j = 0;
            for(j=0; j < data[count].length; j++)
            {
                if (j>0){
                    System.out.print(" ");
                }
                System.out.print(data[count][j]);

            }
            System.out.println();
        }
    }

    private static int[] generateKRandomIndexes(int numberOfPoints, int numOfClusters, Random random) {
        if (numOfClusters > numberOfPoints)
        {
            System.err.println("Number of clusters cant be greater than the number of points.");
            System.exit(1);

        }

        List<Integer> indexArray = new ArrayList<>();
        int i = 0;
        for (i=0; i< numberOfPoints; i++)
        {
            indexArray.add(i);
        }

        Collections.shuffle(indexArray, random);

        int[] randomized = new int[numOfClusters];
        int idx = 0;
        for(idx=0; idx < numOfClusters; idx++)
        {
            randomized[idx]=indexArray.get(idx);
        }

        return randomized;
    }

    //This method will try to read data from the file specified by the users first argument
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

        //Set up the matrix for all points
        double[][] data = new double[numPoints][dimensions];

        int index = 0;
        int j =0;
        //Now we need to add our data to the matrix
        for (index =0; index < numPoints; index++)
        {
            for (j=0; j < dimensions; j++){
                if(!scanner.hasNextDouble()){
                    System.err.println("File either ended or was improperly formated: " + filename);
                    System.exit(1);
                }
                data[index][j] = scanner.nextDouble();

            }

        }

        scanner.close();

        //Now, only if were able to fill our matrix properly, we return the built dataset type
        return new Dataset(numPoints, dimensions, data);
    }

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
        //Indicated desired inputs for reference:
        //% F = iris_bezdek.txt (name of data file)
        final String filename;
        //% K = 3 (number of clusters)
        final int numOfClusters;
        //% I = 100 (maximum number of iterations in a run)
        final int maxNumOfIterations;
        //% T = 0.000001 (convergence threshold)
        final double convergenceThreshold;
        //% R = 100 (number of runs)
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

    //Class will hold the number of points, dimensions, and the actual data values.
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