/* JAT: Java Astrodynamics Toolkit
 *
 * Copyright (c) 2002 The JAT Project. All rights reserved.
 *
 * This file is part of JAT. JAT is free software; you can
 * redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */

package com.fasterlight.exo.orbit.integ;

import java.util.ArrayList;

/** Implements a Runge-Kutta-Fehlberg adaptive step size integrator
 * from Numerical Recipes. Modified to RK78 from the original RK45 in NR.
 * RK78 values from Erwin Fehlberg, NASA TR R-287
 * gets derivs via Derivatives interface
 * 
 * @author <a href="mailto:dgaylor@users.sourceforge.net">Dave Gaylor
 * @author modified by Steven Hugg
 * @version 1.0
 */

public class RungeKuttaFehlberg56 {

    private double minStepSize_;
    private double stepSize_;
    private double accuracy_;
    private double currentStepSize_;
    private boolean verbose;
    private boolean adaptive_;
    
	private double     dxsav = 0.001; 	// Minimum difference between indep. var. values to store steps
    private double[][] simStates;		// intermediate state values
    private int        nSteps;			// number of intermediate steps
    private int		   nEqns;			// number of equations
	private boolean    saveSteps = false;
	
    /** Default constructor.
     */
    public RungeKuttaFehlberg56() {
        currentStepSize_ = stepSize_ = 1.0;
        minStepSize_ = 1.2E-10;
        //        minStepSize_ = 10.0;

        accuracy_ = 1.0e-6;
        adaptive_ = true;
    }

    /** Construct a RungeKuttaFehlberg78 integrator with user specified accuracy.
     * @param accuracy Desired accuracy.
     */
    public RungeKuttaFehlberg56(double accuracy) {
        currentStepSize_ = stepSize_ = 1.0;
        minStepSize_ = 1.2E-10;
        //        minStepSize_ = 10.0;

        accuracy_ = 1.0e-6;
        adaptive_ = true;
    }

    /** Set the step size.
     * @param stepSize Step size.
     */
    public void setStepSize(double stepSize) {
        this.stepSize_ = stepSize;
    }

    /** Set the minimum step size.
     * @param stepSize Minimum step size.
     */
    public void setMinimumStepSize(double stepSize) {
        this.minStepSize_ = stepSize;
    }

    /** Set the integrator to adaptive step size mode.
     */
    public void setAdaptive() {
        adaptive_ = true;
    }

    /** Set the integrator to fixed step size mode.
     */
    public void setNonAdaptive() {
        adaptive_ = false;
    }

    /** Set the accuracy.
     * @param accuracy Desired accuracy.
     */
    public void setAccuracy(double accuracy) {
        this.accuracy_ = accuracy;
    }

    /** Set the verbose mode to true or false.
     * @param verbose Verbose flag.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /** Set the integrator to verbose mode.
     */
    public void setVerbose() {
        verbose = !verbose;
    }

    private int[] nok = new int[1];
    private int[] nbad = new int[1];

    /** Integrate the equations of motion. No printing/plotting interface provided.
     * @param start Initial time.
     * @param x0 Initial state values.
     * @param end Final time.
     * @param dv Equations of Motion.
     * @param pr Printable.
     * @param print_switch Print flag. True = call the print method.
     * @return the final state.
     */
    public double[] integrate(long t0, double start, double[] x0, double end, Derivatives dv) {
    	double[] y=(double[]) x0.clone();
    	boolean print_switch = false;
        double h = stepSize_;
        double hmin = minStepSize_;
        nok[0] = nbad[0] = 0;
        if (adaptive_) {
            odeint(y, t0, start, end, accuracy_, h, hmin, nok, nbad, dv, print_switch);
            if (verbose) {
                System.out.println("nok = " + nok[0] + "\tnbad = " + nbad[0]);
            }
        } else {
            rkdumb(y, t0, start, end, h, dv, print_switch);
        }
        return y;
    }


    private void rkdumb(double[] ystart, long t0, double start, double end,
    double h, Derivatives dv, boolean print_switch) {
        int nvar = ystart.length;

        int nSteps = (int) Math.abs((end - start) / h);
        if (nSteps < 1)
            nSteps = 1;
        h = (end - start) / nSteps;
        double[] dydx = new double[nvar];
        double[] yend = new double[nvar];
        double[] yerr = new double[nvar];
        for (int step = 0; step < nSteps; step++) {
            double x = start + step * h;
            dydx = dv.derivs(t0, x, ystart);
            rkck(ystart, dydx, t0, x, h, yend, yerr, dv);
            for (int n = 0; n < nvar; n++){
                ystart[n] = yend[n];
            }
        }
    }


    private static final int MAXSTP = 1000000;
    private static final double TINY = 1.0e-30;

