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
 * The EquationsOfMotion interface provides the mechanism for passing a method
 * that computes the derivatives to an integrator.
 *
 * @author <a href="mailto:dgaylor@users.sourceforge.net">Dave Gaylor
 * @version 1.0
 */
public interface Derivatives {

    /** Compute the derivatives.
     * @params t    double containing time or the independent variable.
     * @params x    VectorN containing the required data.
     * @return      double [] containing the derivatives.
     */

    public double[] derivs(long tick, double dt, double[] x);

}

