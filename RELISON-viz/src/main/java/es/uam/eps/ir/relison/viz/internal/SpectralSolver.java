/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. See https://mozilla.org/MPL/2.0/. */
package es.uam.eps.ir.relison.viz.internal;
import java.util.*;

/** Sparse orthogonal subspace iteration with small Rayleigh–Ritz solves. */
public final class SpectralSolver {
    private SpectralSolver() { }
    /** Numerical outcome whose arrays are owned by the caller. */
    public static final class Result {
        /** Unit eigenvectors, one vector per output axis. */
        public final double[][] vectors;
        /** Completed subspace sweeps. */
        public final int iterations;
        /** Whether every requested vector met the residual tolerance. */
        public final boolean converged;
        private Result(double[][] vectors,int iterations,boolean converged) { this.vectors=vectors;this.iterations=iterations;this.converged=converged; }
    }
    /**
     * Solves the lowest nonconstant eigenvectors of a connected Laplacian.
     * The combinatorial operator is divided by its maximum degree internally.
     * @param targets symmetric adjacency indices
     * @param weights symmetric nonnegative strengths aligned with targets
     * @param degree row strengths
     * @param dimensions number of output axes
     * @param normalized whether to solve the symmetric normalized Laplacian
     * @param budget maximum subspace sweeps
     * @param tolerance residual threshold on the scaled operator
     * @param random seeded initialization
     * @param checkpoint cancellation check
     * @return unit vectors and convergence information
     */
    public static Result solve(int[][] targets,double[][] weights,double[] degree,int dimensions,boolean normalized,
            int budget,double tolerance,Random random,Runnable checkpoint) {
        int n=degree.length,wanted=Math.min(dimensions,n-1),block=Math.min(n-1,wanted+4);
        if(wanted==0) return new Result(new double[0][n],0,true);
        double maximum=Arrays.stream(degree).max().orElse(1),mass=0;
        double[] constant=new double[n],diagonal=new double[n];double[][] coefficient=new double[n][];
        for(int i=0;i<n;i++) { checkpoint.run();constant[i]=normalized?Math.sqrt(degree[i]/maximum):1;mass+=constant[i]*constant[i];
            diagonal[i]=normalized?1:degree[i]/maximum;coefficient[i]=new double[weights[i].length];
            for(int e=0;e<weights[i].length;e++) coefficient[i][e]=normalized
                ?(weights[i][e]/Math.sqrt(degree[i]))/Math.sqrt(degree[targets[i][e]]):weights[i][e]/maximum;
        }
        mass=Math.sqrt(mass);for(int i=0;i<n;i++)constant[i]/=mass;
        double[][] q=new double[block][n];
        for(int k=0;k<block;k++) { for(int i=0;i<n;i++)q[k][i]=random.nextDouble()-.5;orthogonalize(q[k],q,k,constant); }
        int iterations=0;boolean converged=false;
        // Ritz evaluation is also useful with a zero sweep budget.
        while(true) {
            checkpoint.run(); double[][] lq=new double[block][];
            for(int k=0;k<block;k++)lq[k]=apply(q[k],targets,coefficient,diagonal,checkpoint);
            double[][] small=new double[block][block];
            for(int i=0;i<block;i++)for(int j=0;j<=i;j++) small[i][j]=small[j][i]=dot(q[i],lq[j]);
            Eigen eigen=jacobi(small); double[][] rotated=new double[block][n],rotatedL=new double[block][n];
            for(int k=0;k<block;k++)for(int j=0;j<block;j++)for(int i=0;i<n;i++) {
                rotated[k][i]+=eigen.vectors[j][k]*q[j][i];rotatedL[k][i]+=eigen.vectors[j][k]*lq[j][i];
            }
            q=rotated;converged=true;
            for(int k=0;k<wanted;k++) { double residual=0;for(int i=0;i<n;i++)residual=Math.hypot(residual,rotatedL[k][i]-eigen.values[k]*q[k][i]);
                converged&=residual<=tolerance; }
            if(converged||iterations>=budget)break;
            // I - L/4 is positive definite for both scaled operators (spectrum in [0,2]).
            // Its largest nonconstant eigenspace is the desired lowest Laplacian space.
            for(int k=0;k<block;k++) {
                for(int i=0;i<n;i++)q[k][i]-=.25*rotatedL[k][i];
                orthogonalize(q[k],q,k,constant);
            }
            iterations++;
        }
        double[][] result=Arrays.copyOf(q,wanted);
        for(double[] v:result) { int anchor=0;for(int i=1;i<n;i++)if(Math.abs(v[i])>Math.abs(v[anchor]))anchor=i;
            if(v[anchor]<0)for(int i=0;i<n;i++)v[i]=-v[i]; }
        return new Result(result,iterations,converged);
    }
    private static double[] apply(double[] v,int[][] targets,double[][] coefficients,double[] diagonal,Runnable check) {
        double[] result=new double[v.length];for(int i=0;i<v.length;i++) { if((i&255)==0)check.run();result[i]=diagonal[i]*v[i];
            for(int e=0;e<targets[i].length;e++)result[i]-=coefficients[i][e]*v[targets[i][e]]; }return result;
    }
    private static double dot(double[] a,double[] b) { double value=0;for(int i=0;i<a.length;i++)value+=a[i]*b[i];return value; }
    private static void orthogonalize(double[] v,double[][] basis,int count,double[] constant) {
        for(int pass=0;pass<2;pass++) { double c=dot(v,constant);for(int i=0;i<v.length;i++)v[i]-=c*constant[i];
            for(int k=0;k<count;k++) { double d=dot(v,basis[k]);for(int i=0;i<v.length;i++)v[i]-=d*basis[k][i]; } }
        double norm=0;for(double x:v)norm=Math.hypot(norm,x);
        if(norm<1e-14||!Double.isFinite(norm))throw new IllegalArgumentException("Spectral subspace lost numerical rank; choose another seed");
        for(int i=0;i<v.length;i++)v[i]/=norm;
    }
    private static final class Eigen { final double[] values;final double[][] vectors;Eigen(double[] values,double[][] vectors){this.values=values;this.vectors=vectors;} }
    private static Eigen jacobi(double[][] a) {
        int n=a.length;double[][] v=new double[n][n];for(int i=0;i<n;i++)v[i][i]=1;
        for(int sweep=0;sweep<100;sweep++) { double off=0;
            for(int p=0;p<n;p++)for(int q=p+1;q<n;q++) {
                double apq=a[p][q];off=Math.max(off,Math.abs(apq));if(Math.abs(apq)<1e-15)continue;
                double angle=.5*Math.atan2(2*apq,a[q][q]-a[p][p]),c=Math.cos(angle),s=Math.sin(angle);
                double app=a[p][p],aqq=a[q][q];a[p][p]=c*c*app-2*s*c*apq+s*s*aqq;
                a[q][q]=s*s*app+2*s*c*apq+c*c*aqq;a[p][q]=a[q][p]=0;
                for(int k=0;k<n;k++)if(k!=p&&k!=q) { double akp=a[k][p],akq=a[k][q];a[k][p]=a[p][k]=c*akp-s*akq;a[k][q]=a[q][k]=s*akp+c*akq; }
                for(int k=0;k<n;k++) { double x=v[k][p],y=v[k][q];v[k][p]=c*x-s*y;v[k][q]=s*x+c*y; }
            }
            if(off<1e-14)break;
        }
        Integer[] order=new Integer[n];for(int i=0;i<n;i++)order[i]=i;Arrays.sort(order,Comparator.comparingDouble(i->a[i][i]));
        double[] values=new double[n];double[][] vectors=new double[n][n];for(int k=0;k<n;k++){values[k]=a[order[k]][order[k]];for(int i=0;i<n;i++)vectors[i][k]=v[i][order[k]];}
        return new Eigen(values,vectors);
    }
}
