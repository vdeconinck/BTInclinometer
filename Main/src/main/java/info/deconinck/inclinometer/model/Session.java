package info.deconinck.inclinometer.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

@Entity
public class Session {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public OffsetDateTime startTime = OffsetDateTime.now();

    public String getPrettyTimestamp() {
        return startTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM));
    }

    @Override
    public String toString() {
        return "Session{" +
                "id=" + id +
                ", startTime=" + startTime +
                '}';
    }
}
