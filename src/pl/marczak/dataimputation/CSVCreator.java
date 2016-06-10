package pl.marczak.dataimputation;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Lukasz
 * @since 21.05.2016.
 */
public class CSVCreator {

    public static final String MISSING_VALUE = "X";

    private static final String[] arr = new String[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"};

    public static void main(String[] args) {
        long timeStart = System.nanoTime();
        new CSVCreator().runAll();
        long timeEnd = System.nanoTime();
        long diff = (timeEnd - timeStart);
        Utils.log("Time elapsed = " + diff + " ns = " + diff / 1000 + " ms");
    }


    public void runAll() {
        List<ExpertResponse> expertResponses = new ArrayList<>();
        DataImputationHelper helper = new DataImputationHelper();
        String[] filenames = new String[]{
                "dzial_1_1.csv",
                "dzial_2_1_.csv",
                "dzial_2_2_.csv",
                "dzial_2_3_.csv",
                "dzial_2_8_.csv",
                "dzial_2_9_.csv",
                "dzial_2_10_.csv",
                "dzial_2_11_.csv",
                "dzial_2_12_.csv",
                "dzial_2_13_.csv",
                "dzial_2_14_.csv",
        };
        for (String filename : filenames) {
            Utils.log("Processing  " + filename);
            appendExpertsResponses(filename, expertResponses);
        }
        Utils.log("Summary\nResponses count:  " + expertResponses.size());
        /**
         * HERE WE CREATED NOT NORMALIZED EXPERT RESPONSES
         */

        Set<Float> surveyIds = new HashSet<>();
        helper.appendSurveyIds(surveyIds);
        List<Float> surveyIds1 = new ArrayList<>();
        surveyIds1.addAll(surveyIds);
        Collections.sort(surveyIds1);

        for (float f : surveyIds1) {
            Utils.log(String.format("%.2f", f));
        }
        String expression = "[0-9]*";
        Pattern pattern = Pattern.compile(expression);

        /**
         * CALCULATE NORMS HERE
         */
        //first value: surveyId, second value: survey norm
        Map<Float, Float> norms = new HashMap<>();
        for (Float surveyId : surveyIds1) {
            //all survey responses with given surveyId
            List<Float> valuesOfXthSurvey = new ArrayList<>();
            //iterate over all responses and add to
            for (ExpertResponse response : expertResponses) {
//                Utils.log("survey id = " + surveyId1 + ",,, " + response.responseId);
                if (isTheSame(response.responseId, surveyId)) {
                    for (String s : response.expertResponses) {
                        if (!s.equalsIgnoreCase(MISSING_VALUE)) {
                            if (s.contains("E") && s.length() < 10) {
                                float f = fixedEFloat(s);
                                if (f != -1) valuesOfXthSurvey.add(f);
                            } else {
                                Matcher matcher = pattern.matcher(s.replaceAll("%", ""));
                                if (matcher.matches()) valuesOfXthSurvey.add(Float.valueOf(s.replaceAll("%", "")));
                            }
                        }
                    }
                }
            }
            if (valuesOfXthSurvey.isEmpty()) {
                continue;
            }
            Collections.sort(valuesOfXthSurvey);
            float firstQ = valuesOfXthSurvey.get(valuesOfXthSurvey.size() / 4);
            float thirdQ = valuesOfXthSurvey.get(3 * valuesOfXthSurvey.size() / 4);
            float quartilDifference = Math.abs(firstQ - thirdQ);
            norms.put(surveyId, quartilDifference);
        }
        /**
         * NORMALIZATION HERE
         */
        Set<Float> uniqueIds = new HashSet<>();
        for (int j = 0; j < expertResponses.size(); j++) {
            final ExpertResponse expertWithMissingValues = expertResponses.get(j);
            String[] values = expertWithMissingValues.expertResponses;
            float surveyId = expertWithMissingValues.responseId;

            final String[] normalizedResponses = new String[values.length];
            for (int k = 0; k < 4; k++) {
                String val = values[k];

                if (!val.equalsIgnoreCase(MISSING_VALUE)) {
                    float normalizedValue;
                    if (val.contains("E")) {
                        float candidateFloat = fixedEFloat(val);
//                        if (candidateFloat == -1) continue;
                        normalizedValue = candidateFloat / norms.get(surveyId);
                    } else {
                        if (norms.get(surveyId) == null)
                            Utils.err("empty value for key: " + surveyId);
                        normalizedValue = Float.valueOf(val) / norms.get(surveyId);
                    }
                    normalizedResponses[k] = String.valueOf(normalizedValue);
                } else {
                    normalizedResponses[k] = MISSING_VALUE;
                }
            }
            Utils.log("expertResponse[" + j + "]: \t" + Arrays.toString(normalizedResponses));
            ExpertResponse r1 = new ExpertResponse();
            r1.responseId = expertWithMissingValues.responseId;
            r1.expertId = expertWithMissingValues.expertId;
            r1.surveyId = expertWithMissingValues.surveyId;
            r1.competentIndex = expertWithMissingValues.competentIndex;
            r1.selfEsteemIndex = expertWithMissingValues.selfEsteemIndex;
            r1.expertResponses = normalizedResponses;
            expertResponses.set(j, r1);
        }

        uniqueIds.clear();
        Utils.log("Expert responses size: " + expertResponses.size());
        for (int j = 0; j < expertResponses.size(); j++) {
            ExpertResponse expertWithMissingValues = expertResponses.get(j);
            if (containsMissingValues(expertWithMissingValues) && !uniqueIds.contains(expertWithMissingValues.expertId)) {
//                Utils.log("expertResponse[" + j + "]: " + expertResponse.valuesOnly());
                uniqueIds.add(expertWithMissingValues.expertId);
                Utils.log("\n\n");
//                printExpertAllResponses(expertWithMissingValues, expertResponses);
                List<ExpertResponse> responses = getExpertResponses(expertWithMissingValues, expertResponses);

                for (int l = 0; l < 4; l++) {
                    boolean allMissing = true;
                    for (ExpertResponse response : responses) {
                        if (!response.expertResponses[l].equalsIgnoreCase(MISSING_VALUE)) {
                            allMissing = false;
                        }
                    }
                    if (allMissing) expertResponses.removeAll(responses);
                }
            }
        }
        Utils.log("Expert responses size: " + expertResponses.size());
        /***
         * SAVE EXPERT FORESEES HERE
         */
        PrintWriter writer = null;
        try {
            writer = new PrintWriter("ankiet5K.csv", "UTF-8");
        } catch (FileNotFoundException | UnsupportedEncodingException x) {

        } finally {
            if (writer != null) {
                for (ExpertResponse response : expertResponses) {
                    writer.println(response.toString().replaceAll(MISSING_VALUE, "-1"));
                }
            }
        }
        writer.close();
    }

    public static boolean isTheSame(float f1, float f2) {
        return Integer.compare((int) (1000 * f1), (int) (1000 * f2)) == 0;
    }

    /**
     * Fixes float values given as Strings: "4,00E+06"
     *
     * @param val
     * @return
     */
    private float fixedEFloat(String val) {
        Utils.log("contains E: " + val);
        String s1 = val.replace(",", ".").replace("+0", "");
        String[] dd = s1.split("E");
        if (dd.length == 0 || dd.length == 1) return -1;
        Utils.log(dd[0] + "__" + dd[1]);

        float val1 = (float) (Float.valueOf(dd[0]) * Math.pow(10, Float.valueOf(dd[1])));
        return val1;
    }

    public static List<ExpertResponse> getExpertResponses(ExpertResponse expertResponse, List<ExpertResponse> expertResponses) {
        List<ExpertResponse> outputResponses = new ArrayList<>();

        for (int j = 0; j < expertResponses.size(); j++) {
            ExpertResponse response = expertResponses.get(j);
            if (response.expertId == expertResponse.expertId) {
                outputResponses.add(response);
            }
        }
        return outputResponses;
    }

    private void printExpertAllResponses(ExpertResponse expertResponse, List<ExpertResponse> expertResponses) {

        Utils.log("***Printing expert [" + expertResponse.expertId + "] responses***");

        for (int j = 0; j < expertResponses.size(); j++) {
            ExpertResponse response = expertResponses.get(j);
            if (response.expertId == expertResponse.expertId) {

                Utils.log("expertResponse[" + j + "]: " + response.valuesOnly());
            }
        }
    }

    public static boolean containsMissingValues(ExpertResponse expertResponse) {
        return containsMissingValues(expertResponse,MISSING_VALUE);
    }
    public static boolean containsMissingValues(ExpertResponse expertResponse,String value) {
        for (String s : expertResponse.expertResponses) {
            if (s.equals(value)) return true;
        }
        return false;
    }

    private void appendExpertsResponses(String filename, List<ExpertResponse> expertResponseList) {
        File file = new File(filename);
        Scanner input = null;
        try {
            input = new Scanner(file);
            while (input.hasNextLine()) {
                ExpertResponse expertResponse = ExpertResponseFactory.create(input.nextLine());
                if (expertResponse == null) continue;
                expertResponseList.add(expertResponse);
            }
        } catch (FileNotFoundException e) {
            System.err.println("Error! " + e.getMessage());
            e.printStackTrace();
            throw new NullPointerException("Input scanner broken!");
        } finally {
            if (input != null) input.close();
        }
    }

    private ExpertResponse createResponse(String inputLine) {

        ExpertResponse expertResponse = new ExpertResponse();
        char firstChar = inputLine.charAt(0);
        String[] values = inputLine.split(";");
        if (values[0].isEmpty()) return null;

        if (firstChar == '"') {
            String[] spl = inputLine.split("\"");
            String fullResponseId = spl[1];
            expertResponse.surveyId = fullResponseId;

            //  String secondValue = spl[2];
            //string detected, there should be expert ID!!!
            // if (secondValue.length() > 3) return null;

            //contains some of letters a,b,c,d,e,f,
            if (fullResponseId.length() == 6) {
                int indx = containsABCDEFGHI(fullResponseId);
//                String s11 = fullResponseId.substring(0, 3) + " plus " + (1 + indx);
                String id = String.valueOf(Float.valueOf(fullResponseId.substring(0, 3) + 1 + indx)); //transform [12.1 c] -> [12.13]
//                Utils.log(s11 + " = " + id);
                expertResponse.responseId = Float.valueOf(id);
            } else if (fullResponseId.length() == 7) {
                int indx = containsABCDEFGHI(fullResponseId);
//                String s11 = fullResponseId.substring(0, 4) + " plus " + (1 + indx);
                String id = String.valueOf((Float.valueOf(fullResponseId.substring(0, 4)) + 1 + indx));//transform [12.11 d] -> [12.114]
//                Utils.log(s11 + " = " + id);

                expertResponse.responseId = Float.valueOf(id);
            } else if (fullResponseId.length() == 3) {
                expertResponse.responseId = Float.valueOf(fullResponseId);
            } else {
                return null;
            }
        } else {
            expertResponse.surveyId = values[0];
            expertResponse.responseId = Float.valueOf(values[0]);
        }

        String secondValue = values[1];
        if (secondValue.length() > 3) {
            //string detected, there should be expert ID!!!
            return null;
        }
        expertResponse.expertId = Integer.valueOf(values[1]);
        expertResponse.competentIndex = Float.valueOf(values[2].replaceAll(",", "."));
        expertResponse.selfEsteemIndex = Float.valueOf(values[3].replaceAll(",", "."));
        expertResponse.expertResponses = new String[]{
                values[5].isEmpty() ? MISSING_VALUE : values[5].replaceAll("\"", ""),
                values[6].isEmpty() ? MISSING_VALUE : values[6].replaceAll("\"", ""),
                values[7].isEmpty() ? MISSING_VALUE : values[7].replaceAll("\"", ""),
                values[8].isEmpty() ? MISSING_VALUE : values[8].replaceAll("\"", ""),
        };
        Utils.log("expert: " + expertResponse.toString());
        return expertResponse;
    }

    public static int containsABCDEFGHI(String s) {
//        System.out.println("input: '" + s + "'");
        for (int k = 0; k < arr.length; k++) {
//            System.out.println("if " + s + " contains " + arr[k] + " X");
            if (s.contains(arr[k])) {
                return k;
            }
        }
        return -1;
    }
}
