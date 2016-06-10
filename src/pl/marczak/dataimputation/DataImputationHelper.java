package pl.marczak.dataimputation;

import java.util.List;
import java.util.Set;

/**
 * @author Lukasz
 * @since 09.06.2016.
 */
public class DataImputationHelper {
    public void appendSurveyIds(Set<Float> surveyIds) {
        for (float j = 1; j < 19; j++) {
            for (int k = 0; k < 10; k++) {
                for (int l = 0; l < 10; l++) {
                    for (int m = 0; m < 10; m++) {
                        float f = (1000 * j + 100 * k + 10 * l + m) / 1000;
                        Utils.log("f = " + f);
                        surveyIds.add(f);
                    }
                }
            }
        }
    }

    /**
     * @param capacity
     * @return array of items: [1,2,..,capacity]
     */
    public static float[] createArgumentVector(int capacity) {
        float[] out = new float[capacity];
        for (int j = 0; j < capacity; j++) {
            out[j] = j + 1;
        }
        return out;
    } public static float[] wrapToArray(List<Float> notMissed1) {
        float[] a = new float[notMissed1.size()];
        for (int j = 0; j < notMissed1.size(); j++) {
            a[j] = notMissed1.get(j);
        }
        return a;
    }
}
