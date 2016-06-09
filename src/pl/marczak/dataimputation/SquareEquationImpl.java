package pl.marczak.dataimputation;

/**
 * @author Lukasz
 * @since 09.06.2016.
 */
public class SquareEquationImpl {
    public double x1, x2;

    public SquareEquationImpl(double a, double b, double c, double y) {

        double dwaA = 2 * a;
        double delta = b * b - 2 * dwaA * (c - y);
        if (Double.compare(delta, 0) == 0) {
            x1 = x2 = -b / dwaA;
        } else if (delta > 0) {
            double sqrtD = Math.sqrt(delta);
            x1 = (-b - sqrtD) / dwaA;
            x2 = (-b + sqrtD) / dwaA;
        }
    }

    public double minX() {
        return x1 < x2 ? x1 : x2;
    }
}
