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

package ca.nrc.iit.johnnyvon.gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import java.awt.image.*;
import java.io.OutputStream;
import java.io.IOException;

import sun.awt.image.codec.JPEGImageEncoderImpl;

import ca.nrc.iit.johnnyvon.engine.*;

/** A panel that will draw Codons, their fields, arms and the forces that 
 * were most recently applied to them.
 *
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
public class CodonViewer extends JPanel implements Viewer, Scrollable {

	private static final int MIN_PREFERRED_SIZE = 400;

	private static final Color DARK_BLUE = new Color(0, 0, 160);

	private static final Color[][] FIELD_COLORS = { 
		{ Color.cyan, Color.red, Color.green, DARK_BLUE, Color.orange },
		{ Color.cyan, Color.red, Color.magenta, DARK_BLUE, Color.orange },
		{ Color.cyan, Color.red, Color.cyan, DARK_BLUE, Color.orange },
		{ Color.cyan, Color.red, Color.yellow, DARK_BLUE, Color.orange } 
	};

	/** Amount to zoom when zoomIn() is called */
	private static final double ZOOM_IN_FACTOR = Math.sqrt(2);

	/** Amount to zoom out when zoomOut() is called */
	private static final double ZOOM_OUT_FACTOR = 1 / ZOOM_IN_FACTOR;

	/** Number of steps to wait between drawing requests. */
	public int stepsPerDraw = 500;

	/** True if the simulation should end. */
	private boolean done = false;

	/** How many steps (calls to view(..)) have happened since we last
	 * called repaint().  We don't want to draw every single iteration. */
	private int stepsSinceLastDraw = stepsPerDraw;

	/** The codons that we should draw.  These are a copy of what is given to
	 * us in view(..) so that we know that they won't change as we're
	 * displaying them.
	 */
	private Codon[] codons;

	/** Whether or not to draw each codon.  This is currently based on the
	 * drawOnlyReplicating and drawOnlyFolded settings. */
	private boolean[] drawCodon;

	/** An object to lock on while we're updating thei codons.  */
	private final Object _lock = new Object();

	/** An object to wait on when we're paused. */
	private final Object _pauseObject = new Object();
	
	/** The size of the container we are drawing. */
	private int containerSize;

	/** The transform used to zoom and slide the codons. */
	//private final AffineTransform _transform = new AffineTransform();
	
	/** The stroke we use to draw the codons. */
	private final Stroke stroke;
	
	/** The stroke we use to draw the fields. */
	private final Stroke fieldStroke;

	/** The zoom factor. */
	private float zoom = 1.0f;

	/** The margin in "units" (the same units that the codons are measured in)
	 * that should be made visible around the border of the container.
	 */
	private static final int MARGIN = 6;

	/** The label that we should update to display the current iteration
	 * number. */
	private final JLabel _statusLabel;	

	/** The current iteration number. */
