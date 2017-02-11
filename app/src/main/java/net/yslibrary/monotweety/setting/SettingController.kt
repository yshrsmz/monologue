package net.yslibrary.monotweety.setting

import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.support.v7.widget.SwitchCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.HorizontalChangeHandler
import com.jakewharton.rxbinding.widget.checkedChanges
import net.yslibrary.monotweety.App
import net.yslibrary.monotweety.Navigator
import net.yslibrary.monotweety.R
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.base.ActionBarController
import net.yslibrary.monotweety.base.HasComponent
import net.yslibrary.monotweety.base.RefWatcherDelegate
import net.yslibrary.monotweety.base.findById
import net.yslibrary.monotweety.changelog.ChangelogController
import net.yslibrary.monotweety.data.appinfo.AppInfo
import net.yslibrary.monotweety.license.LicenseController
import net.yslibrary.monotweety.setting.adapter.SettingAdapter
import net.yslibrary.monotweety.setting.adapter.SubHeaderDividerDecoration
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by yshrsmz on 2016/09/24.
 */
class SettingController : ActionBarController(), HasComponent<SettingComponent> {

  @set:[Inject]
  var navigator by Delegates.notNull<Navigator>()

  @set:[Inject]
  var viewModel by Delegates.notNull<SettingViewModel>()

  @set:[Inject]
  var refWatcherDelegate by Delegates.notNull<RefWatcherDelegate>()

  lateinit var bindings: Bindings

  val settingAdapter by lazy { SettingAdapter(applicationContext!!.resources, adapterListener) }

  val adapterListener = object : SettingAdapter.Listener {

    override fun onAppVersionClick() {
      viewModel.onChangelogRequested()
    }

    override fun onDeveloperClick() {
      viewModel.onDeveloperRequested()
    }

    override fun onShareClick() {
      viewModel.onShareRequested()
    }

    override fun onGooglePlayClick() {
      viewModel.onGooglePlayRequested()
    }

    override fun onGitHubClick() {
      viewModel.onGitHubRequested()
    }

    override fun onLicenseClick() {
      viewModel.onLicenseRequested()
    }

    override fun onKeepOpenClick(enabled: Boolean) {
      viewModel.onKeepOpenChanged(enabled)
    }

    override fun onFooterStateChanged(enabled: Boolean, text: String) {
      viewModel.onFooterStateChanged(enabled, text)
    }

    override fun onTimelineAppChanged(selectedApp: AppInfo) {
      viewModel.onTimelineAppChanged(selectedApp)
    }

    override fun onLogoutClick() {
      activity?.let {
        AlertDialog.Builder(it)
            .setTitle(R.string.label_confirm)
            .setMessage(R.string.label_logout_confirm)
            .setCancelable(true)
            .setPositiveButton(R.string.label_logout,
                { dialog, which ->
                  viewModel.onLogoutRequested()
                  dialog.dismiss()
                })
            .setNegativeButton(R.string.label_no,
                { dialog, which ->
                  dialog.cancel()
                })
            .show()
      }
    }

    override fun onOpenProfileClick() {
      viewModel.onOpenProfileRequested()
    }
  }

  override val title: String?
    get() = getString(R.string.title_setting)

  override val component: SettingComponent by lazy {
    val provider = getComponentProvider<SettingViewModule.DependencyProvider>(activity!!)
    val activityBus = provider.activityBus()
    val navigator = provider.navigator()
    DaggerSettingComponent.builder()
        .userComponent(App.userComponent(applicationContext!!))
        .settingViewModule(SettingViewModule(activityBus, navigator))
        .build()
  }

  override fun onCreate() {
    super.onCreate()
    component.inject(this)
    analytics.viewEvent(Analytics.VIEW_SETTING)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.controller_setting, container, false)

    bindings = Bindings(view)

    bindings.list.apply {
      if (itemAnimator is SimpleItemAnimator) {
        (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      }
      layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
      setHasFixedSize(true)
      addItemDecoration(SubHeaderDividerDecoration(activity!!))
      adapter = settingAdapter
    }

    setEvents()

    return view
  }

  fun setEvents() {
    // make sure to get saved status before subscribes to view events
    viewModel.notificationEnabledChanged
        .bindToLifecycle()
        .doOnNext {
          val res = if (it) R.string.label_on else R.string.label_off
          bindings.notificationSwitch.text = getString(R.string.label_notificatoin_state, getString(res))
          bindings.notificationSwitch.isChecked = it
        }
        .subscribe {
          Timber.d("notification enabled: $it")
          if (it) startNotificationService() else stopNotificationService()
        }

    viewModel.keepOpen
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { settingAdapter.updateKeepOpen(it) }

    viewModel.footerState
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { settingAdapter.updateFooterState(it.enabled, it.text) }

    viewModel.selectedTimelineApp
        .switchMap { app ->
          viewModel.installedSupportedApps
              .map { Pair(app, it) }.toObservable()
        }
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { settingAdapter.updateTimelineApp(it.first, it.second) }

    viewModel.user
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          Timber.d("user: $it")
          it?.let { settingAdapter.updateProfile(it) }
        }

    viewModel.openProfileRequests
        .bindToLifecycle()
        .subscribe { navigator.openProfileWithTwitterApp(it) }

    viewModel.logoutRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { logout() }

    viewModel.licenseRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { showLicense() }

    viewModel.developerRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { navigator.openExternalAppWithUrl(it) }

    viewModel.shareRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { navigator.openExternalAppWithShareIntent(getString(R.string.message_share, it)) }

    viewModel.googlePlayRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { navigator.openExternalAppWithUrl(it) }

    viewModel.githubRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { navigator.openExternalAppWithUrl(it) }

    viewModel.changelogRequests
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { showChangelog() }

    bindings.notificationSwitch
        .checkedChanges()
        .bindToLifecycle()
        .subscribe { viewModel.onNotificationEnabledChanged(it) }
  }

  fun startNotificationService() {
    navigator.startNotificationService()
  }

  fun stopNotificationService() {
    navigator.stopNotificationService()
  }

  fun logout() {
    navigator.startLogoutService()
    activity?.finish()
  }

  fun showLicense() {
    router.pushController(RouterTransaction.with(LicenseController())
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  fun showChangelog() {
    router.pushController(RouterTransaction.with(ChangelogController())
        .pushChangeHandler(HorizontalChangeHandler())
        .popChangeHandler(HorizontalChangeHandler()))
  }

  override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
    super.onChangeEnded(changeHandler, changeType)
    refWatcherDelegate.handleOnChangeEnded(isDestroyed, changeType)
  }

  override fun onDestroy() {
    super.onDestroy()
    refWatcherDelegate.handleOnDestroy()
  }

  inner class Bindings(view: View) {
    val notificationSwitch = view.findById<SwitchCompat>(R.id.notification_switch)
    val list = view.findById<RecyclerView>(R.id.list)
  }
}