package pl.marczak.dataimputation;

import static pl.marczak.dataimputation.CSVCreator.MISSING_VALUE;
import static pl.marczak.dataimputation.CSVCreator.containsABCDEFGHI;

/**
 * @author Lukasz
 * @since 09.06.2016.
 */
public class ExpertResponseFactory {
    public static ExpertResponse create(String inputLine){
        return createResponse(inputLine);
    }
    private static ExpertResponse createResponse(String inputLine) {

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
}
