package net.yslibrary.monotweety.login

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import com.twitter.sdk.android.core.identity.TwitterLoginButton
import net.yslibrary.monotweety.R
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.base.ActionBarController
import net.yslibrary.monotweety.base.HasComponent
import net.yslibrary.monotweety.base.RefWatcherDelegate
import net.yslibrary.monotweety.base.findById
import net.yslibrary.monotweety.event.ActivityResult
import net.yslibrary.monotweety.setting.SettingController
import rx.android.schedulers.AndroidSchedulers
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates

/**
 * Created by yshrsmz on 2016/09/27.
 */
class LoginController : ActionBarController(), HasComponent<LoginComponent> {

  override val shouldShowActionBar: Boolean = false

  lateinit var bindings: Bindings

  @set:[Inject]
  var viewModel by Delegates.notNull<LoginViewModel>()

  @set:[Inject]
  var refWatcherDelegate by Delegates.notNull<RefWatcherDelegate>()

  override val component: LoginComponent by lazy {
    Timber.i("create LoginComponent")
    getComponentProvider<LoginComponent.ComponentProvider>(activity!!)
        .loginComponent(LoginViewModule())
  }

  override fun onCreate() {
    super.onCreate()
    Timber.i("onCreate - LoginController")
    component.inject(this)
    analytics.viewEvent(Analytics.VIEW_LOGIN)
  }

  override fun inflateView(inflater: LayoutInflater, container: ViewGroup): View {
    val view = inflater.inflate(R.layout.controller_login, container, false)

    bindings = Bindings(view)

    setEvents()

    return view
  }

  fun setEvents() {
    Timber.i("setEvents - LoginController")
    activityBus.on(ActivityResult::class.java)
        .observeOn(AndroidSchedulers.mainThread())
        .bindToLifecycle()
        .subscribe {
          Timber.i("onActivityResult - LoginController, requestCode: ${it.requestCode}, resultCode: ${it.resultCode}, data: ${it.data}, extra: ${it.data?.extras}")
          bindings.loginButton.onActivityResult(it.requestCode, it.resultCode, it.data)
        }

    viewModel.loginCompleted
        .observeOn(AndroidSchedulers.mainThread())
        .bindToLifecycle()
        .doOnNext { toast(getString(R.string.message_login_succeeded, it.userName))?.show() }
        .subscribe {
          analytics.loginCompleted()
          router.setRoot(RouterTransaction.with(SettingController())
              .popChangeHandler(SimpleSwapChangeHandler())
              .pushChangeHandler(SimpleSwapChangeHandler()))
        }

    viewModel.loginFailed
        .observeOn(AndroidSchedulers.mainThread())
        .bindToLifecycle()
        .subscribe { showSnackBar(getString(R.string.error_login_failed)) }

    bindings.loginButton.callback = object : Callback<TwitterSession>() {
      override fun success(result: Result<TwitterSession>) {
        Timber.d("login success: $result")
        viewModel.onLoginCompleted(result.data)
      }

      override fun failure(exception: TwitterException) {
        Timber.e(exception, exception.message)
        viewModel.onLoginFailed(exception)
      }
    }
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
    val loginButton = view.findById<TwitterLoginButton>(R.id.login)
  }
}