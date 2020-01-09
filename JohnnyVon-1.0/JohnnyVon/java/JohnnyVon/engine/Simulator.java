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

import java.util.Random;
import java.util.StringTokenizer;
import java.io.BufferedReader;
import java.io.IOException;

/** The simulator that creates &amp; manages the codons.
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class Simulator implements Runnable {

	private static final double RADIANS_PER_DEGREE = 2.0 * Math.PI / 360.0;

	private Viewer viewer;
	private final int numCodons;
	private final int containerSize;
	private final AbstractCodon[] codons;
	private boolean done;
	private int iterations = 0;

	// keeps token state while reading tokens from a stream.
	private StringTokenizer tokens;

	// 
	public Simulator(BufferedReader in) throws Exception {
		this.containerSize = (int)this.getNextDouble(in);
		this.numCodons = (int)this.getNextDouble(in);
		int numToRead = (int)this.getNextDouble(in);

		this.codons = new AbstractCodon[numCodons];

		if (numToRead > 0) { this.readFromStream(numToRead, in); }
		this.randomize(numToRead);
	}

	public final void setViewer(Viewer viewer) {
		this.viewer = viewer;
	}

	private final void readFromStream(int num, BufferedReader in) 
		throws IOException {
		
		for (int i = 0; i < num; i++) {
			double x = this.getNextDouble(in);
			double y = this.getNextDouble(in);
			double vx = this.getNextDouble(in);
			double vy = this.getNextDouble(in);
			double a = this.getNextDouble(in) * RADIANS_PER_DEGREE;
			double va = this.getNextDouble(in) * RADIANS_PER_DEGREE;
			int type = (int)this.getNextDouble(in);
			this.codons[i] = 
				new DefaultCodon(i, this, new Pair(x, y), a, new Pair(vx, vy), va, type);
		}
	}

	private final double getNextDouble(BufferedReader in) throws IOException {
		// We will either get a token and return, or hit EOF and an exception
		// will be thrown.
		while (true) {
			if (tokens != null && tokens.hasMoreTokens()) {
				return Double.parseDouble(tokens.nextToken());
			} else {
				String text = in.readLine();
				if (!text.startsWith("#")) {
					tokens = new StringTokenizer(text, " \t\r\n", false);
				}
			}
		}
	}

	private final void randomize(int startIndex) {
		final int MAX_ATTEMPTS = 100;
		
		Random random = new Random();
		
		for (int i = startIndex; i < this.numCodons; i++) {
			Pair center;
			int attempts = 0;
			boolean overlaps = false;
			do {
				attempts++;
				overlaps = false;
				center = new Pair(((random.nextDouble() * 2) - 1) * this.containerSize, 
						((random.nextDouble() * 2) - 1) * this.containerSize);
				for (int j = 0; j < startIndex; j++) {
					overlaps = overlaps || 
						(this.codons[j].getPosition().getDistance(center) <=
							DefaultCodon.MAX_INTERACTION_RADIUS);
				}
			} while (attempts < MAX_ATTEMPTS && overlaps);

			double angle = random.nextDouble() * 2 * Math.PI;
			this.codons[i] = new DefaultCodon(i, this, center, angle, 
					new Pair(0.0, 0.0), 0.0, Math.abs(random.nextInt()) % 2);
		}

	}

	public final void run() { this.simulate(); }
				
	public final void simulate() {

		this.iterations = 0;
		this.done = false;
		while (!this.done) {
			this.iterate();
		}
		//System.out.println("Shutdown: Quit notification received by simulator.");

	}

	public final void shutdown() { this.done = true; }

	private final void iterate() {

		for (int i = 0; i < this.numCodons; i++) {
			this.codons[i].startTimestep();
		}

		this.viewer.view(this.codons, 
				this.iterations * DefaultCodon.TIMESTEP_DURATION, this.iterations);
		this.iterations++;

		for (int i = 0; i < this.numCodons; i++) {
			for (int j = 0; j < i; j++) {//this.numCodons; j++) {
//				if (i != j) {
					this.codons[i].interact(codons[j]);
//				}
			}
		}

		for (int i = 0; i < this.numCodons; i++) {
			this.codons[i].updateVelocities();
		}

		for (int i = 0; i < this.numCodons; i++) {
			this.codons[i].updatePositions();
		}
	}


	public final int getContainerSize() {
		return this.containerSize;
	}

}
