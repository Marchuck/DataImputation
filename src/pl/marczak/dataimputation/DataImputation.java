package pl.marczak.dataimputation;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

import static java.lang.System.err;
import static pl.marczak.dataimputation.CSVCreator.containsMissingValues;
import static pl.marczak.dataimputation.CSVCreator.getExpertResponses;
import static pl.marczak.dataimputation.CSVCreator.isTheSame;

/**
 * @author Lukasz
 * @since 09.06.2016.
 */
public class DataImputation {

    public static void main(String[] args) {
//        new CSVCreator().runAll();
        new DataImputation().run();
//        double[] x = new double[]{0.08, 0.33, 0.66, 1.6, 2.6, 3.05, 3.05};
//        double[] y = new double[]{1, 2, 3, 4, 5, 6, 7};
//        PolynomialRegression p1 = new PolynomialRegression(x, y, 1);
//        PolynomialRegression p2 = new PolynomialRegression(x, y, 2);
//        PolynomialRegression p3 = new PolynomialRegression(x, y, 3);
//        PolynomialRegression p4 = new PolynomialRegression(x, y, 4);
//        PolynomialRegression[] a = new PolynomialRegression[]{p1, p2, p3, p4};
//
//        for (PolynomialRegression xxx : a) {
//            for (int j = 0; j <= xxx.degree(); j++) {
//                Utils.log(xxx.beta(j) + ",");
//            }
//            Utils.log("\n\n");
//        }

    }

    public void run() {
        Utils.log("Run!");
        /**
         * load expert foresees from file
         */
        List<ExpertResponse> expertResponses = new ArrayList<>();
        appendExpertsResponses("ankiet5K.csv", expertResponses);
        Utils.log("Expert responses size: " + expertResponses.size());

        /**
         * data imputation goes here
         */
        Set<Float> expertsWithMissingValuesIndexes = new HashSet<>();
        for (int j = 0; j < expertResponses.size(); j++) {
            /**
             * expert with missing values
             */
            ExpertResponse expert = expertResponses.get(j);

            if (containsMissingValues(expert, "-1") &&
                    !expertsWithMissingValuesIndexes.contains(expert.expertId)) {
                /**add to existing set of experts*/
                expertsWithMissingValuesIndexes.add(expert.expertId);
                Utils.log("\n\n***Expert no " + expert.expertId + " ***");
                /**list of foresees from given expert*/
                List<ExpertResponse> responses = getExpertResponses(expert, expertResponses);

                /**iterate over columns */
                for (int column = 0; column < 4; column++) {
                    //detect whether in THIS |column| is something missing
                    boolean somethingMissing = false;
                    //count how much values has column vector without missing values
                    int vectorSize = 0;
                    //for further purposes
                    float firstNonMissingValue = -1;
                    float secondNonMissingValue = -1;
                    for (ExpertResponse response : responses) {
                        if (someMissing(response, column)) {
                            somethingMissing = true;
                        } else {
                            if (firstNonMissingValue == -1)
                                firstNonMissingValue = response.numericResponses[column];
                            if (firstNonMissingValue != -1 && secondNonMissingValue == -1) {
                                secondNonMissingValue = response.numericResponses[column];
                            }
                            vectorSize++;
                        }
                    }

                    if (somethingMissing) {
                        if (vectorSize == 1) {
                            singleDataImputation(expertResponses, expert.expertId, firstNonMissingValue, column);
                            continue;
                        }
                        //calculate polynomial regression and swap missing data with appropriate value
                        float[] Y_args = new float[vectorSize];
                        int indexOfNextNonMissing = 0;
                        for (ExpertResponse r : responses) {
                            if (r.numericResponses[column] != -1) {
                                Y_args[indexOfNextNonMissing] = r.numericResponses[column];
                                indexOfNextNonMissing++;
                            }
                        }
                        //here we have complete vector for calculating regression
                        //additional cases: various number of points
                        float[] X_args = DataImputationHelper.createArgumentVector(vectorSize);
                        Arrays.sort(Y_args);
                        PolynomialRegression regression = PolynomialRegression.create(X_args, Y_args, 2);

                        double a = 0;
                        try {
                            a = regression.beta(2);
                        } catch (IndexOutOfBoundsException x) {
                            Utils.err(x.toString());
                        }
                        double b = regression.beta(1);
                        double c = regression.beta(0);
                        double minY = Y_args[0], maxY = Y_args[Y_args.length - 1];
                        double minX = new SquareEquationImpl(a, b, c, minY).minX();
                        double maxX = new SquareEquationImpl(a, b, c, maxY).minX();
                        double midX = (maxX + minX) / 2;
                        double meanValue = midX * midX * a + midX * b + c;
                        Utils.log("left value: " + minY + ", right value: " + maxY + ", middle: " + meanValue);
                        singleDataImputation(expertResponses, expert.expertId, (float) meanValue, column);
                    } else {
                        //that means that this vector is complete, do nothing
                    }
                }
            }
        }

        Utils.log("Summarized experts with missing values: " + expertsWithMissingValuesIndexes.size());
        Utils.log("After data imputation: ");
        expertsWithMissingValuesIndexes.clear();
        for (int j = 0; j < expertResponses.size(); j++) {
            /**
             * expert with missing values
             */
            ExpertResponse expert = expertResponses.get(j);

            if (containsMissingValues(expert, "-1") &&
                    !expertsWithMissingValuesIndexes.contains(expert.expertId)) {
                /**add to existing set of experts*/
                expertsWithMissingValuesIndexes.add(expert.expertId);
                Utils.log("\n\n***Expert no " + expert.expertId + " ***");
                /**list of foresees from given expert*/
                List<ExpertResponse> responses = getExpertResponses(expert, expertResponses);
                for (ExpertResponse e : responses) {
                    Utils.log(e.valuesOnly());
                }
            }
        }
        Utils.log("Done ");
    }

