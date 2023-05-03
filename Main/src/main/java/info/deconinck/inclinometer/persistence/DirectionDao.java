package info.deconinck.inclinometer.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import info.deconinck.inclinometer.model.Direction;

@Dao
public interface DirectionDao {
    @Query("SELECT * FROM direction where id=:id")
    List<Direction> getDirection(int id);

    @Query("SELECT * FROM direction")
    List<Direction> getAllDirections();

    @Query("SELECT * FROM direction where session_id = :sessionId")
    List<Direction> getDirectionsBySession(int sessionId);

    @Insert
    void insert(Direction direction);

    @Insert
    void insertDirections(Direction... directions);

    @Delete
    void delete(Direction direction);
    @Query("DELETE FROM direction WHERE session_id = :sessionId")
    void deleteDirectionsForSession(int sessionId);
}