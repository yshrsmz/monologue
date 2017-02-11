package net.yslibrary.monotweety.base

import android.support.annotation.StringRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bluelinelabs.conductor.rxlifecycle.RxController
import net.yslibrary.monotweety.analytics.Analytics
import net.yslibrary.monotweety.base.di.Names
import net.yslibrary.rxeventbus.EventBus
import rx.Completable
import rx.Observable
import rx.Single
import javax.inject.Inject
import javax.inject.Named
import kotlin.properties.Delegates

/**
 * Created by yshrsmz on 2016/09/24.
 */
abstract class BaseController() : RxController() {

  @set:Inject
  @setparam:Named(Names.FOR_ACTIVITY)
  var activityBus by Delegates.notNull<EventBus>()

  @set:Inject
  var analytics by Delegates.notNull<Analytics>()

  private var created: Boolean = false

  @Suppress("UNCHECKED_CAST")
  fun <PROVIDER> getComponentProvider(parent: Any): PROVIDER {
    return ((parent as HasComponent<Any>).component as PROVIDER)
  }

  override final fun onCreateView(inflater: LayoutInflater, container: ViewGroup): View {
    if (!created) {
      created = true
      onCreate()
    }

    return inflateView(inflater, container)
  }

  open fun onCreate() {

  }

  abstract fun inflateView(inflater: LayoutInflater, container: ViewGroup): View

  fun <T> Observable<T>.bindToLifecycle(): Observable<T> = this.compose(this@BaseController.bindToLifecycle<T>())

  fun <T> Single<T>.bindToLifecycle(): Single<T> = this.compose(this@BaseController.bindToLifecycle<T>().forSingle<T>())

  fun <T> Completable.bindToLifecycle(): Completable = this.compose(this@BaseController.bindToLifecycle<T>().forCompletable())

  fun showSnackBar(message: String) = (activity as BaseActivity).showSnackBar(message)

  fun toast(message: String): Toast? = applicationContext?.let { Toast.makeText(it, message, Toast.LENGTH_SHORT) }

  fun toastLong(message: String): Toast? = applicationContext?.let { Toast.makeText(it, message, Toast.LENGTH_LONG) }

  fun getString(@StringRes id: Int): String = applicationContext?.getString(id) ?: ""

  fun getString(@StringRes id: Int, vararg formatArgs: Any): String = applicationContext?.getString(id, *formatArgs) ?: ""
}