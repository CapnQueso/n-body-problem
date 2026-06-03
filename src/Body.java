/**
 * Multi-body simulation base / helper class
 * using 3d graphics to make it look pretty
 * 
 * @author capnqueso
 * @date 5/4/26
 */
public class Body {

    /**
     * body name
     */
    private String name;

    /**
     * velocity in m/s, can be derived from orbital parameters if orbiting
     */
    private double vx, vy, vz;

    /**
     * angles in degrees, can be derived from orbital parameters if orbiting
     * angleX is the angle of the velocity vector in the xz plane, measured from the
     * positive z axis
     * angleY is the angle of the velocity vector in the xy plane, measured from the
     * positive y axis
     */
    private double ax, ay, az;

    /**
     * previous acceleration, used for Velocity Verlet integration
     */
    private double axPrev, ayPrev, azPrev;

    /**
     * positions in the simulation, in meters from origin (arbratrary)
     * these are the actual positions of the body in the simulation, (scaled to 1 au
     * = 1 unit in the simulation)
     * (these will change automatically, but will be able to be set and got)
     */
    private double x;
    private double y;
    private double z;

    /**
     * mass of body (kg)
     */
    private Double mass;

    /**
     * The body that this body orbits (optional)
     * Typed as Body so any subclass (Star, Planet, Moon, etc.) can be passed in
     */
    private Body orbits;

    /**
     * radius of body (m)
     * all bodies are assumed perfectly round at all times untill a later date.
     * 5/4/26
     */
    private Double radius;

    /**
     * density (Mass / pi(radius)^2)
     */
    private Double density;

    /**
     * This is the lowest point in orbit (m, optional)
     * WARNING: if this is less than radius of the body being orbited, then a
     * collision will occur.
     */
    private Double perigee;

    /**
     * This is the highest point in orbit (m, optional)
     * if no perigee is set, orbit will be perfectly round at altitude [apogee].
     * WARNING: if this is less than radius of the body being orbited, then a
     * collision will occur.
     */
    private Double apogee;

    /**
     * eccentricity (internally written to value)
     */
    private Double eccentricity;

    /**
     * Altitude above nearest body (m, can replace perigee on initialization)
     */
    private Double altitude;

    // CONSTRUCTION

