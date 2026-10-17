package com.openjarvis.automation

import android.content.Context
import androidx.room.*
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

@Entity(tableName = "automations")
data class AutomationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val command: String,
    val scheduleType: String,
    val scheduleHour: Int = 0,
    val scheduleMinute: Int = 0,
    val scheduleDayOfWeek: Int = 0,
    val scheduleIntervalMs: Long = 0,
    val enabled: Boolean = true,
    val lastRun: Long? = null,
    val lastResult: String? = null,
    val runCount: Int = 0
)

@Dao
interface AutomationDao {
    @Query("SELECT * FROM automations ORDER BY name")
    suspend fun getAll(): List<AutomationEntity>

    @Query("SELECT * FROM automations WHERE id = :id")
    suspend fun getById(id: String): AutomationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(automation: AutomationEntity)

    @Update
    suspend fun update(automation: AutomationEntity)

    @Query("DELETE FROM automations WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [AutomationEntity::class], version = 1)
abstract class AutomationDB : RoomDatabase() {
    abstract fun automationDao(): AutomationDao

    companion object {
        @Volatile private var INSTANCE: AutomationDB? = null

        fun getInstance(context: Context): AutomationDB {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AutomationDB::class.java,
                    "automations.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

class AutomationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val id = inputData.getString("automation_id") ?: return Result.failure()
        val command = inputData.getString("automation_command") ?: return Result.failure()

        return try {
            val db = AutomationDB.getInstance(applicationContext)
            val dao = db.automationDao()

            val automation = dao.getById(id) ?: return Result.failure()

            kotlinx.coroutines.delay(2000)

            val updated = automation.copy(
                lastRun = System.currentTimeMillis(),
                lastResult = "success",
                runCount = automation.runCount + 1
            )
            dao.update(updated)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

// --- Entity <-> domain model mapping ---

fun AutomationEntity.toModel(): AutomationManager.Automation {
    val schedule = when (scheduleType.lowercase()) {
        "daily" -> AutomationManager.AutomationSchedule.Daily(scheduleHour, scheduleMinute)
        "weekly" -> AutomationManager.AutomationSchedule.Weekly(scheduleDayOfWeek, scheduleHour, scheduleMinute)
        "interval" -> AutomationManager.AutomationSchedule.Interval(scheduleIntervalMs)
        "once" -> AutomationManager.AutomationSchedule.Once(scheduleIntervalMs)
        else -> AutomationManager.AutomationSchedule.Daily(scheduleHour, scheduleMinute)
    }
    return AutomationManager.Automation(
        id = id,
        name = name,
        command = command,
        schedule = schedule,
        enabled = enabled,
        lastRun = lastRun,
        lastResult = lastResult,
        runCount = runCount
    )
}

fun AutomationManager.Automation.toEntity(): AutomationEntity {
    val s = schedule
    return AutomationEntity(
        id = id,
        name = name,
        command = command,
        scheduleType = when (s) {
            is AutomationManager.AutomationSchedule.Daily -> "daily"
            is AutomationManager.AutomationSchedule.Weekly -> "weekly"
            is AutomationManager.AutomationSchedule.Interval -> "interval"
            is AutomationManager.AutomationSchedule.Once -> "once"
        },
        scheduleHour = when (s) {
            is AutomationManager.AutomationSchedule.Daily -> s.hour
            is AutomationManager.AutomationSchedule.Weekly -> s.hour
            else -> 0
        },
        scheduleMinute = when (s) {
            is AutomationManager.AutomationSchedule.Daily -> s.minute
            is AutomationManager.AutomationSchedule.Weekly -> s.minute
            else -> 0
        },
        scheduleDayOfWeek = if (s is AutomationManager.AutomationSchedule.Weekly) s.dayOfWeek else 0,
        scheduleIntervalMs = when (s) {
            is AutomationManager.AutomationSchedule.Interval -> s.intervalMs
            is AutomationManager.AutomationSchedule.Once -> s.atMs
            else -> 0L
        },
        enabled = enabled,
        lastRun = lastRun,
        lastResult = lastResult,
        runCount = runCount
    )
}
