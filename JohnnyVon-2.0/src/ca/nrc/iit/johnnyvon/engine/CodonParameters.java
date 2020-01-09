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

/** Constants that define various attributes about how the codon works, and 
 * how it interacts with other codons.
 * 
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
package ca.nrc.iit.johnnyvon.engine;

public final class CodonParameters {

	/** Shorthand. */
	private static final double PI = Math.PI;

	//// ARMS ////

	/** Number of arms for the codon. */
	public static final int NUM_ARMS = 5;

	/** Index of the arm that points left. */
	/* package */ static final int LEFT_ARM = 0;

	/** Index of the arm that points right */
	/* package */ static final int RIGHT_ARM = 1;

	/** Index of the arm that points up */
	/* package */ static final int UP_ARM = 2;
	
	/** Index of the arm that repels when two chains split apart. */
	/* package */ static final int REPELLER_ARM = 3;

	/** Index of the arm that helps break up overlapping parts of meshes. */
	/* package */ static final int OVERLAP_ARM = 4;

	/** Which arms bond to which?  The index in this array is the arm number
	 * of the current codon, the value is the arm number on the codon bonded
	 * to our arm. */
	/* package */ static final int[] BOND_ARM = new int[] { RIGHT_ARM, LEFT_ARM, UP_ARM, -1, OVERLAP_ARM };

	/** How many iterations we should stay repelling after split. */
	public static final int REPEL_ITERATIONS = (int)(100.0 / SimulationParameters.TIMESTEP_DURATION);

	/** How many iterations we should stay bonded but not phenotyped.  After a
	 * certain amount of time after we last split, if we haven't split again
	 * then we should fold up, and release our partners.  (Actually they release
	 * themselves when they notice their partner has folded.) */
	public static final int ITERATIONS_AFTER_SPLIT = (int)(5000.0 / SimulationParameters.TIMESTEP_DURATION);

	/** How many iterations we should stay out of tolerances on our UP arm
	 * before we shatter.  If we stay out of tolerance longer than this
	 * amount, it should mean that we're in a mesh that was formed badly,
	 * and so we need to give up. */
	public static final int ITERATIONS_OUT_OF_TOLERANCE = (int)(5000.0 / SimulationParameters.TIMESTEP_DURATION);

	/** The tolerance at side-bonds for an up-bond to form.  That is, how many
	 * radians off our desired angle can the sides be before we reject new
	 * partners.  This prevents a chain that's currently folding up from
	 * "catching" on other chains and getting stuck in a high-tension,
	 * generally broken state. */
	public static final double FLEX_TOLERANCE = PI / 96;

	/** The length of the arms. */
	public static final double[] ARM_LENGTH = { 7, 7, 2, 1, 1 };
	
	/** The radius of the fields on each arm  */
	public static final double[] FIELD_RADIUS = { 2.0, 2.0, 3.0, 8.0, 0.5 };

	// 8 for the repeller arm is the largest it can be without growing the
	// codon's total potential interaction radius, which would have speed
	// consequences. 
	
	/** The strength of the force of the arm. */
	public static final double[] ARM_FORCE = { 1.5, 1.5, 1.5, 0.2, 0.2 };
	
	
	/** The angle at which this arm points out from the center. */
	/* package */ static final double[] ARM_ANGLE = { -PI/2, PI/2, PI, PI, 0 };

	/** The maximum angle at which this arm will still bond. */
	/* package */ static final double[] BOND_TOLERANCE = { PI/64, PI/64, PI/12, 0, PI/12 };

	/** The amount of force pulling these codons into alignment.  This should be
	 * between 0 and 1, I think, otherwise I'm not sure that they make sense. */
	/* package */ static final double[] STRAIGHTENING_FORCE = { 0.5, 0.5, 0.5, 0.0, 0.0 };

	/** The target angle for bonds on the given arm once we have reached
	 * the "folded" state.
	 * 
	 * The first dimension is the arm to look at. 
	 *
	 * The latter two dimensions represent the type of codons that are bonding.  This
	 * matrix should be symmetrical, since we only look at it in whatever
	 * left-to-rightiness the codons are processed in.
	 */
	/* package */ static final double[][][] JOINT_ANGLE = {
		// 00 indicates "don't care", i.e. room for expansion for new shapes. GONE
		// 000 is a tricky spot.  a 3-3 bond can have any angle that it wants, but
		// note, once folded, it will not bond with itself.  (This is because of
		// octagons -- they must have only one of the two involved types of codons
		// bonding, otherwise the mesh will mis-form.)
		//      __
		// \   /  \   /
		//  |-|    |-|
		// /   \__/   \
		//
		// If the slashes (/\) could also bond to each other, it's easy to see
		// that the mesh would get messed up.
		
		
		{ {  0,        0,        0,        0, },
		  {  0,  -2*PI/3,    -PI/4,    -PI/2, },
		  {  0,    -PI/4,      000,        0, },	
		  {  0,    -PI/2,        0,    -PI/3, } }, 

		{ {  0,        0,        0,        0, },
		  {  0,   2*PI/3,     PI/4,     PI/2, },
		  {  0,     PI/4,      000,        0, },
		  {  0,     PI/2,        0,     PI/3, } },

		{ { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } },
		{ { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 }, { 0, 0, 0, 0 } },
		{ { PI, PI, PI, PI }, { PI, PI, PI, PI }, { PI, PI, PI, PI }, { PI, PI, PI, PI } }
	};

	// The first row and column are for the "extender" codons.  The 

	/** The site-to-site interaction, -1 for repel, 0 for ignore, 1 for
	 * attract.
	 *
	 * Only the "up" arm is subject to this.  There are 
	 * four types, with various interaction rules.  This should be symmetric, 
	 * so it doesn't matter which one we're examining. 
	 */
	/* package */ static final int[][] SITE_BONDING = {
		// 00 is a "don't care" marker.   (None left!)
		{  0,  0,  0,  0 },  
		{  0,  1,  0,  0 },
		{  0,  0,  0,  1 },
		{  0,  0,  1,  1 },
	};
		
	/** The number of replications a codon (thinks it) goes through before
	 * switching to being a 'phenotype'.  To get k^n growth, MAX_REPLICATIONS
	 * needs to be k+1, since when a chain is formed its codons count that as
	 * a replication. */
	/* package */ static final int MAX_REPLICATIONS = 3;

	/** Figure out the biggest possible radius that this codon could have,
	 * such that it two codons are farther than this distance apart, they
	 * could not possibly need to worry about each other (unless they were
	 * just bonded.) */
	/* package */ static final double getMaxInteractionRadius() {
		double result = 0.0;
		for (int i = 0; i < NUM_ARMS; i++) {
			result = Math.max(result, ARM_LENGTH[i] + FIELD_RADIUS[i]);
		}
		return result;
	}

	/** Find the longest arm.  This is used to determine the "size" of the
	 * codon to figure out its distribution of mass, when we need to calculate
	 * how much of a tangential tug goes to rotation vs. movement. */
	/* package */ static final double getMaxArmLength() {
		double result = 0.0;
		for (int i = 0; i < NUM_ARMS; i++) {
			result = Math.max(result, ARM_LENGTH[i]);
		}
		return result;
	}

	/** The biggest possible radius of this codon. */
	public static final double MAX_INTERACTION_RADIUS = getMaxInteractionRadius();

	/** Figure out the radius of the codon (in terms of the uniform disc that 
		* gets rotated.)  This is the maximum arm length. */
	/* package */ static final double CODON_RADIUS = getMaxArmLength();

}
