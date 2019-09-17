JohnnyVon: self-replicating, self-assembling mobile automata in two-dimensional continuous space
================================================================================================

The JohnnyVon project:

http://johnnyvon.sourceforge.net/

JohnnyVon 1.0
=============

Self-Replicating Machines in Continuous Space with Virtual Physics

Arnold Smith, Peter Turney, Robert Ewaschuk

JohnnyVon is an implementation of self-replicating machines in continuous twodimensional
space. Two types of particles drift about in a virtual liquid. The particles are
automata with discrete internal states but continuous external relationships. Their
internal states are governed by finite state machines but their external relationships are
governed by a simulated physics that includes brownian motion, viscosity, and springlike
attractive and repulsive forces. The particles can be assembled into patterns that
can encode arbitrary strings of bits. We demonstrate that, if an arbitrary “seed” pattern is
put in a “soup” of separate individual particles, the pattern will replicate by assembling
the individual particles into copies of itself. We also show that, given sufficient time, a
soup of separate individual particles will eventually spontaneously form self-replicating
patterns. We discuss the implications of JohnnyVon for research in nanotechnology,
theoretical biology, and artificial life.

https://github.com/pdturney/johnnyvon/blob/master/self-replicating-machines.pdf

https://sourceforge.net/projects/johnnyvon/files/JohnnyVon/1.0/

JohnnyVon 2.0
=============

Self-Replication and Self-Assembly for Manufacturing

Robert Ewaschuk and Peter D. Turney

It has been argued that a central objective of nanotechnology is to make products
inexpensively, and that self-replication is an effective approach to very low-cost
manufacturing. The research presented here is intended to be a step towards this vision.
We describe a computational simulation of nanoscale machines floating in a virtual
liquid. The machines can bond together to form strands (chains) that self-replicate and
self-assemble into user-specified meshes. There are four types of machines and the
sequence of machine types in a strand determines the shape of the mesh they will build.
A strand may be in an unfolded state, in which the bonds are straight, or in a folded
state, in which the bond angles depend on the types of machines. By choosing the
sequence of machine types in a strand, the user can specify a variety of polygonal
shapes. A simulation typically begins with an initial unfolded seed strand in a soup of
unbonded machines. The seed strand replicates by bonding with free machines in the
soup. The child strands fold into the encoded polygonal shape, and then the polygons
drift together and bond to form a mesh. We demonstrate that a variety of polygonal
meshes can be manufactured in the simulation, by simply changing the sequence of
machine types in the seed.

https://sourceforge.net/projects/johnnyvon/files/JohnnyVon/2.0/

