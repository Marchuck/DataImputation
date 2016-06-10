package pl.marczak.dataimputation;

import java.util.Arrays;

/**
 * @author Lukasz
 * @since 08.06.2016.
 */
public class ExpertResponse {
    /**
     * Id of selected response
     * for example: 1, 2.1, 8.3
     */
    public String surveyId;
    public float responseId;
    /**
     * Id of an expert
     * for example 533, 331
     */
    public float expertId;
    /**
     * Coefficient how an expert is competent
     * Value from range [0, 1)
     */
    public float competentIndex;
    /**
     * Coefficient how an expert estimates the foresee himself
     * Value from range [0, 1)
     */
    public float selfEsteemIndex;
    /**
     * Array of given responses (for Delphic foresees: 4 values)
     * Missing values are marked by NaN (for example, char 'X")
     */
    public String[] expertResponses;
    public float[] numericResponses;


    public ExpertResponse() {
    }

    public ExpertResponse(ExpertResponse r) {
        surveyId = r.surveyId;
        responseId = r.responseId;
        expertId = r.expertId;
        competentIndex = r.competentIndex;
        selfEsteemIndex = r.selfEsteemIndex;
        expertResponses = r.expertResponses;
        numericResponses = r.numericResponses;
    }

    public ExpertResponse(ExpertResponse r, float[] newArr) {
        surveyId = r.surveyId;
        responseId = r.responseId;
        expertId = r.expertId;
        competentIndex = r.competentIndex;
        selfEsteemIndex = r.selfEsteemIndex;
        expertResponses = r.expertResponses;
        numericResponses = newArr;
    }


    public String valuesOnly() {
        return responseId + ", "
                + expertId + ", \t"
                + Arrays.toString(numericResponses).replace("[", "").replace("]", "") + ",";

    }

    @Override
    public String toString() {
        return +responseId + ", "
                + expertId + ", "
                + competentIndex + ", "
                + selfEsteemIndex + ", "
                + Arrays.toString(numericResponses).replace("[", "").replace("]", "") + ",";
    }

    public ExpertResponse with(float singleValue, int columnIndex) {
        numericResponses[columnIndex] = singleValue;
        Utils.log("update here");
        return this;
    }


//    public _ExpertResponse clone()  {
//
//        _ExpertResponse response =new _ExpertResponse();
//        response.surveyId = surveyId;
//        response.responseId = responseId;
//        response.expertId = expertId;
//        response.competentIndex = competentIndex;
//        response.selfEsteemIndex = selfEsteemIndex;
//        response.expertResponses = expertResponses;
//        response.numericResponses = numericResponses;
//        return response;
//    }
}
