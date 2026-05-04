/**
 * Multi-body simulation base / helper class
 * using 3d graphics to make it look pretty
 * @author capnqueso
 * @date 5/4/26
 */
public class Body{

    /**
     * Velocity of the body
     * Measured in m/s
     */
    private Double velocity;

    /**
     * positions in the simulation, in meters from origin (arbratrary)
     * (these will change automatically, but will be able to be set and got)
     */
    private double x;  
    private double y;
    private double z;

    /**
     * Angle of motion (vertical)
     * Must be a value between 0-360
     * If not a value in that range it will be divided by 360 before getting passed out of class
     */ 
    private Double angleY;

    /**
     * Angle of motion (horizontal) 
     * Must be a value between 0-360
     * If not a value in that range it will be divided by 360 before getting passed out of class
     */
    private Double angleX;

    /**
     * mass of body (kg)
     */
    private Double mass;

    /**
     * radius of body (m)
     * all bodies are assumed perfectly round at all times untill a later date. 5/4/26
     */
    private Double radius;

    /**
     * density (Mass / pi(radius)^2)
     */
    private Double density;

    /**
     * The Object that this body orbits (optional)
     */
    private Object orbits;

    /**
     * This is the lowest point in orbit (optional)
     * WARNING: if this is less than radius of the body being orbited, then a collision will occur.
     */
    private Double perigee;

    /**
     * This is the highest point in orbit (optional)
     * if no perigee is set, orbit will be perfectly round at altitude [apogee].
     * WARNING: if this is less than radius of the body being orbited, then a collision will occur.
     */
    private Double apogee;

    /**
     * Altitude above nearest body (can replace perigee on initialization)
     */
    private Double altitude;

    // CONSTRUCTION

    /**
     * Free-floating body with full motion vector
     * Use when the body is not in orbit and motion is known
     */
    public Body(Double mass, Double radius, Double velocity, Double angleX, Double angleY) {
        this.mass = mass;
        this.radius = radius;
        this.velocity = velocity;
        this.angleX = angleX;
        this.angleY = angleY;
        this.density = mass / (Math.PI * Math.pow(radius, 2));
    }
    
    /**
     * Free-floating body, motion unknown or to be calculated later
     */
    public Body(Double mass, Double radius) {
        this(mass, radius, null, null, null);
    }

    /**
     * Orbiting body with elliptical orbit (perigee + apogee)
     * Velocity and angles can be derived from orbital parameters
     */
    public Body(Double mass, Double radius, Object orbits, Double perigee, Double apogee) {
        this.mass = mass;
        this.radius = radius;
        this.orbits = orbits;
        this.perigee = perigee;
        this.apogee = apogee;
        this.density = mass / (Math.PI * Math.pow(radius, 2));
    }

    /**
     * Orbiting body with circular orbit (altitude/perigee/apogee only)
     * apogee and perigee will be equal
     */
    public Body(Double mass, Double radius, Object orbits, Double altitude) {
        this.mass = mass;
        this.radius = radius;
        this.orbits = orbits;
        this.altitude = altitude;
        this.perigee = altitude;
        this.apogee = altitude;
        this.density = mass / (Math.PI * Math.pow(radius, 2));
    }

    /**
     * Orbiting body, orbital parameters unknown or to be set later
     */
    public Body(Double mass, Double radius, Object orbits) {
        this(mass, radius, orbits, null, null);
    }

    // GETTERS
    public Double getVelocity() { return velocity; }
    public Double getAngleX()   { return angleX; }
    public Double getAngleY()   { return angleY; }
    public Double getMass()     { return mass; }
    public Double getRadius()   { return radius; }
    public Double getDensity()  { return density; }
    public Object getOrbits()   { return orbits; }
    public Double getPerigee()  { return perigee; }
    public Double getApogee()   { return apogee; }
    public Double getAltitude() { return altitude; }
    public Double X() { return x; }
    public Double Y() { return y; }
    public Double Z() { return z; }


    // SETTERS

    public void setVelocity(Double velocity) { this.velocity = velocity; }
    public void setAngleX(Double angleX)     { this.angleX = normalizeAngle(angleX); }
    public void setAngleY(Double angleY)     { this.angleY = normalizeAngle(angleY); }
    public void setOrbits(Object orbits)     { this.orbits = orbits; }

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
        this.velocity = calcOrbitalVelocity();
    }

    /**
     * Recalculates orbital velocity when apogee changes
     */
    public void setApogee(Double apogee) {
        this.apogee = apogee;
        this.velocity = calcOrbitalVelocity();
    }

}
