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

import java.util.Random;
import java.util.Properties;
import java.io.BufferedReader;
import java.io.IOException;

/** The simulator that creates &amp; manages the codons.
 * 
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
public class Simulator implements Runnable {

	/** Useful constant - The number of radians in a degree. */
	private static final double RADIANS_PER_DEGREE = 2.0 * Math.PI / 360.0;

	/** Something that's watching the codons that we are simulating. */
	private Viewer _viewer;

	/** The number of random codons of each type that we should create. */
	private final double[] _numToCreate;

	/** The total number of codons (seeded + random) */
	private final int _totalCodons;

	/** The number of seeded codons */
	private final int _seededCodons;


	/** The size of the container that they are being simulated in. */
	private final int _containerSize;

	/** The codons being simulated. */
	private final Codon[] _codons;

	/** Whether or not we have been told to finish up. */
	private boolean _done;

	/** The number of iterations performed. */
	private int _iterations = 0;

	/** Create a Simulator, reading the information about the codons from the
	 * data stream given.
	 * Expects a double for the container size, followed by an integer for the
	 * number of codons, then an integer for the number to read from the
	 * stream (the remainder will be generated randomly) followed for the data
	 * for each codon. 
	 *
	 * @param in The BufferedReader to read data from. 
	 */
	public Simulator(Properties properties, String prefix) throws Exception {

		this._containerSize = Integer.parseInt(properties.getProperty(prefix + ".Size"));

		this._numToCreate = this.parseDoubles(properties.getProperty(prefix + ".Create"), Codon.NUM_CODON_TYPES);

		int toCreate = 0;
		for (int i = 0; i < Codon.NUM_CODON_TYPES; i++) {
			toCreate += (int)this._numToCreate[i];
		}
		
		String[] seed = properties.getProperty(prefix + ".Seed").split(";");

		// If there isn't anything there, don't create any codons.
		if (seed.length == 1 && seed[0].trim().length() == 0) { seed = new String[0]; }
	
		this._totalCodons = toCreate + seed.length;

		this._codons = new Codon[this._totalCodons];

		// Read as many from the stream as we've been told.
		this._seededCodons = seed.length;
		this.parseCodons(seed); 
		
		// Randomize the remainder
		this.randomize(seed.length);

	}

	/** Parse the given comma-separated string into doubles, substituting zeros
	 * wherever parsing fails. */
	public double[] parseDoubles(String value, int minLength) {
		String[] values = value.split(",");
		double[] result = new double[Math.max(values.length, minLength)];
		for (int i = 0; i < result.length; i++) {
			try {
				result[i] = Double.parseDouble(values[i]);
			} catch (Exception e) { /* Leave as default zero */ }
		}
		return result;
	}

	/** Read a bunch of Codons from a given stream of data.
	 * @param codons The codons to parse.
	 */
	private final void parseCodons(String[] data) {
		
		for (int i = 0; i < data.length; i++) {
			double[] codonData = this.parseDoubles(data[i], 7);
			// This should be made more generic.  Ideally, the file should specify
			// the class of each Codon, and we should do some dynamic-loading
			// magic.
			this._codons[i] = new Codon(i, new Pair(codonData[1], codonData[2]), codonData[3] * RADIANS_PER_DEGREE, new Pair(codonData[4], codonData[5]), codonData[6] * RADIANS_PER_DEGREE, (int)codonData[0], true, true);
		}

	}

	/** 
	
	/** Register a Viewer to be notified after each iteration that it should
	 * update. */
	public final void setViewer(Viewer viewer) {
		this._viewer = viewer;
	}

	/** Generate a bunch of random Codons, starting at the given index.
	 * 
	 * @param startIndex The index of the first Codon to read in.
	 */
	private final void randomize(int startIndex) {
		final int MAX_ATTEMPTS = 100;
		
		Random random = new Random();
		
		int index = startIndex;
		
		for (int i = 0; i < this._numToCreate.length; i++) {
			for (int j = 0; j < this._numToCreate[i]; j++) {
				Pair center;

				// We continue trying to make Codons up to MAX_ATTEMPTS times until
				// they are not affecting each other.  This creates a non-random
				// dispersion, but is more useful as a seed since it doesn't contain
				// odd initial forces. 
				center = new Pair(((random.nextDouble() * 2) - 1) * this._containerSize, 
						((random.nextDouble() * 2) - 1) * this._containerSize);

				double angle = random.nextDouble() * 2 * Math.PI;
				this._codons[index++] = new Codon(index, center, angle, new Pair(0.0, 0.0), 0.0, i, false, false);
			}
		}

	}

	/** Run simulate().  This is to satisfy the Runnable interface.
	 */
	public final void run() { this.simulate(); }
		
	/** start the simulator.  Does not return until shutdown() has been called. */
	public final void simulate() {
		try {
			this._iterations = 0;
			this._done = false;
			this.iterate(true, this._seededCodons);
			while (!this._done) {
				this.iterate(false, this._totalCodons);
				try { 
					if (System.in.available() > 0) {
						System.in.read();
						this.dumpAll();
					}
				} catch (IOException ioe) { }
			}
		} catch (AssertionError ae) {
			this.dumpAll();
			throw ae;
		}
		//System.out.println("Shutdown: Quit notification received by simulator.");

	}

	private void dumpAll() {
		System.out.println("ID\tfolded\tchainSt\tsplitSt\trepel\tisSplit\tage\tseed\tmesh\tMshSdPar reset");
		for (int i = 0; i < this._codons.length; i++) {
			System.out.println(this._codons[i].toString());
		}
	}

	/** If simulate() is currently running (in a different thread from the one
	 * that calls shutdown, obviously) then it will complete its current
	 * iteration, if any, and then return. 
	 */
	public final void shutdown() { this._done = true; }

	/** Do a single step. */
	private final void iterate(boolean firstRun, int numCodons) {

		System.out.print("#");
		// Set things up
		for (int i = 0; i < numCodons; i++) {
			this._codons[i].startTimestep();
		}

		// Count the iteration
		this._iterations++;

		// Make each codon interact with each other codon.  This calculates the
		// force applied to each codon.
		for (int i = 0; i < numCodons; i++) {
			for (int j = 0; j < i; j++) {
				this._codons[i].interact(_codons[j], firstRun);
			}
		}
		
		for (int i = 0; i < numCodons; i++) {
			this._codons[i].finishTimestep(this._containerSize);
		}
	  // Calculate new velocities, given the forces that were just calculated
		// to be acting on each codon. 
		for (int i = 0; i < numCodons; i++) {
			this._codons[i].copyStates();
		}

		// Tell the viewer to draw the current state
		if (!firstRun) { 
			this._viewer.view(this._codons, this._iterations * SimulationParameters.TIMESTEP_DURATION, this._iterations);
		}

	}

  /** Get the size of the container that the codons are in. 
	 * @return The size of the container. 
	 */
	public final int getContainerSize() {
		return this._containerSize;
	}

}
