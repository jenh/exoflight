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


/**  
 * @author Steven Hugg
 * @author derived from source by <a href="mailto:dgaylor@users.sourceforge.net">Dave Gaylor
 * @version 1.0
 */

public class HeunEuler {

    private boolean verbose;
    
    private double[][] simStates;		// intermediate state values
    private int        nSteps;			// number of intermediate steps
    private int		   nEqns;			// number of equations

	private static final int RK_LEN = 2; 
	
    /** Default constructor.
     */
    public HeunEuler() {
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

    private static final double [] a = { 0, 1 };

    private static final double [] chat = {1, 0};

    private static final double [] c = {0.5, 0.5};

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

        f[0] = dydx;

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * f[0][i];
        }
        f[1] = dv.derivs(t0, xeval[1], ytmp);

        for (int i = 0; i < n; i++) {
            yout[i] = y[i] + h*(c[0]*f[0][i] +c[1]*f[1][i]);
            yerr[i] = y[i] + h*(chat[0]*f[0][i] +chat[1]*f[1][i]) - yout[i];
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

