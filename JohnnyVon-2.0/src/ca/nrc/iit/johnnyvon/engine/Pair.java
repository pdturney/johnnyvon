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

/** A simple 2D Point/Vector class that provides basic vector functionality.
 * @author <a href="mailto:rob@infinitepigeons.org">Rob Ewaschuk</a>, 
 * <a href="mailto:arnold.smith@nrc.ca">Arnold Smith</a>, 
 * <a href="mailto:peter.turney@nrc.ca">Peter Turney</a>
 * @version 2.0  Copyright &copy; 2002-2004 National Research Council Canada
 */
public final class Pair implements Cloneable {

	public static final double PI = Math.PI;

	public double x, y;

	public Pair() { this(0.0, 0.0); }

	public Pair(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public final Object clone() { 
		try {
			return super.clone(); 
		} catch (CloneNotSupportedException cnse) {
			// Not supported in JDK 1.3
			//throw new RuntimeException(cnse);
			throw new RuntimeException(cnse.toString());
		}
	}

	public final void copyFrom(Pair other) {
		this.x = other.x;
		this.y = other.y;
	}

	public final boolean isZero() { return this.x == 0 && this.y == 0; }

	public final boolean isUnit() { return this.getLength() == 1.0; }

	public final boolean isFinite() { 
		return this.isFinite(this.x) && this.isFinite(this.y);
	}

	public static final boolean isFinite(final double value) {
		return !(Double.isNaN(value) || Double.isInfinite(value));
	}

	public final void scale(final double scalar) {
		this.x *= scalar;
		this.y *= scalar;
	}

	public final void scaleTo(final double targetLength) {
		if (!this.isZero()) {
			this.scale(targetLength / this.getLength());
		}
	}

	public final void setZero() {
		this.x = 0;
		this.y = 0;
	}

	public final void negate() {
		this.x = -this.x;
		this.y = -this.y;
	}

	public final void normalize() {
		this.scale(1.0 / this.getLength());
	}

	public final double getLengthSquared() {
		return this.x * this.x + this.y * this.y;
	}

	public final double getLength() {
		return Math.sqrt(this.x * this.x + this.y * this.y);
	}

	public final void add(final Pair other) {
		this.x += other.x;
		this.y += other.y;
	}

	public final void subtract(final Pair other) {
		this.x -= other.x;
		this.y -= other.y;
	}

	public final void orthogonalize() {
		double temp = this.y;
		this.y = this.x;
		this.x = -temp;
	}

	/** Treat this as an (x,y) vector from the origin and rotate it by the given 
	 * angle.
	 * @param angle The angle to rotate by (in radians)
	 */
	public void rotate(double angle) {
		
		// Physics for game developers, page 225
		double newX = x * Math.cos(angle) + y * Math.sin(angle);
		this.y = -x * Math.sin(angle) + y * Math.cos(angle);
		this.x = newX;
	}

	public final double getAngle() {
		return Math.atan2(this.y, this.x);
		/*
		if (this.x == 0) {
			if (this.y == 0) { 
				return 0.0;
			} else if (this.y < 0) {
				return 3.0 * PI / 2.0;
			} else {
				assert (this.y > 0);
				return PI / 2.0;
			}
		} else {
			double angle = atan(this.y / this.x);
			if (this. x < 0) angle += PI;
			else if (angle < 0) angle += 2 * PI;
			return angle;
		}
		*/
	}

	/* TODO: Is this correct?  
	public final double getCrossProduct(final Pair other) {
		return this.x * other.y - this.y * other.x;
	}
	*/

	public final double getDotProduct(final Pair other) {
		return this.x * other.x + this.y * other.y;
	}

	public final double getDistanceSquared(final Pair other) {
		return (this.x - other.x) * (this.x - other.x) + 
			(this.y - other.y) * (this.y - other.y);
	}

	public final double getDistance(final Pair other) {
		return Math.sqrt(this.getDistanceSquared(other));
	}

 	public final Pair getProjectionOnto(final Pair other) {
			
		Pair result = (Pair)other.clone();
		result.normalize();
		result.scale(this.getDotProduct(other) / other.getLengthSquared());
		return result;
		
	}

	/** Return the "amount" of this vector that is perpendicular to the given
	 * vector. 
	 */
	public final Pair getPerpendicularProjectionOnto(final Pair other) {
		Pair result = (Pair)this.clone();
		result.subtract(this.getProjectionOnto(other));
		return result;
	}
	/*
		Pair result = (Pair)other.clone();
		
		result.orthogonalize();
		result.scale(this.getDotProduct(result) / result.getLengthSquared());
		return result;
	}*/
	
	public final String toString() { 
		return "(" + this.x + "," + this.y + ")";
	}

}