    /**
     * Free-floating body with full motion vector
     * Use when the body is not in orbit and motion is known
     * 
     * @param mass    the mass of the body in kg
     * @param radius  the radius of the body in meters
     * @param orbits  the body that this body orbits
     * @param perigee the lowest point in orbit in meters
     * @param apogee  the highest point in orbit in meters
     */
    public Body(String name, Double mass, Double radius, Body orbits, Double perigee, Double apogee) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.orbits = orbits;
        this.perigee = perigee;
        this.apogee = apogee;
        this.density = calcDensity(mass, radius);
        calcOrbitalVelocity(); // derive vx/vy/vz immediately on construction
    }

    /**
     * Free-floating body, motion unknown or to be calculated later
     */
    public Body(String name, Double mass, Double radius) {
        this(name, mass, radius, null, null, null);
    }

    /**
     * Orbiting body with circular orbit, orbital parameters derived from altitude
     * Velocity and angles can be derived from orbital parameters
     * 
     * @param mass     the mass of the body in kg
     * @param radius   the radius of the body in meters
     * @param orbits   the body that this body orbits
     * @param altitude the altitude above the orbited body in meters
     */
    public Body(String name, Double mass, Double radius, Body orbits, Double altitude) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.orbits = orbits;
        this.altitude = altitude;
        this.perigee = altitude;
        this.apogee = altitude;
        this.density = calcDensity(mass, radius);
        calcOrbitalVelocity();
    }

    /**
     * Orbiting body, orbital parameters unknown or to be set later
     * 
     * @param mass   the mass of the body in kg
     * @param radius the radius of the body in meters
     * @param orbits the body that this body orbits
     */
    public Body(String name, Double mass, Double radius, Body orbits) {
        this.name = name;
        this.mass = mass;
        this.radius = radius;
        this.orbits = orbits;
        this.density = calcDensity(mass, radius);
    }

    // GETTERS

    /**
     * Gets the body's name
     * 
     * @return the name of the star
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the x velocity components in m/s
     */
    public double getVelocityX() {
        return vx;
    }

    /**
     * Returns the y velocity components in m/s
     */
    public double getVelocityY() {
        return vy;
    }

    /**
     * Returns the z velocity components in m/s
     */
    public double getVelocityZ() {
        return vz;
    }

    /**
     * Returns the acceleration in the X direction.
     */
    public double getAccX() {
        return ax;
    }

    /**
     * Returns the acceleration in the Y direction.
     */
    public double getAccY() {
        return ay;
    }

    /**
     * Returns the acceleration in the Z direction.
     */
    public double getAccZ() {
        return az;
    }

    /**
     * Returns the body that this body orbits.
     */
    public Body getOrbits() {
        return orbits;
    }

    /**
     * Returns the mass of the body.
     */
    public Double getMass() {
        return mass;
    }

    /**
     * Returns the radius of the body.
     */
    public Double getRadius() {
        return radius;
    }

    /**
     * Returns the density of the body.
     */
    public Double getDensity() {
        return density;
    }

    /**
     * Returns the perigee of the orbit.
     */
    public Double getPerigee() {
        return perigee;
    }

    /**
     * Returns the apogee of the orbit.
     */
    public Double getApogee() {
        return apogee;
    }

    /**
     * Returns the altitude of the body.
     */
    public Double getAltitude() {
        return altitude;
    }

    /**
     * Returns the X position of the body.
     */
    public Double getX() {
        return x;
    }

    /**
     * Returns the Y position of the body.
     */
    public Double getY() {
        return y;
    }

    /**
     * Returns the Z position of the body.
     */
    public Double getZ() {
        return z;
    }

    // SETTERS

    /**
     * Sets the body's name
     * 
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the velocity in the X direction.
     */
    public void setVelocityX(double vx) {
        this.vx = vx;
    }

    /**
     * Sets the velocity in the Y direction.
     */
    public void setVelocityY(double vy) {
        this.vy = vy;
    }

    /**
     * Sets the velocity in the Z direction.
     */
    public void setVelocityZ(double vz) {
        this.vz = vz;
    }

    /**
     * Returns the previous acceleration in the X direction.
     */
    public double getAccPrevX() {
        return axPrev;
    }

    /**
     * Returns the previous acceleration in the Y direction.
     */
    public double getAccPrevY() {
        return ayPrev;
    }

    /**
     * Returns the previous acceleration in the Z direction.
     */
    public double getAccPrevZ() {
        return azPrev;
    }

    /**
     * Sets the X position of the body.
     */
    public void setX(Double x) {
        this.x = x;
    }

    /**
     * Sets the Y position of the body.
     */
    public void setY(Double y) {
        this.y = y;
    }

    /**
     * Sets the Z position of the body.
     */
    public void setZ(Double z) {
        this.z = z;
    }

    /**
     * Sets the body that this body orbits.
     */
    public void setOrbits(Body orbits) {
        this.orbits = orbits;
        calcOrbitalVelocity(); // recalculate when orbit changes
    }

    /**
     * Recalculates density when mass changes
     */
    public void setMass(Double mass) {
        this.mass = mass;
        this.density = calcDensity(mass, this.radius);
    }

    /**
     * Recalculates density when radius changes
     */
    public void setRadius(Double radius) {
        this.radius = radius;
        this.density = calcDensity(this.mass, radius);
    }

    /**
     * Recalculates orbital velocity when perigee changes
     */
    public void setPerigee(Double perigee) {
        this.perigee = perigee;
        calcOrbitalVelocity();
    }

    /**
     * Recalculates orbital velocity when apogee changes
     */
    public void setApogee(Double apogee) {
        this.apogee = apogee;
        calcOrbitalVelocity();
    }

    // HELPERS

    /**
     * Saves the current acceleration values to previous acceleration values.
     */
    public void saveAcc() {
        axPrev = ax;
        ayPrev = ay;
        azPrev = az;
    }

    /**
     * Resets the acceleration components to zero.
     */
    public void zeroAcc() {
        ax = 0;
        ay = 0;
        az = 0;
    }

    /**
     * Adds the given acceleration components to the current acceleration.
     * 
     * @param dax the change in x-acceleration
     * @param day the change in y-acceleration
     * @param daz the change in z-acceleration
     */
    public void addAcc(double dax, double day, double daz) {
        ax += dax;
        ay += day;
        az += daz;
    }

    /**
     * Calculates the density of the body based on its mass and radius
     * 
     * @param mass   the mass of the body
     * @param radius the radius of the body
     * @return the density of the body
     */
    private Double calcDensity(Double mass, Double radius) {
        return mass / (Math.PI * Math.pow(radius, 2));
    }

    /**
     * Calculates orbital velocity vector at perigee and sets vx, vy, vz.
     * At perigee, velocity is purely perpendicular to radius.
     * Uses vis-viva: v = sqrt(μ * (2/r - 1/a))
     */
    private void calcOrbitalVelocity() {
        if (orbits == null || !(orbits instanceof Body))
            return;
        if (perigee == null || apogee == null)
            return;

        Body parent = (Body) orbits;
        final double G = 6.674e-11;
        double mu = G * parent.getMass();

        double a = (perigee + apogee) / 2.0;
        double r = perigee;

        double speed;
        if (Math.abs(apogee - perigee) < 1e-9) {
            speed = Math.sqrt(mu / r);
        } else {
            speed = Math.sqrt(mu * (2.0 / r - 1.0 / a));
        }

        // Place child at a random angle around parent
        double positionAngle = Math.random() * 2.0 * Math.PI;
        x = parent.getX() + r * Math.cos(positionAngle);
        y = parent.getY() + r * Math.sin(positionAngle);
        z = parent.getZ();

        // ALWAYS orbit counter-clockwise (prograde) so moons stay bound.
        // Random direction caused moons to escape in some N-body configurations.
        double tx = -Math.sin(positionAngle);
        double ty =  Math.cos(positionAngle);

        // Inherit parent's velocity so the moon orbits the parent, not the origin.
        // Without this, the moon's absolute velocity doesn't account for the parent
        // already moving through space, causing it to fall toward Sol.
        vx = parent.getVelocityX() + speed * tx;
        vy = parent.getVelocityY() + speed * ty;
        vz = parent.getVelocityZ();
    }
}
