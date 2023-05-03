package info.deconinck.inclinometer.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Direction {
    private static final double LONGITUDE_CHANGE_THRESHOLD = 0.001f;
    private static final double LATITUDE_CHANGE_THRESHOLD = 0.001f;
    private static final double ROLL_CHANGE_THRESHOLD = 0.1f;
    private static final double TILT_CHANGE_THRESHOLD = 0.1f;

    @PrimaryKey(autoGenerate = true)
    public int id;

    public OffsetDateTime time = OffsetDateTime.now();

    @ColumnInfo(name = "session_id")
    public long sessionId;

    public double longitude;

    public double latitude;

    public double roll;

    public double tilt;

    public Direction(Long sessionId, double longitude, double latitude, double roll, double tilt) {
        this.sessionId = sessionId;
        this.longitude = longitude;
        this.latitude = latitude;
        this.roll = roll;
        this.tilt = tilt;
    }

    public String getISOTimestamp() {
        return time.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    @Override
    public String toString() {
        return "Orientation{" +
                "id=" + id +
                ", time=" + time +
                ", sessionId=" + sessionId +
                ", longitude=" + longitude +
                ", latitude=" + latitude +
                ", roll=" + roll +
                ", tilt=" + tilt +
                '}';
    }

    public boolean differsEnoughFrom(Direction otherDirection) {
        return
                otherDirection == null
                        || Math.abs(otherDirection.longitude - this.longitude) > LONGITUDE_CHANGE_THRESHOLD
                        || Math.abs(otherDirection.latitude - this.latitude) > LATITUDE_CHANGE_THRESHOLD
                        || Math.abs(otherDirection.roll - this.roll) > ROLL_CHANGE_THRESHOLD
                        || Math.abs(otherDirection.tilt - this.tilt) > TILT_CHANGE_THRESHOLD
                ;
    }
}
