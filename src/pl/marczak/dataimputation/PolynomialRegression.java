package pl.marczak.dataimputation;

/**
 * @author Lukasz
 * @since 09.06.2016.
 */

import Jama.Matrix;
import Jama.QRDecomposition;

public class PolynomialRegression {
    private final int N;                // number of observations
    private final String variableName;  // name of the predictor variable
    private int degree;                 // degree of the polynomial regression
    private Matrix beta;                // the polynomial regression coefficients
    private double SSE;                 // sum of squares due to error
    private double SST;                 // total sum of squares


    /**
     * Performs a polynomial reggression on the data points <tt>(y[i], x[i])</tt>.
     * Uses N as the name of the predictor variable.
     *
     * @param x      the values of the predictor variable
     * @param y      the corresponding values of the response variable
     * @param degree the degree of the polynomial to fit
     * @throws IllegalArgumentException if the lengths of the two arrays are not equal
     */
    public PolynomialRegression(double[] x, double[] y, int degree) {
        this(x, y, degree, "N");
    }

    public static PolynomialRegression create(float[] x, float[] y, int degree) {
        double[] dX = new double[x.length];
        double[] dY = new double[x.length];
        for (int j = 0; j < x.length; j++) {
            dX[j] = (double) x[j];
            dY[j] = (double) y[j];
        }
        return new PolynomialRegression(dX, dY, degree);
    }

    /**
     * Performs a polynomial reggression on the data points <tt>(y[i], x[i])</tt>.
     *
     * @param x            the values of the predictor variable
     * @param y            the corresponding values of the response variable
     * @param degree       the degree of the polynomial to fit
     * @param variableName the name of the predictor variable
     * @throws IllegalArgumentException if the lengths of the two arrays are not equal
     */
    public PolynomialRegression(double[] x, double[] y, int degree, String variableName) {
        this.degree = degree;
        this.variableName = variableName;

        N = x.length;

        // in case Vandermonde matrix does not have full rank, reduce degree until it does
        boolean done = false;
        while (!done) {

            // build Vandermonde matrix
            double[][] vandermonde = new double[N][this.degree + 1];
            for (int i = 0; i < N; i++) {
                for (int j = 0; j <= this.degree; j++) {
                    vandermonde[i][j] = Math.pow(x[i], j);
                }
            }
            Matrix X = new Matrix(vandermonde);

            // create matrix from vector
            Matrix Y = new Matrix(y, N);

            // find least squares solution
            QRDecomposition qr = new QRDecomposition(X);

            // decrease degree and try again
            if (!qr.isFullRank()) {
                this.degree--;
                continue;
            }

            // linear regression coefficients
            beta = qr.solve(Y);

            // mean of y[] values
            double sum = 0.0;
            for (int i = 0; i < N; i++)
                sum += y[i];
            double mean = sum / N;

            // total variation to be accounted for
            for (int i = 0; i < N; i++) {
                double dev = y[i] - mean;
                SST += dev * dev;
            }

            // variation not accounted for
            Matrix residuals = X.times(beta).minus(Y);
            SSE = residuals.norm2() * residuals.norm2();
            break;
        }
    }

    /**
     * Returns the <tt>j</tt>th regression coefficient.
     *
     * @param j the index
     * @return the <tt>j</tt>th regression coefficient
     */
    public double beta(int j) {
        // to make -0.0 print as 0.0
        if (Math.abs(beta.get(j, 0)) < 1E-4) return 0.0;
        return beta.get(j, 0);
    }

    /**
     * Returns the degree of the polynomial to fit.
     *
     * @return the degree of the polynomial to fit
     */
    public int degree() {
        return degree;
    }

    /**
     * Returns the coefficient of determination <em>R</em><sup>2</sup>.
     *
     * @return the coefficient of determination <em>R</em><sup>2</sup>,
     * which is a real number between 0 and 1
     */
    public double R2() {
        if (SST == 0.0) return 1.0;   // constant function
        return 1.0 - SSE / SST;
    }

    /**
     * Returns the expected response <tt>y</tt> given the value of the predictor
     * variable <tt>x</tt>.
     *
     * @param x the value of the predictor variable
     * @return the expected response <tt>y</tt> given the value of the predictor
     * variable <tt>x</tt>
     */
    public double predict(double x) {
        // horner's method
        double y = 0.0;
        for (int j = degree; j >= 0; j--)
            y = beta(j) + (x * y);
        return y;
    }

    /**
     * Returns a string representation of the polynomial regression model.
     *
     * @return a string representation of the polynomial regression model,
     * including the best-fit polynomial and the coefficient of
     * determination <em>R</em><sup>2</sup>
     */
    public String toString() {
        StringBuilder s = new StringBuilder();
        int j = degree;

        // ignoring leading zero coefficients
        while (j >= 0 && Math.abs(beta(j)) < 1E-5)
            j--;

        // create remaining terms
        while (j >= 0) {
            if (j == 0) s.append(String.format("%.2f ", beta(j)));
            else if (j == 1) s.append(String.format("%.2f %s + ", beta(j), variableName));
            else s.append(String.format("%.2f %s^%d + ", beta(j), variableName, j));
            j--;
        }
        s = s.append("  (R^2 = ").append(String.format("%.3f", R2())).append(")");
        return s.toString();
    }

    /**
     * Unit tests the <tt>PolynomialRegression</tt> data type.
     */
    public static void main(String[] args) {
        double[] x = {10, 20, 40, 80, 160, 200};
        double[] y = {100, 350, 1500, 6700, 20160, 40000};
        PolynomialRegression regression = new PolynomialRegression(x, y, 3);
        Utils.log(regression.toString());
    }
}