//	private int steps = 0;

	/** Whether or not we are paused. */
	// start running immediately -- doesn't give user time to configure the settings
	private boolean paused = false;  

	/** The color to draw the background color in. */
	private Color backgroundColor = Color.black;

	/** The color to draw the arms (or the center) in. */
	private Color armColor = Color.white;

	/** The color to draw dim arms in */
	private Color dimArmColor = Color.gray;

	/** The color to draw bonds in */
	private Color bondsColor = Color.red;

	/** The color of codons that are not within their tolerances */
	private Color intoleranceColor = Color.magenta;

	/** Whether or not to draw the arms.  If not, a point will be drawn at the
	 * center of the codon. */
	private boolean drawArms = true;

	/** Whether or not to draw the fields. */
	private boolean drawFields = false;

	/** Whether or not to draw the forces */
	private boolean drawForces = false;

	/** Whether we should only draw replicating codons. (i.e. ignore folded and free-floaters) */
	private boolean drawOnlyReplicating = false;
	
	/** Whether or not to draw codons that have never split. */
	private boolean drawOnlySplit = false;

	/** Whether or not we should draw bonds. */
	private boolean drawBonds = false;
	
	/** Whether or not we highlight codons that aren't within their tolerances.  */
	private boolean showTolerance = true;

	/** Whether we should only draw folded codons. (i.e. ignore replicating
	 * chains) */
	private boolean drawOnlyFolded = false;

	/** The JScrollPane that we exist in, if any. */
	private final JScrollPane _scrollPane;

	/** Create a CodonViewer component that will draw the Codons. 
		* @param containerSize the size of the container. 
		* @param _statusLabel A status label, if any, to update with the current
		* iteration number and such. */
	public CodonViewer(JLabel statusLabel, JScrollPane scrollPane) {

		this._statusLabel = statusLabel;
		this._scrollPane = scrollPane;
		
		this.stroke = new BasicStroke(0.8f);
		this.fieldStroke = new BasicStroke(0.1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0.0f, new float[] { 0.1f, 0.25f }, 0.0f );

		super.setMinimumSize(new Dimension(MIN_PREFERRED_SIZE, MIN_PREFERRED_SIZE));
		//super.setSize(new Dimension(MIN_PREFERRED_SIZE, MIN_PREFERRED_SIZE));
	}
	
	public synchronized void setSimulator(Simulator sim) {
		this.containerSize = sim.getContainerSize();
//		this.steps = 0;
		this.stepsSinceLastDraw = this.stepsPerDraw;
		this.zoomToFit();
		synchronized (this._lock) { this.codons = null; }
		this.repaint();
	}

	public void view(Codon[] codons, double time, int iterations) {

		if (++this.stepsSinceLastDraw >= this.stepsPerDraw) {

			// This bit of code used to copy the entire array, so that we were
			// guaranteed a consistent viewpoint.  That's a waste of CPU power, so
			// we stopped doing it.  Thus the draw me different if repaint() is
			// called than when it was originally painted.
			synchronized (this._lock) { this.codons = codons; }

			this.updateDrawCodons();

			int free = 0;
			int chain = 0;
			int folded = 0;
			
			for (int i = 0; i < this.codons.length; i++) {
				if (codons[i].isFolded()) { folded++; } 
				else if (codons[i].isBonded()) { chain++; }
				else { free++; }
			}

			StringBuffer display = new StringBuffer();
			/*display.append("Steps: ");
		 	display.append(iterations);*/
			display.append(" Time: ");
			display.append(Math.round(time * 100) / 100.0);
			if (display.charAt(display.length() - 2) == '.') 
				display.append("0");
			
			display.append(" Free: ");
			display.append(free);
			display.append(" Replicating: ");
			display.append(chain);
			display.append(" Folded: ");
			display.append(folded);
			display.append(" Iterations per Draw: ");
			display.append(this.stepsPerDraw);

			this._statusLabel.setText(display.toString());

			this.repaint();
			this.stepsSinceLastDraw = 0;

			synchronized (this._pauseObject) {
				if (this.paused) {
					try {
						this._pauseObject.wait();
					} catch (InterruptedException ie) { }
				}
			}
		}

	}

	// If not paused, do nothing.  If paused, lets one redraw occur.
	public void step() {
		synchronized (this._pauseObject) {
			this._pauseObject.notifyAll();
		}
	}

	/** @return The preferred size.  We want the scroll view to be as big as
	 * we are, if possible. */
	public Dimension getPreferredScrollableViewportSize() {
		return this.getPreferredSize();
	}

	public boolean getScrollableTracksViewportHeight() { return false; }
	public boolean getScrollableTracksViewportWidth() { return false; }
	public int getScrollableBlockIncrement(Rectangle visibleRect, 
			int orientation, int direction) {
		// Scroll 25% of the container.  (
		return (int)(this.zoom * 0.5 * this.containerSize);
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect, 
			int orientation, int direction) {
		// Scroll 5% of the container.  (
		return (int)(this.zoom * 0.1 * this.containerSize);
	}

	public void setStepsPerDraw(int num) {
		if (num < 1) throw new 
			IllegalArgumentException("Invalid - must iterate at least once.");
		this.stepsPerDraw = num;
	} 

	public int getStepsPerDraw() { return this.stepsPerDraw; }
			
	private final void updateZoom() {
		/*
		this._transform.setToIdentity();
		this._transform.scale(this.zoom, this.zoom);
		this._transform.translate(this.containerSize + MARGIN, this.containerSize + MARGIN);
		*/
		
		int widthHeight = (int)((this.containerSize + MARGIN) * 2 * zoom);
		Dimension size = new Dimension(widthHeight, widthHeight);
		this.setPreferredSize(size);
		this.setSize(size);
		this._scrollPane.getViewport().setViewSize(size);
	}

	public void zoomIn() { 
		this.zoom *= ZOOM_IN_FACTOR;
		this.updateZoom();
	}
	
	public void zoomOut() { 
		this.zoom *= ZOOM_OUT_FACTOR;
		this.updateZoom();
	}
	
	public void zoomToFit() {
		Dimension size = this._scrollPane.getViewport().getExtentSize();
		this.zoom = (float)Math.min(size.width, size.height) / ((this.containerSize + MARGIN) * 2);
		this.updateZoom();
	}

	public void setColors(Color background, Color arms, Color dimArms) {
		this.backgroundColor = background;
		this.armColor = arms;
		this.dimArmColor = dimArms;
		this.repaint();
	}

	public void togglePause() { 
		this.setPaused(!this.paused);
	}

	public synchronized void setPaused(boolean paused) {
		synchronized (this._pauseObject) {
			this.paused = paused; 
			if (!this.paused) {
				this._pauseObject.notifyAll();
			}
		}
	}

	public void toggleArms() { 
		this.drawArms = !this.drawArms; 
		this.repaint(); 
	}

	public void toggleOnlyFolded() { 
		this.drawOnlyFolded = !this.drawOnlyFolded;
		this.updateDrawCodons();
		this.repaint(); 
	}

	public void toggleFields() { 
		this.drawFields = !this.drawFields;
		this.repaint(); 
	}

	public void toggleOnlySplit() {
		this.drawOnlySplit = !this.drawOnlySplit;
		this.updateDrawCodons();
		this.repaint();
	}

	public void toggleOnlyReplicating() {
		this.drawOnlyReplicating = !this.drawOnlyReplicating;
		this.updateDrawCodons();
		this.repaint();
	}

	private final void updateDrawCodons() {

		// Create the boolean array if necessary.
		if (this.drawCodon == null || this.drawCodon.length != codons.length) {
			this.drawCodon = new boolean[this.codons.length];
		}

		for (int i = 0; i < codons.length; i++) {
			Codon cur = this.codons[i];
			this.drawCodon[i] = (!this.drawOnlyReplicating || (cur.isBonded() && !cur.isFolded()))
				&& (!this.drawOnlyFolded || cur.isFolded())
				&& (!this.drawOnlySplit || cur.hasSplit());
		}
	}

	public void toggleForces() { 
		this.drawForces = !this.drawForces;
		this.repaint();
	}

	public void toggleBonds() {
		this.drawBonds = !this.drawBonds;
		this.repaint();
	}

	public void toggleTolerances() {
		this.showTolerance = !this.showTolerance;
		this.repaint();
	}

	public void capture(OutputStream out) throws IOException {
		boolean wasPaused = this.paused;
		if (!wasPaused) this.setPaused(true);
		
		int size = (int)((this.containerSize + MARGIN) * 2 * zoom);
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D)image.getGraphics();
		AffineTransform transform = g.getTransform();
		g.setTransform(transform);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		this.printAll(g);
							
		JPEGImageEncoderImpl j = new JPEGImageEncoderImpl(out);
		j.encode(image);

		if (!wasPaused) this.setPaused(false);
	}

	
	public void paintComponent(Graphics g) {

		Graphics2D graphics = (Graphics2D)g;
		Rectangle bounds = graphics.getClipBounds();
	//	graphics.setComposite(AlphaComposite.Src);

		if (this.zoom == 0) {
			this.zoomToFit();
		}
		
		Point viewTopLeft = this._scrollPane.getViewport().getViewPosition();
		
		AffineTransform transform = graphics.getTransform();
		transform.translate(-viewTopLeft.x, -viewTopLeft.y);
		transform.scale(this.zoom, this.zoom);
		transform.translate(this.containerSize + MARGIN, this.containerSize + MARGIN);

		graphics.setColor(this.backgroundColor);
		graphics.fill(bounds);
		graphics.setTransform(transform);

		graphics.setStroke(this.stroke);

		int topLeft = -this.containerSize;
		int widthHeight = this.containerSize * 2;
		Shape container = new Rectangle(topLeft, topLeft, widthHeight, widthHeight);
		graphics.setColor(Color.gray);
		graphics.draw(container);

		synchronized (this._lock) {
			// Abort if we don't ahve codons yet.
			if (this.codons == null) return;

			if (this.drawArms) this.drawArms(graphics);
			else this.drawCenters(graphics);

			if (this.drawFields) this.drawFields(graphics);

			if (this.drawForces) this.drawForces(graphics);

			if (this.drawBonds) this.drawBonds(graphics);
		}
	}

	private void drawBonds(Graphics2D graphics) {

		graphics.setColor(this.bondsColor);

		for (int i = 0; i < this.codons.length; i++) {

			// Check if we should draw details of this codon; if not, go to the
			// next one.
			// Nope.  Draw all bonds.
			//if (!this.drawCodon[i]) continue;

			Codon cur = this.codons[i];

			for (int j = 0; j < CodonParameters.NUM_ARMS; j++) {
				int id = cur.getBondPartnerID(j);

				// Draw only towards higher-numbered codons.  avoids double-drawing,
				// and handles the -1 case cleanly.
				if (id > i) {
					Pair myTip = cur.getArmPosition(j);
					Pair otherTip = cur.getBondPartnerLocation(j);
					graphics.draw(new Line2D.Double(myTip.x, myTip.y, otherTip.x, otherTip.y));
				}
					
			}
		}


	}

	private void drawForces(Graphics2D graphics) {
		graphics.setColor(Color.gray);
		graphics.setPaint(Color.gray);

		final double ARC_SIZE = 2.5;
		final double RAD_TO_DEG = 360 / (2 * Math.PI);
		Arc2D.Double arc = new Arc2D.Double(0, 0, ARC_SIZE * 2, ARC_SIZE * 2, 0, 0, Arc2D.PIE);

		for (int i = 0; i < this.codons.length; i++) {

			// Check if we should draw details of this codon; if not, go to the
			// next one.
			if (!this.drawCodon[i]) continue;
		
			Codon cur = this.codons[i];
			Pair position = cur.getPosition();
			arc.setArcByCenter(position.x, position.y, ARC_SIZE, 
					-cur.getAngle() * RAD_TO_DEG, 
					-cur.getAngularAcceleration() * RAD_TO_DEG * 25, Arc2D.PIE);
			graphics.fill(arc);
			graphics.draw(arc);
		}

	
		graphics.setColor(Color.blue);
		Line2D.Double line = new Line2D.Double();
		for (int i = 0; i < this.codons.length; i++) {

			// Check if we should draw details of this codon; if not, go to the
			// next one.
			if (!this.drawCodon[i]) continue;

			Codon cur = this.codons[i];
			Pair position = cur.getPosition();
			Pair accel = cur.getAcceleration();
			line.x1 = position.x;
			line.y1 = position.y;
			line.x2 = position.x + accel.x * 100;
			line.y2 = position.y + accel.y * 100;
			graphics.draw(line);

			for (int j = 0; j < CodonParameters.NUM_ARMS; j++) {
				position = cur.getArmPosition(j);
				accel = cur.getArmAcceleration(j);
				line.x1 = position.x;
				line.y1 = position.y;
				line.x2 = position.x + accel.x * 25;
				line.y2 = position.y + accel.y * 25;
				graphics.draw(line);
			}

		}
	}

	private void drawFields(Graphics2D graphics) {
		graphics.setStroke(this.fieldStroke);

		Ellipse2D.Double field = new Ellipse2D.Double();
		
		for (int i = 0; i < CodonParameters.NUM_ARMS; i++) {
			Color lastColor = null;
			
			for (int j = 0; j < codons.length; j++) {

				// Check if we should draw details of this codon; if not, go to the
				// next one.
				if (!this.drawCodon[j]) continue;

				Codon cur = codons[j];
				// Swap colours only if necessary.
				if (lastColor != FIELD_COLORS[cur.getType()][i]) {
					graphics.setColor(FIELD_COLORS[codons[j].getType()][i]);
					lastColor = FIELD_COLORS[codons[j].getType()][i];
				}

				Pair center = codons[j].getArmPosition(i);
				double r = Math.max(codons[j].getFieldRadius(i), 0.1);
				field.x = center.x - r;
				field.y = center.y - r;
				field.width = 2 * r;
				field.height = 2 * r;
				
				graphics.draw(field);
			}
		}

		graphics.setStroke(this.stroke);
	}

	private void drawArms(Graphics2D graphics) {
		// Draw the arms.
		for (int i = 0; i < codons.length; i++) {

			Codon cur = codons[i];
			Pair center = cur.getPosition();
			for (int j = 0; j < CodonParameters.NUM_ARMS; j++) {

				// Check if we should draw details of this codon; if not, go to the
				// next one.
				if (this.drawCodon[i] && (!this.showTolerance || this.codons[i].isArmWithinTolerance(j))) {
					graphics.setColor(this.armColor);
				} else if (this.drawCodon[i] && this.showTolerance) {
					assert !this.codons[i].isArmWithinTolerance(j); // Follows logically

					// out of tolerances
					graphics.setColor(this.intoleranceColor);
				} else {
					graphics.setColor(this.dimArmColor);
				}

				Pair arm = cur.getArmPosition(j);
				graphics.draw(new Line2D.Double(center.x, center.y, arm.x, arm.y));
			}
		}
	}

	private void drawCenters(Graphics2D graphics) {
		// We always draw the centers, even for non-bonded codons.

		// Otherwise draw centers.
		for (int i = 0; i < codons.length; i++) {
			Codon cur = codons[i];
			Pair center = cur.getPosition();
			graphics.draw(new Line2D.Double(center.x, center.y, center.x, center.y));
		}
	}

}
