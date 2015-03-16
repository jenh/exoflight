## Frustum Modes ##

  1. Near surface.
  1. Above surface.

Mode 1 we separate into two sections: tracked & planet, and everything beyond planet.
We turn depth testing off until we get to ship's parent, and then we set frustum
to 5 m near and horizon dist far.

If we are far enough above the surface to not do ROAM, we are in Above Surface mode.