    private void odeint(double[] ystart, long t0, double x1, double x2,
    double eps, double h1, double hmin, int[] nok,
    int[] nbad, Derivatives dv, boolean print_switch) {
        int nvar 		= ystart.length;
        double[] x 		= new double[1];
        double[] hnext 	= new double[1];
        double[] hdid 	= new double[1];
        double[] yscal 	= new double[nvar];
        double[] y 		= new double[nvar];
        double[] dydx 	= new double[nvar];
        double h 		= Math.abs(h1);
        double xsav 	= 0;  		// last time stored
		ArrayList steps = new ArrayList();
        
        x[0] = x1;
        if (x2 < x1)
            h = -h;
        nok[0] = nbad[0] = 0;
        for (int i = 0; i < nvar; i++)
            y[i] = ystart[i];
        
        // save initial values
        if (saveSteps){
            xsav = x[0] - dxsav * 2.0;
            store_step(steps, nvar, x[0], y);
            nEqns = nvar;
        }
        
        for (int nstp = 1; nstp <= MAXSTP; nstp++) {
            dydx = dv.derivs(t0, x[0], y);
            for (int i = 0; i < nvar; i++)
                yscal[i] = Math.abs(y[i]) + Math.abs(dydx[i] * h) + TINY;
            
            if ( (x[0] + h - x2) * (x[0] + h - x1) > 0.0)
                h = x2 - x[0];
            rkqs(y, dydx, t0, x, h, eps, yscal, hdid, hnext, dv);
            if (hdid[0] == h)
                ++nok[0];
            else
                ++nbad[0];

            // save intermediate values
            if (saveSteps && Math.abs(x[0] - xsav) > Math.abs(dxsav) ){
                store_step(steps, nvar, x[0], y);
                xsav = x[0];
            }
            
            if ( (x[0] - x2) * (x2 - x1) >= 0.0 ) {   // are we done?
                // save off the data
                for (int i = 0; i < nvar; i++){
                    ystart[i] = y[i];
                }
                if (saveSteps) {
                    simStates = convertIntermediateValues(steps, nvar);
                }
                return;
            }
            if (Math.abs(hnext[0]) <= hmin) {
                error("Step size too small in odeint");
                System.out.println("h = "+hnext[0]);
            }
            h = hnext[0];
            currentStepSize_ = h;            // added for comphys
            //            System.out.println("Current Step Size = "+h);
        }
        error("Too many steps in routine odeint");
        System.out.println("step size = "+currentStepSize_);
    }



    private static final double SAFETY = 0.9;
    private static final double PGROW = -1.0/8.0;
    private static final double PSHRNK = -1.0/7.0;
    private static final double ERRCON = 2.56578451395034701E-8;

    public void rkqs(double[] y, double[] dydx, long t0, double[] x,
    double htry, double eps, double[] yscal,
    double[] hdid, double[] hnext, Derivatives dv) {
        int n = y.length;
        double errmax = 0;
        double[] yerr = new double[n];
        double[] ytemp = new double[n];
        double h = htry;
        for (;;) {
            rkck(y, dydx, t0, x[0], h, ytemp, yerr, dv);
            errmax = 0;
            for (int i = 0; i < n; i++)
                errmax = Math.max(errmax, Math.abs(yerr[i] / yscal[i]));
            errmax /= eps;
            if (errmax <= 1.0)
                break;
            double htemp = SAFETY * h * Math.pow(errmax, PSHRNK);
            h = (h >= 0.0 ? Math.max(htemp, 0.1*h) : Math.min(htemp, 0.1*h));
            double xnew = x[0] + h;
            if (xnew == x[0])
                error("stepsize underflow in rkqs");
        }
        if (errmax > ERRCON)
            hnext[0] = SAFETY * h * Math.pow(errmax, PGROW);
        else
            hnext[0] = 5.0 * h;
        x[0] += (hdid[0] = h);
        for (int i = 0; i < n; i++)
            y[i] = ytemp[i];
    }

    private static final double [] a = { 0, 1d/6d, 4d/15d, 2d/3d, 4d/5d, 1, 0, 1 };

    private static final double [][] b = new double [8][7];
    static {
        b[1][0] = 1d/6d;
        b[2][0] = 4d/75d;
        b[2][1] = 16d/75d;
        b[3][0] = 5d/6d;
        b[3][1] = -8d/3d;
        b[3][2] = 5d/2d;
        b[4][0] = -8d/5d;
        b[4][1] = 144d/25d;
        b[4][2] = -4d;
        b[4][3] = 16d/25d;
        b[5][0] = 361d/320d;
        b[5][1] = -18d/5d;
        b[5][2] = 407d/128d;
        b[5][3] = -11d/80d;
        b[5][4] = 55d/128d;
        b[6][0] = -11d/640d;
        b[6][2] = 11d/256d;
        b[6][3] = -11d/160d;
        b[6][4] = 11d/256d;
        b[7][0] = 93d/640d;
        b[7][1] = -18d/5d;
        b[7][2] = 803d/256d;
        b[7][3] = -11d/160d;
        b[7][4] = 99d/256d;
        b[7][6] = 1;
    }

    private static final double [] c = {31d/384d, 0, 1125d/2816d, 9d/32d, 125d/768d, 5d/66d};

