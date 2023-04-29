package info.deconinck.inclinometer.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.time.OffsetDateTime;
import java.util.List;

import info.deconinck.inclinometer.model.Session;

@Dao
public interface SessionDao {

    @Query("SELECT * FROM session where id=:id")
    List<Session> getSession(int id);

    @Query("SELECT * FROM session order by id desc")
    List<Session> getAllSessions();

    @Query("SELECT * FROM session where startTime < :threshold")
    List<Session> getSessionsOlderThan(OffsetDateTime threshold);

    @Insert
    Long insert(Session session);

    @Insert
    List<Long> insertSessions(Session... sessions);

    @Delete
    void delete(Session session);

}