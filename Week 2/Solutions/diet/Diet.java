import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class Diet {
    public static class SubArraysOfArray {
        private int subArraySize;
        private int arraySize;
        private List<int[]> subArrays;
        private int[] array;

        //Generates all possible subArrays of size subArraySize from an array of size arraySize
        public SubArraysOfArray(int subArraySize, int arraySize) {
            this.subArraySize = subArraySize;
            this.arraySize = arraySize;
            subArrays = new ArrayList<int[]>();
            if (subArraySize < 1) {
                return;
            }

            this.array = new int[arraySize];
            for (int i = 0; i < arraySize; i++) {
                array[i] = i;
            }

            for (int i = 0; i <= arraySize - subArraySize; i++) {
                int[] combo = new int[subArraySize];
                combo[0] = array[i];
                setItem(i + 1, combo, 1);
            }
        }

        private void setItem(int pos, int[] combo, int height) {
            if (height >= this.subArraySize) {
                subArrays.add(combo);
                return;
            }
            for (int p = pos; p <= arraySize - subArraySize + height; p++) {
                int[] newcombo = combo.clone();
                newcombo[height] = array[p];
                setItem(p + 1, newcombo, height + 1);
            }
        }

        //Returns a list of all subarrays
        public List<int[]> getSubArrays() {
            return subArrays;
        }
    }

    //Separates used and unused indices
    private int[] notUsedIndices(int arraySize, int[] indices) {
        
    	if (indices.length >= arraySize)
            return new int[0];
    	
        int[] a = new int[arraySize];
        for (int i = 0; i < arraySize; i++)
            a[i] = i;
        
        
        Set<Integer> used = new HashSet<Integer>();
        for (Integer u : indices)
            used.add(u);
        
        int counter = 0;
        int[] other = new int[a.length - indices.length];
        
        for (int i = 0; i < a.length; i++)
            if (!used.contains(a[i]))
                other[counter++] = a[i];
           
        return other;
    }

    private void currForm(double[][] constraints, double[] budgets, double[][] currForm, double[] currFormBudget, int[] indices) {
        for (int i = 0; i < indices.length; i++) 
        {
            if (indices[i] < constraints.length) 
            {
                currForm[i] = Arrays.copyOf(constraints[indices[i]], constraints[i].length);
                currFormBudget[i] = budgets[indices[i]];
            } 
            else if (indices[i] == constraints.length + constraints[0].length) 
            {
                Arrays.fill(currForm[i], 1);
                currFormBudget[i] = 1000000000.0;
            } 
            else 
                currForm[i][indices[i] - constraints.length] = -1;
        }
    }

    private int solveDietProblem(int constraintNum, int vars, double constraints[][], double[] budgets, double[] pleasures, double[] x) {
        
    	Double maxVal = Double.NaN;
        
    	double[] solution = new double[vars];
    	
        List<int[]> combos = new SubArraysOfArray(vars, constraintNum + vars + 1).getSubArrays();
        for (int[] combo : combos) {
            double[][] currForm = new double[vars][vars];
            double[] currBudget = new double[vars];
            currForm(constraints, budgets, currForm, currBudget, combo);
            double[] solutionCandidate;
            
            //Simplifies the matrix if possible
            try {
                solutionCandidate = rowReduce(currForm, currBudget);
            } catch (Exception e) {
                continue;
            }

            //Updates the current form
            int[] nonUsedIndices = notUsedIndices(constraintNum + vars + 1, combo);
            currForm = new double[nonUsedIndices.length][vars];
            currBudget = new double[nonUsedIndices.length];
            currForm(constraints, budgets, currForm, currBudget, nonUsedIndices);
            
            //Checks the "pleasure" of all of the possible solutions to determine which one is most optimal
            if (verifySolution(solutionCandidate, currForm, currBudget)) {
                double maxValCandidate = maxValCandidate(solutionCandidate, pleasures);
                if (maxVal.isNaN() || maxValCandidate > maxVal) {
                    maxVal = maxValCandidate;
                    solution = Arrays.copyOf(solutionCandidate, solutionCandidate.length);
                }
            }

        }

        //Checks the case
        int NoSolution = -1;
        int BoundedSolution = 0;
        int UnboundedSolution = 1;

        //No solution
        if (maxVal.isNaN()) {
            return NoSolution;
        }
        //Infinite solutions
        if (maxVal > 999999990.0) {
            return UnboundedSolution;
        }

        //Bounded Solution
        for (int i = 0; i < vars; i++) {
            x[i] = solution[i];
        }

        return BoundedSolution;
    }

    
    //Finds the solution out of the possible solution with the maximum "pleasure" score
    private double maxValCandidate(double[] solutionCandidate, double[] pleasures) {
        double maxVal = 0;
        for (int i = 0; i < solutionCandidate.length; i++) {
            maxVal += solutionCandidate[i] * pleasures[i];
        }

        return maxVal;
    }

    //Makes sure the calculated solution values are within the constraints
    private boolean verifySolution(double[] solution, double[][] constraints, double[] budgets) {
        for (int row = 0; row < constraints.length; row++) {
            double LHS = 0;
            double RHS = budgets[row];

            for (int unknown = 0; unknown < solution.length; unknown++) 
                LHS += solution[unknown] * constraints[row][unknown];

            if (LHS > RHS) 
                return false;
        }
        return true;
    }

    //rref function
    public static double[] rowReduce(double A[][], double[] b) {
        int len = A[0].length;
        //Chooses pivot by finding the index of the largest value for that variable
        for (int p = 0; p < len; p++) 
        {
            int max = p;
            
            for (int i = p + 1; i < len; i++)
                if (Math.abs(A[i][p]) > Math.abs(A[max][p]))
                    max = i;

			//Prevents Bounded solution from resulting in an infinite solution
			if (Math.abs(A[max][p]) <= 0.000001) {
                throw new IllegalArgumentException();
            }
            double[] temp = A[p];
            A[p] = A[max];
            A[max] = temp;
            
            double t = b[p];
            b[p] = b[max];
            b[max] = t;

            for (int i = p + 1; i < len; i++) 
            {
                double alpha = A[i][p] / A[p][p];
                b[i] -= alpha * b[p];
                for (int j = p; j < len; j++) 
                    A[i][j] -= alpha * A[p][j];
            }
        }

        double[] solution = new double[len];
        for (int i = len - 1; i >= 0; i--) 
        {
            double sum = 0.0;
            for (int j = i + 1; j < len; j++)
                sum += A[i][j] * solution[j];
            
            solution[i] = (b[i] - sum) / A[i][i];
        }

        return solution;
    }
    
    //Loads all of our input into the proper places, calls a helper method to solve the equation, and then outputs the results
    private void solve() throws IOException {
        //Scan in  the matrix
    	int n = nextInt();
        int m = nextInt();
        
        //Matrix with all of our coefficients
        double[][] A = new double[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                A[i][j] = nextInt();
        
        //Matrix with what each equation is less than or equal to
        double[] b = new double[n];
        for (int i = 0; i < n; i++)
            b[i] = nextInt();
       
        //Matrix with the values we are trying to maximize
        double[] c = new double[m];
        for (int i = 0; i < m; i++)
            c[i] = nextInt();
        
        //Bounded solution values
        double[] ansx = new double[m];
        
        //Solves problem and then returns the correct case
        int sol = solveDietProblem(n, m, A, b, c, ansx);
        if (sol == -1) {
            out.printf("No solution\n");
            return;
        }
        else if (sol == 0) {
            out.printf("Bounded solution\n");
            for (int i = 0; i < m; i++) {
                out.printf("%.18f%c", ansx[i], i + 1 == m ? '\n' : ' ');
            }
            return;
        }
        else if (sol == 1) {
            out.printf("Infinity\n");
            return;
        }
    }

    public static void main(String[] args) throws IOException {
        new Diet();
    }

    Diet() throws IOException {
        br = new BufferedReader(new InputStreamReader(System.in));
        out = new PrintWriter(System.out);
        solve();
        out.close();
    }

    private BufferedReader br;
    private PrintWriter out;
    private StringTokenizer st;

    private String nextToken() {
        while (st == null || !st.hasMoreTokens()) {
            try {
                st = new StringTokenizer(br.readLine());
            } catch (Exception e) {
                return null;
            }
        }
        return st.nextToken();
    }

    private int nextInt() throws IOException {
        return Integer.parseInt(nextToken());
    }
}