package info.deconinck.inclinometer.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Entity
public class Orientation {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public OffsetDateTime time = OffsetDateTime.now();

    @ColumnInfo(name = "session_id")
    public long sessionId;

    public float longitude;

    public float latitude;

    public float roll;

    public float tilt;

    public Orientation(Long sessionId, float roll, float tilt) {
        this.sessionId = sessionId;
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
}
