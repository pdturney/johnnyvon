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

/** An implementation of a Codon that has the desired replication as a
 * behaviour resulting from its configuration.  
 * 
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public final class DefaultCodon extends AbstractCodon {

	/* Shorthand. */
	private static final double PI = Math.PI;

	/* Number of arms, and a symbolic name for each one. */
	private static final int NUM_ARMS = 4;
	private static final int LEFT_ARM = 0;
	private static final int RIGHT_ARM = 1;
	private static final int UP_ARM = 2;
	private static final int REPELLER_ARM = 3;

	/* Names for the different forces.  FORCE_NONE is used when the bonds
	 * need to be updated between two codons, but no force should be applied.
	 */
	private static final int FORCE_ATTRACT = 1;
	private static final int FORCE_NONE = 0;
	private static final int FORCE_REPEL = -1;

	/* Force types.  SPRING is currently unnecessary, but provided because
	 * it's trivial to do so.  */
	private static final int FORCE_TYPE_SPRING = 0;
	private static final int FORCE_TYPE_STRAIGHT_SPRING = 1;

	/* Possible chain position states.  These are the "x y z" states in the
	 * technical paper.  DEFAULT is the starting state.  END is when the codon
	 * believes that it might be at the end of a complete double strand.
	 * TRUE_END is when it is sure that it is at the end of a compelte double
	 * strand. */
	private static final int CHAIN_DEFAULT = 0;
	private static final int CHAIN_END = 1;
	private static final int CHAIN_TRUE_END = 2;

	/* The possible splitting states.  These are the "0 1 2" of the  tech
	 * paper.  NONE is the starting state. READY is when the codon thinks that
	 * it should split, based on its neighbours.  This propagates through the
	 * chain and then when it hits the end, SPLIT_GO propagates the other way.
	 */
	private static final int SPLIT_NONE = 0;
	private static final int SPLIT_READY = 1;
	private static final int SPLIT_GO = 2;
	
	/** How many time units pass during each iteration. */
  public static final double TIMESTEP_DURATION = 0.15;

	/** The linear viscosity of the liquid. */
	public static final double 
		LINEAR_VISCOSITY = 1 - Math.pow(1 - 0.10, TIMESTEP_DURATION);

	/** The angular viscosity of the liquid. */
	public static final double 
		ANGULAR_VISCOSITY = 1 - Math.pow(1 - 0.05, TIMESTEP_DURATION);

	/** The amount of damping towards the average velocity of two bonded
	 * codons. */
	public static final double 
		LINEAR_SPRING_DAMPING = 1 - Math.pow(1 - 0.90, TIMESTEP_DURATION);
	/** The amount of extra damping towards zero rotation of two bonded
	 * codons. */
	public static final double 
		ANGULAR_SPRING_DAMPING = 1 - Math.pow(1 - 0.99, TIMESTEP_DURATION);

	/** How many iterations we should stay repelling after split. */
	public static final int 
		ITERATIONS_AFTER_SPLIT = (int)(150.0 / TIMESTEP_DURATION);

	/** The length of the arms. */
	public static final double[] ARM_LENGTH 
		= { 7, 7, 4, 1 };
	
	/** The radius of the fields on each arm when they are not large. */
	public static final double[] SMALL_FIELD_RADIUS 
		= { 0.01, 0.01, 0.01, 0.01 };
	/** The radius of the fields on each arm when they are large. */
	public static final double[] LARGE_FIELD_RADIUS 
		= { 4, 4, 4, 6 };
	
	/** The strength of the force of the arm. */
	public static final double[] ARM_FORCE 
		= { 1.8, 1.8, 1.0, 1.0 };
	
	/** The angle at which this arm points out from the center. */
	private static final double[] ARM_ANGLE 
		= { -PI/2, PI/2, PI, PI };

	/** The maximum angle at which this arm will still bond. */
	private static final double[] ANGLE_TOLERANCE 
		= { PI/256, PI/256, PI/3, 0 };
	/** The amount of force pulling these codons into alignment. */
	private static final double[] STRAIGHTENING_FORCE 
		= { 1, 1, 0.5, 0.0 };

	/** Figure out the biggest possible radius that this codon could have,
	 * such that it two codons are farther than this distance apart, they
	 * could not possibly need to worry about each other (unless they were
	 * just bonded.) */
	private static final double getMaxInteractionRadius() {
		double result = 0.0;
		for (int i = 0; i < NUM_ARMS; i++) {
			result = Math.max(result, ARM_LENGTH[i] + LARGE_FIELD_RADIUS[i]);
		}
		return result;
	}

	/** Find the longest arm.  This is used to determine the "size" of the
	 * codon to figure out its distribution of mass, when we need to calculate
	 * how much of a tangential tug goes to rotation vs. movement. */
	private static final double getMaxArmLength() {
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
	private static final double CODON_RADIUS = getMaxArmLength();
	
	/** The current chain position state. */
	private int chainPositionState;
	/** The chain position state as updated during the current timestep. */
	private int timestepChainPositionState;
	/** The current splitting state. */
	private int splittingState;
	/** The splitting state as updated during the current timestep. */
	private int timestepSplittingState;

	/** How many iterations we've done while split. */
	private int iterationsWhileSplit;

	/** The codons we are bonded to. */
	protected final DefaultCodon[] bonds;
	/** The codons we have become bonded to (or broken bonds from) since the
	 * beginning of this timestep. 
	 */
	protected final DefaultCodon[] timestepBonds;

	/** Cached from the simulator for speed. */
	private final int containerSize;

	// Used for cloning.  See AbstractCodon constructors for details 
	private DefaultCodon(int id, Simulator simulator, 
			Pair position, double angle,
			Pair velocity, double angularVelocity, 
			Pair timestepAcceleration, double timestepAngularAcceleration, 
			int type, Pair[] armPositions, Pair[] timestepForces, 
			int chainPositionState, int splittingState, DefaultCodon[] bonds) {
		super(id, simulator, position, angle, velocity, angularVelocity,
				timestepAcceleration, timestepAngularAcceleration, type,
				armPositions, timestepForces);
		this.chainPositionState = chainPositionState;
		this.splittingState = splittingState;
		this.bonds = bonds;
		this.timestepBonds = new DefaultCodon[NUM_ARMS];
		this.containerSize = simulator.getContainerSize();
	}

	// Use for creating a new codon.  
	public DefaultCodon(int id, Simulator simulator, Pair position, 
			double angle, Pair velocity, double	angularVelocity, int type) {
		super(id, simulator, position, angle, velocity, angularVelocity, type);

		this.chainPositionState = 0;
		this.splittingState = 0;

		this.bonds = new DefaultCodon[NUM_ARMS];
		this.timestepBonds = new DefaultCodon[NUM_ARMS];

		this.containerSize = simulator.getContainerSize();
	}
	

	/** {@inheritDoc} */
 	public Object clone() {
		
		Pair[] armPositions = new Pair[this.armPositions.length];
		Pair[] timestepForces = new Pair[this.timestepForces.length];
		for (int i = 0; i < armPositions.length; i++) {
			armPositions[i] = (Pair)this.armPositions[i].clone();
			timestepForces[i] = (Pair)this.timestepForces[i].clone();
		}
			
		return new DefaultCodon(this.id, this.simulator, 
			(Pair)this.position.clone(), this.angle, 
			(Pair)this.velocity.clone(), this.angularVelocity, 
			this.timestepAcceleration, this.timestepAngularAcceleration, 
			this.type, armPositions, timestepForces, this.chainPositionState,
			this.splittingState, this.bonds);
	}
	
	/** {@inheritDoc} */
	public final int getNumArms() { return NUM_ARMS; }

	/** {@inheritDoc} */
	public final Pair getArmAcceleration(int arm) {
		return this.timestepForces[arm];
	}

	/** {@inheritDoc} */
	public final Pair getArmPosition(int arm) { return this.armPositions[arm]; }

	/** {@inheritDoc} */
	public final double getFieldRadius(int arm) {
		if (this.isLarge(arm)) return LARGE_FIELD_RADIUS[arm];
		else return SMALL_FIELD_RADIUS[arm];
	}

	/** {@inheritDoc} */
	public final void startTimestep() {

		this.timestepAcceleration.setZero();
		this.timestepAngularAcceleration = 0.0;

		for (int i = 0; i < NUM_ARMS; i++) {
			// Update the arm positions.
			this.armPositions[i].x = 
				this.position.x + Math.cos(this.angle + ARM_ANGLE[i]) * ARM_LENGTH[i];
			this.armPositions[i].y = 
				this.position.y + Math.sin(this.angle + ARM_ANGLE[i]) * ARM_LENGTH[i];

			this.timestepForces[i].setZero();

			this.timestepBonds[i] = this.bonds[i];

		}

		if (this.splittingState == SPLIT_GO) 
			this.iterationsWhileSplit++;


	}

	/** {@inheritDoc} */
	public final void interact(AbstractCodon partner) {
		DefaultCodon other = (DefaultCodon)partner;

		// Drop out immediately if we're not even close to the other one.
		if (this.position.getDistanceSquared(other.position) 
				> (4 * MAX_INTERACTION_RADIUS * MAX_INTERACTION_RADIUS)) {
			return;
		}

		if (this.splittingState == SPLIT_GO) {
			// Up arms always repel while split.
			this.interactArms(other, UP_ARM, UP_ARM, FORCE_NONE, 
					FORCE_TYPE_STRAIGHT_SPRING);
		} else {

			// Check if we should attract or repel.  Likes repel, opposites attract.
			int direction = (this.type == other.type)?FORCE_NONE:FORCE_ATTRACT;
			this.interactArms(other, UP_ARM, UP_ARM, direction, 
					FORCE_TYPE_STRAIGHT_SPRING);
		}

		// Check the left and right arms.
		this.interactArms(other, LEFT_ARM, RIGHT_ARM, FORCE_ATTRACT, 
				FORCE_TYPE_STRAIGHT_SPRING);

		// Check the right and left arms. (opposite order from above wrt left/right,
		// everything else is the same.
	 this.interactArms(other, RIGHT_ARM, LEFT_ARM, FORCE_ATTRACT,
			 FORCE_TYPE_STRAIGHT_SPRING);

		if (this.splittingState == SPLIT_GO) 
			this.interactArms(other, REPELLER_ARM, REPELLER_ARM, FORCE_REPEL, 
					FORCE_TYPE_SPRING);
	}

	/** {@inheritDoc} */
	private final boolean interactArms(DefaultCodon other, 
			int myArm, int otherArm, int forceDirection, int forceType) {

		// Bonding must be bidirectional
		boolean bonded = (this.bonds[myArm] == other 
				&& other.bonds[otherArm] == this);

		// Get the distance, and its square.
		double distSq = this.armPositions[myArm].getDistanceSquared(
				other.armPositions[otherArm]);
		double dist = Math.sqrt(distSq);

		if (!bonded 
				&& (dist > LARGE_FIELD_RADIUS[myArm] + LARGE_FIELD_RADIUS[otherArm])) {
			return false;
		}
			
		// Figure out which radii we want for each arm.	
		double myRadius = this.isLarge(myArm)?
			LARGE_FIELD_RADIUS[myArm]:SMALL_FIELD_RADIUS[myArm];

		double otherRadius = other.isLarge(otherArm)?
			LARGE_FIELD_RADIUS[otherArm]:SMALL_FIELD_RADIUS[otherArm];

		// check if they're touching.
		boolean touching = dist < (myRadius + otherRadius);

		if (!touching) {
			if (bonded) {
				// break the bond.
				this.timestepBonds[myArm] = null;
				other.timestepBonds[otherArm] = null;
				//System.out.println("Bond: Bond broken.");
			}
			return false;
		} 

		//assert (touching);
		
		if (!bonded && forceDirection == FORCE_ATTRACT) {
			// Try to bond.
			 
			// Figure out the angle difference to see if we should bond.
			 
			// the angle difference is adjusted by PI because we want them to be at
			// 180 degrees, not the same angle.
			double difference = PI +
				(this.angle + ARM_ANGLE[myArm]) - 
				(other.angle + ARM_ANGLE[otherArm]);
			// force the angle to be between PI and -PI
			while (difference > PI) difference -= (2 * PI);
			while (difference < -PI) difference += (2 * PI);
			double tolerance = ANGLE_TOLERANCE[myArm] + ANGLE_TOLERANCE[otherArm];
			if ((difference <= tolerance) && (difference >= -tolerance)
					&& (this.timestepBonds[myArm] == null)
					&& (other.timestepBonds[otherArm] == null)
					&& !(this.isLarge(myArm) && other.isLarge(otherArm))) {
				// If we're inside the *inner* radius of both codons, and our angles are
				// sufficiently close, and we haven't already bonded to someone else in
				// this timestep, then bond.
				//System.out.println("Bond: Bonding - angle : " + difference);

				// create a bond
				this.timestepBonds[myArm] = other;
				other.timestepBonds[otherArm] = this;
				bonded = true;

			} else if ((difference > tolerance) || (difference < -tolerance)) {
				// Bond failed.
				return false;
			} else if (this.isLarge(myArm) && other.isLarge(otherArm)) {
				// Bond failed.
				return false;
			} else return false;
		} 
			

		/*assert ((bonded && touching && forceDirection == FORCE_ATTRACT) 
				|| (touching && forceDirection == FORCE_REPEL) 
				|| forceDirection == FORCE_NONE); */

		// Create a unit vector pointing from this's arm to the other's arm.
		Pair force = (Pair)this.armPositions[myArm].clone();
		force.subtract(other.armPositions[otherArm]);
		force.normalize();

		double rotationalAcceleration = 0.0;
		double targetAngle;
		double rotationAngle;

		switch (forceType) {
			case FORCE_TYPE_STRAIGHT_SPRING:
				if (forceDirection == FORCE_ATTRACT) {
					// atan2 will be between -PI and +PI.  FIXME account for 0,0
					// result will be between 4PI and -2PI

					// This is the angle that we want to the Codon to end
					// up at, with respect to the current interaction.
					targetAngle = -ARM_ANGLE[myArm] +
						Math.atan2(-this.position.y + other.position.y, -this.position.x + other.position.x);

					rotationAngle = -this.angle + targetAngle;
					
					// Make it be between -PI and PI
					while (rotationAngle > PI) rotationAngle -= (2 * PI);
					while (rotationAngle < -PI) rotationAngle += (2 * PI);
					
					this.timestepAngularAcceleration += 
						(rotationAngle * STRAIGHTENING_FORCE[myArm]);
					other.timestepAngularAcceleration -= 
						(rotationAngle * STRAIGHTENING_FORCE[myArm]);
				}
			case FORCE_TYPE_SPRING:      
				// A spring that is attracting acts like a spring being stretched - it
				// pulls more the farther away you are.
				if (forceDirection == FORCE_ATTRACT) {
					double scalar = (ARM_FORCE[myArm] / (myRadius + otherRadius)) * dist;
					if (scalar == 0) {
						force.x = 0; force.y = 0;
					} else {
						force.scale(-scalar);
					}

					// Dampen this pair towards their average velocity
					Pair centerOfMassVel = (Pair)this.velocity.clone();
					centerOfMassVel.add(other.velocity);
					centerOfMassVel.scale(0.5);

					Pair relativeVelocity = (Pair)this.velocity.clone();
					relativeVelocity.subtract(centerOfMassVel);
					// Dampen it 
					relativeVelocity.scale(LINEAR_SPRING_DAMPING);// * TIMESTEP_DURATION);

					//assert (relativeVelocity.isFinite());
					this.timestepAcceleration.subtract(relativeVelocity);
					other.timestepAcceleration.add(relativeVelocity);
					
					// This should have been commented out before...
					// this.timestepAngularAcceleration *= (1 - ANGULAR_SPRING_DAMPING);
					
				} else if (forceDirection == FORCE_REPEL) {
					
					// A spring that is repelling acts like a spring under compression -
					// it pushes more and more the closer you get.
					force.scale(forceDirection * (ARM_FORCE[myArm] / LARGE_FIELD_RADIUS[myArm])
							* (LARGE_FIELD_RADIUS[myArm] + LARGE_FIELD_RADIUS[otherArm] - dist) );
					force.negate();
					
				} else {
					// No force actually acting.  The call to interact() was made just to
					// update the bond information.
					//assert(forceDirection == FORCE_NONE);
				}
					
				break;
		}

		if (forceDirection != FORCE_NONE) {
			this.timestepForces[myArm].add(force);
			force.negate();
			other.timestepForces[myArm].add(force);
		}
		return true;
	 
	}

	/** Modify the velocity of this codon with some brownian motion. */
	private final void brownianMotion() {
		this.velocity.x += TIMESTEP_DURATION * (Math.random() - 0.5) / 2.0;
		this.velocity.y += TIMESTEP_DURATION * (Math.random() - 0.5) / 2.0;
		this.angularVelocity += TIMESTEP_DURATION * (Math.random() - 0.5) / 10.0;
	}

	/** Update the state of this codon. */
	private void updateState() {

		switch (this.splittingState) {
			case SPLIT_NONE:
				if ((this.chainPositionState == CHAIN_TRUE_END
							&& this.bonds[UP_ARM] != null 
							&& this.bonds[UP_ARM].chainPositionState == CHAIN_TRUE_END
							&& this.bonds[LEFT_ARM] == null)
						|| 
						((this.chainPositionState == CHAIN_TRUE_END || 
							this.chainPositionState == CHAIN_DEFAULT)
						 && this.bonds[UP_ARM] != null 
						 && (this.bonds[UP_ARM].chainPositionState == CHAIN_TRUE_END
							 || this.bonds[UP_ARM].chainPositionState == CHAIN_DEFAULT)
						 && this.bonds[LEFT_ARM] != null
						 && this.bonds[LEFT_ARM].splittingState == SPLIT_READY)) {
					this.timestepSplittingState = SPLIT_READY;
					//System.out.println("State: Split: None . Ready.");
				}
				break;
				
			case SPLIT_READY:
				if ((this.chainPositionState == CHAIN_TRUE_END 
							&& this.bonds[UP_ARM] != null 
							&& this.bonds[UP_ARM].chainPositionState == CHAIN_TRUE_END
							&& this.bonds[RIGHT_ARM] == null)
						||
						((this.chainPositionState == CHAIN_TRUE_END || 
							this.chainPositionState == CHAIN_DEFAULT)
						 && this.bonds[UP_ARM] != null 
						 && (this.bonds[UP_ARM].chainPositionState == CHAIN_TRUE_END
							 || this.bonds[UP_ARM].chainPositionState == CHAIN_DEFAULT)
						 && this.bonds[RIGHT_ARM] != null
						 && this.bonds[RIGHT_ARM].splittingState == SPLIT_GO)) {
					this.timestepSplittingState = SPLIT_GO;
					//System.out.println("State: Split: Ready . Go.");
				}
				break;
			case SPLIT_GO:
				if ((this.iterationsWhileSplit >= ITERATIONS_AFTER_SPLIT
							&& this.bonds[LEFT_ARM] == null)
						||
						(this.bonds[LEFT_ARM] != null 
						 && this.bonds[LEFT_ARM].splittingState == SPLIT_NONE)) {
					this.timestepSplittingState = SPLIT_NONE;
					this.iterationsWhileSplit = 0;
				}
				break;
			default:
		}

		switch (this.chainPositionState) {
			case CHAIN_DEFAULT:
				if (((this.bonds[LEFT_ARM] == null) != (this.bonds[RIGHT_ARM] == null))
						&& this.bonds[UP_ARM] != null) {
					this.timestepChainPositionState = CHAIN_END;
					//System.out.println("State: Chain: Default . End.");
				}
				break;
			case CHAIN_END:
				// No longer bonded on exactly one of left and right, or no longer
				// bonded above.
				if ((((this.bonds[LEFT_ARM] == null) == (this.bonds[RIGHT_ARM] == null))
						|| this.bonds[UP_ARM] == null)) {
					this.timestepChainPositionState = CHAIN_DEFAULT;
					//System.out.println("State: Chain: End . Default");
					
				} else if (this.bonds[UP_ARM] != null && 
						(this.bonds[UP_ARM].chainPositionState == CHAIN_END 
						 || this.bonds[UP_ARM].chainPositionState == CHAIN_TRUE_END)) {
					this.timestepChainPositionState = CHAIN_TRUE_END;
					//System.out.println("State: Chain: End . True End");
				}
				break;
			case CHAIN_TRUE_END:
				// No longer bonded on exactly one of left and right, or no longer
				// bonded above.
				if (((this.bonds[LEFT_ARM] == null) == (this.bonds[RIGHT_ARM] == null))
						|| this.bonds[UP_ARM] == null
						|| this.bonds[UP_ARM].chainPositionState == CHAIN_DEFAULT) {
					this.timestepChainPositionState = CHAIN_DEFAULT;
					//System.out.println("State: Chain: True End . Default");
				}
				break;
			default:
				//System.err.println("Error: Unknown chain position state: " 
					//+ this.timestepChainPositionState);
		}

		
	}


	public final void updateVelocities() {

		this.chainPositionState = this.timestepChainPositionState;
		this.splittingState = this.timestepSplittingState;
		this.updateState();

		for (int i = 0; i < NUM_ARMS; i++) { 
			this.bonds[i] = this.timestepBonds[i];
			
			// Don't bother doing anything if the force was zero for this arm.
			if (!this.timestepForces[i].isZero()) {
				
				// Create a unit vector pointing from the position to the end of the arm,
				// as of the beginning of this timestep.
				Pair armVector = (Pair)this.armPositions[i].clone();
				armVector.subtract(position);
				armVector.normalize();

				// Calculate the magnitude of the tangential force
				// FIXME - This is an excessive calculation for what we are trying to
				// get.
				Pair tangential = 
					this.timestepForces[i].getPerpendicularProjectionOnto(armVector);
	//      double tangentialForce = 
	//				timestepForces[i].perpendicularProjectionOnto(armVector).getLength();
				//double tangentialForce = .crossProduct(timestepForces[i]);
				// Calculate the rotation caused by the tangential force
				// This converts some of the tangential tug into rotation
				double angularAcceleration = (2 * ARM_LENGTH[i] * tangential.getLength())
					/ (CODON_RADIUS * CODON_RADIUS + 2 * ARM_LENGTH[i] * ARM_LENGTH[i]);

				if (armVector.getDotProduct(timestepForces[i]) < 0) {
					angularAcceleration = -angularAcceleration;
				} 
				
				// Calculate the movement of the codon, by getting a unit vector 
				// orthogonal to the arm pointing in the direction of the tangential
				// force, then scaling it to the size of the tangential force.
				//Pair acceleration = armVector;
				//acceleration.orthogonalize();

				// The rest of the tug is converted into (linear) acceleration.
				//  Because our mass is treated as being a unit, forces and
				//  accelerations are interchangeable.  
				Pair acceleration = tangential;
				//assert(acceleration.isFinite());
				if (angularAcceleration > 0) {
					acceleration.scaleTo(tangential.getLength() - angularAcceleration);
				} else {				
					acceleration.scaleTo(tangential.getLength() + angularAcceleration);
				}
				//assert(acceleration.isFinite());

				// Output the force
				//System.err.println("armvector.orth dot timestepforce[" + i + "] : " 
				//	+ acceleration.dotProduct(timestepForces[i]));
				
				//acceleration.scale(tangentialForce - ARM_LENGTH[i] * angularAcceleration);
				// Calculate the radial force.  This is easy peasy.  Add it to the
				// radial part of the tangential force. 
				
				//acceleration.x = 0; acceleration.y = 0;
				Pair radial = this.timestepForces[i].getProjectionOnto(armVector);
				acceleration.add(radial);

				// Add the position change..
				//assert (acceleration.isFinite());
				this.timestepAcceleration.add(acceleration);
				//assert (this.timestepAcceleration.isFinite());

				// ..and the rotation.
				this.timestepAngularAcceleration += angularAcceleration;
				
			}
		}

		// Update the Codon's velocities
		Pair deltaVel = (Pair)this.timestepAcceleration.clone();
		deltaVel.scale(TIMESTEP_DURATION);
		this.velocity.add(deltaVel);

		this.angularVelocity += (this.timestepAngularAcceleration * TIMESTEP_DURATION);

		// The codons are in a liquid, so we dampen their velocity that was carried
		// over from the previous timestep.

		// To do this, we calculate how many straight-spring-attraction interactions
		// occurred.  We multiply the angular velocity by the damping factor once
		// for each of these.
		for (int i = 0; i < NUM_ARMS; i++) {
			if (this.timestepBonds[i] != null) {
				this.angularVelocity *= (1 - LINEAR_SPRING_DAMPING);
			}
		}
		this.angularVelocity *= (1 - ANGULAR_VISCOSITY);

		// The velocity is damped towards zero.
		this.velocity.scale(1 - LINEAR_VISCOSITY);
		
		// Brownian motion
		this.brownianMotion();

	}

	public boolean isLarge(int arm) {
		switch (arm) {
			case LEFT_ARM:
			case RIGHT_ARM:
				return this.bonds[arm] != null;
			case UP_ARM:
				return (this.bonds[LEFT_ARM] != null) || (this.bonds[RIGHT_ARM] != null);
			case REPELLER_ARM:
				return (this.splittingState == SPLIT_GO);
			default:
				return false;
		}
	}

	public final void updatePositions() {
		// Update the codon's position 
		// Angle
		this.angle += (this.angularVelocity * TIMESTEP_DURATION);

		// (Linear) position 
		Pair deltaPos = (Pair)this.velocity.clone();
		deltaPos.add(this.velocity);
		deltaPos.scale(TIMESTEP_DURATION);

		this.position.add(deltaPos);

		// Reduce the angle "mod pi"
		while (this.angle >= 2 * PI) this.angle -= 2*PI;
		while (this.angle < 0) this.angle += 2*PI;
		//assert (this.angle >= 0 && this.angle <= 2 * PI);
		
		// Bounce the codon off the walls.  If the possible forces were much larger
		// than the container, it would become necessary to repeat this process
		// until the codon ended up within the walls. 
		if (this.position.x < -containerSize) {
			 this.position.x = (-2 * containerSize) - this.position.x;
			 this.velocity.x = -this.velocity.x;
		} else if (this.position.x > containerSize) {
			 this.position.x = (2 * containerSize) - this.position.x;
			 this.velocity.x = -this.velocity.x;
		}
		
		if (this.position.y < -containerSize) {
			 this.position.y = (-2 * containerSize) - this.position.y;
			 this.velocity.y = -this.velocity.y;
		} else if (this.position.y > containerSize) {
			 this.position.y = (2 * containerSize) - this.position.y;
			 this.velocity.y = -this.velocity.y;
		}

		// Everything's updated, now change the arm locations to reflect that.

	}
}