    private boolean someMissing(ExpertResponse response, int column) {
        return response.expertResponses[column].contains("-1");
    }

    private void singleDataImputation(List<ExpertResponse> expertResponses, float expertId,
                                      float singleValue, int columnIndex) {
        for (int x = 0; x < expertResponses.size(); x++) {
            ExpertResponse response = expertResponses.get(x);
            if (isTheSame(expertId, response.expertId) && response.numericResponses[columnIndex] == -1) {
                float[] arr = response.numericResponses;
                arr[columnIndex] = singleValue;
                ExpertResponse exp = new ExpertResponse(response);
                exp.numericResponses = arr;
                expertResponses.set(x, exp);
                Utils.log("Updated vector: " + Arrays.toString(arr));
            }
        }
    }

    private void appendExpertsResponses(String filename, List<ExpertResponse> expertResponseList) {
        File file = new File(filename);
        Scanner input = null;
        try {
            input = new Scanner(file);
            while (input.hasNextLine()) {
                ExpertResponse expertResponse = loadExpert(input.nextLine());
                if (expertResponse == null) continue;
                expertResponseList.add(expertResponse);
            }
        } catch (FileNotFoundException e) {
            err.println("Error! " + e.getMessage());
            e.printStackTrace();
            throw new NullPointerException("Input scanner broken!");
        } finally {
            if (input != null) input.close();
        }
    }

    private ExpertResponse loadExpert(String s) {
        ExpertResponse expertResponse = new ExpertResponse();
        String[] items = s.replaceAll(" ", "").split(",");
        expertResponse.responseId = Float.valueOf(items[0]);
        expertResponse.expertId = Float.valueOf(items[1]);
        expertResponse.competentIndex = Float.valueOf(items[2]);
        expertResponse.selfEsteemIndex = Float.valueOf(items[3]);
        expertResponse.numericResponses = new float[]{
                Float.valueOf(items[4]), Float.valueOf(items[5]),
                Float.valueOf(items[6]), Float.valueOf(items[7]),
        };
        expertResponse.expertResponses = new String[]{
                items[4], items[5], items[6], items[7],
        };
        return expertResponse;
    }
}
