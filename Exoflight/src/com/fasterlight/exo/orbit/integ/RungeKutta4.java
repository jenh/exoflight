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
 * <P>
 * RungeKutta8 is a fixed stepsize Runge-Kutta 8th order integrator.
 * Integrator parameter values came from:
 * Fehlberg, Erwin, NASA TR R-287.
 *
 * @author <a href="mailto:dgaylor@users.sourceforge.net">Dave Gaylor
 * @version 1.0
 */

public class RungeKutta4 {

    /** Step size.
     */
    public double step_size;

    /** Default constructor
     */
    public RungeKutta4() {
        this.step_size = 1.0;
    }

    /** Construct RungeKutta8 integrator with specified step size.
     * @param s Step size.
     */

    public RungeKutta4(double s) {
        this.step_size = s;
    }

    /** Set step size
     * @param s     step size
     */

    public void setStepSize(double s) {
        this.step_size = s;
    }

    /** Take a single integration step. Note: it is assumed that any printing
     * will be done by the calling function.
     * @param x     time or independent variable
     * @param y     double[] containing needed inputs (usually the state)
     * @param neqns number of equations to integrate
     * @param derivs   Object containing the Equations of Motion
     * @return      double[] containing the new state
     */
    public double[] step(long t0, double x, double[] y, Derivatives derivs) {
        int n = y.length;
        double f[][] = new double[4][n];

        double yout[] = new double[n];
        double ytmp[] = new double[n];

        double h = this.step_size;
        
        double hh = h * 0.5;
        double h6 = h/6.0;
        double xh = x + hh;

        // build f matrix
        f[0] = derivs.derivs(t0, x, y);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + hh * f[0][i];
        }
        f[1] = derivs.derivs(t0, xh, ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + hh * f[1][i];
        }
        f[2] = derivs.derivs(t0, xh, ytmp);

        for (int i = 0; i < n; i++) {
            ytmp[i] = y[i] + h * f[2][i];
        }
        f[3] = derivs.derivs(t0, x + h, ytmp);


        // construct solutions
        for (int i = 0; i < n; i++) {
            yout[i] = y[i] + h6*(f[0][i] + 2.0 *(f[1][i] + f[2][i]) +f[3][i]);
        }

        return yout;
    }

    
}
