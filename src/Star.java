/**
 * Multi-body simulation star class
 * using 3d graphics to make it look pretty
 * @author capnqueso
 * @date 5/4/26
 */
public class Star extends Body{
    
    /**
     * Stars Brightness
     * used for display
     */
    private Double Brightness;

    /**
     * temperture of the star (in kelvin)
     * used for display
     */
    private double temperature;

    /**
     * charachter to determine color class
     * used for display
     */
    private Char spectralClass;

    /**
     * energy output in watts
     */
    private Double luminosity;

    /**
     * 1 solar mass (for easier input)
     * 1 solar mass is equal to 1.989 * 10^30 kg
     */
    private static final double solarMass = 1.989e30;

    /**
     * 1 solar radius (for easier input)
     * 1 solar mass is equal to 6.957 * 10^8 m
     */
    private static final double solarRadius = 6.957e8;

    /**
     * Free-floating star with full motion vector
     */
    public Star(Double mass, Double radius, Double velocity, Double angleX, Double angleY,
                Double brightness, double temperature) {
        super(mass, radius, velocity, angleX, angleY);
        this.brightness = brightness;
        this.temperature = temperature;
        this.luminosity = calcLuminosity(temperature, radius);
        this.spectralClass = calcSpectralClass(temperature);
    }

    /**
     * Free-floating star, motion unknown or to be calculated later
     */
    public Star(Double mass, Double radius, Double brightness, double temperature) {
        super(mass, radius);
        this.brightness = brightness;
        this.temperature = temperature;
        this.luminosity = calcLuminosity(temperature, radius);
        this.spectralClass = calcSpectralClass(temperature);
    }

    /**
     * Orbiting star with elliptical orbit
     * (e.g. binary star systems)
     */
    public Star(Double mass, Double radius, Object orbits, Double perigee, Double apogee,
                Double brightness, double temperature) {
        super(mass, radius, orbits, perigee, apogee);
        this.brightness = brightness;
        this.temperature = temperature;
        this.luminosity = calcLuminosity(temperature, radius);
        this.spectralClass = calcSpectralClass(temperature);
    }

    /**
     * Orbiting star with circular orbit
     */
    public Star(Double mass, Double radius, Object orbits, Double altitude,
                Double brightness, double temperature) {
        super(mass, radius, orbits, altitude);
        this.brightness = brightness;
        this.temperature = temperature;
        this.luminosity = calcLuminosity(temperature, radius);
        this.spectralClass = calcSpectralClass(temperature);
    }

    /**
     * Calculates luminosity using Stefan-Boltzmann law
     * L = 4 * pi * r^2 * sigma * T^4
     */
    private double calcLuminosity(double temperature, double radius) {
        final double stefanBoltzmann = 5.670374419e-8;
        return 4 * Math.PI * Math.pow(radius, 2) * stefanBoltzmann * Math.pow(temperature, 4);
    }

    /**
     * Approximates spectral class from temperature
     */
    private char calcSpectralClass(double temperature) {
        if (temperature >= 30000) return 'O';
        if (temperature >= 10000) return 'B';
        if (temperature >= 7500)  return 'A';
        if (temperature >= 6000)  return 'F';
        if (temperature >= 5200)  return 'G';
        if (temperature >= 3700)  return 'K';
        return 'M';
    }

}
