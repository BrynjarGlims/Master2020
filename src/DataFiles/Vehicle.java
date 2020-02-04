package DataFiles;

public class Vehicle {

    public int vehicleNumber;
    public String vehicleName;
    public int capacity;
    public String trailerNumberPlate;
    public int costPerHour;
    public int costPerHourOvertime;
    public int costPerDay;
    public int costPerKm;

    public Vehicle(int vehicleNumber, String vehicleName, int capacity, String trailerNumberPlate, int costPerDay, int costPerKm,
                   int costPerHour, int costPerHourOvertime){
        this.vehicleNumber = vehicleNumber;
        this.vehicleName = vehicleName;
        this.capacity = capacity;
        this.trailerNumberPlate = trailerNumberPlate;
        this.costPerDay = costPerDay;
        this.costPerKm = costPerKm;
        this.costPerHour = costPerHour;
        this.costPerHourOvertime = costPerHourOvertime;
        }
    }

