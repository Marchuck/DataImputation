package pl.marczak.dataimputation;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.System.err;
import static pl.marczak.dataimputation.CSVReader.containsMissingValues;
import static pl.marczak.dataimputation.CSVReader.isTheSame;
import static pl.marczak.dataimputation.DataImputationHelper.createArgumentVector;
import static pl.marczak.dataimputation.DataImputationHelper.wrapToArray;

/**
 * @author Lukasz
 * @since 09.06.2016.
 */
public class DataImputation {
    public interface Callable<T> {
        void call(List<T> data);
    }

    public static void main(String[] args) {
        DataImputation dataImputation = new DataImputation();
        dataImputation.anotherDataImputation();
        dataImputation.runDataImputation(new Callable<ExpertResponse>() {
            @Override
            public void call(List<ExpertResponse> data) {
                Utils.log("Callback:");
                List<List<Float>> theSameExpertResponses = new ArrayList<>();

                List<Float> uniqueExperts = new ArrayList<>();
                for (ExpertResponse expertResponse : data) {
                    if (!uniqueExperts.contains(expertResponse.expertId))
                        uniqueExperts.add(expertResponse.expertId);
                }
                for (Float f : uniqueExperts)
                    theSameExpertResponses.add(new ArrayList<Float>());
                for (Float f : uniqueExperts) {
                    for (ExpertResponse expertResponse : data) {
                        if (isTheSame(f, expertResponse.expertId)) {
                            theSameExpertResponses.get(f.intValue()).add(expertResponse.expertId);
                            for (float value : expertResponse.numericResponses)
                                theSameExpertResponses.get(f.intValue()).add(value);
                        }
                    }
                }

                for (List<Float> list : theSameExpertResponses) {
                    StringBuilder sb = new StringBuilder();
                    Utils.log("Next expert: " + list.get(0));
                    sb.append("[");
                    for (float f : list) {
                        sb.append(f).append(",");
                    }
                    sb.append("]");
                    Utils.log(sb.toString());
                }
            }
        });
    }

    private void printExperts() {
        appendExpertsResponses("ankiet5K.csv", expertResponses);
        Utils.log("Expert responses size: " + expertResponses.size());


        final List<Float> missingExperts = expertsWithMissingValues();
//        for (Float f : missingExperts) {
        Utils.log("Expert id" + 128);
        List<ExpertResponse> res = CSVReader.getExpertResponses(128f, expertResponses);
        Utils.log("size = " + res.size());
        for (ExpertResponse r : res) {
            Utils.log("->" + r.valuesOnly());
        }
        Utils.log("\n\n");

    }

    private void anotherDataImputation() {
        appendExpertsResponses("ankiety13.csv", expertResponses);
        Utils.log("Expert responses size: " + expertResponses.size());

    }

    private void runDataImputation(final Callable<ExpertResponse> callback) {
//        appendExpertsResponses("ankiet5K.csv", expertResponses);
//        Utils.log("Expert responses size: " + expertResponses.size());
//        printExpertsWithNoMiss();

        final List<Float> missingExperts = expertsWithMissingValues();

//        runWithSingleExpert(128);
        Observable.from(missingExperts)
                .subscribeOn(Schedulers.trampoline())
                .flatMap(new Func1<Float, Observable<List<ExpertResponse>>>() {
                    @Override
                    public Observable<List<ExpertResponse>> call(Float expertId) {
                        return Observable.just(getExpertResponses(expertId));
                    }
                }, new Func2<Float, List<ExpertResponse>, Boolean>() {
                    @Override
                    public Boolean call(Float expertId, List<ExpertResponse> expertResponses) {
                        return performDataImputation(expertId, expertResponses);
                    }
                }).subscribe(new Subscriber<Boolean>() {
            @Override
            public void onCompleted() {
                avoidMissingValues().subscribe(new Subscriber<List<ExpertResponse>>() {
                    @Override
                    public void onCompleted() {
                        Utils.log("onCompleted");
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Utils.err(throwable.getMessage());
                        throwable.printStackTrace();
                    }

                    @Override
                    public void onNext(final List<ExpertResponse> expertResponses) {


                        Set<Float> uniqueSurveys = new HashSet<>();

                        for (ExpertResponse e : expertResponses) {
                            uniqueSurveys.add(e.responseId);
                            if (!containsMissingValues2(e)) {
                                Utils.log(e.toString());
                            }
                        }
                        callback.call(expertResponses);
                        List<Float> surveys = new ArrayList<>();
                        surveys.addAll(uniqueSurveys);
                        Collections.sort(surveys);

                        List<List<ExpertResponse>> separated = new ArrayList<>();
                        for (float f : surveys) separated.add(new ArrayList<ExpertResponse>());
                        for (ExpertResponse e : expertResponses) {
                            for (int k = 0; k < surveys.size(); k++) {
                                if (isTheSame(surveys.get(k), e.responseId)) {
                                    separated.get(k).add(e);
                                }
                            }
                        }
                    }
                });
            }

            boolean containsMissingValues2(ExpertResponse e) {
                for (float f : e.numericResponses) if (f < 0) return true;
                return false;
            }

            @Override
            public void onError(Throwable throwable) {
                Utils.err(throwable.getLocalizedMessage());
                throwable.printStackTrace();
            }

            @Override
            public void onNext(Boolean aBoolean) {
//                Utils.log(aBoolean.toString());
            }
        });


    }

