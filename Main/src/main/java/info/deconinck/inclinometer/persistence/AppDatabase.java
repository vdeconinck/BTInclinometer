package info.deconinck.inclinometer.persistence;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import info.deconinck.inclinometer.model.DateTypeConverter;
import info.deconinck.inclinometer.model.Direction;
import info.deconinck.inclinometer.model.Session;

@Database(entities = {Session.class, Direction.class}, version = 1)
@TypeConverters({DateTypeConverter.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract SessionDao sessionDao();
    public abstract DirectionDao directionDao();
}
