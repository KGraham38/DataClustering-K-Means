//Kody Graham
//01/18/2026
//Phase 1
//For: Dr. Emre Celebi's Data Clustering Online Class - 4372

//Coding practices resource I have decided to primarily use: https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html

public class UniformRandomSelection {
    public static void main(String[] args) {
        Parameters parameters = parseUserArguments(args);

    }

    private static Parameters parseUserArguments(String[] args) {
        //Must take 5 parameters
        if(args.length != 5)
        {
            System.err.println("Incorrect Number of Arguments: Must have exactly 5 arguments");
            System.exit(1);
        }
        return null;
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
        final float convergenceThreshold;
        //% R = 100 (number of runs)
        final int numOfRuns;

        //Constructor params for Parameters class
        private Parameters(String filename, int numOfClusters, int maxNumOfIterations, float convergenceThreshold, int numOfRuns) {
            this.filename = filename;
            this.numOfClusters = numOfClusters;
            this.maxNumOfIterations = maxNumOfIterations;
            this.convergenceThreshold = convergenceThreshold;
            this.numOfRuns = numOfRuns;
        }
    }

    //Class will hold the number of points, dimensions, and the actual data values.
    private static final class Dataset{
        final int numberOfPoints;
        final int numOfDimensions;
        final double[][] data;

        //Constructor parameters for Dataset class
        private Dataset(int numPoints, int numDimensions, double[][] data) {
            this.numberOfPoints = numPoints;
            this.numOfDimensions = numDimensions;
            this.data = data;
        }


        //% test is the name of the executable file
            //% “>” indicates command-prompt, which is not part of the output


        }



        //Valid command prompt
        //test iris_bezdek.txt 3 100 0.000001 100

        //Example output format:
        //5.1 3.4 1.5 0.2
        //7.2 3.2 6 1.8
        //4.6 3.1 1.5 0.2

        //Four dimensions so 4 items in each row, 3 lines because k = 3

}



//Java Practices and Style Guide from my Google search: This one seems to cover a little more https://google.github.io/styleguide/javaguide.html
//However, I like the layout of the sections better on this one, so I will reference both (they should contain basically the same information as these practice are kind of industry standard I think)
//but I will primarily use https://www.cs.cornell.edu/courses/JavaAndDS/JavaStyle.html