    private Boolean saveResponses(List<ExpertResponse> expertResponses) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("delficSurvey_" + String.valueOf(expertResponses.get(0).responseId).replace(".", "_") + ".csv", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException x) {

        } finally {
            if (writer != null) {
                for (ExpertResponse response : expertResponses) {
                    writer.println(response.toString());
                }
                writer.close();
            }
        }
        return true;
    }

    private Observable<List<ExpertResponse>> avoidMissingValues() {

        return Observable.defer(new Func0<Observable<List<ExpertResponse>>>() {
            @Override
            public Observable<List<ExpertResponse>> call() {
                return Observable.just(fillMissingValues());
            }
        });
    }

    private List<ExpertResponse> fillMissingValues() {
        for (int k = 0; k < expertResponses.size(); k++) {
            if (containsMissingValues(expertResponses.get(k), "-1")) expertResponses.remove(k);
        }
        return expertResponses;
    }

    private Boolean performDataImputation(Float expertId, List<ExpertResponse> expertResponses) {
        List<List<Float>> f = new ArrayList<>();
        for (int j = 0; j < 4; j++) f.add(new ArrayList<Float>());
        for (ExpertResponse expertResponse : expertResponses) {
            for (int j = 0; j < 4; j++) {
                if (expertResponse.numericResponses[j] != -1)
                    f.get(j).add(expertResponse.numericResponses[j]);
            }
        }
        for (int j = 0; j < 4; j++) {
            List<Float> nonMissingValues = f.get(j);
            if (nonMissingValues.size() < 3) {
                linearImput(expertId, nonMissingValues, 0);
            } else {
                float[] vector = wrapToArray(nonMissingValues);
                quadraticInputation(expertId, vector, 0);
            }
        }

        return true;
    }

    public void quadraticInputation(float expertId, float[] Y_args, int column) {

        //here we have complete vector for calculating regression
        //additional cases: various number of points
        float[] X_args = createArgumentVector(Y_args.length);
        Arrays.sort(Y_args);
        PolynomialRegression regression = PolynomialRegression.create(X_args, Y_args, 2);

        double a = regression.beta(2);
        double b = regression.beta(1);
        double c = regression.beta(0);
        double minY = Y_args[0], maxY = Y_args[Y_args.length - 1];
        double minX = new SquareEquationImpl(a, b, c, minY).minX();
        double maxX = new SquareEquationImpl(a, b, c, maxY).minX();
        double midX = (maxX + minX) / 2;
        double meanValue = midX * midX * a + midX * b + c;
        //Utils.log("left value: " + minY + ", right value: " + maxY + ", middle: " + meanValue);
        singleDataImputationImpl(expertId, (float) meanValue, column);
    }

    private void singleDataImputationImpl(float expertId, float meanValue, int column) {
        for (int j = 0; j < expertResponses.size(); j++) {
            ExpertResponse response = expertResponses.get(j);
            int difference = (int) Math.abs(expertId - response.expertId);
            if (difference == 0 || difference == 1) {
                float[] oldResponses = expertResponses.get(j).numericResponses;
                expertResponses.get(j).numericResponses = newResponses(oldResponses, meanValue, column);
            }
        }
    }

    private float[] newResponses(float[] oldResponses, float newValue, int column) {
        float[] f = new float[oldResponses.length];
        for (int k = 0; k < oldResponses.length; k++) {
            if (column == k) {
                f[k] = newValue;
            } else f[k] = oldResponses[k];
        }
        return f;
    }

    private void linearImput(float expertId, List<Float> foresees, int c) {
        float sum = 0;
        for (float f : foresees) sum += f;
        singleDataImputationImpl(expertId, sum / foresees.size(), c);
    }
