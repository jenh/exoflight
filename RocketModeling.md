## How to Make Cool-Looking 3D Rocket Models ##

Each stage, or each booster, should be a separate object.  This includes
tricky bits like the boosters on the sides of some Russian rockets.

Engines should be separate objects.  Many engines can be reused in
different stages/rockets.

Use layout to make sure the seams in the stages line up, and to place
engines.

Any other major feature that's duplicated more than once and contains over
about 50 polygons should also be a separate object.  I can't think of any
examples right now, except engines.

The payload will also be a separate object but let's not worry about that
now.

Inspire can't change units -- it's always meters.  But just use whatever
units the source materials uses.  If it's inches, just enter it as meters
and I'll scale down afterwards.  This is so if editing is required later we
still have the file with the original units.

Polygon count -- most stages should have 300-1000 polygons, not counting
engines.

Most rocket bodies should be 24 segments around, 16 for small stages or
boosters.  Very large rockets like Saturn V or shuttle main tank should
be 32 segments.

Try to make main fins three-dimensional, not flat -- I think the extrude
command is good for this?

Use numerics whenever possible.  Tip: placing a point at (0,0,0) and using
the LD\_Move tool is good for locating points at precise locations, so that
you can later make curves (how do you make lines?)

Most coloration will be done with textures, so use a different surface for
each texture type.  There are some exceptions, like the A-4, which has a
crazy yellow-black color scheme.  Here it might make sense to color the
individual polygons, since there are large patches of solid color.

Be careful with Boolean ops -- they are good sometimes for adding features
to the side of a rocket, but don't let them create weird geometry.