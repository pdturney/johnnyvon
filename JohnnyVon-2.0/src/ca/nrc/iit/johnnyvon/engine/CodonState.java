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


/** A complete description of the state of a codon.  Used to maintain the 
 * state as it existed at the beginning of at timestep, as well as keep an
 * updated copy as a timestep is executed.
 * 
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
/* package */ final class CodonState {

	// Possible chain position states.  These are the "x y z" states in the
	// technical paper. 
	
	/** The starting state. */
	/* package */ static final int CHAIN_DEFAULT = 0;
	
	/** The codon might be at the end of a complete double strand. */
	/* package */ static final int CHAIN_END = 1;
	
	/** When it is at the end of a complete double strand. */
	/* package */ static final int CHAIN_TRUE_END = 2;

	// The possible splitting states.  These are the "0 1 2" of the  tech
	// paper.  

	/** The starting state. */
	/* package */ static final int SPLIT_NONE = 0;
	 
	/** the codon should split, based on its neighbours.  This propagates through the
	 * chain. */
	/* package */ static final int SPLIT_READY = 1;
	
	/** Once SPLIT_READY has gone all the way down a chain, SPLIT_GO propagates the 
	 * other way, triggering the repelling "UP_ARM" as it goes. */
	/* package */ static final int SPLIT_GO = 2;

	/** This is a special state.  If something goes wrong (i.e a bond breaks)
	 * we don't know what happened, so we shatter.  If one of our neighbours
	 * is in this state, we break off from them and switch to this state. */
	/* package */ static final int SPLIT_SHATTER = 3;

	/** The linear position of this codon. */
	/* package */ final Pair _position;
	
	/** The angular position of this codon. */
	/* package */ double _angle;

	/** The linear velocity of this codon. */
	/* package */ final Pair _velocity;

	/** The angular velocity of this codon. */
	/* package */ double _angularVelocity;
		
	/** The current chain position state. */
	/* package */ int _chainPositionState = CHAIN_DEFAULT;

	/** The current splitting state. */
	/* package */ int _splittingState = SPLIT_NONE;

	/** How many iterations we've done while split. */
	/* package */ int _repelIterations = 0;

	/** How many iterations since we last split?   */
	/* package */ int _iterationsSinceSplit = 0;

	/** How many iterations have we been out of tolerance? */
	/* package */ int _iterationsOutOfTolerance = 0;

	/** Have we split yet?  If so, we're either a replicating chain, or folded
	 * up. */
	/* package */ boolean _hasSplit;
	
	/** Is this the seed?  The seed never folds up.  That makes it special. */
	/* package */ boolean _isReplicationSeed;

	/** Are we part of the mesh?  The mesh is started by a single folded chain
	 * to which only single folded shapes can join. 
	 */
	/* package */ boolean _inMesh;

	/** Signal to indicate that a folded shape should unfold, because it was not
	 * correctly attached to the mesh.  This signal should reset _folded and
	 * _inMesh as it propagates.  (Could use _inMesh && !_folded, but this is
	 * clearer.) */
	/* package */ boolean _unfoldSignal;

	/** Should our child (i.e. the first chain that replicates off of us) fold 
	 * up immediately to seed the mesh?  After that, this will be reset. */
	/* package */ boolean _childIsMeshSeed;

	/** This is a simple signal (which can be thought of as a boolean state)
	 * that is tripped whenever a chain forms a new bond.  It propagates to the
	 * left, which causes the leftmost codon to reset its _iterationsSinceSplit
	 * counter.  Since it's the leftmost codon that triggers folding, this
	 * resets how long we will wait to fold up this chain, hence the name. */
	/* package */ boolean _resetCounter = false;

	/** Whether we are genotypic or folded.  This is set to true when
	 * _iterationsSinceSplit hits ITERATIONS_AFTER_SPLIT. */
	/* package */ boolean _folded = false;

	/** The codons we are bonded to. */
	/* package */ final Codon[] _bonds = new Codon[CodonParameters.NUM_ARMS];
 
	/** The number of times that this codon believes it has taken part in a
	 * replication.  Note that when a chain is "born" (unless it is a seed) it
	 * percieves a replication. */
	/* package */ //int _replications;
	// _replications is no longer needed.

	/* package */ CodonState(Pair position, double angle, Pair velocity, double angularVelocity, boolean hasSplit, boolean isReplicationSeed) {
		this._position = position;
		this._angle = angle;
		this._velocity = velocity;
		this._angularVelocity = angularVelocity;
		this._hasSplit = hasSplit;
		this._isReplicationSeed = isReplicationSeed;
		this._childIsMeshSeed = isReplicationSeed;
		this._inMesh = false;
		this._unfoldSignal = false;
	}

	/* package */ CodonState() {
		this(new Pair(), 0.0, new Pair(), 0.0, false, false);
	}

	/* package */ void copyFrom(CodonState other) {
		this._position.copyFrom(other._position);
		this._angle = other._angle;
		this._velocity.copyFrom(other._velocity);
		this._angularVelocity = other._angularVelocity;
		this._chainPositionState = other._chainPositionState;
		this._splittingState = other._splittingState;
		this._repelIterations = other._repelIterations;
		this._iterationsSinceSplit = other._iterationsSinceSplit;
		this._iterationsOutOfTolerance = other._iterationsOutOfTolerance;
		this._hasSplit = other._hasSplit;
		this._isReplicationSeed = other._isReplicationSeed;
		this._childIsMeshSeed = other._childIsMeshSeed;
		this._inMesh = other._inMesh;
		this._unfoldSignal = other._unfoldSignal;
		this._resetCounter = other._resetCounter;
		this._folded = other._folded;

		assert this._bonds.length == other._bonds.length;
		
		for (int i = 0; i < this._bonds.length; i++) {
			this._bonds[i] = other._bonds[i];
		}

	}

	public boolean equals(Object other) {
		if (other instanceof CodonState) {
			CodonState state = (CodonState)other;

			if (this._position.equals(state._position)
				&& this._angle == state._angle
				&& this._velocity.equals(state._velocity)
				&& this._angularVelocity == state._angularVelocity
				&& this._chainPositionState == state._chainPositionState
				&& this._splittingState == state._splittingState 
				&& this._repelIterations == state._repelIterations
				&& this._iterationsSinceSplit == state._iterationsSinceSplit
				&& this._iterationsOutOfTolerance == state._iterationsOutOfTolerance
				&& this._hasSplit == state._hasSplit
				&& this._isReplicationSeed == state._isReplicationSeed
				&& this._childIsMeshSeed == state._childIsMeshSeed
				&& this._inMesh == state._inMesh
				&& this._unfoldSignal == state._unfoldSignal
				&& this._resetCounter == state._resetCounter
				&& this._folded == state._folded) {

				// Please Mr. JIT Compiler, unroll my loop..
				for (int i = 0; i < this._bonds.length; i++) {
					if (this._bonds[i] != state._bonds[i]) {
						return false;
					}
				}
				// Made it through the loop, so all bonds match.
				return true;

			} 
		} 
		return false;
	}
		
	public String toString() {
	
		return "[ " + _folded + "\t" + _chainPositionState + "\t" 
		+ _splittingState + "\t" + _repelIterations + "\t" + _hasSplit
		+ "\t" + _iterationsSinceSplit + "\t" + _iterationsOutOfTolerance
		+ "\t" + _isReplicationSeed + "\t" + _inMesh + "\t" + _unfoldSignal + "\t"
		+ _childIsMeshSeed + "\t" + _resetCounter + "]";

	}
	
}

