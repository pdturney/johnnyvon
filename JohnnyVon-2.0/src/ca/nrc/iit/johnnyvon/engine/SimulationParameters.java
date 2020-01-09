/* JohnnyVon -- An implementation of self-replicating automata 
   in two-dimensional continuous space.
   Copyright (C) 2002-2004 National Research Council Canada

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

Authors:
	Robert Ewaschuk - rob@infinitepigeons.org
	Arnold Smith - arnold.smith@nrc.ca
	Peter Turney - peter.turney@nrc.ca

Postal Contact:
	Peter Turney
	Institute for Information Technology
	National Research Council Canada
	M-50, Montreal Road
	Ottawa, ON, Canada
	K1A 0R6

*/

/** The parameters of the simulation that are external to codons -- how the 
 * various forces actually work.
 * 
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
package ca.nrc.iit.johnnyvon.engine;

/* package */ final class SimulationParameters {
		
	/** How many time units pass during each iteration. */
	public static final double TIMESTEP_DURATION = 0.20;

	/** The linear brownian motion factor. */
	public static final double LINEAR_BROWNIAN_MOTION = 0.20;
	
	/** The angular brownian motion factor. */
	public static final double ANGULAR_BROWNIAN_MOTION = 0.10;

	/** Linear viscosity, between 0 and 1.  Closer to 1 means more energy is
	 * removed at each step. */
	public static final double LINEAR_VISCOSITY = 0.25;

	/** The resultant linear viscosity factor for the "liquid". */
	public static final double LINEAR_VISCOSITY_FACTOR = Math.pow(1 - LINEAR_VISCOSITY, TIMESTEP_DURATION);

	/** Linear viscosity, between 0 and 1.  Closer to 1 means more energy is
	 * removed at each step. */
	private static final double ANGULAR_VISCOSITY = 0.25;
	
	/** The resultant angular viscosity of the "liquid".  */
	public static final double ANGULAR_VISCOSITY_FACTOR = Math.pow(1 - ANGULAR_VISCOSITY, TIMESTEP_DURATION);

	/** Linear spring damping.  1.0 means two bonded codons are fully damped
	 * towards their average velocity.  0.0 means two bonded codons are fully
	 * independent. */
	private static final double LINEAR_SPRING_DAMPING = 0.95;

	/** The resultant linear spring damping. */
	public static final double LINEAR_SPRING_DAMPING_FACTOR = 1 - Math.pow(1 - LINEAR_SPRING_DAMPING, TIMESTEP_DURATION);

	private static final double ANGULAR_SPRING_DAMPING = 0.95;
	
	/** The amount of extra damping towards zero rotation of two bonded
	 * codons.  The larger the number, the more two bonded codons will tend
	 * towards the same velocity.  Higher number (inside the brackets)*/
	public static final double ANGULAR_SPRING_DAMPING_FACTOR =  Math.pow(1 - ANGULAR_SPRING_DAMPING, TIMESTEP_DURATION);

	static {
		System.out.println("LIN DAMP: " + LINEAR_SPRING_DAMPING_FACTOR);
		System.out.println("ANG DAMP: " + ANGULAR_SPRING_DAMPING_FACTOR);
	}
}
