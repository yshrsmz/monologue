package net.yslibrary.monotweety.data.user.local

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.yslibrary.monotweety.data.user.User
import net.yslibrary.monotweety.data.user.UserPreferences
import net.yslibrary.monotweety.di.UserScope
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

interface UserLocalGateway {
    val userFlow: Flow<User?>
    suspend fun update(user: User)
    suspend fun delete()
}

@UserScope
internal class UserLocalGatewayImpl @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
) : UserLocalGateway {
    override val userFlow: Flow<User?> = dataStore.data
        .map { it.takeUnless { it.updatedAt == 0L } }
        .catch { e ->
            if (e is IOException) {
                Timber.e(e, "Error reading UsePreferences")
                emit(null)
            } else {
                throw e
            }
        }
        .map { it.toEntity() }

    override suspend fun update(user: User) {
        dataStore.updateData { prefs ->
            prefs.toBuilder()
                .setId(user.id)
                .setName(user.name)
                .setScreenName(user.screenName)
                .setProfileImageUrl(user.profileImageUrl)
                .setUpdatedAt(user.updatedAt)
                .build()
        }
    }

    override suspend fun delete() {
        dataStore.updateData { UserPreferences.getDefaultInstance() }
    }

    private fun UserPreferences?.toEntity(): User? {
        if (this == null) return null
        return User(
            id = this.id,
            name = this.name,
            screenName = this.screenName,
            profileImageUrl = this.profileImageUrl,
            updatedAt = this.updatedAt,
        )
    }
}

internal object UserPreferencesSerializer : Serializer<UserPreferences> {
    override fun readFrom(input: InputStream): UserPreferences {
        try {
            return UserPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override fun writeTo(t: UserPreferences, output: OutputStream) {
        t.writeTo(output)
    }

    override val defaultValue: UserPreferences
        get() = UserPreferences.getDefaultInstance()
}
