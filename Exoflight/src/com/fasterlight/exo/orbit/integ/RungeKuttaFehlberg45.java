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


/** Implements a Runge-Kutta-Fehlberg adaptive step size integrator
 * from Numerical Recipes. Modified to RK78 from the original RK45 in NR.
 * RK78 values from Erwin Fehlberg, NASA TR R-287
 * gets derivs via Derivatives interface
 * 
 * @author <a href="mailto:dgaylor@users.sourceforge.net">Dave Gaylor
 * @author modified by Steven Hugg
 * @version 1.0
 */

public class RungeKuttaFehlberg45 {

    private boolean verbose;
    
    private double[][] simStates;		// intermediate state values
    private int        nSteps;			// number of intermediate steps
    private int		   nEqns;			// number of equations

	private static final int RK_LEN = 6; 
	
    /** Default constructor.
     */
    public RungeKuttaFehlberg45() {
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

    private static final double [] a = { 0, 1d/4d, 3d/8d, 12d/13d, 1, 1d/2d };

    private static final double [][] b = new double [6][5];
    static {
    	b[1][0] = 1d/4d;
    	b[2][0] = 3d/32d;
    	b[2][1] = 9d/32d;
    	b[3][0] = 1932d/2197d;
    	b[3][1] = -7200d/2197d;
    	b[3][2] = 7296d/2197d;
    	b[4][0] = 439d/216d;
    	b[4][1] = -8d;
    	b[4][2] = 3680d/513d;
    	b[4][3] = -845d/4104d;
    	b[5][0] = -8d/27d;
    	b[5][1] = 2d;
    	b[5][2] = -3544d/2565d;
    	b[5][3] = 1859d/4104d;
    	b[5][4] = -11d/40d;
    }

    private static final double [] chat = {25d/216d, 0, 1408d/2565d, 2197d/4104d, -1d/5d};

    private static final double [] c = {16d/135d, 0, 6656d/12825d, 28561d/56430d, -9d/50d, 2d/55d};

    public void rkck(double[] y, double[] dydx, long t0, double x, double h,
    double[] yout, double[] yerr, Derivatives dv) {

        int n = y.length;

        double f[][] = new double[RK_LEN][n];

        //	   double y7th[] = new double[n];
        double ytmp[] = new double[n];

        //	   System.out.println("step size = "+h);

        double xeval[] = new double [RK_LEN];
        for (int i = 0; i < RK_LEN; i++)  // find times for function evals
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

        // construct solutions
        // yout is the 8th order solution

        for (int i = 0; i < n; i++) {
            yout[i] = y[i] + h*(c[0]*f[0][i] +c[2]*f[2][i] + c[3]*f[3][i] + c[4]*f[4][i] + c[5]*f[5][i]);
            yerr[i] = y[i] + h*(chat[0]*f[0][i] +chat[2]*f[2][i] + chat[3]*f[3][i] + chat[4]*f[4][i]) - yout[i];
            //yout[i] = y[i] + h*(chat[5]*f[5][i] + chat[6]*f[6][i] + chat[7]*f[7][i] + chat[8]*f[8][i] + chat[9]*f[9][i] + chat[11]*f[11][i] + chat[12]*f[12][i]);
            //yerr[i] = h*c[0]*(f[11][i] + f[12][i] - f[0][i] - f[10][i]);
        }

    }

    public void print(double t, double [] y){
        // do nothing
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
    
}

