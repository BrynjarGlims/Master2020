package Individual;

import java.util.Objects;

public class SimpleArc {

    private int fromLocation;
    private int toLocation;

    public SimpleArc(int fromLocation, int toLocation){

        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;
        if (!(o instanceof Arc)) {
            return false;
        }
        SimpleArc arc = (SimpleArc) o;
        return fromLocation == arc.fromLocation &&
                toLocation == arc.toLocation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromLocation, toLocation);
    }
}
