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

package ca.nrc.iit.johnnyvon.engine;

/** An implementation of a Codon that has the desired replication as a
 * behaviour resulting from its configuration.  
 * 
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
public final class Codon {

	/* Shorthand. */
	private static final double PI = Math.PI;

	/** Not sure if this belongs somewhere else, but it can go here for now.
	 */
	public static final int NUM_CODON_TYPES = 4;

	/** Only this one needs a constant, since we need to talk about bendy vs.
	 * non-bendy codons. */
	private static final int EXTENSION_CODON = 0;

	/** Attract or repel proportional to distance. */
	private static final int SPRING = 0;

	/** Spring force, plus a straighening effect proportional to the
	 * difference between the current and desired angles. */
	private static final int STRAIGHT_SPRING = 1;

	/** Indicate that two fields attract each other */
	private static final int ATTRACT = 1;

	/** Used when the bonds need to be updated between two codons, but 
	 * no force should be applied.  */
	private static final int NONE = 0;
	
	/** Indicate that two fields repel each other */
	private static final int REPEL = -1;

	// If a codon does not have a partner on one side, it is treated as having
	// a bendy one on that side.  Whether these should be treated as the same is
	// open for discussion, but works with all shapes currently foreseen.
	//
	// "Bendy" codons are all those with types other than 0.  0 is straight
	// with everything.
	
	/** Bendy, and joined to a bendy (or nothing) on both sides. */
	private static final int IN_BEND = 1;

	/** Straight codon on the right, bendy (or nothing) on the left. */
	private static final int RIGHT_OF_BEND = 2;

	/** Straight codon on the left, bendy (or nothing) on the right. */
	private static final int LEFT_OF_BEND = 3;

	/** All the other possible states.  All straight codons (_type = 0) will
	 * be in this state, as well as bendy codons between two straight codons.
	 */
	private static final int BEND_OTHER = 4;

	/** This is the "definitive" state of the Codon.  At the start of a
	 * timestep, we sync these up.  During the timestep, all test are done
	 * against this _state variable, but all changes are made to the _timestep
	 * variable below.  This ensures that we do operations based on a
	 * consistent state. */
	private final CodonState _state;

	/** This is the state of a codon as various things happen to it during a
	 * time step.  At the beginning of a timestep, it gets synced with the
	 * _state above, and at the end of a timestep, changes are copied back to
	 * the _state variable. */
	private final CodonState _timestep;

	/** The type that this codon is.  This normally encodes a single bit of
	 * information.   i.e. it is either 0 or 1.  No reason it couldn't be
	 * more, though. */
	// Should this be in the state?  It never changes, but it still defines
	// behaviour.
	private final int _type;

	/** This is not really part of the theoretical codon state, it's just
	 * something we need for accounting and optimizations for the simulation.
	 */
	// This is so that the codons can know their relative orderings.  This 
	// permits a codon modifying another only if the other's id number is 
	// (greater, smaller) than this one's id.  Thus you can make sure all pairwise 
	// interactions occur without duplicating reflexive calculations. 
	private final int _id;

	/** The linear acceleration that is accumulated based on the forces
	 * affecting this codon.  (Brownian motion should not modify this, but
	 * should modify the velocity directly.) */
	private final Pair _acceleration;
	
	/** The angular acceleration that is accumulated based on the forces
	 * affecting this codon.  (Brownian motion should not modify this, but
	 * should modify the velocity directly.) */
	private double _angularAcceleration;

	/** The positions of each arm, updated at the beginning of each timestep.
	 * Derivable from the codon's position, angle and arm information.
	 */
	private final Pair[] _armPositions;
		
	/** True if and only if all existing bond angles are within 
	 * CodonParameters.FLEX_TOLERANCE.  No new bonds should be formed if we're not within our
	 * flex tolerances.  (This is a derivative of the above "state"
	 * information, which is why it isn't included there.) */
	private boolean _withinTolerances;

	/** Whether we have any bond partners.  Derivative info. */
	private boolean _bonded = false;
		
	/** The location of this codon in a (possibly extended) shape.  Can be on 
	 * the left or the right of an extension piece, in the middle of two 
	 * extension pieces (though that never happens for us), or connected only 
	 * to bending pieces.  This is derived from the neighbours' state.
	 */
	private int _bendState;

	/** The forces that are applied to each arm during this timestep. */
	protected final Pair[] _forces;

	/** Create a codon.
	 *
	 * @param id The id of this codon.  Each codon should have a unique ID.
	 * They are used to optimize certain symmetrical calculations. 
	 *
	 * @param position The starting location of this codon. 
	 *
	 * @param angle The starting angle of this codon. 
	 *
	 * @param velocity The starting linear velocity of this codon.
	 *
	 * @param angularVelocity The starting angular velocity of this codon.
	 *
	 * @param type The type of this codon.  Current 0,1,2 or 3.  Governs
	 * various interactions, angles, etc.
	 *
	 * @param hasSplit Whether this codon should be treated as one that has
	 * split in the past.
	 */
	public Codon(int id, Pair position, double angle, Pair velocity, double	angularVelocity, int type, boolean hasSplit, boolean isReplicationSeed) {
		this._id = id;
		this._type = type;
		
		this._state = new CodonState(position, angle, velocity, angularVelocity, hasSplit, isReplicationSeed);
		this._timestep = new CodonState(position, angle, velocity, angularVelocity, hasSplit, isReplicationSeed);

		this._acceleration = new Pair();
		this._armPositions = new Pair[CodonParameters.NUM_ARMS];
		this._forces = new Pair[CodonParameters.NUM_ARMS];

		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
			this._armPositions[i] = new Pair();
			this._forces[i] = new Pair();
		}

	}

	/** Initialize things that need to be initialized for a timestep. */
	public final void startTimestep() {

		// Copy the state.
		// TODO: We can get rid of one of these copyFrom calls, with a bit of
		// care.
		this._timestep.copyFrom(this._state);

		this._acceleration.setZero();
		this._angularAcceleration = 0.0;

		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
			double angle = this._state._angle + CodonParameters.ARM_ANGLE[i];

			// Update the arm positions.
			this._armPositions[i].x = this._state._position.x + Math.cos(angle) * CodonParameters.ARM_LENGTH[i];
			this._armPositions[i].y = this._state._position.y + Math.sin(angle) * CodonParameters.ARM_LENGTH[i];

			this._forces[i].setZero();

			assert this._timestep._bonds[i] == this._state._bonds[i]: "Bonds buggy for " + this + "@" + i + " was: " + this._timestep._bonds[i] + " is " + this._state._bonds[i];

		}

		if (this._state._splittingState == CodonState.SPLIT_GO) {
			this._timestep._repelIterations++;
		} else {
			// TODO: It's not clear to me why this is necessary, but for some reason
			// this counter isn't getting correctly reset.
			this._timestep._repelIterations = 0;
		}

		// We care fairly frequently about being bonded, so we calculate this
		// and cache the result for each timestep.
		this._bonded = false;
		for (int i = 0; i < this._state._bonds.length; i++) {
			if (this._state._bonds[i] != null) {
				this._bonded = true;
				break;
			}
		}

		// Cannot be folded if we aren't bonded, unless we're still dealing with
		// a shatter.
		assert !(this._state._folded && this._state._splittingState != CodonState.SPLIT_SHATTER) || _bonded: "Cannot be folded without being bonded: " + this;
		assert !(this._state._hasSplit && !this._state._isReplicationSeed && this._state._splittingState != CodonState.SPLIT_SHATTER) || _bonded: "Cannot be split without being bonded: " + this;

		// True, except for first iteration.
		// TODO: Support specified bonds in the input.txt, so that we odn't have
		// a special first-case.
		//assert !this._state._hasSplit || _bonded: "Cannot be split and not bonded: " + this;

		Codon up = this._state._bonds[CodonParameters.UP_ARM];
		// Cannot have an up-bond to a split codon if we're split and not folded.
		assert up == null || this._state._folded || (this._state._hasSplit != up._state._hasSplit): "Cannot be bonded to a like-split codon: " + this + " :: " + up;
					
		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
			assert (this._state._bonds[i] == null) || (this == this._state._bonds[i]._state._bonds[CodonParameters.BOND_ARM[i]]): "me, arm, them, bond: " + this + "," + i + "," + this._state._bonds[i] + "," + this._state._bonds[i]._state._bonds[CodonParameters.BOND_ARM[i]];
		}

		this.checkTolerances();

		// Increment or reset.
		if (this._withinTolerances || !this._state._inMesh) {
			this._timestep._iterationsOutOfTolerance = 0;
		} else {
			this._timestep._iterationsOutOfTolerance++;
		}
		
	}

	/** Check that all arm angles are within the desired tolerance.  If any of
	 * them isn't, we're either in a state of change (i.e. folding up) or in a
	 * state of stress (caught in some bad position).  In either case, we will
	 * reject new bonds when we're outside of tolerances.
	 */
	private void checkTolerances() {
			
		// Assume we're within CodonParameters.FLEX_TOLERANCE on all arms, look for one that
		// isn't.
		this._withinTolerances = true;

		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
			if (!this.isArmWithinTolerance(i)) {
				this._withinTolerances = false;
				break;
			}
		}
		
	}

	/** Is the given arm within CodonParameters.FLEX_TOLERANCE?
	 */
	public boolean isArmWithinTolerance(int arm) {
		if (this._state._bonds[arm] != null) {
			Codon other = this._state._bonds[arm];
			int otherArm = CodonParameters.BOND_ARM[arm];

			double difference = PI + (this._state._angle + CodonParameters.ARM_ANGLE[arm]) - (other._state._angle + CodonParameters.ARM_ANGLE[otherArm]);
			if (this._state._folded) {	
				difference -= CodonParameters.JOINT_ANGLE[arm][this._type][other._type];
			}

			difference = this.normalize(difference);
			if (Math.abs(difference) > CodonParameters.FLEX_TOLERANCE) {
				return false;
			}
		}
		return true;
	}

	private void handleResetCounter() {
		// If we gained an up-partner during this timestep and we aren't folded,
		// then trigger a _resetCounter.
		if (this._timestep._bonds[CodonParameters.UP_ARM] != this._state._bonds[CodonParameters.UP_ARM] && !this._state._folded) {
			this._timestep._resetCounter = true;
		}

		// If we have a _resetCounter, turn it off.  (It will still propagate,
		// since our partner reads _state, but we're only updating _timestep)
		if (this._state._resetCounter) { 
			this._timestep._resetCounter = false; 
			
			// Reset the counter for everybody; it's used to detect incomplete splits.
			this._timestep._iterationsSinceSplit = 0;

		}

		// If our right-neighbour has _resetCounter, pick it up.
		if (this._state._bonds[CodonParameters.RIGHT_ARM] != null && this._state._bonds[CodonParameters.RIGHT_ARM]._state._resetCounter) {
			this._timestep._resetCounter = true;
		}
	}
	
	/** Check whether we should shatter, or do other similar things.
	 * 
	 * After a chain has been trying to collect partners for long enough, it
	 * should fold up.
	 *
	 * If a partner notices that the chain it has attached to has folded, it
	 * should let go, and set itself to the SHATTER state, which will
	 * propagate to its neighbours.
	 *
	 * Chains self-identify because they have a running _iterationsSinceSplit
	 * counter.  Codons that are part of a potential copy (i.e. were recently
	 * free-floating) can self-identify because they have no such counter.
	 *
	 * Note that by using the _iterationsSinceSplit counter, the initial seed will never
	 * do this until it has made a copy.  That's a good thing -- we wouldn't
	 * want the whole thing to fail just because our seed folded up and went
	 * home.
	 */
	private void handleReleasing() {

		if (this._state._hasSplit) {
			this._timestep._iterationsSinceSplit++;
		}

		if (!this._state._folded) {
				
			// Check if we should fold.  There are two cases to fold.  
			// 1. If our _iterationsSinceSplit has hit its limit, and we have no
			//    left bond, and we're not currently splitting, then we fold up.
			//    (Unless we're the seed, which never folds.)
			// 2. If our left partner has folded up (and we haven't, since that's
			//    a requisite for being in this if block), then we fold.
			// 
			// Under no circumstances do we fold if we or our left neighbour has an
			// _unfoldSignal.
			//
			// In effect, the leftmost codon decides when to fold.  This is
			// because we want someone to decide based on the
			// _iterationsSinceSplit, but that might be different for different
			// codons, because of the _resetCounter signal/state.

			Codon left = this._state._bonds[CodonParameters.LEFT_ARM];
			if ((this._state._iterationsSinceSplit >= CodonParameters.ITERATIONS_AFTER_SPLIT && left == null && this._state._splittingState != CodonState.SPLIT_GO && this._state._splittingState != CodonState.SPLIT_SHATTER && !this._state._isReplicationSeed)
					|| (left != null && left._state._folded && !left._state._unfoldSignal)) {
				System.out.println("State: (" + this + ") folded");
				assert (this._state._hasSplit);
				this._timestep._folded = true;
				this._timestep._iterationsSinceSplit = 0;
			}

			Codon other = this._state._bonds[CodonParameters.UP_ARM];

			// Check if our partner folded, so we should shatter off.
			if (other != null && (other._state._folded)) { 
				this._timestep._splittingState = CodonState.SPLIT_SHATTER;
				this.changeBond(CodonParameters.UP_ARM, null, "partner folded");
			} 	

		}

	}

	/** Change the given arm's bond.  This is silently ignored if the bond has
	 * already been changed during this timestep. 
	 *
	 * @return true if the change was successful, false otherwise.
	 */
	private final boolean changeBond(int arm, Codon newBond, String reason) {

		int otherArm = CodonParameters.BOND_ARM[arm];

		// Assert: Our partner on the given arm has changed if and only if we have changed.
		
		if (this._state._bonds[arm] == this._timestep._bonds[arm]) {
			Codon oldBond = this._state._bonds[arm];

			if (newBond == null && oldBond != null && oldBond._state._bonds[otherArm] == oldBond._timestep._bonds[otherArm]) {
				System.out.println("Bond: (" + this._id + " @ " + arm + "," +
				oldBond._id + " @ " + otherArm + ") broken: " + reason + ". <" + this._state + "," + oldBond._state + ">.");
				// Check if we have a bond -- if so, tell our partner to break.  (If
				// the new bond is null and the old bond is null, we don't need to
				// do anything.)
				oldBond._timestep._bonds[otherArm] = null;
				this._timestep._bonds[arm] = null;
				return true;
				
			} else if (newBond != null && (newBond._state._bonds[otherArm] == newBond._timestep._bonds[otherArm])) {
				// New bond is not null, and the new bond partner hasn't changed in
				// this timestep, so we can bond.
				this._timestep._bonds[arm] = newBond;
				newBond._timestep._bonds[otherArm] = this;

				// Here, if we're actually breaking a bond and replacing it (seems
				// unlikely to occur?), then we need to tell the old bond that much.
				if (oldBond != null) {
					System.out.println("Bond switch!");
					oldBond._timestep._bonds[otherArm] = null;
				}
				return true;

			} // else do nothing.
		}

		return false;

	}
	
	/** {@inheritDoc} */
	public final void interact(Codon other, boolean firstRun) {
		
		// Drop out immediately if we're not even close to the other one and
		// either one of us has no bonds.  (If one of us has bonds, it's
		// conceivable that we're bonded to each other, although this should be
		// impossible..)
		if (this._state._position.getDistanceSquared(other._state._position) > (4 * CodonParameters.MAX_INTERACTION_RADIUS * CodonParameters.MAX_INTERACTION_RADIUS)) {
			if (!this._bonded || !other._bonded) { 
				return;
			} else {
				boolean found = false;
				for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
					if (this._state._bonds[i] == other) {
						found = true;
						break;
					}
				}
				if (!found) return;
			}
		}

		if (this._state._splittingState == CodonState.SPLIT_GO) {
			// Up arms don't do anything during a split, but they need to be
			// called to make sure bonding information is being updated.
			this.interactArms(other, CodonParameters.UP_ARM, CodonParameters.UP_ARM, NONE, STRAIGHT_SPRING, false);
		} else {

			// Check if we should attract or repel.  Likes ignore, opposites attract.
			if (!this._state._folded && !other._state._folded && this._type == other._type) {
				
				// One of the two participants must have a left or right bond, and
				// their split-value must be different.

				boolean hasBond = (this._state._bonds[CodonParameters.LEFT_ARM] != null || this._state._bonds[CodonParameters.RIGHT_ARM] != null || other._state._bonds[CodonParameters.RIGHT_ARM] != null || other._state._bonds[CodonParameters.LEFT_ARM] != null);

				boolean oneHasSplit = (this._state._hasSplit != other._state._hasSplit);
				
				boolean noSplitChange = (this._state._hasSplit == this._timestep._hasSplit && other._state._hasSplit == other._timestep._hasSplit);
				
				boolean notSplitting = (this._state._splittingState != CodonState.SPLIT_GO && other._state._splittingState != CodonState.SPLIT_GO);

				boolean notUnfolding = (!this._state._unfoldSignal && !other._state._unfoldSignal);

				boolean canBond = hasBond && oneHasSplit && noSplitChange && notSplitting && notUnfolding;
				
				this.interactArms(other, CodonParameters.UP_ARM, CodonParameters.UP_ARM, ATTRACT, STRAIGHT_SPRING, canBond);

			} else if (this._state._folded && other._state._folded) {
				
				int myBend = this.getBendState();
				int otherBend = other.getBendState();
				
				// One or 'tother must be in the mesh.  Also, the bending states
				// have to match up.  TODO: document beding states more.
				boolean canBond = (this._state._inMesh || other._state._inMesh)
					&& (!this._state._unfoldSignal && !other._state._unfoldSignal)
					&& ((myBend == LEFT_OF_BEND && otherBend == RIGHT_OF_BEND) 
					 || (myBend == RIGHT_OF_BEND && otherBend == LEFT_OF_BEND) 
					 || (myBend == IN_BEND && otherBend == IN_BEND));
					
				this.interactArms(other, CodonParameters.UP_ARM, CodonParameters.UP_ARM, CodonParameters.SITE_BONDING[this._type][other._type], STRAIGHT_SPRING, canBond);
				
				// This is the only circumstance for interacting the OVERLAP_ARMs.  (If
				// they bond, they end up broken by a shatter later.)
				if (this._state._inMesh && other._state._inMesh) {
					this.interactArms(other, CodonParameters.OVERLAP_ARM, CodonParameters.OVERLAP_ARM, ATTRACT, STRAIGHT_SPRING, true);
				}
				

			}

		}
		
		// We only allow bonding for left/right arms if they're up-bonded or
		// folded.  This prevents errant pairs from forming.  As a special
		// case, in the first iteration, we can bond so that the seed forms a
		// chain properly.
		// We also block left/right bonds if we've split before, since split
		// chains basically shouldn't change.
		boolean canBond = firstRun 
				|| (this._state._folded && other._state._folded) 
				|| ((this._state._bonds[CodonParameters.UP_ARM] != null && other._state._bonds[CodonParameters.UP_ARM] != null) && !this._state._hasSplit && !other._state._hasSplit);

		
		// Check the left and right arms.
		this.interactArms(other, CodonParameters.LEFT_ARM, CodonParameters.RIGHT_ARM, ATTRACT, STRAIGHT_SPRING, canBond);

		// Check the right and left arms. (opposite order from above wrt left/right,
		// everything else is the same.
	 this.interactArms(other, CodonParameters.RIGHT_ARM, CodonParameters.LEFT_ARM, ATTRACT, STRAIGHT_SPRING, canBond);

		if (this._state._splittingState == CodonState.SPLIT_GO && other._state._splittingState == CodonState.SPLIT_GO) 
			this.interactArms(other, CodonParameters.REPELLER_ARM, CodonParameters.REPELLER_ARM, REPEL, SPRING, false);
	}

	/** Interact two arms, applying forces to the codons on each end, etc.  
	 */
	private final void interactArms(Codon other, int myArm, int otherArm, int forceDirection, int forceType, boolean canBond) {

		boolean bonded = (this._state._bonds[myArm] == other);
		// Bonding must be bidirectional
		assert bonded == (other._state._bonds[otherArm] == this);

		// Get the distance, and its square.
		double distSq = this._armPositions[myArm].getDistanceSquared(other._armPositions[otherArm]);
		double dist = Math.sqrt(distSq);

		if (!bonded && (dist > CodonParameters.FIELD_RADIUS[myArm] + CodonParameters.FIELD_RADIUS[otherArm])) {
			return;
		}
			
		// Figure out which radii we want for each arm.	
		double myRadius = CodonParameters.FIELD_RADIUS[myArm];

		double otherRadius = CodonParameters.FIELD_RADIUS[otherArm];

		// check if they're touching.
		boolean touching = dist < (myRadius + otherRadius);

		if (!touching) {
			if (bonded) {
				// break the bond.
				this.changeBond(myArm, null, "not touching");
				if (this._state._hasSplit && (myArm == CodonParameters.LEFT_ARM || myArm == CodonParameters.RIGHT_ARM)) {
					System.out.println("Split codon lost partner!");
				}

				boolean splitting = 
					(this._state._splittingState == CodonState.SPLIT_GO &&
					other._state._splittingState == CodonState.SPLIT_GO);
					
				// TODO The UP_ARM is out of place here; some sort of boolean
				// parameter(s) should probably be passed in to determine whether we
				// should trigger shatter.  But for now, left-right breaks should
				// always shatter, even if we're SPLIT_GO.

				if (!splitting || myArm != CodonParameters.UP_ARM) {
					System.out.println("Broken bond -> Shatter self and partner!");
					this._timestep._splittingState = CodonState.SPLIT_SHATTER;
					other._timestep._splittingState = CodonState.SPLIT_SHATTER;
				} 

			}
			return;
		}

		if (!bonded && forceDirection == ATTRACT) {
			// Try to bond.

			if (
					// Not allowed to bond
					!canBond 
					
					// We know we're not bonded to each other, but one of us is bonded
					// somewhere else, so don't bond.
					|| (this._state._bonds[myArm] != null || other._state._bonds[otherArm] != null)

					// Different foldednesses
					|| (this._state._folded != other._state._folded) 

					// One or 'tother is shattering
					|| (this._state._splittingState == CodonState.SPLIT_SHATTER || other._state._splittingState == CodonState.SPLIT_SHATTER)

					// Some bonds are not in line
					|| (!this._withinTolerances || !other._withinTolerances)) {

				return;
			}
			
			// Figure out the angle difference to see if we should bond.
			// The angle difference is adjusted by PI because we want them to be at
			// 180 degrees, not the same angle.
			double difference = PI + this._state._angle + CodonParameters.ARM_ANGLE[myArm] - (other._state._angle + CodonParameters.ARM_ANGLE[otherArm]);
			if (this._state._folded) {
				assert other._state._folded;
				difference -= CodonParameters.JOINT_ANGLE[myArm][this._type][other._type];
			}
		
			double tolerance = CodonParameters.BOND_TOLERANCE[myArm] + CodonParameters.BOND_TOLERANCE[otherArm];
			difference = this.normalize(difference);

			if ((difference <= tolerance) && (-difference <= tolerance)) {
				// If we're inside the radius of both codons, and our angles are
				// sufficiently close, and we haven't already bonded to someone else in
				// this timestep, then bond.
				
				System.out.println("Bond: (" + this._id + ", " + other._id + ")\tbonding @ (" + myArm + ", " + otherArm + ")\t (" + this._state + " ; " + other._state + ")]\tBonded: <" + this.isBonded()+ ", " + other.isBonded() + ">\tangle: " + difference);
				// create a bond
				this.changeBond(myArm, other, "bonded");
				bonded = true;

			} else return;
		} 
			

		/*assert ((bonded && touching && forceDirection == ATTRACT) 
				|| (touching && forceDirection == REPEL) 
				|| forceDirection == NONE); */

		// Create a unit vector pointing from this's arm to the other's arm.
		Pair force = (Pair)this._armPositions[myArm].clone();
		force.subtract(other._armPositions[otherArm]);
		force.normalize();

		double rotationalAcceleration = 0.0;
		double targetAngle = 0.0;
		double rotationAngle;

		switch (forceType) {
			case STRAIGHT_SPRING:
				if (forceDirection == ATTRACT) {

					// The angle that we want this bond to be at.
					if (this._state._folded && this._state._splittingState != CodonState.SPLIT_GO) {
						// If we've replicated enough times, we're now a 'phenotype',
						// and so we should bend and not accept any new bonds.  But we
						// don't want to do this until we're totally done splitting --
						// i.e., until we have repelled away our neighbour.  Otherwise
						// we tend to get tangled up.
						// This is divided by two because we want to "spread out" the
						// cumulative rotation between the two codons.
						targetAngle = (CodonParameters.JOINT_ANGLE[myArm][this._type][other._type] - CodonParameters.JOINT_ANGLE[otherArm][other._type][this._type]) / 4.0;

						// The formula below should work, but doesn't seem to.  The one
						// above is magic, and shouldn't work, but seems to.  TODO.
						//CodonParameters.JOINT_ANGLE[myArm][this._type][other._type];
					}

					// atan2 will be between -PI and +PI.  FIXME account for 0,0
					// result will be between 4PI and -2PI

					// This is the angle that we want to the codons to end
					// up at, with respect to the current interaction.
					// TODO: Why is this negative??!
					targetAngle += -CodonParameters.ARM_ANGLE[myArm] + Math.atan2(-this._state._position.y + other._state._position.y, -this._state._position.x + other._state._position.x);

					// Make it be between -PI and PI
					rotationAngle = this.normalize(targetAngle - this._state._angle );
					
					// Decrease the force with ln.  Have to deal with negative
					// rotations.

					this._angularAcceleration += rotationAngle * CodonParameters.STRAIGHTENING_FORCE[myArm];
					other._angularAcceleration -= rotationAngle * CodonParameters.STRAIGHTENING_FORCE[otherArm];
				}

				// switch case FALL-THROUGH!
				
			case SPRING:      
				// A spring that is attracting acts like a spring being stretched - it
				// pulls more the farther away you are.
				if (forceDirection == ATTRACT) {
					double scalar = (CodonParameters.ARM_FORCE[myArm] / (myRadius + otherRadius)) * dist;
					if (scalar == 0) {
						force.x = 0; force.y = 0;
					} else {
						force.scale(-scalar);
					}

					// Dampen this pair towards their average velocity
					Pair centerOfMassVel = (Pair)this._state._velocity.clone();
					centerOfMassVel.add(other._state._velocity);
					centerOfMassVel.scale(0.5);

					Pair relativeVelocity = (Pair)this._state._velocity.clone();
					relativeVelocity.subtract(centerOfMassVel);

					// Dampen it, and turn it into an acceleration
					relativeVelocity.scale(SimulationParameters.LINEAR_SPRING_DAMPING_FACTOR);

					//assert (relativeVelocity.isFinite());
					this._acceleration.subtract(relativeVelocity);
					other._acceleration.add(relativeVelocity);
					
				} else if (forceDirection == REPEL) {
					
					// A spring that is repelling acts like a spring under compression -
					// it pushes more and more the closer you get.
					force.scale(forceDirection * (CodonParameters.ARM_FORCE[myArm] / CodonParameters.FIELD_RADIUS[myArm])
							* (CodonParameters.FIELD_RADIUS[myArm] + CodonParameters.FIELD_RADIUS[otherArm] - dist) );
					force.negate();
					
				} else {
					// No force actually acting.  The call to interact() was made just to
					// update the bond information.
					//assert(forceDirection == NONE);
				}
					
				break;
		}

		if (forceDirection != NONE) {
			this._forces[myArm].add(force);
			other._forces[otherArm].subtract(force);
		}
	 
	}

	/** Modify the velocity of this codon with some brownian motion. */
	private final void brownianMotion() {
		// See e.g. http://en.wikipedia.org/wiki/Talk:Brownian_motion
		double tsSqrt = Math.sqrt(SimulationParameters.TIMESTEP_DURATION);
		this._timestep._velocity.x += tsSqrt * (Math.random() - 0.5) * SimulationParameters.LINEAR_BROWNIAN_MOTION;
		this._timestep._velocity.y += tsSqrt * (Math.random() - 0.5) * SimulationParameters.LINEAR_BROWNIAN_MOTION;
		this._timestep._angularVelocity += tsSqrt * (Math.random() - 0.5) * SimulationParameters.ANGULAR_BROWNIAN_MOTION;
	}

	/** Figure out which of the bending states this codon is in.  It might be
	 * tempting to calculate this information for each timestep, but if you
	 * think about it, most codons will need it only in a tiny percent of their
	 * timesteps, so it's not worth it. */
	private int getBendState() {
		Codon left = this._state._bonds[CodonParameters.LEFT_ARM];
		Codon right = this._state._bonds[CodonParameters.RIGHT_ARM];

		// TODO: This could probably use some cleanup.

		if (this._type == EXTENSION_CODON) {
			return BEND_OTHER;
		} else if (left == null || left._type != EXTENSION_CODON) {
			if (right == null || right._type != EXTENSION_CODON) {
				return IN_BEND;
			} else {
				return RIGHT_OF_BEND;
			}
		} else if (right == null || right._type != EXTENSION_CODON) {
			// We know the left arm is there and of type EXTENSION_CODON.
			return LEFT_OF_BEND;
		} else {
			return BEND_OTHER;
		}

	}

	public void finishTimestep(int containerSize) {
		this.updateState();
		this.updateVelocities();
		this.updatePositions(containerSize);
	}

	/** Moves all of the changes accumulated during the passed timestep to the
	 * definitive state.  This must be called after finishTimestep() has been
	 * called for <em>all</em> codons. */
	public void copyStates() {
		this._state.copyFrom(this._timestep);
	}

	/** Update the state of this codon.  Must be called before updateBonds().*/
	private void updateState() {

		this.handleReleasing();
		this.handleResetCounter();

		Codon up = this._state._bonds[CodonParameters.UP_ARM];
		Codon left = this._state._bonds[CodonParameters.LEFT_ARM];
		Codon right = this._state._bonds[CodonParameters.RIGHT_ARM];
		Codon overlap = this._state._bonds[CodonParameters.OVERLAP_ARM];

		if(up != null && up._state._childIsMeshSeed) { 
			// TODO: This should not be copied until the chain thinks it's actually
			// splitting off correctly!
			this._timestep._inMesh = true; 
		} else {
			// Propagate _inMesh from anywhere we can, but only if we're w/i
			// tolerance.
			boolean meshedPartner = 
				((up != null && up._state._inMesh)
				 || (left != null && left._state._inMesh)
				 || (right != null && right._state._inMesh));
			if (meshedPartner && !this._timestep._inMesh && this._withinTolerances && !this._state._isReplicationSeed) {
				this._timestep._inMesh = true;
			}
		}

		int chainState = this._state._chainPositionState;

		// The chain state of the up codon, or -1 if there isn't one. 
		int upChainState = (up == null)?-1:up._state._chainPositionState;
		
		// Propagate shattering from left and right always, and from up only if
		// we're not folded.  Note that we don't allow a "SHATTER" signal to
		// propagate from a codon with _hasSplit false to a codon with _hasSplit
		// true.
		boolean shouldShatter = (left != null && left._state._splittingState == CodonState.SPLIT_SHATTER)
			|| (right != null && right._state._splittingState == CodonState.SPLIT_SHATTER)
			|| (!this._state._folded && up != null && (!this._state._hasSplit || up._state._hasSplit) && up._state._splittingState == CodonState.SPLIT_SHATTER);

		// Propagate unfolding from left and right if we're folded, trigger it if we're
		// overlapping or have been out of tolerance for a long time, plus keep the
		// unfold signal if we haven't unfolded successfully yet.

		boolean triggerUnfold = (overlap != null && this._id < overlap._id)
			|| (this._timestep._iterationsOutOfTolerance > CodonParameters.ITERATIONS_OUT_OF_TOLERANCE && up != null && this._id < up._id);

		boolean propagateUnfold = 
			(this._state._folded && (
				(left != null && left._state._unfoldSignal)
				|| (right != null && right._state._unfoldSignal)))
			|| (overlap != null && this._id < overlap._id)
			|| (this._timestep._iterationsOutOfTolerance > CodonParameters.ITERATIONS_OUT_OF_TOLERANCE && up != null && this._id < up._id)
			|| (this._state._unfoldSignal && (this._state._folded || up != null));

		// We change this now; we don't assume that it actually worked until the
		// next timestep when we check that we made it through this timestep
		// properly unfolded.
		if (triggerUnfold || propagateUnfold) {
			this._timestep._unfoldSignal = true;
			this.changeBond(CodonParameters.UP_ARM, null, "unfolding: " + triggerUnfold + "," + propagateUnfold);
			this.changeBond(CodonParameters.OVERLAP_ARM, null, "unfolding: " + triggerUnfold + "," + propagateUnfold);
			this._timestep._folded = false;
			this._timestep._inMesh = false;


			// Only if we're the source of the unfolding, break our right bond, so
			// that we can unfold properly.)
			if (triggerUnfold) {
				System.out.println("\nTriggering unfold from overlap: " + this + ", " + overlap);
				//try { System.in.read(); } catch (Exception e) { }
				this.changeBond(CodonParameters.RIGHT_ARM, null, "Unfolding-break");
			} else {
				System.out.println("\nPropagating unfold from overlap: " + this);
			}

		}

			
		/*
		if (overlap != null && this._id < overlap._id) {
			System.out.println("Shattering from overlap: " + this + ", " + overlap);
			try { System.in.read(); } catch (Exception e) { }
		}

		if (this._timestep._iterationsOutOfTolerance > CodonParameters.ITERATIONS_OUT_OF_TOLERANCE && up != null && this._id < up._id) {
			System.out.println("Shattering from long term intolerances: " + this + ", " + overlap);
			try { System.in.read(); } catch (Exception e) { }
		}*/

		if (this._state._splittingState == CodonState.SPLIT_SHATTER) {
			
			// Break all bonds.
			for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
				this.changeBond(i, null, "shattering");
			}

			// This makes sure we keep trying until we have no more bonds.  It's
			// not clear to me why this is necessary, but it is.
			if (!this._bonded) {
				this._timestep._splittingState = CodonState.SPLIT_NONE;
			}
			this._timestep._chainPositionState = CodonState.CHAIN_DEFAULT;
			this._timestep._iterationsSinceSplit = 0;
			this._timestep._hasSplit = false;
			this._timestep._folded = false;
			this._timestep._unfoldSignal = false;
			System.out.println("State: (" + this._id + ") Shatter executed, returned to default state.");

		} else if (shouldShatter) {// && !this._state._folded) {
			
			this._timestep._splittingState = CodonState.SPLIT_SHATTER;
			System.out.println("State: Neighbour in CodonState.SPLIT_SHATTER, so we are too: " + this);

		} else if (this._state._splittingState == CodonState.SPLIT_NONE) {

			if ((this._state._chainPositionState == CodonState.CHAIN_TRUE_END && upChainState == CodonState.CHAIN_TRUE_END && left == null)
					|| ((chainState == CodonState.CHAIN_TRUE_END || chainState == CodonState.CHAIN_DEFAULT) && 
							(upChainState == CodonState.CHAIN_TRUE_END || upChainState == CodonState.CHAIN_DEFAULT)
							&& left != null && left._state._splittingState == CodonState.SPLIT_READY)) {
				this._timestep._splittingState = CodonState.SPLIT_READY;
			}
				
		} else if (this._state._splittingState == CodonState.SPLIT_READY) {
			
			if ((chainState == CodonState.CHAIN_TRUE_END && upChainState == CodonState.CHAIN_TRUE_END && right == null)
					|| ((chainState == CodonState.CHAIN_TRUE_END || chainState == CodonState.CHAIN_DEFAULT)
					 && (upChainState == CodonState.CHAIN_TRUE_END || upChainState == CodonState.CHAIN_DEFAULT)
					 && right != null && right._state._splittingState == CodonState.SPLIT_GO)) {
				
				this._timestep._splittingState = CodonState.SPLIT_GO;
				//System.out.println("State: Split: Ready . Go.");
			}

		} else if (this._state._splittingState == CodonState.SPLIT_GO) {
		
			if (this._state._repelIterations >= CodonParameters.REPEL_ITERATIONS) {
				// FIXME: Add counter-reset propagation, check for no-left-bond.
				 //(left != null && left._state._splittingState == CodonState.SPLIT_NONE)) {
				/* No worky. ...dunno why.. */
				if (up != null) { // && !this._state._hasSplit) {
					// Somehow we went through a split without losing our partner.
					// Shatter.
					System.out.println("FAILED TO SPLIT: SHATTERING: " + this + " UP=" + up);
					//try { System.in.read(); } catch (Exception e) { }
					this._timestep._splittingState = CodonState.SPLIT_SHATTER;
					this._timestep._repelIterations = 0;
				} else {
					// Split up properly, no problems.
					this._timestep._splittingState = CodonState.SPLIT_NONE;
					this._timestep._repelIterations = 0;
					this._timestep._iterationsSinceSplit = 0;
					this._timestep._hasSplit = true;
					this._timestep._childIsMeshSeed = false;

					// This will be true when we divide if our partner had
					// _childIsMeshSeed when we met them, and so we set our _inMesh.
					// Now we fold.
					if (this._state._inMesh) {
						this._timestep._folded = true;
					}

				}
			}

		}

		switch (this._state._chainPositionState) {
			case CodonState.CHAIN_DEFAULT:
				if (((left == null) != (right == null)) && up != null) {
					this._timestep._chainPositionState = CodonState.CHAIN_END;
					//System.out.println("State: Chain: Default . End.");
				}
				break;
			case CodonState.CHAIN_END:
				// No longer bonded on exactly one of left and right, or no longer
				// bonded above.
				if ((((left == null) == (right == null)) || up == null)) {
					this._timestep._chainPositionState = CodonState.CHAIN_DEFAULT;
				} else if (upChainState == CodonState.CHAIN_END || upChainState == CodonState.CHAIN_TRUE_END) {
					this._timestep._chainPositionState = CodonState.CHAIN_TRUE_END;
				}
				break;
			case CodonState.CHAIN_TRUE_END:
				// No longer bonded on exactly one of left and right, or no longer
				// bonded above.
				if (((left == null) == (right == null)) || up == null || upChainState == CodonState.CHAIN_DEFAULT) {
					this._timestep._chainPositionState = CodonState.CHAIN_DEFAULT;
				}
				break;
			default:
		}
		
	}

	/** Store any bond-changes that occurred during the last iteration.  Must be called after updateState() has been called for <em>all</em> codons.
	 */
	private void updateVelocities() {

		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) { 
			
			// Don't bother doing anything if the force was zero for this arm.
			if (!this._forces[i].isZero()) {
				
				// Create a unit vector pointing from the position to the end of the arm,
				// as of the beginning of this timestep.
				Pair armVector = (Pair)this._armPositions[i].clone();
				armVector.subtract(this._state._position);
				armVector.normalize();

				// Calculate the magnitude of the tangential force
				// TODO - This is an excessive calculation for what we are trying to
				// get.
				Pair tangential = this._forces[i].getPerpendicularProjectionOnto(armVector);
				
				// Calculate the rotation caused by the tangential force
				// This converts some of the tangential tug into rotation
				//double armLength = CodonParameters.ARM_FORCE[i];
				double armLength = CodonParameters.ARM_LENGTH[i];
				double angularAcceleration = 
					 (2 * armLength * tangential.getLength())
					/ (CodonParameters.CODON_RADIUS * CodonParameters.CODON_RADIUS + 2 * armLength * armLength);

				/* from _Physics for Game Developers_
				 * "To calculate the torque applied by a force acting on an
				 * object, you need to calculate the perpendicular distance from the
				 * axis of rotation to the line of action of the force and then
				 * multiply this distance by the magnitude of the force.  This
				 * calculation gives the magnitude of the torque...As a vector, the
				 * line of action of torque is along the axis of rotation, with the
				 * direction determined by the direction of rotation and the
				 * righthand rule." p.65
				 */

				assert angularAcceleration > 0;
				assert tangential.getLength() - angularAcceleration > 0;

				// So currently we have the correct magnitude for the angular
				// acceleration, but we need to know which way.  But while it's
				// positive, we calculate the linear acceleration.

				// The rest of the tug is converted into (linear) acceleration.
				//  Because our mass is treated as being a unit, forces and
				//  accelerations are interchangeable.  
				Pair acceleration = tangential;
				acceleration.scaleTo(tangential.getLength() - angularAcceleration);
				
				// We want the sign on the angularAcceleration to be positive if the
				// angle between the force and the arm is 0..PI, and negative if
				// it's PI..2PI (equivalently -PI..0).
				// http://www.geocities.com/SiliconValley/2151/math2d.html
				
				// The force, rotated by 90 degrees.
				Pair temp = new Pair(this._forces[i].y, -this._forces[i].x);
				if (armVector.getDotProduct(temp) < 0) {
					angularAcceleration = -angularAcceleration;
				} 
				
				// Calculate the movement of the codon, by getting a unit vector 
				// orthogonal to the arm pointing in the direction of the tangential
				// force, then scaling it to the size of the tangential force.
				//Pair acceleration = armVector;
				//acceleration.orthogonalize();
				//
				// Sometimes this force is tiny but negative, presumably due to
				// rounding errors.  
				assert tangential.getDotProduct(this._forces[i]) >= -0.0000001: "Tangential force " + tangential + " opposes arm force: " + this._forces[i] + " for " + this;

				// Calculate the radial force.  This is easy peasy.  Add it to the
				// radial part of the tangential force. 
				
				Pair radial = this._forces[i].getProjectionOnto(armVector);
				acceleration.add(radial);

				// Add the linear change..
				this._acceleration.add(acceleration);

				// ..and the rotation.
				this._angularAcceleration += angularAcceleration;
				
			}
		}

		// Update the Codon's velocities
		Pair deltaVel = (Pair)this._acceleration.clone();
		deltaVel.scale(SimulationParameters.TIMESTEP_DURATION);
		this._timestep._velocity.add(deltaVel);

		this._timestep._angularVelocity += (this._angularAcceleration * SimulationParameters.TIMESTEP_DURATION);

		// The codons are in a liquid, so we dampen their velocity that was carried
		// over from the previous timestep.

		// To do this, we calculate how many straight-spring-attraction interactions
		// occurred.  We multiply the angular velocity by the damping factor once
		// for each of these.
		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
			if (this._state._bonds[i] != null) {
				this._timestep._angularVelocity *= (SimulationParameters.ANGULAR_SPRING_DAMPING_FACTOR);
			}
		}
		this._timestep._angularVelocity *= (SimulationParameters.ANGULAR_VISCOSITY_FACTOR);

		// The velocity is damped towards zero.
		this._timestep._velocity.scale(SimulationParameters.LINEAR_VISCOSITY_FACTOR);
		
		// Brownian motion
		this.brownianMotion();

	}


	/** Update the position of the codons.  This should be called after all
	 * codons have interacted with each other, and the velocities have been
	 * calculated.  
	 * @param containerSize The size of the container.  This should always
	 * have the same value.  Behaviour is undefined otherwise. */
	private final void updatePositions(int containerSize) {
		
		// Update the codon's position 
		
		// Angle
		this._timestep._angle += (this._state._angularVelocity * SimulationParameters.TIMESTEP_DURATION);

		// (Linear) position 
		Pair deltaPos = (Pair)this._state._velocity.clone();
		deltaPos.add(this._state._velocity);
		deltaPos.scale(SimulationParameters.TIMESTEP_DURATION);

		this._timestep._position.add(deltaPos);

		// Reduce the angle "mod pi"
		this._timestep._angle = normalize(this._timestep._angle);
		
		// Bounce the codon off the walls.  If the possible forces were much larger
		// than the container, it would become necessary to repeat this process
		// until the codon ended up within the walls. 
		if (this._timestep._position.x < -containerSize) {
			 this._timestep._position.x = (-2 * containerSize) - this._timestep._position.x;
			 this._timestep._velocity.x = -this._timestep._velocity.x;
		} else if (this._timestep._position.x > containerSize) {
			 this._timestep._position.x = (2 * containerSize) - this._timestep._position.x;
			 this._timestep._velocity.x = -this._timestep._velocity.x;
		}
		
		if (this._state._position.y < -containerSize) {
			 this._timestep._position.y = (-2 * containerSize) - this._timestep._position.y;
			 this._timestep._velocity.y = -this._timestep._velocity.y;
		} else if (this._state._position.y > containerSize) {
			 this._timestep._position.y = (2 * containerSize) - this._timestep._position.y;
			 this._timestep._velocity.y = -this._timestep._velocity.y;
		}

	}

	/** Return an angle equivalent to the given angle, guaranteed to be
	 * between -PI and PI.
	 */
	private static final double normalize(double angle) {
		while (angle > PI) angle -= (2 * PI);
		while (angle < -PI) angle += (2 * PI);
		return angle;
	}

	/** @return the current linear acceleration.  If this is done at the end
	 * of a timestep, it's meaningful.  If it's done during a timestep, then
	 * it will show only part of the accumulated acceleration. */
	public Pair getAcceleration() {
		return this._acceleration;
	}

	/** @return the current angular acceleration.  If this is done at the end
	 * of a timestep, it's meaningful.  If it's done during a timestep, then
	 * it will show only part of the accumulated acceleration. */
	public double getAngularAcceleration() {
		return this._angularAcceleration;
	}

	/** @return The ID of the codon bonded to the given arm, or -1 if there is
	 * no such codon.  This is gross, but it allows us an optimization when
	 * drawing, to avoid drawing all bonds twice (from both sides).  see
	 * CodonViewer.drawBonds(..). */
	public int getBondPartnerID(int arm) {
		if (this._state._bonds[arm] == null) {
			return -1;
		} else {
			return this._state._bonds[arm]._id;
		}
	}

	/** @return The location of the tip of the arm that the given arm of this
	 * codon is bonded to, or null if the given arm isn't bonded to anyone. */
	public Pair getBondPartnerLocation(int arm) {
		if (this._state._bonds[arm] == null) {
			return null;
		} else {
			return this._state._bonds[arm]._armPositions[CodonParameters.BOND_ARM[arm]];
		}
	}

	/** Return our ID number */
	public String toString() {
		return this._id + "\t" + this._state;
	}

	public Pair getPosition() { 
		return this._state._position;
	}

	public int getType() {
		return this._type;
	}

	public double getAngle() {
		return this._state._angle;
	}
		

	/** Check whether this codon has split already. */
	public boolean hasSplit() { 
		return this._state._hasSplit; 
	}

	/** Check whether any of this codon's arms are currently bonded. */
	public boolean isBonded() { 
		return this._bonded; 
	}

	public boolean isFolded() {
		return this._state._folded;
	}

	/** Strictly for drawing.  
	 * @return a clone of the current acceleration on the given arm. */
	public final Pair getArmAcceleration(int arm) { return (Pair)this._forces[arm].clone(); }

	/** Strictly for drawing.
	 * @return a clone of the current position of the given arm. */
	public final Pair getArmPosition(int arm) { return (Pair)this._armPositions[arm].clone(); }

	/** Get the current radius of the field at the end of the given arm. */
	public final double getFieldRadius(int arm) {
		return CodonParameters.FIELD_RADIUS[arm];
	}

	public boolean isWithinTolerance() {
		return this._withinTolerances;
	}
		
}
