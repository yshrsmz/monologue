package net.yslibrary.monotweety.splash

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import net.yslibrary.monotweety.R
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.base.ActionBarController
import net.yslibrary.monotweety.base.HasComponent
import net.yslibrary.monotweety.base.RefWatcherDelegate
import net.yslibrary.monotweety.login.LoginController
import net.yslibrary.monotweety.login.LoginTransitionChangeHandlerCompat
import net.yslibrary.monotweety.setting.SettingController
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.lang.kotlin.addTo
import rx.schedulers.Schedulers
import rx.subscriptions.CompositeSubscription
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.properties.Delegates


/**
 * Created by yshrsmz on 2016/09/25.
 */
class SplashController : ActionBarController(), HasComponent<SplashComponent> {

  @set:[Inject]
  var viewModel by Delegates.notNull<SplashViewModel>()

  @set:[Inject]
  var refWatcherDelegate by Delegates.notNull<RefWatcherDelegate>()

  var subscriptions: CompositeSubscription = CompositeSubscription()

  override val shouldShowActionBar: Boolean = false

  override val component: SplashComponent by lazy {
    Timber.i("create SplashComponent")
    getComponentProvider<SplashComponent.ComponentProvider>(activity!!)
        .splashComponent(SplashViewModule())
  }

  override fun onCreate() {
    super.onCreate()
    Timber.i("onCreate - SplashController")
    component.inject(this)
    analytics.viewEvent(Analytics.VIEW_SPLASH)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.controller_splash, container, false)

    subscriptions = CompositeSubscription()
    setEvents()

    return view
  }

  fun setEvents() {
    viewModel.loggedIn
        .zipWith(Observable.interval(1500, TimeUnit.MILLISECONDS).first()) { t1, t2 -> t1 }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          val next: Controller = if (it) SettingController() else LoginController()

          router.setRoot(RouterTransaction.with(next)
              .pushChangeHandler(if (it) SimpleSwapChangeHandler() else LoginTransitionChangeHandlerCompat())
              .popChangeHandler(if (it) SimpleSwapChangeHandler() else LoginTransitionChangeHandlerCompat()))
        }.addTo(subscriptions)
  }

  override fun onDestroyView(view: View) {
    super.onDestroyView(view)
    subscriptions.unsubscribe()
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