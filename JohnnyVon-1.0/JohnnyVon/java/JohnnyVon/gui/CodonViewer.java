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

package JohnnyVon.gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

import JohnnyVon.engine.*;

/** A panel that will draw Codons, their fields, arms and the forces that 
 * were most recently applied to them.
 * @author <a href="mailto:raewasch@uwaterloo.ca">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 1.0  Copyright &copy; 2002 National Research Council Canada
 */
public class CodonViewer extends JPanel implements Viewer, Scrollable {

	private static final int MIN_PREFERRED_SIZE = 400;

	private static final Color[][] FIELD_COLORS = { 
		{ Color.cyan, Color.red, Color.green, Color.yellow },
		{ Color.cyan, Color.red, Color.magenta, Color.yellow } 
	};

	private static final double ZOOM_IN_FACTOR = Math.sqrt(2);
	private static final double ZOOM_OUT_FACTOR = 1 / ZOOM_IN_FACTOR;

	/** Number of steps to wait between drawing requests. */
	public int stepsPerDraw = 25;

	/** True if the simulation should end. */
	private boolean done = false;

	/** How many steps (calls to view(..)) have happened since we last
	 * called repaint().  We don't want to draw every single iteration. */
	private int stepsSinceLastDraw = 25;

	/** The codons that we should draw.  These are a copy of what is given to
	 * us in view(..) so that we know that they won't change as we're
	 * displaying them.
	 */
	private AbstractCodon[] codons;

	/** An object to lock on while we're updating thei codons.  */
	// Can we just lock on codons instead?
	private final Object lock = new Object();

	/** An object to wait on when we're paused. */
	private final Object pauseObject = new Object();
	
	/** The size of the container we are drawing. */
	private int containerSize;

	/** The transform used to zoom and slide the codons. */
	private final AffineTransform transform = new AffineTransform();
	
	/** The stroke we use to draw the codons. */
	private final Stroke stroke;

	/** The zoom factor. */
	private float zoom = 1.0f;

	/** The margin in "units" (the same units that the codons are measured in)
	 * that should be made visible around the border of the container.
	 */
	private static final int MARGIN = 4;

	/** The label that we should update to display the current iteration
	 * number. */
	private final JLabel statusLabel;	

	/** The current iteration number. */
//	private int steps = 0;

	/** Whether or not we are paused. */
	// do not start running immediately -- give user time to configure the settings
	private boolean paused = true;  

	/** The color to draw the background color in. */
	private Color backgroundColor = Color.black;

	/** The color to draw the arms (or the center) in. */
	private Color armColor = Color.white;

	/** Whether or not to draw the arms.  If not, a point will be drawn at the
	 * center of the codon. */
	private boolean drawArms = true;

	/** Whether or not to draw the fields. */
	private boolean drawFields = true;

	/** Whether or not to draw the forces */
	private boolean drawForces = true;

	/** The viewport that we exist in, if any. */
	private final JViewport viewport;

	/** Create a CodonViewer component that will draw the Codons. 
		* @param containerSize the size of the container. 
		* @param statusLabel A status label, if any, to update with the current
		* iteration number and such. */
	public CodonViewer(JLabel statusLabel, JViewport viewport) {

		this.statusLabel = statusLabel;
		this.viewport = viewport;
		
		this.stroke = new BasicStroke(0.2f);

		super.setPreferredSize(
				new Dimension(MIN_PREFERRED_SIZE, MIN_PREFERRED_SIZE));
		super.setSize(
				new Dimension(MIN_PREFERRED_SIZE, MIN_PREFERRED_SIZE));
	}
	
	public synchronized void setSimulator(Simulator sim) {
		this.containerSize = sim.getContainerSize();
//		this.steps = 0;
		this.stepsSinceLastDraw = this.stepsPerDraw;
		this.zoomToFit();
		synchronized (this.lock) { this.codons = null; }
	}

	private long lastThousandFramesTime = System.currentTimeMillis();
	
	public void view(AbstractCodon[] codons, double time, int iterations) {

/*
		if (this.steps % 1000 == 0) {
			long cur = System.currentTimeMillis();
			System.out.println("Time since last 1000 steps: " +
					(cur - this.lastThousandFramesTime));
			this.lastThousandFramesTime = cur;
		}
*/	

		if (++this.stepsSinceLastDraw >= this.stepsPerDraw) {
			synchronized (lock) {
				if (this.codons == null) 
					this.codons = new AbstractCodon[codons.length];

				for (int i = 0; i < this.codons.length; i++) {
					this.codons[i] = (AbstractCodon)codons[i].clone();
				}
			}
			StringBuffer display = new StringBuffer();
			display.append("Steps: ");
		 	display.append(iterations);
			display.append(" Time: ");
			display.append(Math.round(time * 100) / 100.0);
			if (display.charAt(display.length() - 2) == '.') 
				display.append("0");
			this.statusLabel.setText(display.toString());

			this.repaint();
			this.stepsSinceLastDraw = 0;

			synchronized (this.pauseObject) {
				if (this.paused) {
					try {
						this.pauseObject.wait();
					} catch (InterruptedException ie) { }
				}
			}
		}

	}

	// If not paused, do nothing.  If paused, lets one redraw occur.
	public void step() {
		synchronized (this.pauseObject) {
			this.pauseObject.notifyAll();
		}
	}

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
		this.transform.setToIdentity();
		this.transform.scale(this.zoom, this.zoom);
		this.transform.translate(this.containerSize + MARGIN, 
				this.containerSize + MARGIN);
		
