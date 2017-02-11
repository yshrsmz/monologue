package net.yslibrary.monotweety.base

import com.bluelinelabs.conductor.ControllerChangeType
import com.squareup.leakcanary.RefWatcher
import javax.inject.Inject

interface RefWatcherDelegate {
  fun handleOnDestroy()
  fun handleOnChangeEnded(isDestroyed: Boolean, changeType: ControllerChangeType)
}

class RefWatcherDelegateImpl @Inject constructor(val refWatcher: RefWatcher) : RefWatcherDelegate {

  private var hasExisted: Boolean = false

  override fun handleOnDestroy() {
    if (hasExisted) {
      refWatcher.watch(this)
    }
  }

  override fun handleOnChangeEnded(isDestroyed: Boolean, changeType: ControllerChangeType) {

    hasExisted = !changeType.isEnter
    if (isDestroyed) {
      refWatcher.watch(this)
    }
  }
}