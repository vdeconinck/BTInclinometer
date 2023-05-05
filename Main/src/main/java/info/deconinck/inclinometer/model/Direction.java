package info.deconinck.inclinometer.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Entity
public class Direction {
    private static final double LATITUDE_CHANGE_THRESHOLD = 0.00001f; // about 1m
    private static final double LONGITUDE_CHANGE_THRESHOLD = 0.001f; // about 1m at the equator
    private static final double ROLL_CHANGE_THRESHOLD = 0.1f;
    private static final double PITCH_CHANGE_THRESHOLD = 0.1f;

    @PrimaryKey(autoGenerate = true)
    public int id;

    public OffsetDateTime time;

    @ColumnInfo(name = "session_id")
    public long sessionId;
    public double latitude;
    public double longitude;
    public double roll;
    public double pitch;

    public Direction(Long sessionId, double latitude, double longitude, double roll, double pitch) {
        time = OffsetDateTime.now();
        this.sessionId = sessionId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.roll = roll;
        this.pitch = pitch;
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
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", roll=" + roll +
                ", pitch=" + pitch +
                '}';
    }

    public boolean differsEnoughFrom(Direction otherDirection) {
        return
                otherDirection == null
                        || Math.abs(otherDirection.latitude - this.latitude) > LATITUDE_CHANGE_THRESHOLD
                        || Math.abs(otherDirection.longitude - this.longitude) > LONGITUDE_CHANGE_THRESHOLD
                        || Math.abs(otherDirection.roll - this.roll) > ROLL_CHANGE_THRESHOLD
                        || Math.abs(otherDirection.pitch - this.pitch) > PITCH_CHANGE_THRESHOLD
                ;
    }
}
