package info.deconinck.inclinometer.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import info.deconinck.inclinometer.model.DateTypeConverter;
import info.deconinck.inclinometer.model.Orientation;
import info.deconinck.inclinometer.model.Session;

@Database(entities = {Session.class, Orientation.class}, version = 1)
@TypeConverters({DateTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract OrientationDao orientationDao();
}