    private static final double [] chat = {7d/1408d, 0, 1125d/2816d, 9d/32d, 125d/768d, 0, 5d/66d, 5d/66d };

    public void rkck(double[] y, double[] dydx, long t0, double x, double h,
    double[] yout, double[] yerr, Derivatives dv) {

        int n = y.length;

        double f[][] = new double[8][n];

        //	   double y7th[] = new double[n];
        double ytmp[] = new double[n];

        //	   System.out.println("step size = "+h);

        double xeval[] = new double [8];
        for (int i = 0; i < 8; i++)  // find times for function evals
        {
            xeval[i] = x + a[i] * h;
        }

        // build f matrix
        //	   f[0] = derivs.derivs(x, y);
        f[0] = dydx;

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * b[1][0] * f[0][i];
        }
        f[1] = dv.derivs(t0, xeval[1], ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * (b[2][0]*f[0][i] + b[2][1]*f[1][i]);
        }
        f[2] = dv.derivs(t0, xeval[2], ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * (b[3][0]*f[0][i] + b[3][1]*f[1][i]+ b[3][2]*f[2][i]);
        }
        f[3] = dv.derivs(t0, xeval[3], ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * (b[4][0]*f[0][i] + b[4][1]*f[1][i] + b[4][2]*f[2][i] + b[4][3]*f[3][i]);
        }
        f[4] = dv.derivs(t0, xeval[4], ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * (b[5][0]*f[0][i] + b[5][1]*f[1][i] + b[5][2]*f[2][i] + b[5][3]*f[3][i] + b[5][4]*f[4][i]);
        }
        f[5] = dv.derivs(t0, xeval[5], ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * (b[6][0]*f[0][i] + b[6][1]*f[1][i] + b[6][2]*f[2][i] + b[6][3]*f[3][i] + b[6][4]*f[4][i] + b[6][5]*f[5][i]);
        }
        f[6] = dv.derivs(t0, xeval[6], ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * (b[7][0]*f[0][i] + b[7][1]*f[1][i] + b[7][2]*f[2][i] + b[7][3]*f[3][i] + b[7][4]*f[4][i] + b[7][5]*f[5][i] + b[7][6]*f[6][i]);
        }
        f[7] = dv.derivs(t0, xeval[7], ytmp);

        // construct solutions
        // yout is the 8th order solution

        for (int i = 0; i < n; i++) {
            //		  y7th[i] = y[i] + h*(c[0]*f[0][i] +c[5]*f[5][i] + c[6]*f[6][i] + c[7]*f[7][i] + c[8]*f[8][i] + c[9]*f[9][i] + c[10]*f[10][i]);
            yout[i] = y[i] + h*(chat[5]*f[5][i] + chat[6]*f[6][i] + chat[7]*f[7][i] + chat[8]*f[8][i] + chat[9]*f[9][i] + chat[11]*f[11][i] + chat[12]*f[12][i]);
            yerr[i] = h*c[0]*(f[11][i] + f[12][i] - f[0][i] - f[10][i]);
        }
    }

	/**
	 * Store one integration step in an ArrayList
	 * @param steps The ArrayList to store the step into
	 * @param nvar dimension of state vector
	 * @param x independent variable 
	 * @param y state vector
	 * @param ntry number of steps to achieve requested accuracy
	 */
	private void store_step(ArrayList steps, int nvar, double x, double[] y)
	{
		double[] step = new double[nvar + 1];
		step[0] = x;
		for (int i = 0; i < nvar; i++)
			step[i + 1] = y[i];
		steps.add(step);
	}

	/**
	 * Convert an ArrayList to a two-dimensional double array and time vector
	 * @param steps the ArrayList
	 * @param nvar dimension of state vector
	 * @return
	 */
	private double[][] convertIntermediateValues(ArrayList steps, int nvar)
	{
		nSteps = steps.size();
		double[][] steps_array = new double[nSteps][nvar + 1];
		for (int i = 0; i < nSteps; i++)
		{
			double[] step = (double[]) steps.get(i);
			for (int j = 0; j < step.length; j++)
			{
				steps_array[i][j] = step[j];
			}
		}
		return steps_array;
	}

    public void print(double t, double [] y){
        // do nothing
    }

    private void error(String msg) {
        System.err.println("RungeKuttaFehlberg78: " + msg);
    }
    
    public double[] getIntermediateTimes(){
    	double[] simTimes = new double[nSteps];
    	for(int i=0; i<nSteps; i++)
    		simTimes[i] = simStates[i][0];
    	return simTimes;
    }

    public double[][] getIntermediateStates(){
    	double[][] onlyStates = new double[nSteps][nEqns];
    	for(int i=0; i<nSteps; i++){
    		for( int j=0; j<nEqns; j++ ){
    			onlyStates[i][j] = simStates[i][j+1];
    		}
    	}
    	return onlyStates;
    }
    
    public double[][] getIntermediateValues(){
    	return simStates;
    }
    
    public void setSaveSteps(boolean value){
    	saveSteps = value;
    }
}

