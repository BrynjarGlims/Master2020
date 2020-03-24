package Individual;

import java.util.Objects;

public class Arc {
    private int period;
    private int vehicleTypeID;
    private int fromLocation;
    private int toLocation;

    public Arc(int period, int vehicleTypeID, int fromLocation, int toLocation){
        this.period = period;
        this.vehicleTypeID = vehicleTypeID;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Arc)) {
            return false;
        }
        Arc arc = (Arc) o;
        return period == arc.period &&
                vehicleTypeID == arc.vehicleTypeID &&
                fromLocation == arc.fromLocation &&
                toLocation == arc.toLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(period, vehicleTypeID, fromLocation, toLocation);
    }
}
