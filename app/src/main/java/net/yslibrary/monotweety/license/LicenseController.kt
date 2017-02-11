package net.yslibrary.monotweety.license

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import net.yslibrary.licenseadapter.LicenseAdapter
import net.yslibrary.licenseadapter.LicenseEntry
import net.yslibrary.monotweety.App
import net.yslibrary.monotweety.R
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.base.ActionBarController
import net.yslibrary.monotweety.base.RefWatcherDelegate
import net.yslibrary.monotweety.base.findById
import rx.android.schedulers.AndroidSchedulers
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by yshrsmz on 2016/10/10.
 */
class LicenseController : ActionBarController() {

  override val hasBackButton: Boolean = true

  lateinit var bindings: Bindings

  @set:[Inject]
  var viewModel by Delegates.notNull<LicenseViewModel>()

  @set:[Inject]
  var refWatcherDelegate by Delegates.notNull<RefWatcherDelegate>()

  override val title: String?
    get() = getString(R.string.title_license)

  val component: LicenseComponent by lazy {
    val activityBus = getComponentProvider<LicenseViewModule.DependencyProvider>(activity!!).activityBus()
    DaggerLicenseComponent.builder()
        .userComponent(App.userComponent(applicationContext!!))
        .licenseViewModule(LicenseViewModule(activityBus))
        .build()
  }

  override fun onCreate() {
    super.onCreate()
    component.inject(this)
    analytics.viewEvent(Analytics.VIEW_LICENSE)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.controller_license, container, false)

    bindings = Bindings(view)

    setEvents()

    return view
  }

  override fun onChangeEnded(changeHandler: ControllerChangeHandler, changeType: ControllerChangeType) {
    super.onChangeEnded(changeHandler, changeType)
    refWatcherDelegate.handleOnChangeEnded(isDestroyed, changeType)
  }

  override fun onDestroy() {
    super.onDestroy()
    refWatcherDelegate.handleOnDestroy()
  }

  fun setEvents() {
    viewModel.licenses
        .bindToLifecycle()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { initAdapter(it) }

  }

  fun initAdapter(dataSet: List<LicenseEntry>) {
    bindings.list.layoutManager = LinearLayoutManager(activity)
    bindings.list.adapter = LicenseAdapter(dataSet)
  }

  class Bindings(view: View) {
    val list = view.findById<RecyclerView>(R.id.list)
  }
}