package info.deconinck.inclinometer.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import info.deconinck.inclinometer.model.Orientation;

@Dao
public interface OrientationDao {
    @Query("SELECT * FROM orientation where id=:id")
    List<Orientation> getOrientation(int id);

    @Query("SELECT * FROM orientation")
    List<Orientation> getAllOrientations();

    @Query("SELECT * FROM orientation where session_id = :sessionId")
    List<Orientation> getOrientationsBySession(int sessionId);

    @Insert
    void insert(Orientation orientation);

    @Insert
    void insertOrientations(Orientation... orientations);

    @Delete
    void delete(Orientation orientation);
    @Query("DELETE FROM orientation WHERE session_id = :sessionId")
    void deleteOrientationsForSession(int sessionId);
}