package net.yslibrary.monotweety.changelog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import net.yslibrary.monotweety.App
import net.yslibrary.monotweety.R
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.base.ActionBarController
import net.yslibrary.monotweety.base.RefWatcherDelegate
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by yshrsmz on 2016/10/11.
 */
class ChangelogController() : ActionBarController() {

  @set:[Inject]
  var refWatcherDelegate by Delegates.notNull<RefWatcherDelegate>()

  override val hasBackButton: Boolean = true

  override val title: String?
    get() = applicationContext?.getString(R.string.title_changelog)

  val component: ChangelogComponent by lazy {
    val activityBus = getComponentProvider<ChangelogViewModule.DependencyProvider>(activity!!).activityBus()
    DaggerChangelogComponent.builder()
        .userComponent(App.userComponent(applicationContext!!))
        .changelogViewModule(ChangelogViewModule(activityBus))
        .build()
  }

  override fun onCreate() {
    super.onCreate()
    component.inject(this)
    analytics.viewEvent(Analytics.VIEW_CHANGELOG)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    return inflater.inflate(R.layout.controller_changelog, container, false)
  }

  override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
    super.onChangeEnded(changeHandler, changeType)
    refWatcherDelegate.handleOnChangeEnded(isDestroyed, changeType)
  }

  override fun onDestroy() {
    super.onDestroy()
    refWatcherDelegate.handleOnDestroy()
  }
}