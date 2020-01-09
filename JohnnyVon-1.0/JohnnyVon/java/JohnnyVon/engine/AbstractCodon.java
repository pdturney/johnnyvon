/* JohnnyVon -- An implementation of self-replicating automata 
   in two-dimensional continuous space.
   Copyright (C) 2002 National Research Council Canada

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
	Robert Ewaschuk - raewasch@uwaterloo.ca
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

package JohnnyVon.engine;

/** A general abstract framework for a Codon.  
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public abstract class AbstractCodon implements Cloneable {

	public static final int NORTH_CODON = 0;
	public static final int SOUTH_CODON = 0;

	/** The simulator that is running this codon. */
	protected final Simulator simulator;

	/** The type that this codon is.  This normally encodes a single bit of
	 * information.   i.e. it is either 0 or 1.  No reason it couldn't be
	 * more, though. */
	protected final int type;
	
	/** The linear position of this codon. */
	protected final Pair position;
	/** The angular position of this codon. */
	protected double angle;

	/** The linear velocity of this codon. */
	protected final Pair velocity;
	/** The angular velocity of this codon. */
	protected double angularVelocity;

	/** The linear acceleration that is accumulated based on the forces
	 * affecting this codon.  (Brownian motion should not modify this, but
	 * should modify the velocity directly.) */
	protected final Pair timestepAcceleration;
	/** The angular acceleration that is accumulated based on the forces
	 * affecting this codon.  (Brownian motion should not modify this, but
	 * should modify the velocity directly.) */
	protected double timestepAngularAcceleration;

	/** The index of this codon in the simulator's list.  This is so that the
	 * codons can know their relative orderings.  This permits a codon
	 * modifying another only if the other's id number is (greater, smaller)
	 * than this ones.  Thus you can make sure all pairwise interactions occur
	 * without duplicating reflexive calculations. */
	protected final int id;
	/** The forces that are applied to each arm during this timestep. */
	protected final Pair[] timestepForces;
	/** The positions of each arm, updated at the beginning of each timestep.
	 */
	protected final Pair[] armPositions;

	/** This constructor is used when a Codon is cloned.  It allows a subclass
	  * to set all of the variables in the Codon.
		* @see Viewer#view
		* @param id the index of this codon in the simulator's codon list.
		* @param simulator The simulator that has this codon.  This is currently
		* only used to get the container size. 
		* @param position The linear position of this codon.
		* @param angle The angular position of this codon.
		* @param velocity The linear velocity of this codon.
		* @param angularVelocity the angular velocity of this codon.
		* @param timestepAcceleration The overall linear acceleration during the 
		* most recent timestep.
		* @param timestepAngularAcceleration The overal angular acceleration
		* during the more recent timestep.
		* @param type The type of this codon.  Usually 0 or 1.  Could have more
		* types.
		* @param armPositions The locations of this codon's arms at the
		* beginning of the last timestep.  
		* @param timestepForces The force vectors applied to each arm during the
		* most recent timestep.
		*/
	protected AbstractCodon(int id, Simulator simulator, 
			Pair position, double angle, 
			Pair velocity, double	angularVelocity, 
			Pair timestepAcceleration, double timestepAngularAcceleration, 
			int type, Pair[] armPositions, Pair[] timestepForces) {

		if (type < 0 || type > 1) 
			throw new IllegalArgumentException("Bad type: " + type);
		this.timestepAngularAcceleration = timestepAngularAcceleration;
		this.timestepAcceleration = timestepAcceleration;
		this.id = id;
		this.simulator = simulator;
		this.position = position;
		this.angle = angle;
		this.velocity = velocity;
		this.angularVelocity = angularVelocity;
		this.type = type;

		if (armPositions != null) this.armPositions = armPositions;
		else this.armPositions = new Pair[this.getNumArms()];
			
		if (timestepForces != null) this.timestepForces = timestepForces;
		else this.timestepForces = new Pair[this.getNumArms()];
	}

	/** Public constructor for creating Codons.
		* @see Viewer#view
		* @param id the index of this codon in the simulator's codon list.
		* @param simulator The simulator that has this codon.  This is currently
		* only used to get the container size. 
		* @param position The linear position of this codon.
		* @param angle The angular position of this codon.
		* @param velocity The linear velocity of this codon.
		* @param type The type of this codon.  Usually 0 or 1.  Could have more
		* types.
		*/
	public AbstractCodon(int id, Simulator simulator, Pair position, double angle, 
			Pair velocity, double	angularVelocity, int type) {
		this(id, simulator, position, angle, velocity, angularVelocity, 
				new Pair(), 0.0, type, null, null);

		for (int i = 0; i < this.getNumArms(); i++) {
			this.timestepForces[i] = new Pair();
			this.armPositions[i] = new Pair();
		}

	}

	/** Copy this codon.  This can be used by the viewer, when it receives the
	 * codon array, to take a clone of each codon and then return, allowing
	 * the simulation to continue.  If it did not take a clone, then the
	 * simulator would continue to act on the codons that the viewer was
	 * trying to draw. */
	public abstract Object clone();

	/** Get the number of arms that this codon has. */
	public abstract int getNumArms();

	/** Find out if the given arm is in the "large" state.
		* @param arm the arm to find out about. 
		* @return True iff this arm is in its large state. 
	 */
	public abstract boolean isLarge(int arm); 

	/** Setup for a timestep.  Zero accelerations, reset arm positions to be
	 * correct with respect to the new position, etc. */
	public abstract void startTimestep();

	/** Calculate forces between two codons.
		* @param other The codon to calculate forces for.  The simulator assumes
		* (and guarantees) that a particular pair of codons need only interact 
		* one way.  That is, if and only if a.interact(b) is called, then
		* b.interact(a) will not be called.
	 */
	public abstract void interact(AbstractCodon other);

	/** Update the velocities given the forces that were accumulated during
	 * all of the interactions, and brownian motion. */
	public abstract void updateVelocities();

	/** Update the positions based on the newly updated velocities. */
	public abstract void updatePositions();

	/** Get the coordinates of the time of an arm.
		* @param arm the arm to find out about.
		* @return The position of this arm. 
	  */
	public abstract Pair getArmPosition(int arm);

	/** Get the acceleration that was accumlated on this arm during
	 * the last timestep.  
		* @param arm the arm to find out about.
		* @return The force on this arm. 
 	  */
	public abstract Pair getArmAcceleration(int arm);

	/** Get the current field radius for the given arm.
		* @param arm The arm to find out about.
		* @return The radius of this field for the arm.  This will depend on
		* isLarge().
		*/
	public abstract double getFieldRadius(int arm);

	/** Get the position of the center of this codon. */	
	public final Pair getPosition() { return this.position; } 
	
	/** Get the angular position of this codon. */
	public final double getAngle() { return this.angle; } 

	/** Get the type (usually 0 or 1) of this codon. */
	public final int getType() { return this.type; }

	/** Get the acceleration on this codon during the last timestep.  */
	public final Pair getTimestepAcceleration() { 
		return this.timestepAcceleration; 
	}

	/** Get the angular acceleration on this codon during the last timestep.
	 */
	public final double getTimestepAngularAcceleration() {
		return this.timestepAngularAcceleration;
	}
			
}
