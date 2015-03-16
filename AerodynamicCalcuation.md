If above ceiling, exit.

Compute airstream velocity with (or without wind, if expendable)

Compute atmosphere parameters at altitude.

Compute density, v^2 in meters.

Compute mach = v/spdsound

Transform to structure coordinates

Compute vv = normalized velocity rel to airstream (0,0,1 = 0 alpha,beta)

Calculate aerodynamic forces (struct.calculateDragCoeff())

For each surface:
```
	area = calculate area exposed (dot product * occluded area)
	if linked_cap:
		drag modifier *= time_drag_curve(t)
		lift modifier *= time_lift_curve(t)
	if chute is opening:
		drag modifier *= area multiplier(t)

	bc = drag_mach_curve(mach)*area
	get drag position, add force (-bc*drag modifier)
	
	if lift_aa_curve:
		sinaa = -vv.y
		lift = lift_aa_curve(sinaa)
		liftdir = vel cross right_vector
		lift *= bc*lift modifier

	if control_surfaces_torque:
		torque += bc * control_surfaces_torque

Q = density*v^2/2
T = Q/1000
```