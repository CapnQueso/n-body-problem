
import java.util.ArrayList;
import java.util.List;

/**
 * Multi-body simulation main simulation runner class using 3d graphics to make
 * it look pretty
 *
 * @author capnqueso
 * @date 5/4/26
 */
public class Simulation {

    /**
     * Gravitational constant in m^3 kg^-1 s^-2
     */
    private static final double G = 6.674e-11;

    /**
     * List of all bodies in the simulation
     */
    private List<Body> bodies = new ArrayList<>();

    /**
     * Time passed in the simulation (in seconds) This will be used to calculate
     * positions of bodies in orbit, and will be able to be set and got
     */
    private double time;

    private static final double AU = 1.496e11; // Astronomical unit in meters
    private static final double SOFTENING = 1e3; // meters, small softening avoids singularities

    // MAKE SURE THIS FILEPATH IS CORRECT
    private static final String DIR = "src/"; // Directory for output files

    public static void main(String[] args) throws Exception {
        SimBroadcaster bc = new SimBroadcaster("localhost", 9000);
        bc.setStepSkip(1);

        while (true) {                          // outer loop — restarts on reset
            bc.resetRequested = false;
            Simulation sim = buildSolarSystem(bc);

            final double BASE_DT = 3600.0;
            while (!bc.resetRequested) {        // inner loop — normal sim
                double mult = bc.speedMultiplier;
                double dt = BASE_DT * mult;
                int stepsPerFrame = 1;
                if (dt > 86400 * 7) {
                    stepsPerFrame = (int) Math.min(500, dt / (86400 * 7));
                    dt = BASE_DT * mult / stepsPerFrame;
                }
                for (int s = 0; s < stepsPerFrame; s++) sim.step(dt);
                bc.send(sim.getBodies(), sim.getTime());
                Thread.sleep(16);
            }
            System.out.println("[SIM] resetting...");
        }
    }

    /** Builds and returns a fresh Simulation with all bodies added. */
    private static Simulation buildSolarSystem(SimBroadcaster bc) {
        Simulation sim = new Simulation();

        Star sol = new Star("Sol", 1.0, 1.0, 1.0, 5778);
        sim.addBody(sol);

        Body mercury  = new Body("Mercury",  3.285e23, 2.440e6, sol, 0.387 * AU);
        Body venus    = new Body("Venus",    4.867e24, 6.052e6, sol, 0.723 * AU);
        Body earth    = new Body("Earth",    5.972e24, 6.371e6, sol, 1.000 * AU);
        Body mars     = new Body("Mars",     6.390e23, 3.390e6, sol, 1.524 * AU);
        Body jupiter  = new Body("Jupiter",  1.898e27, 6.991e7, sol, 5.203 * AU);
        Body saturn   = new Body("Saturn",   5.683e26, 5.823e7, sol, 9.537 * AU);
        Body uranus   = new Body("Uranus",   8.681e25, 2.536e7, sol, 19.19 * AU);
        Body neptune  = new Body("Neptune",  1.024e26, 2.462e7, sol, 30.07 * AU);

        sim.addBody(mercury); sim.addBody(venus); sim.addBody(earth);
        sim.addBody(mars);    sim.addBody(jupiter); sim.addBody(saturn);
        sim.addBody(uranus);  sim.addBody(neptune);

        // Moons
        sim.addBody(new Body("Moon",      7.342e22, 1.737e6, earth,   3.844e8));
        sim.addBody(new Body("Phobos",    1.066e16, 1.130e4, mars,    9.376e6));
        sim.addBody(new Body("Deimos",    1.476e15, 6.200e3, mars,    2.346e7));
        sim.addBody(new Body("Io",        8.932e22, 1.822e6, jupiter, 4.216e8));
        sim.addBody(new Body("Europa",    4.800e22, 1.561e6, jupiter, 6.709e8));
        sim.addBody(new Body("Ganymede",  1.482e23, 2.634e6, jupiter, 1.070e9));
        sim.addBody(new Body("Callisto",  1.076e23, 2.410e6, jupiter, 1.883e9));
        sim.addBody(new Body("Titan",     1.345e23, 2.575e6, saturn,  1.222e9));
        sim.addBody(new Body("Enceladus", 1.080e20, 2.521e5, saturn,  2.380e8));
        sim.addBody(new Body("Rhea",      2.307e21, 7.640e5, saturn,  5.270e8));

        return sim;
    }

    /**
     * Advances the simulation by one timestep using Velocity Verlet integration
     * O(n²) per step
     */
    public void step(double dt) {
        // If this is the first timestep, compute initial accelerations.
        // This makes Velocity Verlet stable for bodies already in motion.
        if (time == 0) {
            computeAccelerations();
        }

        // Update positions using current velocity and acceleration.
        for (Body b : bodies) {
            b.setX(b.getX() + b.getVelocityX() * dt + 0.5 * b.getAccX() * dt * dt);
            b.setY(b.getY() + b.getVelocityY() * dt + 0.5 * b.getAccY() * dt * dt);
            b.setZ(b.getZ() + b.getVelocityZ() * dt + 0.5 * b.getAccZ() * dt * dt);
        }

        // Compute new accelerations from updated positions.
        computeAccelerations();

        // Update velocities using average of old and new acceleration values.
        for (Body b : bodies) {
            b.setVelocityX(b.getVelocityX() + 0.5 * (b.getAccPrevX() + b.getAccX()) * dt);
            b.setVelocityY(b.getVelocityY() + 0.5 * (b.getAccPrevY() + b.getAccY()) * dt);
            b.setVelocityZ(b.getVelocityZ() + 0.5 * (b.getAccPrevZ() + b.getAccZ()) * dt);
        }

        time += dt;
    }

    private void computeAccelerations() {
        // Save old accelerations, then reset to zero before summing new contributions.
        for (Body bi : bodies) {
            bi.saveAcc();
            bi.zeroAcc();
        }

        // Newton's third law allows half the pairs to be computed.
        for (int i = 0; i < bodies.size(); i++) {
            Body bi = bodies.get(i);
            for (int j = i + 1; j < bodies.size(); j++) {
                Body bj = bodies.get(j);

                double dx = bj.getX() - bi.getX();
                double dy = bj.getY() - bi.getY();
                double dz = bj.getZ() - bi.getZ();

                double r2 = dx * dx + dy * dy + dz * dz + SOFTENING * SOFTENING;
                if (r2 == 0) {
                    continue;
                }

                double r = Math.sqrt(r2);
                double r3 = r2 * r;
                double f = G / r3;

                bi.addAcc(f * bj.getMass() * dx,
                        f * bj.getMass() * dy,
                        f * bj.getMass() * dz);

                bj.addAcc(-f * bi.getMass() * dx,
                        -f * bi.getMass() * dy,
                        -f * bi.getMass() * dz);
            }
        }
    }

    // get set
    public void addBody(Body b) {
        bodies.add(b);
    }

    public void removeBody(Body b) {
        bodies.remove(b);
    }

    public List<Body> getBodies() {
        return bodies;
    }

    public double getTime() {
        return time;
    }

    public void setTime(double time) {
        this.time = time;
    }
}