//    private List<Float> printExpertsWithNoMiss() {
//        Set<Float> ids = new HashSet<>();
//        for (ExpertResponse response : expertResponses) {
//            if (!containsMissingValues(response, "-1")) {
//                ids.add(response.expertId);
//            }
//        }
//        List<Float> floatList = new ArrayList<>();
//        floatList.addAll(ids);
//        Collections.sort(floatList);
//
//        for (float ex : floatList) Utils.log("Expert has NO missing values: " + ex);
//    }

    private List<Float> expertsWithMissingValues() {
        Set<Float> ids = new HashSet<>();
        for (ExpertResponse response : expertResponses) {
            if (containsMissingValues(response, "X")) {
                ids.add(response.expertId);
            }
        }
        List<Float> floatList = new ArrayList<>();
        floatList.addAll(ids);
//        Collections.sort(floatList);
//        for (float ex : floatList) Utils.log("Expert has missing values: " + ex);
        return floatList;
    }

    public List<ExpertResponse> getExpertResponses(ExpertResponse expertResponse) {

        return getExpertResponses(expertResponse.expertId);
    }

    public List<ExpertResponse> getExpertResponses(float expertResponseId) {
        List<ExpertResponse> outputResponses = new ArrayList<>();

        for (int j = 0; j < expertResponses.size(); j++) {
            ExpertResponse response = expertResponses.get(j);
            if (response.expertId == expertResponseId) {
                outputResponses.add(response);
            }
        }
        return outputResponses;
    }

    List<ExpertResponse> expertResponses = new CopyOnWriteArrayList<>();

    public void run() {
        Utils.log("Run!");
        /**
         * load expert foresees from file
         */

        appendExpertsResponses("ankiet5K.csv", expertResponses);
        Utils.log("Expert responses size: " + expertResponses.size());

        /**
         * data imputation goes here
         */
        Set<Float> missingExpertsIndexes = new HashSet<>();


        for (int j = 0; j < expertResponses.size(); j++) {
            /**
             * expert with missing values
             */
            ExpertResponse expert = expertResponses.get(j);

            if (containsMissingValues(expert, "-1") &&
                    !missingExpertsIndexes.contains(expert.expertId)) {
                /**add to existing set of experts*/
                missingExpertsIndexes.add(expert.expertId);
                Utils.log("\n\n***Expert no " + expert.expertId + " ***");
                /**list of foresees from given expert*/
                List<ExpertResponse> responses = getExpertResponses(expert);

                dataImputationProcedure(responses, expert);
//                List<_ExpertResponse> response1s = getExpertResponses(expert, expertResponses);
//                for (_ExpertResponse e : response1s) {
//                    Utils.log(e.valuesOnly());
//                }
//                System.exit(100);
            }
        }
        Utils.log("Summarized experts with missing values: " + missingExpertsIndexes.size());
        Utils.log("After data imputation: ");
        missingExpertsIndexes.clear();
        for (int j = 0; j < expertResponses.size(); j++) {
            /**
             * expert with missing values
             */
            ExpertResponse expert = expertResponses.get(j);

            if (containsMissingValues(expert, "-1") &&
                    !missingExpertsIndexes.contains(expert.expertId)) {
                /**add to existing set of experts*/
                missingExpertsIndexes.add(expert.expertId);
                Utils.log("\n\n***Expert no " + expert.expertId + " ***");
                /**list of foresees from given expert*/
                List<ExpertResponse> responses = getExpertResponses(expert);
                for (ExpertResponse e : responses) {
                    Utils.log(e.valuesOnly());
                }
            }
        }
        Utils.log("Done ");
    }


    private void runWithSingleExpert(int i) {

        for (ExpertResponse e : expertResponses) {
            if (isTheSame(e.expertId, (float) i)) {
                List<ExpertResponse> responses = getExpertResponses(e);
                for (ExpertResponse e1 : responses) {
                    Utils.log(e1.valuesOnly());
                }
                dataImputationProcedure2(responses, e);
                break;
            }
        }
        Utils.err("After data imputation: " + valuesUpdated + " values updated");
        for (ExpertResponse r : yetAnotherList) Utils.log(r.valuesOnly());
        Utils.err("_____________________________________");

        for (ExpertResponse e : expertResponses) {
            if (isTheSame(e.expertId, (float) i)) {
                List<ExpertResponse> responses = getExpertResponses(e);
                for (ExpertResponse e1 : responses) {
                    Utils.log(e1.valuesOnly());
                }
                break;
            }
        }
    }

    private void dataImputationProcedure(List<ExpertResponse> responses, ExpertResponse expert) {
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
                    if (firstNonMissingValue < 0)
                        firstNonMissingValue = response.numericResponses[column];
                    if (firstNonMissingValue > -1 && secondNonMissingValue < 0) {
                        secondNonMissingValue = response.numericResponses[column];
                    }
                    vectorSize++;
                }
            }
            if (somethingMissing) {
                if (vectorSize == 1) {
                    Utils.err("Insert 1 val");
                    singleDataImputation(expert.expertId, firstNonMissingValue, column);
                } else if (vectorSize == 2) {
                    Utils.err("Insert 2 val");
                    singleDataImputation(expert.expertId, (firstNonMissingValue
                            + secondNonMissingValue) / 2, column);
                } else {
                    //calculate polynomial regression and swap missing data with appropriate value
                    float[] Y_args = new float[vectorSize];
                    Utils.log("vector size: " + vectorSize + ", full: " + responses.size());
                    int indexOfNextNonMissing = 0;
                    for (ExpertResponse r : responses) {
                        if (r.numericResponses[column] > -1) {
                            Y_args[indexOfNextNonMissing] = r.numericResponses[column];
                            indexOfNextNonMissing++;
                            if (indexOfNextNonMissing >= responses.size() - 1) break;
                        }
                    }
                    //here we have complete vector for calculating regression
                    //additional cases: various number of points
                    float[] X_args = createArgumentVector(vectorSize);
                    Arrays.sort(Y_args);
                    PolynomialRegression regression = PolynomialRegression.create(X_args, Y_args, 2);

                    double a = regression.beta(2);
                    double b = regression.beta(1);
                    double c = regression.beta(0);
                    double minY = Y_args[0], maxY = Y_args[Y_args.length - 1];
                    double minX = new SquareEquationImpl(a, b, c, minY).minX();
                    double maxX = new SquareEquationImpl(a, b, c, maxY).minX();
                    double midX = (maxX + minX) / 2;
                    double meanValue = midX * midX * a + midX * b + c;
                    //Utils.log("left value: " + minY + ", right value: " + maxY + ", middle: " + meanValue);
                    Utils.err("Insert regression val");
                    singleDataImputation(expert.expertId, (float) meanValue, column);
                }

            }
        }
    }

    private boolean someMissing(ExpertResponse response, int column) {
        return response.expertResponses[column].contains("-1");
    }

    int valuesUpdated = 0;
    private List<ExpertResponse> yetAnotherList = new ArrayList<>();

    private synchronized void singleDataImputation(float expertId, float singleValue, int columnIndex) {
        Utils.log("singleDataImputation");
        for (int x = 0; x < expertResponses.size(); x++) {
            ExpertResponse response = expertResponses.get(x);
            int difference = (int) Math.abs(expertId - response.expertId);
            if (difference == 1 || difference == 0) {
//                _ExpertResponse exp = new _ExpertResponse(response);
//                exp.numericResponses[columnIndex] = singleValue;
                if (expertResponses.get(x).numericResponses[columnIndex] < 0) {
                    ++valuesUpdated;
                    Utils.log("response [" + response.responseId + "], " + expertId
                            + " updated from " + expertResponses.get(x).numericResponses[columnIndex] + " to " + singleValue);
                    float[] oldArr = expertResponses.get(x).numericResponses;
                    float[] newArr = new float[oldArr.length];
                    for (int k = 0; k < oldArr.length; k++) {

                        if (k == columnIndex) {
                            newArr[k] = singleValue;
                        } else {
                            newArr[k] = oldArr[k];
                        }
//                        Utils.log("-> " + newArr[k]);
                    }

                    expertResponses.get(x).numericResponses = newArr;
                    ExpertResponse r = new ExpertResponse(response, newArr);
                    expertResponses.set(x, r);
                    yetAnotherList.add(r);
//                if (!hasMissingValues(expertResponses.get(x).numericResponses))
//                    Utils.log("Updated vector: " + Arrays.toString(expertResponses.get(x).numericResponses));
                } else {
                    Utils.err("current value = " + expertResponses.get(x).numericResponses[columnIndex]);
                }
            }
        }
    }

    public static boolean expertTheSame(float f1, float f2) {
        return Integer.compare((int) (f1), (int) (f2)) == 0;
    }

    private boolean hasMissingValues(float[] numericResponses) {
        for (float f : numericResponses) if (f < 0) return true;
        return false;
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

    private void dataImputationProcedure2(List<ExpertResponse> responses, ExpertResponse expert) {
        /**iterate over columns */
        for (int column = 0; column < 4; column++) {
            //detect whether in THIS |column| is something missing
            boolean somethingMissing = false;
            //count how much values has column vector without missing values
            //int vectorSize = 0;
            int missingPerColumn = 0;
            //for further purposes
            float firstNonMissingValue = -1;
            float secondNonMissingValue = -1;
            for (ExpertResponse response : responses) {
                if (someMissing(response, column)) {
                    somethingMissing = true;
                    missingPerColumn++;
                } else {
                    if (firstNonMissingValue < 0)
                        firstNonMissingValue = response.numericResponses[column];
                    else if (firstNonMissingValue > -1 && secondNonMissingValue < 0) {
                        secondNonMissingValue = response.numericResponses[column];
                    }
                }
            }
            int vectorSize = responses.size() - missingPerColumn;
            if (somethingMissing) {
                Utils.log("column[" + column + "], vector size: " + vectorSize + ", " + responses.size());
                Utils.log("vector size: " + vectorSize + ", full: " + responses.size());

                if (vectorSize == 1) {

                    singleDataImputation(expert.expertId, firstNonMissingValue, column);
                } else if (vectorSize == 2) {
                    singleDataImputation(expert.expertId, (firstNonMissingValue
                            + secondNonMissingValue) / 2, column);
                } else {
                    //calculate polynomial regression and swap missing data with appropriate value
                    float[] Y_args = new float[vectorSize];
                    int indexOfNextNonMissing = 0;
                    for (ExpertResponse r : responses) {
                        if (r.numericResponses[column] > -1) {
                            Y_args[indexOfNextNonMissing] = r.numericResponses[column];
                            indexOfNextNonMissing++;
                            if (indexOfNextNonMissing >= responses.size() - 1) break;
                        }
                    }
                    //here we have complete vector for calculating regression
                    //additional cases: various number of points
                    float[] X_args = createArgumentVector(vectorSize);
                    Arrays.sort(Y_args);
                    PolynomialRegression regression = PolynomialRegression.create(X_args, Y_args, 2);

                    double a = regression.beta(2);
                    double b = regression.beta(1);
                    double c = regression.beta(0);
                    double minY = Y_args[0], maxY = Y_args[Y_args.length - 1];
                    double minX = new SquareEquationImpl(a, b, c, minY).minX();
                    double maxX = new SquareEquationImpl(a, b, c, maxY).minX();
                    double midX = (maxX + minX) / 2;
                    double meanValue = midX * midX * a + midX * b + c;
                    //Utils.log("left value: " + minY + ", right value: " + maxY + ", middle: " + meanValue);
                    singleDataImputation(expert.expertId, (float) meanValue, column);
                }

            } else {
                //that means that this vector is complete, do nothing
                Utils.err("_______________________________________________________________________________");
            }
        }
    }
}