		int widthHeight = (int)((this.containerSize + MARGIN) * 2 * zoom);
		Dimension size = new Dimension(widthHeight, widthHeight);
		this.setPreferredSize(size);
		this.setSize(size);
		this.viewport.setViewSize(size);
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
		Dimension size = this.viewport.getExtentSize();
		this.zoom = (float)Math.min(size.width, size.height) 
			/ ((this.containerSize + MARGIN) * 2);
		this.updateZoom();
	}

	public void setColors(Color background, Color arms) {
		this.backgroundColor = background;
		this.armColor = arms;
		this.repaint();
	}

	public void togglePause() { 
		this.setPaused(!this.paused);
	}

	public synchronized void setPaused(boolean paused) {
		synchronized (this.pauseObject) {
			this.paused = paused; 
			if (!this.paused) {
				this.pauseObject.notifyAll();
			}
		}
	}

	public void toggleArms() { 
		this.drawArms = !this.drawArms; 
		this.repaint(); 
	}

	public void toggleFields() { 
		this.drawFields = !this.drawFields;
		this.repaint(); 
	}

	public void toggleForces() { 
		this.drawForces = !this.drawForces;
		this.repaint();
	}
	
	public void paintComponent(Graphics g) {

		Graphics2D graphics = (Graphics2D)g;
		GraphicsConfiguration conf = graphics.getDeviceConfiguration();
		Rectangle bounds = graphics.getClipBounds();
	//	graphics.setComposite(AlphaComposite.Src);

		if (this.zoom == 0) {
			this.zoomToFit();
		}
		
		Point viewTopLeft = this.viewport.getViewPosition();
		this.transform.setToIdentity();
		this.transform.translate(-bounds.x, -bounds.y);
		this.transform.scale(this.zoom, this.zoom);
		this.transform.translate(this.containerSize + MARGIN, 
				this.containerSize + MARGIN);
		
		graphics.setColor(this.backgroundColor);
		graphics.fill(bounds);
		graphics.setTransform(this.transform);

		graphics.setStroke(this.stroke);

		int topLeft = -this.containerSize;
		int widthHeight = this.containerSize * 2;
		Shape container = 
				new Rectangle(topLeft, topLeft, widthHeight, widthHeight);
		graphics.setColor(Color.gray);
		graphics.draw(container);

		synchronized (this.lock) {
			// Abort if we don't ahve codons yet.
			if (this.codons == null) return;

			if (this.drawForces) this.drawForces(graphics);

			graphics.setColor(this.armColor);
			if (this.drawArms) this.drawArms(graphics);
			else this.drawCenters(graphics);

			if (this.drawFields) this.drawFields(graphics);
		}
	}

	private void drawForces(Graphics2D graphics) {
		graphics.setColor(Color.gray);
		graphics.setPaint(Color.gray);

		final double ARC_SIZE = 2.5;
		final double RAD_TO_DEG = 360 / (2 * Math.PI);
		Arc2D.Double arc = 
			new Arc2D.Double(0, 0, ARC_SIZE * 2, ARC_SIZE * 2, 0, 0, Arc2D.PIE);

		for (int i = 0; i < this.codons.length; i++) {
			AbstractCodon cur = this.codons[i];
			Pair position = cur.getPosition();
			arc.setArcByCenter(position.x, position.y, ARC_SIZE, 
					-cur.getAngle() * RAD_TO_DEG, 
					-cur.getTimestepAngularAcceleration() * RAD_TO_DEG * 25, Arc2D.PIE);
			graphics.fill(arc);
			graphics.draw(arc);
		}

	
		graphics.setColor(Color.blue);
		Line2D.Double line = new Line2D.Double();
		for (int i = 0; i < this.codons.length; i++) {
			AbstractCodon cur = this.codons[i];
			Pair position = cur.getPosition();
			Pair accel = cur.getTimestepAcceleration();
			line.x1 = position.x;
			line.y1 = position.y;
			line.x2 = position.x + accel.x;
			line.y2 = position.y + accel.y;
			graphics.draw(line);

			for (int j = 0; j < cur.getNumArms(); j++) {
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
		int numArms = codons[0].getNumArms();

		Ellipse2D.Double field = new Ellipse2D.Double();
		
		for (int i = 0; i < numArms; i++) {
			Color lastColor = null;
			
			for (int j = 0; j < codons.length; j++) {
				AbstractCodon cur = codons[j];
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

	}

	private void drawArms(Graphics2D graphics) {
		// Draw the arms.
		for (int i = 0; i < codons.length; i++) {
			AbstractCodon cur = codons[i];
			Pair center = cur.getPosition();
			for (int j = 0; j < cur.getNumArms(); j++) {
				Pair arm = cur.getArmPosition(j);
				graphics.draw(
						new Line2D.Double(center.x, center.y, arm.x, arm.y));
			}
		}
	}

	private void drawCenters(Graphics2D graphics) {

		// Otherwise draw centers.
		for (int i = 0; i < codons.length; i++) {
			AbstractCodon cur = codons[i];
			Pair center = cur.getPosition();
			graphics.draw(
						new Line2D.Double(center.x, center.y, center.x, center.y));
		}
	}

}
