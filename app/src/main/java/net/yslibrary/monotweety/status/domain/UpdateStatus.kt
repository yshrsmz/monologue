package net.yslibrary.monotweety.status.domain

import io.reactivex.Completable
import net.yslibrary.monotweety.appdata.status.StatusRepository
import net.yslibrary.monotweety.di.UserScope
import javax.inject.Inject

@UserScope
class UpdateStatus @Inject constructor(private val statusRepository: StatusRepository) {

    fun execute(status: String, inReplyToStatusId: Long? = null): Completable {
        return statusRepository.updateStatus(status, inReplyToStatusId)
    }
}
