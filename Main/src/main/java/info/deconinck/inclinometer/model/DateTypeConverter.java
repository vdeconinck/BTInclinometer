package info.deconinck.inclinometer.model;

import androidx.room.TypeConverter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import kotlin.jvm.JvmStatic;

public class DateTypeConverter {
    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @TypeConverter
    @JvmStatic
    public OffsetDateTime toOffsetDateTime(String value) {
        if (value == null) return null;
        return formatter.parse(value, OffsetDateTime::from);
    }

    @TypeConverter
    @JvmStatic
    public String fromOffsetDateTime(OffsetDateTime date) {
        if (date == null) return null;
        return date.format(formatter);
    }
}
