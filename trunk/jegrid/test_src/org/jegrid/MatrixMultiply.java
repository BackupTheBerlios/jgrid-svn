package org.jegrid;

/**
 * <br> User: jdavis
 * Date: Oct 14, 2006
 * Time: 1:37:15 PM
 */
public class MatrixMultiply
{
    public static void main(String[] args)
    {
        double[][] a = {
                {3, 0},
                {11, -8},
        };
        double[][] b = {
                {7, 8, -1},
                {2, 1, 66},
        };

        double[][] x = {
                {21, 24, -3},
                {61, 80, 539},
        };

        int rows = a.length;
        int columns = b[0].length;
        double[][] m = new double[rows][columns];

        // for each row in a...
        for (int i = 0; i < rows; i++)
        {
            // for each column in b...
            for (int j = 0; j < b[0].length; j++)
            {
                // for each column in a...
                for (int k = 0; k < a[0].length; k++)
                {
                    // i is the row in a
                    // j is the column in b
                    // k is the column in a, the row in b
                    m[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        for (int i = 0; i < rows; i++)
        {
            System.out.print("{ ");
            for (int j = 0; j < columns; j++)
            {
                if (j != 0)
                    System.out.print(" , ");
                System.out.print(m[i][j]);
            }
            System.out.println(" }");
        }
    }
}
